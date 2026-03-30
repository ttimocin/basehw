# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- General Optimization & Obfuscation ---
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*
-renamesourcefileattribute SourceFile

# --- Firebase & Google Services ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# --- Hilt / Dependency Injection ---
-keep class dagger.hilt.** { *; }
-keep interface dagger.hilt.** { *; }
-keep class com.taytek.basehw.di.** { *; }

# --- Room Persistence ---
-keep class androidx.room.paging.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# --- Retrofit & OkHttp (Network) ---
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn okio.**
-dontwarn javax.annotation.**

# --- Gson / Serialization ---
# Keep your data models to prevent shrinking of fields used in JSON/Firestore
-keep class com.taytek.basehw.domain.model.** { *; }
-keep class com.taytek.basehw.data.local.entity.** { *; }
-keep @com.google.gson.annotations.SerializedName class * { *; }

# --- Compose & Coil ---
-keep class coil.** { *; }
-keep interface coil.** { *; }

# --- ML Kit & Camera ---
-keep class com.google.mlkit.** { *; }