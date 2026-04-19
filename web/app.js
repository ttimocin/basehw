const translations = {
    en: {
        nav_features: "Features",
        nav_about: "About",
        nav_home: "Home",
        btn_download: "Download",
        hero_badge: "50,000+ Model Database • Live Updates",
        hero_title: "Master Your <br><span>Die-Cast Empire</span>",
        hero_subtitle: "The most advanced collection manager with AI-powered recognition, cloud sync, and a vibrant community.",
        btn_playstore: "Download on Play Store",
        btn_explore: "Explore Features",
        features_header: "Why Collectors Choose <span class='accent'>BaseHW</span>",
        feat1_title: "AI Car Recognition",
        feat1_desc: "Identify rare models instantly with our vision-AI directly from your camera.",
        feat2_title: "Cloud Auto-Sync",
        feat2_desc: "Never lose your collection. Securely backup and sync across all your devices.",
        feat3_title: "Advanced Stats",
        feat3_desc: "Track your portfolio value, brand distribution, and STH rarity counts with live charts.",
        feat4_title: "Global Community",
        feat4_desc: "Chat, trade, and showcase your collection to thousands of enthusiasts worldwide.",
        footer_tagline: "Designed for collectors, by collectors.",
        footer_rights: "All rights reserved.",
        legal_privacy: "Privacy",
        legal_terms: "Terms",
        lang_label: "English",
        
        about_title: "Your Collection, Your Passion, Your World",
        about_desc: "BaseHW aims to be the most comprehensive and user-friendly collection management platform for die-cast model enthusiasts. It is not just an inventory tool, but a vibrant ecosystem that brings together collectors from all around the world. By combining modern technology with your hobby, we help you preserve and share the story of every model.",
        
        privacy_policy_title: "Privacy Policy",
        privacy_last_updated: "Last updated: March 2026",
        privacy_intro: "This privacy policy explains how the BaseHW application collects, uses, and protects your personal data.",
        privacy_s1_t: "1. Data We Collect",
        privacy_s1_c: "The app may collect the following data:<br><br>• Optional car photos (stored on Supabase Storage)<br>• Google Sign-in: Your email and profile picture (Firebase Authentication)<br>• Collection data (brand, series, year, condition, price/market value, notes)<br>• Community data: Posts you share, your likes, and comments you write<br>• Social graph: Users you follow and your followers<br>• Direct messaging between users (DM)<br>• Device camera (for AI model identification and taking photos)",
        privacy_s2_t: "2. How We Use Your Data",
        privacy_s2_c: "Your data is used for:<br><br>• Cloud backup and sync across devices<br>• Rendering the community feed and managing interactions<br>• Content moderation using Groq AI<br>• Analysis of photos using artificial intelligence for automatic extraction of model information<br><br>Your data is never sold or used for third-party advertising.",
        privacy_s3_t: "3. Cloud Services and AI Moderation",
        privacy_s3_c: "• Firebase: Collection and social data are securely stored on Firestore's cloud.<br>• Supabase: Car photos are kept on Supabase's cloud storage.<br>• AI Moderation: Comments and captions are automatically moderated by Groq AI for community safety. No personal IDs or names are shared during this text analysis process.",
        privacy_s4_t: "4. Data Deletion and Control",
        privacy_s4_c: "• Delete Comment: You can delete your own comments anytime by long-pressing on the comment.<br>• Delete Post: You can remove your posts from your profile page.<br>• Delete Account: Use the 'Delete Account' button on the Profile page to permanently erase all your data (collection, social interactions, and account).",
        privacy_s5_t: "5. Minors and account use",
        privacy_s5_c: "BaseHW is built for adults and teens who can lawfully agree to our terms. We do not promote the app to children under 13 or aim to gather their personal data on purpose. If we find that we received personal information from someone under 13 without suitable permission, we will take steps to remove it.<br><br>Where local rules treat older minors differently, you may only register or keep actively using an account if a parent or legal guardian agrees and, when the law asks for it, provides any proof we must collect.<br><br>If you are a parent or guardian and think we may hold information about a child by mistake, email us; we will review the request and delete qualifying data.",
        privacy_s6_t: "6. Security practices and limits",
        privacy_s6_c: "We shape our systems around safeguards that fit the kinds of data we process. Any online service carries residual risk: networks and devices can be attacked or misconfigured. We work to lower that risk and to respond when issues arise, but we cannot promise that information will never be seen, disclosed, changed, or lost in ways we do not intend.",
        privacy_s7_t: "7. Contact",
        privacy_s7_c: "For privacy inquiries:<br>taytekofficial@gmail.com",

        terms_title: "Terms of Use",
        terms_last_updated: "Last updated: March 2026",
        terms_intro: "By using the BaseHW application, you agree to the following terms.",
        terms_s1_t: "1. Purpose",
        terms_s1_c: "BaseHW is a management and community app designed for diecast model car collectors.",
        terms_s2_t: "2. User Responsibility and Community Rules",
        terms_s2_c: "• Content: You are solely responsible for the photos, captions, and comments you share.<br>• Etiquette: Profanity, hate speech, harassment, and illegal content are strictly prohibited.<br>• AI Moderation: Content violating community rules may be automatically blocked or removed by our Groq AI systems.",
        terms_s3_t: "3. Intellectual Property",
        terms_s3_c: "Model car images are sourced from official and collector communities for informational purposes. Ownership belongs to the respective trademark holders.",
        terms_s4_t: "4. Limitation of Liability",
        terms_s4_c: "The service is provided 'as is'. Developers are not liable for any data loss or technical interruptions.",
        terms_s5_t: "5. Contact",
        terms_s5_c: "For support:<br>taytekofficial@gmail.com",

        delete_account_title: "Account & Data Deletion",
        delete_account_intro: "We value your privacy. You can delete your account and all associated data through the following methods:",
        delete_method1_title: "Method 1: In-App Deletion (Recommended)",
        delete_method1_desc: "1. Open the BaseHW app.<br>2. Go to the Profile screen.<br>3. Scroll down to Data Management.<br>4. Tap Delete Account and confirm.",
        delete_method2_title: "Method 2: Email Request",
        delete_method2_desc: "If you cannot access the app, you can request account deletion by emailing us at: <a href='mailto:taytekofficial@gmail.com' class='accent'>taytekofficial@gmail.com</a>",
        delete_account_note: "Upon deletion, all your collection data, custom photos, and account information will be permanently removed from our servers within 30 days.",
        nav_delete_account: "Account Deletion"
    },
    tr: {
        nav_features: "Özellikler",
        nav_about: "Hakkında",
        nav_home: "Anasayfa",
        btn_download: "İndir",
        hero_badge: "50.000+ Modellik Veritabanı • Sürekli Güncel",
        hero_title: "Die-Cast İmparatorluğunu <br><span>Yönetmeye Başla</span>",
        hero_subtitle: "AI destekli model tanıma, bulut senkronizasyonu ve canlı bir topluluk ile en gelişmiş koleksiyon yöneticisi.",
        btn_playstore: "Play Store'dan İndir",
        btn_explore: "Özellikleri Keşfet",
        features_header: "Koleksiyonerler Neden <span class='accent'>BaseHW</span> Seçiyor?",
        feat1_title: "AI Model Tanıma",
        feat1_desc: "Nadir modelleri vision-AI ile kameranızdan anında tanımlayın.",
        feat2_title: "Bulut Yedekleme",
        feat2_desc: "Koleksiyonunuzu asla kaybetmeyin. Tüm cihazlarınızda güvenle senkronize edin.",
        feat3_title: "Gelişmiş İstatistikler",
        feat3_desc: "Portföy değerini, marka dağılımını ve STH sayılarını canlı grafiklerle izleyin.",
        feat4_title: "Küresel Topluluk",
        feat4_desc: "Dünya çapındaki binlerce meraklı ile sohbet edin, takas yapın ve koleksiyonunuzu sergileyin.",
        footer_tagline: "Koleksiyonerler tarafından, koleksiyonerler için tasarlandı.",
        footer_rights: "Tüm hakları saklıdır.",
        legal_privacy: "Gizlilik",
        legal_terms: "Şartlar",
        lang_label: "Türkçe",

        about_title: "Koleksiyonunuz, Tutkunuz, Dünyanız",
        about_desc: "BaseHW, die-cast model araba tutkunları için en kapsamlı ve kullanıcı dostu koleksiyon yönetim platformu olmayı hedefler. Sadece bir envanter aracı değil, aynı zamanda dünyanın dört bir yanından koleksiyoncuları bir araya getiren canlı bir ekosistemdir. Modern teknoloji ile hobinizi birleştirerek, her modelin hikayesini korumanıza ve paylaşmanıza yardımcı oluyoruz.",

        privacy_policy_title: "Gizlilik Politikası",
        privacy_last_updated: "Son güncelleme: Mart 2026",
        privacy_intro: "Bu gizlilik politikası, BaseHW uygulamasının kişisel verilerinizi nasıl topladığını, kullandığını ve koruduğunu açıklamaktadır.",
        privacy_s1_t: "1. Toplanan Veriler",
        privacy_s1_c: "Uygulama aşağıdaki verileri toplayabilir:<br><br>• İsteğe bağlı olarak yüklediğiniz araba fotoğrafları (Supabase Storage platformunda saklanır)<br>• Google ile girişte: E-posta adresiniz ve profil görseliniz (Firebase Authentication)<br>• Koleksiyon verileriniz (marka, seri, yıl, durum, fiyat/piyasa değeri, notlar)<br>• Topluluk verileri: Paylaştığınız gönderiler, beğenileriniz ve yazdığınız yorumlar<br>• Takip bilgileri: Takip ettiğiniz kullanıcılar ve takipçileriniz<br>• Kullanıcılar arası doğrudan mesajlaşmalar (DM)<br>• Cihaz kamerası (AI ile model otomatik tanıma ve fotoğraf çekimi için)",
        privacy_s2_t: "2. Verilerin Kullanımı",
        privacy_s2_c: "Verileriniz şu amaçlarla kullanılır:<br><br>• Koleksiyonunuzun bulut yedeklenmesi ve cihazlar arası senkronizasyon<br>• Topluluk akışının oluşturulması ve etkileşimlerin yönetilmesi<br>• İçerik güvenliğinin Groq AI yardımıyla denetlenmesi<br>• Fotoğrafların yapay zeka ile analiz edilerek model bilgilerinin otomatik çıkarılması<br><br>Verileriniz üçüncü taraflara satılmaz veya reklam amaçlı kullanılmaz.",
        privacy_s3_t: "3. Bulut Hizmetleri ve AI Denetimi",
        privacy_s3_c: "• Firebase: Koleksiyon ve topluluk verileri Firestore'da güvenle saklanır.<br>• Supabase: Yüklediğiniz fotoğraflar Supabase bulut depolamasında tutulur.<br>• AI Moderasyon: Topluluk güvenliği için yorum ve açıklamalar Groq AI ile otomatik olarak denetlenir. Bu işlem sırasında kişisel kimlik bilgileriniz (isim, ID) paylaşılmaz, sadece metin içeriği analiz edilir.",
        privacy_s4_t: "4. Veri Silme ve Kontrol",
        privacy_s4_c: "• Yorum Silme: Paylaştığınız yorumları, yorumun üzerine basılı tutarak dilediğiniz zaman silebilirsiniz.<br>• Gönderi Silme: Paylaştığınız gönderileri kendi profilinizden silebilirsiniz.<br>• Hesap Silme: Profil sayfasındaki 'Hesabımı Sil' butonu ile tüm verilerinizi (koleksiyon, topluluk etkileşimleri ve hesap) kalıcı olarak silebilirsiniz.",
        privacy_s5_t: "5. Çocuklar ve hesap kullanımı",
        privacy_s5_c: "BaseHW, yasal olarak sözleşmeyi kabul edebilen yetişkin ve ergen kullanıcılar için tasarlanmıştır. 13 yaş altına yönelik tanıtım yapmıyor ve bu yaş grubundan bilgiyi bilinçli olarak toplamayı hedeflemiyoruz. 13 yaşın altından, uygun izin olmadan kişisel veri aldığımızı öğrenirsek bunu silmek için adım atarız.<br><br>Bulunduğunuz ülkede reşit olmayanlar için farklı kurallar geçerliyse, hesap açma veya uygulamayı aktif kullanma yalnızca ebeveyn veya yasal vasinin onayıyla ve gerekirse kanunun istediği kanıtlarla mümkündür.<br><br>Ebeveyn veya vasiseniz ve çocuğunuza ait olduğunu düşündüğünüz bir kayıt hatası söz konusuysa bize yazın; talebi inceleyip uygun verileri sileriz.",
        privacy_s6_t: "6. Veri güvenliği ve riskler",
        privacy_s6_c: "İşlediğimiz veri türüne uygun önlemlerle sistemleri tasarlarız. Çevrimiçi hizmetlerde her zaman belirli riskler vardır: ağlar ve cihazlar saldırıya veya yanlış yapılandırmaya açık olabilir. Riski azaltmaya ve sorunlara yanıt vermeye çalışırız; buna rağmen bilgilerin hiçbir zaman istenmeyen şekilde görülmesinin, açıklanmasının, değiştirilmesinin veya silinmesinin mümkün olmadığını vaat edemeyiz.",
        privacy_s7_t: "7. İletişim",
        privacy_s7_c: "Gizlilikle ilgili sorularınız için:<br>taytekofficial@gmail.com",

        terms_title: "Kullanım Koşulları",
        terms_last_updated: "Son güncelleme: Mart 2026",
        terms_intro: "BaseHW uygulamasını kullanarak aşağıdaki koşulları kabul etmiş sayılırsınız.",
        terms_s1_t: "1. Uygulamanın Amacı",
        terms_s1_c: "BaseHW, diecast model araba koleksiyoncuları için tasarlanmış bir yönetim ve topluluk uygulamasıdır.",
        terms_s2_t: "2. Kullanıcı Sorumluluğu ve Topluluk Kuralları",
        terms_s2_c: "• İçerik Sorumluluğu: Paylaştığınız fotoğraf, açıklama ve yorumlardan tamamen siz sorumlusunuz.<br>• Saygılı Etkileşim: Küfür, hakaret, nefret söylemi ve taciz içeren her türlü içerik kesinlikle yasaktır.<br>• AI Moderasyon: Topluluk kurallarını ihlal eden içerikler Groq AI sistemlerimiz tarafından otomatik olarak engellenebilir veya silinebilir.",
        terms_s3_t: "3. Fikri Mülkiyet",
        terms_s3_c: "Araba bilgileri ve görselleri Hot Wheels, Matchbox ve diğer ilgili kaynaklardan bilgilendirme amaçlı alınmaktadır. Bu görsellerin mülkiyeti ilgili markalara aittir.",
        terms_s4_t: "4. Hizmet Kesintisi",
        terms_s4_c: "Hizmet 'olduğu gibi' sunulur. Veri kaybı veya teknik aksaklıklardan dolayı geliştirici sorumlu tutulamaz.",
        terms_s5_t: "5. İletişim",
        terms_s5_c: "Destek ve sorularınız için:<br>taytekofficial@gmail.com",

        delete_account_title: "Hesap ve Veri Silme",
        delete_account_intro: "Gizliliğinize önem veriyoruz. Hesabınızı ve ilişkili tüm verileri aşağıdaki yöntemlerle silebilirsiniz:",
        delete_method1_title: "Yöntem 1: Uygulama İçi Silme (Önerilen)",
        delete_method1_desc: "1. BaseHW uygulamasını açın.<br>2. Profil ekranına gidin.<br>3. Veri Yönetimi bölümüne kaydırın.<br>4. Hesabı Sil seçeneğine tıklayın ve onaylayın.",
        delete_method2_title: "Yöntem 2: E-posta ile Talep",
        delete_method2_desc: "Uygulamaya erişemiyorsanız, hesap silme talebinizi şu adrese e-posta göndererek iletebilirsiniz: <a href='mailto:taytekofficial@gmail.com' class='accent'>taytekofficial@gmail.com</a>",
        delete_account_note: "Silme işlemi gerçekleştiğinde; tüm koleksiyon verileriniz, özel fotoğraflarınız ve hesap bilgileriniz 30 gün içinde sunucularımızdan kalıcı olarak kaldırılacaktır.",
        nav_delete_account: "Hesap Silme"
    },
    de: {
        nav_features: "Funktionen",
        nav_about: "Über uns",
        nav_home: "Startseite",
        btn_download: "Download",
        hero_badge: "50.000+ Modell-Datenbank • Live-Updates",
        hero_title: "Meistern Sie Ihr <br><span>Die-Cast Imperium</span>",
        hero_subtitle: "Der fortschrittlichste Sammlungsmanager mit KI-gestützter Erkennung, Cloud-Synchronisierung und einer lebendigen Community.",
        btn_playstore: "Im Play Store herunterladen",
        btn_explore: "Funktionen erkunden",
        features_header: "Warum Sammler <span class='accent'>BaseHW</span> wählen",
        feat1_title: "KI-Auto-Erkennung",
        feat1_desc: "Identifizieren Sie seltene Modelle sofort mit unserer Vision-KI direkt über Ihre Kamera.",
        feat2_title: "Cloud-Auto-Sync",
        feat2_desc: "Verlieren Sie nie Ihre Sammlung. Sichern und synchronisieren Sie sicher auf allen Ihren Geräten.",
        feat3_title: "Erweiterte Statistiken",
        feat3_desc: "Verfolgen Sie Ihren Portfoliowert, die Markenverteilung und STH-Raritätenzählungen mit Live-Charts.",
        feat4_title: "Globale Gemeinschaft",
        feat4_desc: "Chatten, tauschen und präsentieren Sie Ihre Sammlung Tausenden von Enthusiasten weltweit.",
        footer_tagline: "Von Sammlern für Sammler entwickelt.",
        footer_rights: "Alle Rechte vorbehalten.",
        legal_privacy: "Datenschutz",
        legal_terms: "Bedingungen",
        lang_label: "Deutsch",

        about_title: "Ihre Sammlung, Ihre Leidenschaft, Ihre Welt",
        about_desc: "BaseHW möchte die umfassendste und benutzerfreundlichste Plattform für die Verwaltung von Sammlungen für Die-Cast-Enthusiasten sein. Es ist nicht nur ein Inventar-Tool, sondern ein lebendiges Ökosystem, das Sammler aus aller Welt zusammenbringt. Durch die Kombination moderner Technologie mit Ihrem Hobby helfen wir Ihnen, die Geschichte jedes Modells zu bewahren und zu teilen.",

        privacy_policy_title: "Datenschutzrichtlinie",
        privacy_last_updated: "Zuletzt aktualisiert: März 2026",
        privacy_intro: "Diese Richtlinie erläutert, wie BaseHW Ihre personenbezogenen Daten erfasst, nutzt und schützt.",
        privacy_s1_t: "1. Erfasste Daten",
        privacy_s1_c: "Die App kann folgende Daten erfassen:<br><br>• Optionale Fotos (gespeichert auf Supabase Storage)<br>• Google-Anmeldung: E-Mail und Profilbild (Firebase Authentication)<br>• Sammlungsdaten (Marke, Serie, Jahr, Zustand, Preis/Marktwert, Notizen)<br>• Community-Daten: Beiträge, Likes und Kommentare<br>• Soziale Daten: Gefolgte Nutzer und Follower<br>• Direktnachrichten zwischen Benutzern (DM)<br>• Gerätekamera (für die KI-Modellerkennung und zum Aufnehmen von Fotos)",
        privacy_s2_t: "2. Verwendung der Daten",
        privacy_s2_c: "Ihre Daten werden verwendet für:<br><br>• Cloud-Backup und Synchronisierung<br>• Bereitstellung des Community-Feeds und Interaktionen<br>• Inhaltsmoderation mittels Groq KI<br>• Analyse von Fotos mittels künstlicher Intelligenz zur automatischen Extraktion von Modellinformationen<br><br>Ihre Daten werden nicht verkauft oder für Werbung Dritter genutzt.",
        privacy_s3_t: "3. Cloud-Dienste und KI-Moderation",
        privacy_s3_c: "• Firebase: Sammlungs- und Sozialdaten werden sicher auf Firestore's Cloud gespeichert.<br>• Supabase: Fotos werden sicher auf Supabase Cloud Storage aufbewahrt.<br>• KI-Moderation: Kommentare und Beschreibungen werden durch Groq KI automatisiert geprüft. Es werden keine persönlichen IDs oder Namen übertragen.",
        privacy_s4_t: "4. Datenlöschung und Kontrolle",
        privacy_s4_c: "• Kommentar löschen: Eigene Kommentare können jederzeit durch langes Drücken gelöscht werden.<br>• Beitrag löschen: Eigene Beiträge können über die Profilseite entfernt werden.<br>• Konto löschen: Über 'Konto löschen' in den Profileinstellungen können Sie alle Daten permanent entfernen.",
        privacy_s5_t: "5. Minderjährige und Kontonutzung",
        privacy_s5_c: "BaseHW richtet sich an volljährige Nutzer und Jugendliche, die unsere Bedingungen rechtswirksam akzeptieren dürfen. Wir bewerben die App nicht gezielt bei Kindern unter 13 Jahren und wollen deren Daten nicht absichtlich erheben. Erhalten wir Kenntnis davon, dass wir ohne angemessene Erlaubnis Daten einer Person unter 13 erhalten haben, entfernen wir diese nach Möglichkeit.<br><br>Gelten bei Ihnen besondere Regeln für ältere Minderjährige, dürfen Sie sich nur registrieren oder das Konto weiter nutzen, wenn eine sorgeberechtigte Person zustimmt und gegebenenfalls die gesetzlich vorgeschriebenen Nachweise liefert.<br><br>Sind Sie Erziehungsberechtigte und vermuten Sie einen Fehler bei Daten zu Ihrem Kind, schreiben Sie uns; wir prüfen den Fall und löschen betreffende Inhalte, sofern angebracht.",
        privacy_s6_t: "6. Sicherheit und verbleibendes Risiko",
        privacy_s6_c: "Wir gestalten unsere Systeme mit Schutzmaßnahmen, die zu den verarbeiteten Datentypen passen. Online-Dienste bergen immer Restrisiken: Netzwerke und Geräte können angegriffen oder fehlerhaft eingestellt sein. Wir bemühen uns, Risiken zu senken und auf Vorfälle zu reagieren; dennoch können wir nicht zusichern, dass Informationen niemals in unerwünschter Weise einsehbar, offengelegt, verändert oder verloren gehen.",
        privacy_s7_t: "7. Kontakt",
        privacy_s7_c: "Bei Fragen zum Datenschutz:<br>taytekofficial@gmail.com",

        terms_title: "Nutzungsbedingungen",
        terms_last_updated: "Zuletzt aktualisiert: März 2026",
        terms_intro: "Mit der Nutzung von BaseHW akzeptieren Sie die folgenden Bedingungen.",
        terms_s1_t: "1. Zweck",
        terms_s1_c: "BaseHW ist eine Verwaltungs- und Community-App für Diecast-Modellsammler.",
        terms_s2_t: "2. Nutzerverantwortung und Community-Regeln",
        terms_s2_c: "• Inhalte: Sie sind verantwortlich für alle geteilten Fotos, Texte und Kommentare.<br>• Verhalten: Beleidigungen, Hassrede und Belästigungen sind streng untersagt.<br>• KI-Moderation: Verstöße gegen Community-Regeln können durch unsere Groq KI-Systeme automatisch blockiert oder gelöscht werden.",
        terms_s3_t: "3. Geistiges Eigentum",
        terms_s3_c: "Fahrzeugbilder stammen zu Informationszwecken aus offiziellen Sammlerquellen. Das Eigentum verbleibt bei den jeweiligen Rechteinhabern.",
        terms_s4_t: "4. Haftungsausschluss",
        terms_s4_c: "Der Dienst wird 'wie besehen' bereitgestellt. Die Entwickler haften nicht für Datenverluste.",
        terms_s5_t: "5. Kontakt",
        terms_s5_c: "Bei Fragen:<br>taytekofficial@gmail.com",

        delete_account_title: "Konto- und Datenlöschung",
        delete_account_intro: "Wir schätzen Ihre Privatsphäre. Sie können Ihr Konto und alle zugehörigen Daten mit den folgenden Methoden löschen:",
        delete_method1_title: "Methode 1: In-App-Löschung (Empfohlen)",
        delete_method1_desc: "1. Öffnen Sie die BaseHW-App.<br>2. Gehen Sie zum Profil-Bildschirm.<br>3. Scrollen Sie nach unten zu Datenverwaltung.<br>4. Tippen Sie auf Konto löschen und bestätigen Sie.",
        delete_method2_title: "Methode 2: E-Mail-Anfrage",
        delete_method2_desc: "Wenn Sie keinen Zugriff auf die App haben, können Sie die Löschung Ihres Kontos per E-Mail anfordern: <a href='mailto:taytekofficial@gmail.com' class='accent'>taytekofficial@gmail.com</a>",
        delete_account_note: "Nach der Löschung werden alle Ihre Sammlungsdaten, benutzerdefinierten Fotos und Kontoinformationen innerhalb von 30 Tagen endgültig von unseren Servern entfernt.",
        nav_delete_account: "Kontolöschung"
    }
};

