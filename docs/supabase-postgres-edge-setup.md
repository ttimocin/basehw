# Supabase Postgres + Edge Function Senkronizasyon Kurulumu

Bu kılavuz, katalog veritabanını Supabase üzerinden incremental (sadece değişen kayıtlar) şekilde
uygulamaya çeken bir pipeline kurar.

**Ne sağlar?**
- Marka bazlı ayrı Postgres tablolarında `updated_at` ile değişiklik takibi
- Uygulama her senkronda yalnızca son syncten sonra değişen kayıtları çeker (cursor modeli)
- Supabase yönetim panelinden veri ekle/güncelle → uygulama haftalık veya manuel sync ile otomatik alır
- Play Store güncellemesi gerekmez

---

## Adım 1 — Supabase'de proje oluştur

1. https://supabase.com adresine git ve oturum aç.
2. **New Project** butonuna bas.
3. Proje adı, şifre gir ve bölge seç.
4. Proje oluşturulduktan sonra sol menüden **SQL Editor**ı aç.

---

## Adım 2 — Marka bazlı tabloları oluştur

Aşağıdaki SQL kodunu SQL Editor'a yapıştır ve **Run** butonuna bas:

```sql
do $$
declare t text;
begin
  foreach t in array array[
    'catalog_hot_wheels',
    'catalog_matchbox',
    'catalog_mini_gt',
    'catalog_majorette',
    'catalog_jada'
  ]
  loop
    execute format('
      create table if not exists public.%I (
        id bigserial primary key,
        model_name text not null,
        series text not null default '''',
        series_num text not null default '''',
        year text,
        color text not null default '''',
        image_url text not null default '''',
        scale text not null default ''1:64'',
        toy_num text not null default '''',
        col_num text not null default '''',
        is_premium boolean not null default false,
        data_source text not null default '''',
        case_num text not null default '''',
        updated_at timestamptz not null default now()
      );
    ', t);

    execute format('create index if not exists %I on public.%I(updated_at desc);', 'idx_' || t || '_updated_at', t);
    execute format('create unique index if not exists %I on public.%I(model_name, year, data_source);', 'uq_' || t || '_identity', t);
    execute format('create unique index if not exists %I on public.%I(toy_num, data_source) where toy_num <> '''';', 'uq_' || t || '_toynum_source', t);
  end loop;
end $$;
```

---

## Adım 3 — updated_at otomatik güncellensin

Bir kayıt her değiştirildiğinde `updated_at` alanı otomatik olarak şu anki zamanı alsın.
Aşağıdaki SQL'i de aynı editörden çalıştır:

```sql
create or replace function public.touch_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

do $$
declare t text;
begin
  foreach t in array array[
    'catalog_hot_wheels',
    'catalog_matchbox',
    'catalog_mini_gt',
    'catalog_majorette',
    'catalog_jada'
  ]
  loop
    execute format('drop trigger if exists trg_%I_touch_updated_at on public.%I;', t, t);
    execute format('create trigger trg_%I_touch_updated_at before update on public.%I for each row execute function public.touch_updated_at();', t, t);
  end loop;
end $$;
```

---

## Adım 4 — Satır seviyesi güvenlik (RLS)

Tablo herkese açık okuma iznine sahip olsun, ama sadece yetkili kullanıcılar yazabilsin:

```sql
do $$
declare t text;
begin
  foreach t in array array[
    'catalog_hot_wheels',
    'catalog_matchbox',
    'catalog_mini_gt',
    'catalog_majorette',
    'catalog_jada'
  ]
  loop
    execute format('alter table public.%I enable row level security;', t);
    execute format('drop policy if exists catalog_read_all on public.%I;', t);
    execute format('create policy catalog_read_all on public.%I for select using (true);', t);
  end loop;
end $$;
```

> Tablo sadece model bilgisi içerir; kullanıcı verisi veya sır burada tutulmaz.

---

## Adım 4.1 — Eski tek tablo verisini yeni marka tablolarına taşı (tek seferlik)

Eğer geçmişte `public.catalog_models` kullandıysan, mevcut veriyi yeni tablo yapısına taşımak için şu dosyadaki SQL'i çalıştır:

`
supabase/migrations/20260315_split_catalog_models_into_brand_tables.sql
`

Bu script:
- Eski tablodan (`catalog_models`) markaya göre kayıtları ayırır.
- Yeni tablolara (`catalog_hot_wheels`, `catalog_matchbox`, `catalog_mini_gt`, `catalog_majorette`, `catalog_jada`) upsert eder.
- Aynı kaydı tekrar çalıştırdığında duplicate üretmez (idempotent).

Kontrol için örnek sayım sorgusu:

