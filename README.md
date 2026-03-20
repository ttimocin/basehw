<div align="center">
  <img src="logo.png" width="128" alt="BaseHW Logo">
  <h1>BaseHW - Model Car Collector's Vault 🏎️</h1>
  
  <p>
    <b>English</b> • 
    <a href="README_tr.md">Türkçe</a> • 
    <a href="README_de.md">Deutsch</a>
  </p>

  [![License: MIT](https://img.shields.io/badge/License-MIT-teal.svg?style=flat-square)](LICENSE)
  [![Android](https://img.shields.io/badge/Platform-Android_24%2B-blue?style=flat-square&logo=android)](https://developer.android.com/)
  [![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-purple?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
</div>

---

**BaseHW** is a state-of-the-art Android application tailored for diecast car enthusiasts. Built with **Jetpack Compose** and **Clean Architecture (MVVM)**, it provides a premium experience to track, sync, and discover your model car collection natively.

## 📸 Visual Showcase
<div align="center">
  <img src="homepage_v3.png" width="280"> &nbsp; &nbsp;
  <img src="search_ui_final.png" width="280">
</div>

## 🌟 Key Features
- **🎨 High-Fidelity UI/UX:** Nature-inspired Krem/Olive Green design system with modern typography and smooth animations.
- **🔄 Cloud Sync & Auth:** Seamless Google Sign-In via Credential Manager. Instant cloud backup with Firebase Firestore and Supabase.
- **📡 Remote Catalog Sync:** Update your vehicle database without a Play Store update! Uses GitHub-hosted JSON catalogs for real-time model updates.
- **📊 Stats & Insights:** Deep-dive into your collection statistics (brand distribution, box condition, market value).
- **📋 Management:** Native "Wanted" (Wishlist) system with high-res image support through Supabase Storage.

## 🛠️ Tech Stack
- **Core:** Kotlin 2.0, Jetpack Compose, Coroutines, Flow.
- **Architecture:** MVVM, Clean Architecture.
- **DI / DB:** Hilt, Room Database, Paging 3.
- **Backend:** Firebase (Auth/Firestore), Supabase (Postgres/Storage).

## 📜 Legal
- 🔒 **[Privacy Policy](https://ttimocin.github.io/basehw/privacy.html)**
- 📝 **[Terms of Use](https://ttimocin.github.io/basehw/terms.html)**

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
