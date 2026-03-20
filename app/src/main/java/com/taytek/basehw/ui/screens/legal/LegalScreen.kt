package com.taytek.basehw.ui.screens.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taytek.basehw.R

enum class LegalType { PRIVACY_POLICY, TERMS_OF_USE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    type: LegalType,
    onNavigateBack: () -> Unit
) {
    val title = when (type) {
        LegalType.PRIVACY_POLICY -> stringResource(R.string.privacy_policy)
        LegalType.TERMS_OF_USE   -> stringResource(R.string.terms_of_use)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (type) {
                LegalType.PRIVACY_POLICY -> PrivacyPolicyContent()
                LegalType.TERMS_OF_USE   -> TermsOfUseContent()
            }
        }
    }
}

@Composable
private fun PrivacyPolicyContent() {
    val locale = LocalContext.current.resources.configuration.locales[0].language
    when (locale) {
        "de" -> PrivacyPolicyDE()
        "en" -> PrivacyPolicyEN()
        else -> PrivacyPolicyTR()
    }
}

@Composable
private fun PrivacyPolicyTR() {
    LegalTitle("Gizlilik Politikasi")
    LegalSubtitle("Son guncelleme: Mart 2026")
    LegalParagraph("Bu gizlilik politikasi, BaseHW uygulamasinin kisisel verilerinizi nasil topladigini, kullandigini ve korudugunu aciklamaktadir.")
    LegalSection("1. Toplanan Veriler")
    LegalParagraph("Uygulama asagidaki verileri toplayabilir:\n\n- Isteye bagli olarak yuklediginiz veya galeriden sectiginiz araba fotograflari (ImgBB'ye yuklenir)\n- Google Hesabi ile girisde: e-posta adresiniz ve profil gorseliniz (Firebase Authentication araciligiyla)\n- Koleksiyonunuza eklediginiz model arac bilgileri (marka, seri, yil, durum, notlar)\n- Cihazinizin kamerasi (yalnizca fotoğraf cekmek istediginizde kullanilir)\n- Arananlar listeniz")
    LegalSection("2. Verilerin Kullanimi")
    LegalParagraph("Toplanan veriler yalnizca su amaclarla kullanilir:\n\n- Koleksiyonunuzun bulut yedeklenmesi ve farkli cihazlar arasinda senkronizasyonu\n- Uygulama ozelliklerinin kisisellestirilmesi\n\nVerileriniz hicbir sekilde ucuncu taraflarla paylasilmaz, satilmaz veya reklam amaciyla kullanilmaz.")
    LegalSection("3. Google Firebase ve ImgBB Hizmetleri")
    LegalParagraph("Uygulama Google Firebase ve ImgBB platformlarini kullanmaktadir:\n\n- Firebase Authentication: Google ile guvenli oturum acma\n- Firebase Firestore: Koleksiyon ve arananlar verilerinizin bulut depolama. Fotoğraf linkleri burada guvenli bir sekilde saklanir.\n- ImgBB: Yuklediginiz fotograflar ImgBB veritabaninda saklanir. Fotoğraf linkleri kamuya acik yayinlanmaz ancak linkleri bilenler dosyalara erisebilir.\n\nFirebase Gizlilik Politikasi: https://firebase.google.com/support/privacy\nImgBB Gizlilik Politikasi: https://imgbb.com/privacy")
    LegalSection("4. Cihaz Izinleri ve Yerel Depolama")
    LegalParagraph("Uygulama, fotoğraflarınızı çekebilmek için Kamera (CAMERA) izni ve mevcut fotoğrafları seçebilmek için Galeri/Medya erişimi talep eder. Fotoğraflar cihazınızın dahili depolama alanında saklanır. Eğer oturum açmışsanız, bu veriler bulut yedekleme amacıyla güvenli bir şekilde bulut tabanlı sistemlerimize (Firebase ve ImgBB) senkronize edilir.")
    LegalSection("5. Veri Guvenligi")
    LegalParagraph("Verileriniz Firebase'in guvenlik altyapisiyla korunmaktadir. Buluta yuklenen tum veriler yalnizca Google hesabinizla erisilebilmektedir.")
    LegalSection("6. Veri Silme")
    LegalParagraph("Hesabinizi ve tum verilerinizi uygulama icindeki 'Profil' sayfasindan 'Hesabimi Sil' butonunu kullanarak aninda silebilirsiniz. Alternatif olarak, silme talebiniz icin taytekofficial@gmail.com adresine e-posta gonderbilirsiniz.")
    LegalSection("7. Iletisim")
    LegalParagraph("Gizlilikle ilgili sorulariniz icin:\ntaytekofficial@gmail.com")
}

