<div align="center">
  <img src="logo.png" width="128" alt="BaseHW Logo">
  <h1>BaseHW - Model Car Collector's Vault 🏎️</h1>
  
  <p>
    <b>Türkçe</b> • 
    <a href="README.md">English</a> • 
    <a href="README_de.md">Deutsch</a>
  </p>

  [![License: MIT](https://img.shields.io/badge/License-MIT-teal.svg?style=flat-square)](LICENSE)
  [![Android](https://img.shields.io/badge/Platform-Android_24%2B-blue?style=flat-square&logo=android)](https://developer.android.com/)
  [![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-purple?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
</div>

---

**BaseHW**, diecast model araba tutkunları için özel olarak tasarlanmış, **Jetpack Compose** ve **Clean Architecture (MVVM)** ile inşa edilmiş modern bir Android uygulamasıdır. Koleksiyonunuzu dijitalleştirmeniz, senkronize etmeniz ve yeni modeller keşfetmeniz için premium bir deneyim sunar.

## 📸 Görsel Tanıtım
<div align="center">
  <img src="homepage_v3.png" width="280"> &nbsp; &nbsp;
  <img src="search_ui_final.png" width="280">
</div>

## 🌟 Öne Çıkan Özellikler
- **🎨 Yüksek Kaliteli Tasarım:** Modern tipografi ve akıcı animasyonlarla desteklenen, doğadan ilham alan Krem/Zeytin Yeşili tasarım sistemi.
- **🔄 Bulut Senkronizasyonu:** Credential Manager üzerinden kesintisiz Google Girişi. Firebase Firestore ve Supabase ile anlık bulut yedekleme.
- **📡 Uzaktan Katalog Güncelleme:** Uygulama güncellemesine gerek duymadan araç veritabanını güncelleyin! GitHub tabanlı JSON katalogları ile gerçek zamanlı veri çekme.
- **📊 İstatistikler ve Analizler:** Koleksiyon istatistiklerinize (marka dağılımı, kutu durumu, piyasa değeri) derinlemesine bakın.
- **📋 Yönetim:** Supabase Storage üzerinden yüksek çözünürlüklü görsel destekli yerleşik "Arananlar" (Wishlist) sistemi.

## 🛠️ Teknoloji Yığını
- **Temel:** Kotlin 2.0, Jetpack Compose, Coroutines, Flow.
- **Mimari:** MVVM, Clean Architecture.
- **DI / DB:** Hilt, Room Database, Paging 3.
- **Backend:** Firebase (Auth/Firestore), Supabase (Postgres/Storage).

## 📜 Hukuki Bilgiler
- 🔒 **[Gizlilik Politikası](https://ttimocin.github.io/basehw/privacy.html)**
- 📝 **[Kullanım Koşulları](https://ttimocin.github.io/basehw/terms.html)**

---

## 🚀 Kurulum ve Yapılandırma
1. Depoyu klonlayın.
2. `google-services.json` dosyanızı `app/` dizinine ekleyin.
3. `strings.xml` dosyasındaki `default_web_client_id` değerini güncelleyin.
4. **Android Studio Meerkat** veya daha yeni bir sürümle derleyin.

---
<div align="center">
  Developed with ❤️ by <b>ttimocin</b>
</div>