const localizedAssets = {
    tr: { screenshot: "assets/tr_home.jpeg" },
    en: { screenshot: "assets/tr_home.jpeg" },
    de: { screenshot: "assets/tr_home.jpeg" } 
};

// Make setLanguage global
window.setLanguage = function(lang) {
    console.log(`Setting language to: ${lang}`);
    if (!translations[lang]) return;

    localStorage.setItem('basehw_lang', lang);
    
    // Update text elements
    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (translations[lang][key]) {
            el.innerHTML = translations[lang][key];
        }
    });

    // Update current lang label in UI
    const labelEl = document.getElementById('current-lang-label');
    if (labelEl) {
        labelEl.innerText = translations[lang].lang_label;
    }

    // Update localized images
    if (localizedAssets[lang]) {
        const screenshot = document.getElementById('main-screenshot');
        if (screenshot) {
            // Account for subdirectory if needed
            const isSubDir = window.location.pathname.includes('/docs/');
            const basePath = isSubDir ? '../' : '';
            screenshot.src = basePath + localizedAssets[lang].screenshot;
        }
    }
}

// Initialization
document.addEventListener('DOMContentLoaded', () => {
    console.log("App initialized - V1.3 (click-based dropdown)");

    // 1. Detect language: Saved -> Browser -> Default (en)
    const savedLang = localStorage.getItem('basehw_lang');
    const browserLang = navigator.language.split('-')[0];
    const defaultLang = translations[savedLang] ? savedLang : (translations[browserLang] ? browserLang : 'en');
    
    window.setLanguage(defaultLang);

    // 2. Click-toggle dropdown (replaces unreliable hover)
    const langBtn = document.querySelector('.lang-btn');
    const langDropdown = document.querySelector('.lang-dropdown');

    if (langBtn && langDropdown) {
        // Toggle open/close on button click
        langBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            langDropdown.classList.toggle('open');
        });

        // Close when clicking anywhere outside
        document.addEventListener('click', () => {
            langDropdown.classList.remove('open');
        });

        // Prevent clicks inside dropdown from closing it
        langDropdown.addEventListener('click', (e) => {
            e.stopPropagation();
        });
    }

    // 3. Language option click listeners
    document.querySelectorAll('.lang-option').forEach(option => {
        option.addEventListener('click', () => {
            const lang = option.getAttribute('data-lang');
            if (lang) {
                window.setLanguage(lang);
                // Close dropdown after selection
                if (langDropdown) langDropdown.classList.remove('open');
            }
        });
    });
});
