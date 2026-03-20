# BaseHW - Model Car Collection Tracker 🏎️

![License](https://img.shields.io/badge/License-MIT-green.svg)
![Android API](https://img.shields.io/badge/API-24%2B-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)

BaseHW is a modern Android application built with **Jetpack Compose** and **Clean Architecture (MVVM)** designed for diecast car collectors. It natively tracks your collection and wishlist across major brands such as Hot Wheels, Matchbox, and MiniGT.

## 🌟 Key Features
- **Nature-Inspired High-Fidelity Design:** A stunning Krem/Olive Green UI prioritizing premium appearance with modern typography (Inter Font).
- **Google Sign-In & Firebase Cloud Sync:** Securely log in using Android's native Credential Manager and instantly backup/restore your collection to/from Firestore.
- **Remote JSON Auto-Sync Structure:** Update your app’s vehicle database remotely without requiring a Play Store update! It reads newly released cars directly from GitHub `database/` catalogs.
- **Wishlist & Statistics:** Track how many cars you possess, manage your wishlist, and get meaningful statistical insights based on brand and box status.

## 🛠️ Tech Stack & Architecture
- **Programming Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Architecture:** MVVM + Clean Architecture (Domain / Data / Presentation Layers)
- **Database (Local):** Room Database + Paging 3
- **Network / Remote Data:** Firebase Auth, Firestore, OkHttp, Retrofit
- **Dependency Injection:** Hilt
- **Background Processes:** WorkManager, Coroutines, Flow
- **Image Loading:** Coil

## 🚀 Getting Started

### Prerequisites
1. Open the project in **Android Studio Meerkat** or newer.
2. Obtain a `google-services.json` file from your Firebase Console.
3. Place the `google-services.json` file inside the `app/` directory.
   *(Note: This file is ignored by git for security purposes).*

### Configuration
Update the `default_web_client_id` inside `res/values/strings.xml` with your own Firebase/Google Cloud Platform OAuth 2.0 Client ID for Google Login.

### Remote Catalog Synchronization
The application uses `RemoteYearSyncWorker.kt` to pull new models automatically. To add new catalog models:
1. Navigate to the `database/{brand}/{year}.json` files in this repository.
2. Add new JSON entries.
3. Once pushed, the app will automatically fetch them in the background (weekly) or upon manual `Sync` from the settings.

### Supabase Incremental Sync (Optional)
For Postgres + Edge Function based incremental sync (`since` cursor model), see:

- `docs/supabase-postgres-edge-setup.md`

## 🤝 Contribution
Contributions, issues, and feature requests are welcome!

## 📜 License
Distributed under the MIT License. See `LICENSE` for more information.