```sql
select 'legacy' as t, count(*) from public.catalog_models
union all
select 'hot_wheels', count(*) from public.catalog_hot_wheels
union all
select 'matchbox', count(*) from public.catalog_matchbox
union all
select 'mini_gt', count(*) from public.catalog_mini_gt
union all
select 'majorette', count(*) from public.catalog_majorette
union all
select 'jada', count(*) from public.catalog_jada;
```

---

## Adım 5 — Edge Function (API uç noktası) yaz

Supabase Edge Function, uygulamanın çağıracağı API görevi görür.
Yalnızca son senkronizasyondan bu yana değişen kayıtları döndürür.

Projenin kök dizininde şu klasörü ve dosyayı oluştur:

```
supabase/
  functions/
    catalog-sync/
      index.ts
```

`supabase/functions/catalog-sync/index.ts` içeriği:

```ts
import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

serve(async (req) => {
  try {
    const url = new URL(req.url);
    const since = url.searchParams.get("since");          // cursor: son sync zamanı
    const limit = Math.min(
      Number(url.searchParams.get("limit") ?? "5000"),
      10000
    );

    const sb = createClient(supabaseUrl, serviceRoleKey, {
      auth: { persistSession: false }
    });

    let query = sb
      .from("catalog_models")
      .select("brand,model_name,series,series_num,year,color,image_url,scale,toy_num,col_num,is_premium,data_source,case_num,updated_at")
      .order("updated_at", { ascending: true })
      .limit(limit);

    // since verilmişse sadece o tarihten sonraki değişiklikleri getir
    if (since) {
      query = query.gt("updated_at", since);
    }

    const { data, error } = await query;

    if (error) {
      return new Response(JSON.stringify({ error: error.message }), {
        status: 500,
        headers: { "content-type": "application/json" }
      });
    }

    // Android uygulamasının beklediği format (camelCase)
    const records = (data ?? []).map((r) => ({
      brand: r.brand,
      modelName: r.model_name,
      series: r.series,
      seriesNum: r.series_num,
      year: r.year,
      color: r.color,
      imageUrl: r.image_url,
      scale: r.scale,
      toyNum: r.toy_num,
      colNum: r.col_num,
      isPremium: r.is_premium,
      dataSource: r.data_source,
      caseNum: r.case_num
    }));

    // Yeni cursor: sonraki syncte buradan devam edilir
    const cursor = data && data.length > 0
      ? data[data.length - 1].updated_at
      : (since ?? "");

    return new Response(JSON.stringify({ cursor, records }), {
      status: 200,
      headers: { "content-type": "application/json" }
    });
  } catch (e) {
    return new Response(JSON.stringify({ error: String(e) }), {
      status: 500,
      headers: { "content-type": "application/json" }
    });
  }
});
```

---

## Adım 6 — Edge Function'ı deploy et

Bilgisayarında Supabase CLI kurulu değilse önce yükle:

```bash
npm install -g supabase
```

Ardından deploy et:

```bash
supabase login
supabase functions deploy catalog-sync --project-ref <proje-ref-id>
```

> `<proje-ref-id>` Supabase panelinde **Project Settings → General** altında yazar.

Deploy başarılı olunca function URL'in şu formatta olur:

```
https://<proje-ref-id>.functions.supabase.co/catalog-sync
```

Tarayıcıda test et:

```
https://<proje-ref-id>.functions.supabase.co/catalog-sync?limit=5
```

JSON cevabı görüyorsan çalışıyor demektir.

---

## Adım 7 — Firebase Remote Config'i güncelle

Firebase Console → Remote Config sayfasına git ve şu key-value çiftlerini ekle/güncelle:

| Key | Değer |
|---|---|
| `sync_provider` | `supabase_edge` |
| `sync_edge_url` | `https://<proje-ref-id>.functions.supabase.co/catalog-sync` |
| `sync_edge_api_key` | Supabase **anon key** (Project Settings → API) |
| `sync_base_url` | Mevcut değer olduğu gibi kalabilir (yedek) |

Değerleri kaydet ve **Publish** et.

> Önemli ayrım: `sync_edge_api_key` sadece **catalog-sync Edge Function** çağrısı içindir.
> Fotoğraf yedekleme (Supabase Storage upload/delete) için kullanılmaz.

Fotoğraf yedekleme de aktif olacaksa aşağıdaki Remote Config anahtarlarını da set edip publish et:

| Key | Değer |
|---|---|
| `photo_backup_enabled` | `true` |
| `photo_backup_supabase_url` | `https://<proje-ref-id>.supabase.co` |
| `photo_backup_api_key` | Supabase **anon/publishable key** |
| `photo_backup_bucket` | `user-car-photos` |

