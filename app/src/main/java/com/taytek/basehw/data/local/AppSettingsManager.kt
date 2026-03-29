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
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // 0: System, 1: Light, 2: Dark  — default 2 (Dark)
    private val _themeFlow = MutableStateFlow(prefs.getInt(KEY_THEME, 2))
    val themeFlow: StateFlow<Int> = _themeFlow.asStateFlow()

    // "tr", "en", "de", or "" for system default
    private val _languageFlow = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "") ?: "")
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    // "USD", "EUR", "GBP", "TRY" or "" if not set
    private val _currencyFlow = MutableStateFlow(
        prefs.getString(KEY_CURRENCY, null) ?: if (_languageFlow.value == "tr") "TRY" else "USD"
    )
    val currencyFlow: StateFlow<String> = _currencyFlow.asStateFlow()

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

    companion object {
        private const val KEY_THEME = "pref_theme"
        private const val KEY_LANGUAGE = "pref_language"
        private const val KEY_CURRENCY = "pref_currency"
        private const val KEY_CATALOG_SYNC_CURSOR = "pref_catalog_sync_cursor"
        private const val KEY_HAS_COMPLETED_2026_CLEANUP = "pref_has_completed_2026_cleanup"
    }
}
