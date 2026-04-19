package com.taytek.basehw.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // 0: System, 1: Light, 2: Dark  — default 2 (Dark)
    private val _themeFlow = MutableStateFlow(prefs.getInt(KEY_THEME, 2))
    val themeFlow: StateFlow<Int> = _themeFlow.asStateFlow()

    // Initial language detection
    private val _languageFlow = MutableStateFlow(
        prefs.getString(KEY_LANGUAGE, "")?.takeIf { it.isNotEmpty() } ?: run {
            val systemLang = java.util.Locale.getDefault().language
            if (listOf("tr", "en", "de", "fr", "ar", "es", "pt", "ru", "uk").contains(systemLang)) systemLang else "en"
        }
    )
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    // "USD", "EUR", "GBP", "TRY" or "" if not set
    private val _currencyFlow = MutableStateFlow(
        prefs.getString(KEY_CURRENCY, null) ?: if (_languageFlow.value == "tr") "TRY" else "USD"
    )
    val currencyFlow: StateFlow<String> = _currencyFlow.asStateFlow()

    // 0: Space Grotesk, 1: Inter — default 0 (Space Grotesk)
    private val _fontFlow = MutableStateFlow(prefs.getInt(KEY_FONT_FAMILY, 0))
    val fontFlow: StateFlow<Int> = _fontFlow.asStateFlow()

    fun setTheme(theme: Int) {
        prefs.edit().putInt(KEY_THEME, theme).apply()
        _themeFlow.value = theme
    }

    fun setLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
        _languageFlow.value = languageCode
    }

    fun setCurrency(currencyCode: String) {
        prefs.edit().putString(KEY_CURRENCY, currencyCode).apply()
        _currencyFlow.value = currencyCode
    }

    fun setFontFamily(fontFamily: Int) {
        prefs.edit().putInt(KEY_FONT_FAMILY, fontFamily).apply()
        _fontFlow.value = fontFamily
    }

    fun getCatalogSyncCursor(): String {
        return prefs.getString(KEY_CATALOG_SYNC_CURSOR, "") ?: ""
    }

    fun setCatalogSyncCursor(cursor: String) {
        prefs.edit().putString(KEY_CATALOG_SYNC_CURSOR, cursor).apply()
    }

    fun clearCatalogSyncCursor() {
        prefs.edit().remove(KEY_CATALOG_SYNC_CURSOR).apply()
    }

    fun hasCompleted2026Cleanup(): Boolean {
        return prefs.getBoolean(KEY_HAS_COMPLETED_2026_CLEANUP, false)
    }

    fun setCompleted2026Cleanup(completed: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_COMPLETED_2026_CLEANUP, completed).apply()
    }

    fun hasAcceptedCommunityRules(): Boolean {
        return prefs.getBoolean(KEY_COMMUNITY_RULES_ACCEPTED, false)
    }

    fun setAcceptedCommunityRules(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_COMMUNITY_RULES_ACCEPTED, accepted).apply()
    }

    fun hasAcceptedPrivacyTerms(): Boolean {
        return prefs.getBoolean(KEY_PRIVACY_TERMS_ACCEPTED, false)
    }

    fun setAcceptedPrivacyTerms(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_PRIVACY_TERMS_ACCEPTED, accepted).apply()
    }

    fun getCommunityInboxLastSeenAt(): Long {
        return prefs.getLong(KEY_COMMUNITY_INBOX_LAST_SEEN_AT, 0L)
    }

    fun setCommunityInboxLastSeenAt(timestampMillis: Long) {
        prefs.edit().putLong(KEY_COMMUNITY_INBOX_LAST_SEEN_AT, timestampMillis).apply()
    }

    fun getCommunityPostTimestamps(uid: String): List<Long> {
        if (uid.isBlank()) return emptyList()
        val key = "$KEY_COMMUNITY_POST_TIMESTAMPS_PREFIX$uid"
        val raw = prefs.getString(key, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(',')
            .mapNotNull { it.toLongOrNull() }
            .sorted()
    }

    fun setCommunityPostTimestamps(uid: String, timestamps: List<Long>) {
        if (uid.isBlank()) return
        val key = "$KEY_COMMUNITY_POST_TIMESTAMPS_PREFIX$uid"
        if (timestamps.isEmpty()) {
            prefs.edit().remove(key).apply()
            return
        }
        val serialized = timestamps.sorted().joinToString(",")
        prefs.edit().putString(key, serialized).apply()
    }

    fun getCommunityCommentTimestamps(uid: String): List<Long> {
        if (uid.isBlank()) return emptyList()
        val key = "$KEY_COMMUNITY_COMMENT_TIMESTAMPS_PREFIX$uid"
        val raw = prefs.getString(key, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(',')
            .mapNotNull { it.toLongOrNull() }
            .sorted()
    }

    fun setCommunityCommentTimestamps(uid: String, timestamps: List<Long>) {
        if (uid.isBlank()) return
        val key = "$KEY_COMMUNITY_COMMENT_TIMESTAMPS_PREFIX$uid"
        if (timestamps.isEmpty()) {
            prefs.edit().remove(key).apply()
            return
        }
        val serialized = timestamps.sorted().joinToString(",")
        prefs.edit().putString(key, serialized).apply()
    }

    fun getDirectMessageTimestamps(uid: String): List<Long> {
        if (uid.isBlank()) return emptyList()
        val key = "$KEY_DIRECT_MESSAGE_TIMESTAMPS_PREFIX$uid"
        val raw = prefs.getString(key, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(',')
            .mapNotNull { it.toLongOrNull() }
            .sorted()
    }

    fun setDirectMessageTimestamps(uid: String, timestamps: List<Long>) {
        if (uid.isBlank()) return
        val key = "$KEY_DIRECT_MESSAGE_TIMESTAMPS_PREFIX$uid"
        if (timestamps.isEmpty()) {
            prefs.edit().remove(key).apply()
            return
        }
        val serialized = timestamps.sorted().joinToString(",")
        prefs.edit().putString(key, serialized).apply()
    }

    fun getHomeStatsPagerPage(): Int =
        prefs.getInt(KEY_HOME_STATS_PAGER_PAGE, 0).coerceIn(0, 1)

    fun setHomeStatsPagerPage(page: Int) {
        prefs.edit().putInt(KEY_HOME_STATS_PAGER_PAGE, page.coerceIn(0, 1)).apply()
    }

    companion object {
        private const val KEY_THEME = "pref_theme"
        private const val KEY_LANGUAGE = "pref_language"
        private const val KEY_CURRENCY = "pref_currency"
        private const val KEY_FONT_FAMILY = "pref_font_family"
        private const val KEY_CATALOG_SYNC_CURSOR = "pref_catalog_sync_cursor"
        private const val KEY_HAS_COMPLETED_2026_CLEANUP = "pref_has_completed_2026_cleanup"
        private const val KEY_COMMUNITY_RULES_ACCEPTED = "pref_community_rules_accepted"
        private const val KEY_PRIVACY_TERMS_ACCEPTED = "pref_privacy_terms_accepted"
        private const val KEY_COMMUNITY_INBOX_LAST_SEEN_AT = "pref_community_inbox_last_seen_at"
        private const val KEY_COMMUNITY_POST_TIMESTAMPS_PREFIX = "pref_community_post_timestamps_"
        private const val KEY_COMMUNITY_COMMENT_TIMESTAMPS_PREFIX = "pref_community_comment_timestamps_"
        private const val KEY_DIRECT_MESSAGE_TIMESTAMPS_PREFIX = "pref_direct_message_timestamps_"
        private const val KEY_HOME_STATS_PAGER_PAGE = "pref_home_stats_pager_page"
    }
}
