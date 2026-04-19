# Supabase Build Baseline (Android)

This document captures the currently working dependency/toolchain baseline for the Android app and a repeatable smoke-check flow.

## Working Baseline

- AGP: 8.13.2
- Kotlin: 2.3.10
- KSP plugin: 2.3.6
- Hilt: 2.57.2
- Room: 2.8.2
- kotlinx-serialization-json: 1.10.0
- Supabase BOM: 3.4.1
- Ktor client (OkHttp): 3.4.0
- JavaPoet buildscript classpath pin: 1.13.0

## Source of Truth Files

- gradle/libs.versions.toml
- build.gradle.kts
- app/build.gradle.kts

## Supabase Modules Enabled

- postgrest-kt
- storage-kt
- realtime-kt

## BuildConfig Keys

Defined in app module from local.properties:

- SUPABASE_URL
- SUPABASE_ANON_KEY

## Repeatable Smoke Check

Run from project root:

1. .\\gradlew :app:clean
2. .\\gradlew :app:assembleDebug --stacktrace

Expected outcome:

- BUILD SUCCESSFUL
- No hiltAggregateDepsDebug NoSuchMethodError for JavaPoet

## If Build Regresses

1. Re-check version drift in gradle/libs.versions.toml.
2. Confirm buildscript classpath includes com.squareup:javapoet:1.13.0 in root build.gradle.kts.
3. Re-run with --stacktrace and inspect first failing task (not warning lines).
4. If failure starts at hiltAggregateDepsDebug again, re-check Hilt/KSP/Kotlin alignment first.
