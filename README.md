<div align="center">
  <img src="logo.png" width="128" alt="BaseHW Logo">
  <h1>BaseHW - Model Car Collector's Vault 🏎️</h1>
  
  [![License: MIT](https://img.shields.io/badge/License-MIT-teal.svg?style=flat-square)](LICENSE)
  [![Android](https://img.shields.io/badge/Platform-Android_24%2B-blue?style=flat-square&logo=android)](https://developer.android.com/)
  [![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-purple?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
  [![Architecture](https://img.shields.io/badge/Architecture-MVVM_%2B_Clean-orange?style=flat-square)](https://developer.android.com/topic/architecture)
</div>

---

### [TR] Türkçe Özet
**BaseHW**, diecast model araba koleksiyoncuları için geliştirilmiş, **Jetpack Compose** ve **Clean Architecture** ile inşa edilmiş modern bir Android uygulamasıdır. Koleksiyonunuzu dijitalleştirir, Hot Wheels, Matchbox ve MiniGT gibi markaları takip etmenizi sağlar.

### [EN] English Overview
**BaseHW** is a state-of-the-art Android application tailored for diecast car enthusiasts. Built with **Jetpack Compose** and **Clean Architecture (MVVM)**, it provides a premium experience to track, sync, and discover your model car collection natively.

### [DE] Deutsche Übersicht
**BaseHW** ist eine moderne Android-App für Diecast-Sammler. Entwickelt mit **Jetpack Compose** und **Clean Architecture**, bietet sie eine erstklassige Lösung zur Verwaltung und Synchronisierung Ihrer Modellautosammlung (Hot Wheels, Matchbox, MiniGT).

---

## 📸 Visual Showcase / Görsel Tanıtım
<div align="center">
  <img src="homepage_v3.png" width="280"> &nbsp; &nbsp;
  <img src="search_ui_final.png" width="280">
</div>

---

## 🌟 Key Features / Öne Çıkan Özellikler

- **🎨 High-Fidelity UI/UX:** Nature-inspired Krem/Olive Green design system with modern typography and smooth animations.
- **🔄 Cloud Sync & Auth:** Seamless Google Sign-In via Credential Manager. Instant cloud backup with Firebase Firestore and Supabase.
- **📡 Remote Catalog Sync:** Update your vehicle database without a Play Store update! Uses GitHub-hosted JSON catalogs for real-time model updates.
- **📊 Stats & Insights:** Deep-dive into your collection statistics (brand distribution, box condition, market value).
- **📋 Management:** Native "Wanted" (Wishlist) system with high-res image support through Supabase Storage.

---

## 🛠️ Tech Stack / Teknoloji Yığını

- **Core:** Kotlin 2.0, Jetpack Compose, Coroutines, Flow.
- **Architecture:** MVVM, Clean Architecture, Repository Pattern.
- **Dependency Injection:** Hilt (Dagger).
- **Persistence:** Room Database (Offline-first), Paging 3.
- **Backend:** Firebase (Auth & Firestore), Supabase (Postgres & Storage).
- **Networking:** Retrofit, OkHttp, Coil (Image Loading).
- **Work Management:** WorkManager for background catalog synchronization.

---

## 📜 Legal / Hukuki Bilgiler
Please review the legal documents for information regarding data usage and terms of service:

- 🔒 **[Privacy Policy / Gizlilik Politikası](docs/privacy.html)**
- 📝 **[Terms of Use / Kullanım Koşulları](docs/terms.html)**

---

## 🚀 Installation & Setup
1. Clone the repository.
2. Add your `google-services.json` to the `app/` directory.
3. Update `default_web_client_id` in `strings.xml`.
4. Build in **Android Studio Meerkat** or newer.

---
<div align="center">
  Developed with ❤️ by <b>ttimocin</b>
</div>
