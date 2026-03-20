<div align="center">
  <img src="logo.png" width="128" alt="BaseHW Logo">
  <h1>BaseHW - Model Car Collector's Vault 🏎️</h1>
  
  <p>
    <b>Deutsch</b> • 
    <a href="README.md">English</a> • 
    <a href="README_tr.md">Türkçe</a>
  </p>

  [![License: MIT](https://img.shields.io/badge/License-MIT-teal.svg?style=flat-square)](LICENSE)
  [![Android](https://img.shields.io/badge/Platform-Android_24%2B-blue?style=flat-square&logo=android)](https://developer.android.com/)
  [![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-purple?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
</div>

---

**BaseHW** ist eine hochmoderne Android-Anwendung, die speziell für Diecast-Sammler entwickelt wurde. Erstellt mit **Jetpack Compose** und **Clean Architecture (MVVM)**, bietet sie ein erstklassiges Erlebnis zur Verwaltung, Synchronisierung und Entdeckung Ihrer Modellautosammlung.

## 📸 Visuelle Präsentation
<div align="center">
  <img src="homepage_v3.png" width="280"> &nbsp; &nbsp;
  <img src="search_ui_final.png" width="280">
</div>

## 🌟 Hauptmerkmale
- **🎨 High-Fidelity UI/UX:** Von der Natur inspiriertes Designsystem in Creme/Olivgrün mit moderner Typografie und flüssigen Animationen.
- **🔄 Cloud-Sync & Auth:** Nahtlose Google-Anmeldung über den Credential Manager. Sofortiges Cloud-Backup mit Firebase Firestore und Supabase.
- **📡 Remote-Katalog-Synchronisierung:** Aktualisieren Sie Ihre Fahrzeugdatenbank ohne App-Update! Verwendet GitHub-gehostete JSON-Kataloge für Modell-Updates in Echtzeit.
- **📊 Statistiken & Einblicke:** Tauchen Sie tief in Ihre Sammlungsstatistiken ein (Markenverteilung, Boxzustand, Marktwert).
- **📋 Verwaltung:** Integriertes "Gesucht"-System (Wunschliste) mit Unterstützung für hochauflösende Bilder über Supabase Storage.

## 🛠️ Tech Stack
- **Kern:** Kotlin 2.0, Jetpack Compose, Coroutines, Flow.
- **Architektur:** MVVM, Clean Architecture.
- **DI / DB:** Hilt, Room Database, Paging 3.
- **Backend:** Firebase (Auth/Firestore), Supabase (Postgres/Storage).

## 📜 Rechtliches
- 🔒 **[Datenschutzrichtlinie](docs/privacy.html)**
- 📝 **[Nutzungsbedingungen](docs/terms.html)**

---

## 🚀 Installation & Einrichtung
1. Klonen Sie das Repository.
2. Fügen Sie Ihre `google-services.json` zum Verzeichnis `app/` hinzu.
3. Aktualisieren Sie `default_web_client_id` in `strings.xml`.
4. Erstellen Sie die App in **Android Studio Meerkat** oder neuer.

---
<div align="center">
  Developed with ❤️ by <b>ttimocin</b>
</div>
