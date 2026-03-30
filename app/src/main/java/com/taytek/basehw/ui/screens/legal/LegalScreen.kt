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
    LegalTitle("Gizlilik Politikası")
    LegalSubtitle("Son güncelleme: Mart 2026")
    LegalParagraph("Bu gizlilik politikası, BaseHW uygulamasının kişisel verilerinizi nasıl topladığını, kullandığını ve koruduğunu açıklamaktadır.")
    
    LegalSection("1. Toplanan Veriler")
    LegalParagraph("Uygulama aşağıdaki verileri toplayabilir:\n\n" +
            "• İsteğe bağlı olarak yüklediğiniz araba fotoğrafları (Supabase Storage platformunda saklanır)\n" +
            "• Google ile girişte: E-posta adresiniz ve profil görseliniz (Firebase Authentication)\n" +
            "• Koleksiyon verileriniz (marka, seri, yıl, durum, notlar)\n" +
            "• Topluluk verileri: Paylaştığınız gönderiler, beğenileriniz ve yazdığınız yorumlar\n" +
            "• Takip bilgileri: Takip ettiğiniz kullanıcılar ve takipçileriniz\n" +
            "• Cihaz kamerası (sadece fotoğraf çekimi için)")

    LegalSection("2. Verilerin Kullanımı")
    LegalParagraph("Verileriniz şu amaçlarla kullanılır:\n\n" +
            "• Koleksiyonunuzun bulut yedeklenmesi ve cihazlar arası senkronizasyon\n" +
            "• Topluluk akışının oluşturulması ve etkileşimlerin yönetilmesi\n" +
            "• İçerik güvenliğinin AI (Google Gemini) yardımıyla denetlenmesi\n\n" +
            "Verileriniz üçüncü taraflara satılmaz veya reklam amaçlı kullanılmaz.")

    LegalSection("3. Bulut Hizmetleri ve AI Denetimi")
    LegalParagraph("• Firebase: Koleksiyon ve topluluk verileri Firestore'da güvenle saklanır.\n" +
            "• Supabase: Yüklediğiniz fotoğraflar Supabase bulut depolamasında tutulur.\n" +
            "• AI Moderasyon: Topluluk güvenliği için yorum ve açıklamalar Google Gemini AI ile otomatik olarak denetlenir. Bu işlem sırasında kişisel kimlik bilgileriniz (isim, ID) paylaşılmaz, sadece metin içeriği analiz edilir.")

    LegalSection("4. Veri Silme ve Kontrol")
    LegalParagraph("• Yorum Silme: Paylaştığınız yorumları, yorumun üzerine basılı tutarak dilediğiniz zaman silebilirsiniz.\n" +
            "• Gönderi Silme: Paylaştığınız gönderileri kendi profilinizden silebilirsiniz.\n" +
            "• Hesap Silme: Profil sayfasındaki 'Hesabımı Sil' butonu ile tüm verilerinizi (koleksiyon, topluluk etkileşimleri ve hesap) kalıcı olarak silebilirsiniz.")

    LegalSection("5. İletişim")
    LegalParagraph("Gizlilikle ilgili sorularınız için:\ntaytekofficial@gmail.com")
}