@Composable
private fun PrivacyPolicyEN() {
    LegalTitle("Privacy Policy")
    LegalSubtitle("Last updated: March 2026")
    LegalParagraph("This privacy policy explains how the BaseHW application collects, uses, and protects your personal data.")
    LegalSection("1. Data We Collect")
    LegalParagraph("The app may collect the following data:\n\n- Car photos you optionally upload or select from the gallery (stored on ImgBB)\n- When signing in with Google: your email address and profile picture (via Firebase Authentication)\n- Model car information you add to your collection (brand, series, year, condition, notes)\n- Your device's camera (used only when you want to take a photo)\n- Your Wanted list")
    LegalSection("2. How We Use Your Data")
    LegalParagraph("Collected data is used only for:\n\n- Cloud backup of your collection and synchronization across devices\n- Personalizing app features\n\nYour data is never shared with, sold to, or used by third parties for advertising purposes.")
    LegalSection("3. Google Firebase and ImgBB Services")
    LegalParagraph("The app uses Google Firebase and ImgBB:\n\n- Firebase Authentication: Secure sign-in with Google\n- Firebase Firestore: Cloud storage of your collection and wanted list. Image links are securely stored here.\n- ImgBB: Photos you upload are stored in the ImgBB database. Image links are not publicly published, but those who know the links can access the files.\n\nFirebase Privacy Policy: https://firebase.google.com/support/privacy\nImgBB Privacy Policy: https://imgbb.com/privacy")
    LegalSection("4. Device Permissions and Cloud Sync")
    LegalParagraph("The app requests Camera (CAMERA) permission to take photos and Gallery/Media access to select existing photos. Photos are stored in your device's internal storage. If you are signed in, this data is securely synchronized to our cloud systems (Firebase and ImgBB) for backup purposes.")
    LegalSection("5. Data Security")
    LegalParagraph("Your data is protected by Firebase's security infrastructure. All data uploaded to the cloud is accessible only through your Google account.")
    LegalSection("6. Data Deletion")
    LegalParagraph("You can delete your account and all associated data instantly using the 'Delete Account' button on the 'Profile' page within the app. Alternatively, you can send an email to taytekofficial@gmail.com for your deletion request.")
    LegalSection("7. Contact")
    LegalParagraph("For privacy-related questions:\ntaytekofficial@gmail.com")
}

