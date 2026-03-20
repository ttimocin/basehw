import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const BRAND_TABLES = [
  { table: "catalog_hot_wheels", brand: "HOT_WHEELS", defaultDataSource: "hotwheels" },
  { table: "catalog_matchbox", brand: "MATCHBOX", defaultDataSource: "matchbox" },
  { table: "catalog_mini_gt", brand: "MINI_GT", defaultDataSource: "minigt" },
  { table: "catalog_majorette", brand: "MAJORETTE", defaultDataSource: "majorette" },
  { table: "catalog_jada", brand: "JADA", defaultDataSource: "jada" },
  { table: "catalog_siku", brand: "SIKU", defaultDataSource: "siku" },
  { table: "catalog_sth_th", brand: "HOT_WHEELS", defaultDataSource: "hotwheels_th_sth" }
] as const;

type BrandRow = {
  model_name: string;
  series: string;
  series_num: string;
  year: string | null;
  color: string;
  image_url: string;
  scale: string;
  toy_num: string;
  col_num: string;
  category: string | null;
  data_source: string;
  case_num: string;
  feature: string | null;
  updated_at: string;
};

serve(async (req: Request) => {
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

    const perTableResults = await Promise.all(
      BRAND_TABLES.map(async (cfg) => {
        let query = sb
          .from(cfg.table)
          .select("model_name,series,series_num,year,color,image_url,scale,toy_num,col_num,category,data_source,case_num,feature,updated_at")
          .order("updated_at", { ascending: true })
          .limit(limit);

        if (since) {
          query = query.gt("updated_at", since);
        }

        const { data, error } = await query;
        if (error) {
          throw new Error(`${cfg.table}: ${error.message}`);
        }

        const rows = (data ?? []) as BrandRow[];
        return rows.map((r) => ({
          brand: cfg.brand,
          modelName: r.model_name,
          series: r.series,
          seriesNum: r.series_num,
          year: r.year,
          color: r.color,
          imageUrl: r.image_url,
          scale: r.scale,
          toyNum: r.toy_num,
          colNum: r.col_num,
          category: r.category,
          dataSource: r.data_source || cfg.defaultDataSource,
          caseNum: r.case_num,
          feature: r.feature,
          updatedAt: r.updated_at
        }));
      })
    );

    const sorted = perTableResults
      .flat()
      .sort((a, b) => a.updatedAt.localeCompare(b.updatedAt))
      .slice(0, limit);

    // Android uygulamasının beklediği format (camelCase)
    const records = sorted.map((r) => ({
      brand: r.brand,
      modelName: r.modelName,
      series: r.series,
      seriesNum: r.seriesNum,
      year: r.year,
      color: r.color,
      imageUrl: r.imageUrl,
      scale: r.scale,
      toyNum: r.toyNum,
      colNum: r.colNum,
      category: r.category,
      dataSource: r.dataSource,
      caseNum: r.caseNum,
      feature: r.feature
    }));

    // Yeni cursor: sonraki syncte buradan devam edilir
    const cursor = sorted.length > 0
      ? sorted[sorted.length - 1].updatedAt
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
