<div align="center">
  <img src="logo.png" width="128" alt="BaseHW Logo">
  <h1>BaseHW - Premium Diecast Collector's Vault 🏎️</h1>
  
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

**BaseHW** is the ultimate, state-of-the-art Android companion for diecast car enthusiasts. Meticulously built with **Jetpack Compose** and **Clean Architecture (MVVM)**, it provides an exquisite premium experience to track, sync, and discover your entire model car collection. From robust cloud syncing to advanced AI-powered text recognition, BaseHW redefines how collectors manage their passion.

## 📸 Visual Showcase
*A fully redesigned, nature-inspired user interface crafted with precision.*
<div align="center">
  <img src="homepage_v3.png" width="280"> &nbsp; &nbsp;
  <img src="search_ui_final.png" width="280">
</div>

## 🌟 Next-Generation Features

- **💡 ML Kit Smart OCR:** Effortlessly add new cars to your vault using your camera! Our AI-powered text recognition automatically identifies model names directly from the physical packaging.
- **🎨 Premium Fidelity UI/UX:** A bespoke, nature-inspired Krem/Olive Green design system emphasizing modern typography, smooth glassmorphic elements, and dynamic micro-animations for an unparalleled user experience.
- **🏎️ Ultra-Brand Support:** Not just the basics. Manage collections across top-tier brands including **Hot Wheels, Matchbox, Majorette, MiniGT, Inno64, Tarmac Works, and Kaido House**.
- **🔄 Secure Cloud Sync & Auth:** Seamless One-Tap Google Sign-In via Credential Manager. True cross-device instant cloud backup powered by Firebase Firestore and **Supabase** (Postgres/Storage).
- **📡 Over-The-Air (OTA) Catalog Sync:** Expand your model database instantly without waiting for app updates. We use GitHub-hosted JSON catalogs for real-time injections of new releases.
- **📊 Advanced Analytics:** Deep-dive into your vault with comprehensive collection statistics—visualize your brand distribution, track box condition, and monitor the total market value of your assets.
- **📋 High-Res Wishlist Management:** A native "Wanted" system seamlessly integrated with Supabase Storage, ensuring your dream cars are tracked with high-resolution imagery.

## 🛠️ Tech Stack & Architecture
BaseHW is a showcase of modern Android development standards:
- **Core Engine:** Kotlin 2.0, Jetpack Compose, Coroutines, Flow.
- **Architecture Design:** MVVM layered with strictly enforced Clean Architecture principles.
- **Local Persistence & DI:** Hilt for robust Dependency Injection, Room Database for offline caching, and Paging 3 for infinite scroll performance.
- **Backend Infrastructure:** Firebase (Auth, Firestore) combined with Supabase (Postgres, Storage) for immense scalability.
- **AI & Processing:** Google ML Kit (Text Recognition) and uCrop for image handling. Image loading optimized by Coil.

## 🚀 Installation & Setup
1. Clone the repository to your local machine: `git clone https://github.com/ttimocin/basehw.git`
2. Add your `google-services.json` securely to the `app/` directory.
3. Update `default_web_client_id` inside `strings.xml` to match your Google Cloud project structure.
4. Open and build using **Android Studio Meerkat** (or newer) to fully leverage Compose preview features.

## 📜 Legal & Resources
- 🔒 **[Privacy Policy](https://ttimocin.github.io/basehw/privacy.html)**
- 📝 **[Terms of Use](https://ttimocin.github.io/basehw/terms.html)**

---
<div align="center">
  Crafted with ❤️ for the collector community by <b>ttimocin</b>
</div>
