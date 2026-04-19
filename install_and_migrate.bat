@echo off
echo ========================================
echo Avatar Duzeltmesi - Kurulum ve Migration
echo ========================================
echo.

echo [1/3] Supabase migration pushlaniyor...
call npx supabase db push
if %errorlevel% neq 0 (
    echo HATA: Migration basarisiz!
    pause
    exit /b 1
)
echo Migration tamamlandi!
echo.

echo [2/3] ADB cihazlari kontrol ediliyor...
call adb devices
echo.

echo [3/3] Debug APK telefona yukleniyor...
call gradlew.bat installDebug
if %errorlevel% neq 0 (
    echo HATA: Kurulum basarisiz!
    pause
    exit /b 1
)

echo.
echo ========================================
echo BASARILI! Uygulama telefonunuza kuruldu.
echo Avatar ozelligi artik calisiyor!
echo ========================================
pause