Not: Uygulama upload sırasında `Authorization` header'ında Firebase ID token gönderir. `photo_backup_api_key` değeri, `apikey` header'ı ve token yoksa fallback için kullanılır.

---

## Adım 8 — Veri ekle / güncelle

Artık Supabase panelinden **Table Editor** ile ilgili marka tablosuna satır eklediğinde:
- `updated_at` otomatik güncellenir.
- Uygulama bir sonraki senkronda (haftada bir veya manuel) sadece bu değişiklikleri çeker.
- Tüm tabloyu her seferinde indirmek gerekmez.

Bir örnek satır:

| brand | model_name | series | year | toy_num | data_source |
|---|---|---|---|---|---|
| HOT_WHEELS | Dodge Charger | Muscle Mania | 2026 | HW234 | hotwheels |

---

## Adım 9 — Uygulamayı test et

1. Telefonda **Profil → Senkronize Et** butonuna bas.
2. İlk senkronda tüm kayıtlar gelir (cursor boş olduğundan).
3. Supabase'de bir kayıt güncelle.
4. Tekrar **Senkronize Et** bas → sadece değişen kayıt gelir.

Logcat'te şu satırları görüyorsan senkron çalışıyor demektir:

```
RemoteYearSync: ✅ Supabase edge sync complete: records=1, changed=1, cursor=updated
```

---

## Adım 10 — Legacy tabloyu arşivle (opsiyonel, önerilir)

Eğer her şey çalışıyorsa eski tabloyu silmeden arşivleyebilirsin.
Bu adım yanlışlıkla `catalog_models` kullanımı riskini azaltır.

Çalıştırılacak dosya:

`
supabase/migrations/20260315_archive_legacy_catalog_models.sql
`

Sonuç:
- `public.catalog_models` -> `public.catalog_models_legacy_archive` olarak rename edilir.
- `anon` ve `authenticated` rollerinden erişim kaldırılır.

---

## Final Checklist

- [ ] `sync_provider = supabase_edge` publish edildi.
- [ ] `sync_edge_url` doğru function URL.
- [ ] `sync_edge_api_key` doğru Supabase anon JWT.
- [ ] 5 marka tablosu mevcut ve dolu.
- [ ] Edge Function deploy edildi.
- [ ] Telefonda Profil -> Senkronize Et başarılı.
- [ ] Supabase'de bir kayıt güncellenince incremental sync ile geliyor.

---

## Güvenlik Checklisti (Sync + Photo Backup)

Bu checklist, yetkisiz erişim riskini hızlıca kontrol etmek içindir.

- [x] `catalog-sync` endpoint'i Bearer token olmadan `401` döndürüyor.
- [x] `catalog-sync` içinde token `auth.getUser(jwt)` ile doğrulanıyor.
- [x] Storage policy path kuralı aktif: `(storage.foldername(name))[1] = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, '')`.
- [x] Storage policy fix migration uygulandı: `20260316001_storage_policy_role_fix.sql`.
- [x] Upload çalışıyor (uygulama tarafından doğrulandı).
- [x] Delete çalışıyor (uygulama tarafından doğrulandı).
- [x] `android:allowBackup="false"` aktif.
- [x] `sync_edge_api_key` yalnızca edge sync içindir; photo backup için kullanılmıyor.
- [ ] Firebase Remote Config'de `photo_backup_enabled=true` publish edildi.
- [ ] Firebase Third-party JWT (JWKS + Issuer) Supabase Dashboard'da doğru tanımlı.

Kısa not: "hiç açık yok" ifadesi pratikte garanti edilemez; ancak bu maddeler tamamlandığında bilinen kritik erişim açıkları kapanmış olur.

---

## Rollback Planı (gerekirse)

Probleme hızlı dönüş için:

1. Firebase Remote Config:
  - `sync_provider = github`
  - Publish
2. Uygulamayı kapat-aç, manuel senkronize et.
3. Supabase tarafını düzeltip tekrar `sync_provider = supabase_edge` yap.

Not: Legacy arşiv yaptıysan ve geri almak istersen:

```sql
alter table if exists public.catalog_models_legacy_archive
rename to catalog_models;
```

---

## Özet: GitHub'dan Supabase'e Geçiş

| | GitHub JSON | Supabase Edge |
|---|---|---|
| Veri yönetimi | Dosya düzenle + commit | Panel veya SQL |
| Artımlı sync | ❌ Her seferinde tüm dosya | ✅ Sadece değişenler |
| Versiyonlama | Git geçmişi | `updated_at` cursor |
| Yönetim paneli | GitHub web UI | Supabase Table Editor |
| Güvenlik | Token opsiyonel | RLS + anon key |
| Fallback | GitHub çalışıyor | `sync_provider=github` ile geri dönülür |