@Composable
private fun PrivacyPolicyDE() {
    LegalTitle("Datenschutzrichtlinie")
    LegalSubtitle("Letzte Aktualisierung: Maerz 2026")
    LegalParagraph("Diese Datenschutzrichtlinie erlaeutert, wie die BaseHW-App Ihre personenbezogenen Daten erfasst, verwendet und schuetzt.")
    LegalSection("1. Erfasste Daten")
    LegalParagraph("Die App kann folgende Daten erfassen:\n\n- Fotos von Autos, die Sie optional hochladen oder aus der Galerie auswählen (auf ImgBB gespeichert)\n- Bei der Google-Anmeldung: Ihre E-Mail-Adresse und Ihr Profilbild (ueber Firebase Authentication)\n- Modellauto-Informationen, die Sie Ihrer Sammlung hinzufuegen (Marke, Serie, Jahr, Zustand, Notizen)\n- Kamerazugriff Ihres Geräts (wird nur verwendet, wenn Sie ein Foto aufnehmen möchten)\n- Ihre Wunschliste")
    LegalSection("2. Verwendung Ihrer Daten")
    LegalParagraph("Die erfassten Daten werden nur verwendet fuer:\n\n- Cloud-Backup Ihrer Sammlung und geraeteuebergreifende Synchronisierung\n- Personalisierung der App-Funktionen\n\nIhre Daten werden niemals an Dritte weitergegeben, verkauft oder fuer Werbezwecke verwendet.")
    LegalSection("3. Google Firebase- und ImgBB-Dienste")
    LegalParagraph("Die App verwendet Google Firebase und ImgBB:\n\n- Firebase Authentication: sichere Anmeldung mit Google\n- Firebase Firestore: Cloud-Speicherung Ihrer Sammlung und Wunschliste. Bildlinks werden hier sicher gespeichert.\n- ImgBB: Von Ihnen hochgeladene Fotos werden in der ImgBB-Datenbank gespeichert. Bildlinks werden nicht oeffentlich veroeffentlicht, aber wer die Links kennt, kann auf die Dateien zugreifen.\n\nFirebase-Datenschutzrichtlinie: https://firebase.google.com/support/privacy\nImgBB-Datenschutzrichtlinie: https://imgbb.com/privacy")
    LegalSection("4. Gerätezugriff und Cloud-Synchronisierung")
    LegalParagraph("Die App benötigt Kamerazugriff (CAMERA), um Fotos aufzunehmen, und Galerie-/Medienzugriff, um vorhandene Fotos auszuwählen. Fotos werden im internen Speicher Ihres Geräts gespeichert. Wenn Sie angemeldet sind, werden diese Daten zu Sicherungszwecken sicher mit unseren Cloud-Systemen (Firebase und ImgBB) synchronisiert.")
    LegalSection("5. Datensicherheit")
    LegalParagraph("Ihre Daten sind durch Googles Firebase-Sicherheitsinfrastruktur geschuetzt. Alle in die Cloud hochgeladenen Daten sind nur ueber Ihr Google-Konto zugaenglich.")
    LegalSection("6. Datenloeschung")
    LegalParagraph("Sie können Ihr Konto und alle zugehörigen Daten sofort über die Schaltfläche 'Konto löschen' auf der Seite 'Profil' in der App löschen. Alternativ können Sie eine E-Mail an taytekofficial@gmail.com senden, um Ihren Löschantrag zu stellen.")
    LegalSection("7. Kontakt")
    LegalParagraph("Fuer datenschutzbezogene Fragen:\ntaytekofficial@gmail.com")
}

@Composable
private fun TermsOfUseContent() {
    val locale = LocalContext.current.resources.configuration.locales[0].language
    when (locale) {
        "de" -> TermsDE()
        "en" -> TermsEN()
        else -> TermsTR()
    }
}

@Composable
private fun TermsTR() {
    LegalTitle("Kullanim Kosullari")
    LegalSubtitle("Son guncelleme: Mart 2026")
    LegalParagraph("BaseHW uygulamasini kullanarak asagidaki kullanim kosullarini kabul etmis sayilirsiniz.")
    LegalSection("1. Uygulamanin Amaci")
    LegalParagraph("BaseHW, diecast model araba koleksiyoncilarina koleksiyonlarini yonetmleri icin tasarlanmis kisisel bir takip uygulamasidir.")
    LegalSection("2. Kullanici Sorumlulugu")
    LegalParagraph("- Hesabinizi guvende tutmaktan siz sorumlusunuz.\n- Uygulamaya yukledिginiz iceriklerden (fotograflar, notlar) siz sorumlusunuz.\n- Uygulamayi yalnizca kisisel, ticari olmayan amaclarla kullanabilirsiniz.")
    LegalSection("3. Gorsel Icerik Atfi")
    LegalParagraph("Araba gorselleri Hot Wheels Wiki, Matchbox Wiki, Mini GT Wiki ve Majorette Wiki (Fandom) kaynaklarindan alinmaktadir. Bu gorseller ilgili wiki topluluklarina aittir ve yalnizca bilgilendirme amacli kullanilmaktadir.")
    LegalSection("4. Hizmetin Kullanilabilirligi")
    LegalParagraph("Uygulama oldugu gibi sunulmaktadir. Hizmetin kesintisiz ya da hatasiz calisacagi garanti edilmez. Gelistiriciler, veri kayiplarindan sorumlu tutulamaz.")
    LegalSection("5. Degisiklikler")
    LegalParagraph("Bu kosullar onceden bildirmeksizin degistirilebilir. Uygulamayi kullanmaya devam etmek, guncel kosullari kabul etmek anlamina gelir.")
    LegalSection("6. Iletisim")
    LegalParagraph("Sorulariniz icin:\ntaytekofficial@gmail.com")
}