@Composable
private fun PrivacyPolicyEN() {
    LegalTitle("Privacy Policy")
    LegalSubtitle("Last updated: March 2026")
    LegalParagraph("This privacy policy explains how the BaseHW application collects, uses, and protects your personal data.")

    LegalSection("1. Collected Data")
    LegalParagraph("The app may collect the following data:\n\n" +
            "• Optional car photos (stored on Supabase Storage)\n" +
            "• Google Sign-in: Your email and profile picture (Firebase Authentication)\n" +
            "• Collection data (brand, series, year, condition, notes)\n" +
            "• Community data: Posts you share, your likes, and comments you write\n" +
            "• Social graph: Users you follow and your followers\n" +
            "• Device camera (for taking photos only)")

    LegalSection("2. How We Use Your Data")
    LegalParagraph("Your data is used for:\n\n" +
            "• Cloud backup and sync across devices\n" +
            "• Rendering the community feed and managing interactions\n" +
            "• Content moderation using AI (Google Gemini)\n\n" +
            "Your data is never sold or used for third-party advertising.")

    LegalSection("3. Cloud Services and AI Moderation")
    LegalParagraph("• Firebase: Collection and social data are securely stored in Firestore.\n" +
            "• Supabase: Photos are kept in Supabase cloud storage.\n" +
            "• AI Moderation: Comments and captions are automatically moderated by Google Gemini AI for safety. No personal identifiers (names, IDs) are shared during this text-only analysis.")

    LegalSection("4. Data Deletion and Control")
    LegalParagraph("• Delete Comment: You can delete your own comments anytime by long-pressing on the comment.\n" +
            "• Delete Post: You can remove your posts from your profile page.\n" +
            "• Delete Account: Use the 'Delete Account' button on the Profile page to permanently erase all your data (collection, social interactions, and account).")

    LegalSection("5. Contact")
    LegalParagraph("For privacy inquiries:\ntaytekofficial@gmail.com")
}

@Composable
private fun PrivacyPolicyDE() {
    LegalTitle("Datenschutzrichtlinie")
    LegalSubtitle("Zuletzt aktualisiert: März 2026")
    LegalParagraph("Diese Richtlinie erläutert, wie BaseHW Ihre personenbezogenen Daten erfasst, nutzt und schützt.")

    LegalSection("1. Erfasste Daten")
    LegalParagraph("Die App kann folgende Daten erfassen:\n\n" +
            "• Optionale Fotos (gespeichert auf Supabase Storage)\n" +
            "• Google-Anmeldung: E-Mail und Profilbild (Firebase Authentication)\n" +
            "• Sammlungsdaten (Marke, Serie, Jahr, Zustand, Notizen)\n" +
            "• Community-Daten: Beiträge, Likes und Kommentare\n" +
            "• Soziale Daten: Gefolgte Nutzer und Follower\n" +
            "• Kamera (nur für Fotoaufnahmen)")

    LegalSection("2. Verwendung der Daten")
    LegalParagraph("Ihre Daten werden verwendet für:\n\n" +
            "• Cloud-Backup und Synchronisierung\n" +
            "• Bereitstellung des Community-Feeds und Interaktionen\n" +
            "• Inhaltsmoderation mittels KI (Google Gemini)\n\n" +
            "Ihre Daten werden nicht verkauft oder für Werbung Dritter genutzt.")

    LegalSection("3. Cloud-Dienste und KI-Moderation")
    LegalParagraph("• Firebase: Sammlungs- und Sozialdaten werden sicher in Firestore gespeichert.\n" +
            "• Supabase: Fotos werden im Supabase-Cloud-Speicher aufbewahrt.\n" +
            "• KI-Moderation: Kommentare und Beschreibungen werden zur Sicherheit automatisiert durch Google Gemini KI geprüft. Dabei werden keine persönlichen Identifikatoren übertragen.")

    LegalSection("4. Datenlöschung und Kontrolle")
    LegalParagraph("• Kommentar löschen: Eigene Kommentare können jederzeit durch langes Drücken gelöscht werden.\n" +
            "• Beitrag löschen: Eigene Beiträge können über die Profilseite entfernt werden.\n" +
            "• Konto löschen: Über 'Konto löschen' in den Profileinstellungen können Sie alle Daten permanent entfernen.")

    LegalSection("5. Kontakt")
    LegalParagraph("Bei Fragen zum Datenschutz:\ntaytekofficial@gmail.com")
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
    LegalTitle("Kullanım Koşulları")
    LegalSubtitle("Son güncelleme: Mart 2026")
    LegalParagraph("BaseHW uygulamasını kullanarak aşağıdaki koşulları kabul etmiş sayılırsınız.")
    
    LegalSection("1. Uygulamanın Amacı")
    LegalParagraph("BaseHW, diecast model araba koleksiyoncuları için tasarlanmış bir yönetim ve topluluk uygulamasıdır.")

    LegalSection("2. Kullanıcı Sorumluluğu ve Topluluk Kuralları")
    LegalParagraph("• İçerik Sorumluluğu: Paylaştığınız fotoğraf, açıklama ve yorumlardan tamamen siz sorumlusunuz.\n" +
            "• Saygılı Etkileşim: Küfür, hakaret, nefret söylemi ve taciz içeren her türlü içerik kesinlikle yasaktır.\n" +
            "• AI Moderasyon: Topluluk kurallarını ihlal eden içerikler AI sistemlerimiz tarafından otomatik olarak engellenebilir veya silinebilir.")

    LegalSection("3. Fikri Mülkiyet")
    LegalParagraph("Araba görselleri Hot Wheels, Matchbox ve diğer ilgili wiki kaynaklarından bilgilendirme amaçlı alınmaktadır. Bu görsellerin mülkiyeti ilgili topluluklara ve markalara aittir.")

    LegalSection("4. Hizmet Kesintisi")
    LegalParagraph("Hizmet 'olduğu gibi' sunulur. Veri kaybı veya teknik aksaklıklardan dolayı geliştirici sorumlu tutulamaz.")

    LegalSection("5. İletişim")
    LegalParagraph("Destek ve sorularınız için:\ntaytekofficial@gmail.com")
}