@Composable
private fun TermsEN() {
    LegalTitle("Terms of Use")
    LegalSubtitle("Last updated: March 2026")
    LegalParagraph("By using the BaseHW application, you agree to the following terms of use.")
    LegalSection("1. Purpose of the App")
    LegalParagraph("BaseHW is a personal tracking app designed for diecast model car collectors to manage their collections.")
    LegalSection("2. User Responsibility")
    LegalParagraph("- You are responsible for keeping your account secure.\n- You are responsible for the content you upload (photos, notes).\n- You may only use the app for personal, non-commercial purposes.")
    LegalSection("3. Image Attribution")
    LegalParagraph("Car images are sourced from Hot Wheels Wiki, Matchbox Wiki, Mini GT Wiki, and Majorette Wiki (Fandom). These images belong to their respective wiki communities and are used for informational purposes only.")
    LegalSection("4. Service Availability")
    LegalParagraph("The app is provided \"as is.\" We do not guarantee uninterrupted or error-free service. The developers cannot be held liable for data loss.")
    LegalSection("5. Changes")
    LegalParagraph("These terms may be changed without prior notice. Continued use of the app constitutes acceptance of the updated terms.")
    LegalSection("6. Contact")
    LegalParagraph("For questions:\ntaytekofficial@gmail.com")
}

@Composable
private fun TermsDE() {
    LegalTitle("Nutzungsbedingungen")
    LegalSubtitle("Letzte Aktualisierung: Maerz 2026")
    LegalParagraph("Durch die Nutzung der BaseHW-App erklaeren Sie sich mit den folgenden Nutzungsbedingungen einverstanden.")
    LegalSection("1. Zweck der App")
    LegalParagraph("BaseHW ist eine persoenliche Tracking-App fuer Diecast-Modellfahrzeug-Sammler zur Verwaltung ihrer Sammlungen.")
    LegalSection("2. Nutzerverantwortung")
    LegalParagraph("- Sie sind fuer die Sicherheit Ihres Kontos verantwortlich.\n- Sie sind fuer die von Ihnen hochgeladenen Inhalte (Fotos, Notizen) verantwortlich.\n- Sie duerfen die App nur fuer private, nicht-kommerzielle Zwecke nutzen.")
    LegalSection("3. Bild-Attribution")
    LegalParagraph("Fahrzeugbilder stammen aus Hot Wheels Wiki, Matchbox Wiki, Mini GT Wiki und Majorette Wiki (Fandom). Diese Bilder gehoeren den jeweiligen Wiki-Gemeinschaften und werden nur zu Informationszwecken verwendet.")
    LegalSection("4. Dienstverfuegbarkeit")
    LegalParagraph("Die App wird ohne Gewaehrleistung bereitgestellt. Wir garantieren keinen unterbrechungsfreien oder fehlerfreien Betrieb. Die Entwickler haften nicht fuer Datenverluste.")
    LegalSection("5. Aenderungen")
    LegalParagraph("Diese Bedingungen koennen ohne vorherige Ankuendigung geaendert werden. Die weitere Nutzung der App gilt als Zustimmung zu den aktualisierten Bedingungen.")
    LegalSection("6. Kontakt")
    LegalParagraph("Bei Fragen:\ntaytekofficial@gmail.com")
}

// ─── Shared components ────────────────────────────────────────────────────────

@Composable
private fun LegalTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun LegalSubtitle(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LegalSection(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun LegalParagraph(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight)
}