@Composable
private fun TermsEN() {
    LegalTitle("Terms of Use")
    LegalSubtitle("Last updated: March 2026")
    LegalParagraph("By using the BaseHW application, you agree to the following terms.")

    LegalSection("1. Purpose")
    LegalParagraph("BaseHW is a management and community app designed for diecast model car collectors.")

    LegalSection("2. User Responsibility and Community Rules")
    LegalParagraph("• Content: You are solely responsible for the photos, captions, and comments you share.\n" +
            "• Etiquette: Profanity, hate speech, harassment, and illegal content are strictly prohibited.\n" +
            "• AI Moderation: Content violating community rules may be automatically blocked or removed by our AI systems.")

    LegalSection("3. Intellectual Property")
    LegalParagraph("Model car images are sourced from Wiki communities for informational purposes. Ownership belongs to the respective trademark holders and communities.")

    LegalSection("4. Limitation of Liability")
    LegalParagraph("The service is provided 'as is'. Developers are not liable for any data loss or technical interruptions.")

    LegalSection("5. Contact")
    LegalParagraph("For support:\ntaytekofficial@gmail.com")
}

@Composable
private fun TermsDE() {
    LegalTitle("Nutzungsbedingungen")
    LegalSubtitle("Zuletzt aktualisiert: März 2026")
    LegalParagraph("Mit der Nutzung von BaseHW akzeptieren Sie die folgenden Bedingungen.")

    LegalSection("1. Zweck")
    LegalParagraph("BaseHW ist eine Verwaltungs- und Community-App für Diecast-Modellsammler.")

    LegalSection("2. Nutzerverantwortung und Community-Regeln")
    LegalParagraph("• Inhalte: Sie sind verantwortlich für alle geteilten Fotos, Texte und Kommentare.\n" +
            "• Verhalten: Beleidigungen, Hassrede und Belästigungen sind streng untersagt.\n" +
            "• KI-Moderation: Verstöße gegen Community-Regeln können durch unsere KI-Systeme automatisch blockiert oder gelöscht werden.")

    LegalSection("3. Geistiges Eigentum")
    LegalParagraph("Fahrzeugbilder stammen zu Informationszwecken aus Wiki-Quellen. Das Eigentum verbleibt bei den jeweiligen Rechteinhabern.")

    LegalSection("4. Haftungsausschluss")
    LegalParagraph("Der Dienst wird 'wie besehen' bereitgestellt. Die Entwickler haften nicht für Datenverluste.")

    LegalSection("5. Kontakt")
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
