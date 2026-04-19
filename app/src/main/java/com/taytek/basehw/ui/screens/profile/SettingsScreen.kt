package com.taytek.basehw.ui.screens.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.activity.ComponentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.taytek.basehw.R
import com.taytek.basehw.domain.model.AppCurrency
import com.taytek.basehw.domain.model.CollectionImportMode
import androidx.compose.material.icons.filled.FileOpen
import com.taytek.basehw.ui.screens.community.CommunityUiState
import com.taytek.basehw.ui.screens.community.FollowUsersDialog
import com.taytek.basehw.ui.theme.*
import kotlinx.coroutines.launch

enum class ExportAction { DOWNLOAD, SHARE }
enum class ExportFormat { CSV, JSON, PDF }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: CommunityUiState,
    profileUiState: ProfileUiState,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    themeState: Int,
    fontFamilyState: Int,
    languageState: String,
    currencyCode: String,
    onBack: () -> Unit,
    onUpdateCollectionVisibility: (Boolean) -> Unit,
    onUpdateWishlistVisibility: (Boolean) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExport: () -> Unit,
    onSetTheme: (Int) -> Unit,
    onSetFontFamily: (Int) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetCurrency: (String) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTermsOfUse: () -> Unit,
    onOpenForumRules: () -> Unit,
    onOpenSupport: () -> Unit,
    onLogout: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onAdminPanelClick: () -> Unit = {},
    isAuthenticated: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showExportFormatDialog by remember { mutableStateOf(false) }
    var pendingExportAction by remember { mutableStateOf<ExportAction?>(null) }
    var showImportModeDialog by remember { mutableStateOf(false) }
    var showImportReplaceWarning by remember { mutableStateOf(false) }
    var pendingImportMode by remember { mutableStateOf<CollectionImportMode?>(null) }

    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val mode = pendingImportMode
        pendingImportMode = null
        if (uri != null && mode != null) {
            val mime = context.contentResolver.getType(uri)
            coroutineScope.launch {
                profileViewModel.importCollectionFromUri(uri, mime, mode)
            }
        }
    }

    val languageOptions = listOf(
        "tr" to stringResource(R.string.language_tr),
        "en" to stringResource(R.string.language_en),
        "de" to stringResource(R.string.language_de),
        "fr" to stringResource(R.string.language_fr),
        "ar" to stringResource(R.string.language_ar),
        "es" to stringResource(R.string.language_es),
        "pt" to stringResource(R.string.language_pt),
        "ru" to stringResource(R.string.language_ru),
        "uk" to stringResource(R.string.language_uk)
    )
    val selectedLanguageLabel = languageOptions.find { it.first == languageState }?.second
        ?: stringResource(R.string.language_tr)
    val themeOptions = listOf(
        0 to stringResource(R.string.theme_auto),
        1 to stringResource(R.string.theme_light),
        2 to stringResource(R.string.theme_dark),
        3 to stringResource(R.string.theme_cyber),
        4 to stringResource(R.string.theme_neon_cyan)
    )
    val selectedThemeLabel = themeOptions.find { it.first == themeState }?.second
        ?: stringResource(R.string.theme_auto)
    val fontOptions = listOf(
        0 to stringResource(R.string.font_space_grotesk),
        1 to stringResource(R.string.font_inter)
    )
    val selectedFontLabel = fontOptions.find { it.first == fontFamilyState }?.second
        ?: stringResource(R.string.font_space_grotesk)
    val currencyOptions = AppCurrency.entries
    val selectedCurrency = AppCurrency.fromCode(currencyCode)
    val selectedCurrencyLabel = "${selectedCurrency.code} (${selectedCurrency.symbol})"

    val excelExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                context.contentResolver.openOutputStream(it)?.let { outputStream ->
                    profileViewModel.exportToExcel(outputStream)
                }
            }
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                context.contentResolver.openOutputStream(it)?.let { outputStream ->
                    profileViewModel.exportToJson(outputStream)
                }
            }
        }
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                context.contentResolver.openOutputStream(it)?.let { outputStream ->
                    profileViewModel.exportToPdf(outputStream)
                }
            }
        }
    }

    var pendingExportFormat by remember { mutableStateOf<ExportFormat?>(null) }
    var pendingExportMimeType by remember { mutableStateOf<String?>(null) }

    fun handleExport(format: ExportFormat, mimeType: String, extension: String) {
        pendingExportFormat = format
        pendingExportMimeType = mimeType
        if (pendingExportAction == ExportAction.SHARE) {
            // Paylaşma için dosyayı cache'e yaz, sonra paylaş
            coroutineScope.launch {
                try {
                    val timestamp = System.currentTimeMillis()
                    val fileName = "basehw_collection_$timestamp.$extension"
                    val cacheFile = File(context.cacheDir, fileName)
                    
                    cacheFile.outputStream().use { outputStream ->
                        when (format) {
                            ExportFormat.CSV -> profileViewModel.exportToExcel(outputStream)
                            ExportFormat.JSON -> profileViewModel.exportToJson(outputStream)
                            ExportFormat.PDF -> profileViewModel.exportToPdf(outputStream)
                        }
                    }
                    
                    // FileProvider ile URI al
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        cacheFile
                    )
                    
                    // Paylaşma intent'i oluştur
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(Intent.EXTRA_SUBJECT, "BaseHW Collection")
                    }
                    
                    // Paylaşma dialogunu aç
                    val chooserIntent = Intent.createChooser(shareIntent, "Paylaş")
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooserIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Paylaşım hatası: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            // İndirme için CreateDocument launcher kullan
            when (format) {
                ExportFormat.CSV -> excelExportLauncher.launch("basehw_collection_${System.currentTimeMillis()}.csv")
                ExportFormat.JSON -> jsonExportLauncher.launch("basehw_collection_${System.currentTimeMillis()}.json")
                ExportFormat.PDF -> pdfExportLauncher.launch("basehw_collection_${System.currentTimeMillis()}.pdf")
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    languageOptions.forEach { (code, label) ->
                        TextButton(
                            onClick = {
                                onSetLanguage(code)
                                showLanguageDialog = false
                                (context as? ComponentActivity)?.recreate()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                color = if (languageState == code) AppTheme.tokens.primaryAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.theme), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    themeOptions.forEach { (themeCode, themeLabel) ->
                        TextButton(
                            onClick = {
                                onSetTheme(themeCode)
                                showThemeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = themeLabel,
                                color = if (themeState == themeCode) AppTheme.tokens.primaryAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }

    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text(stringResource(R.string.font_family), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    fontOptions.forEach { (fontCode, fontLabel) ->
                        TextButton(
                            onClick = {
                                onSetFontFamily(fontCode)
                                showFontDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = fontLabel,
                                color = if (fontFamilyState == fontCode) AppTheme.tokens.primaryAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text(stringResource(R.string.currency_setting), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    currencyOptions.forEach { currency ->
                        val label = "${currency.code} (${currency.symbol})"
                        TextButton(
                            onClick = {
                                onSetCurrency(currency.code)
                                showCurrencyDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                color = if (currencyCode == currency.code) AppTheme.tokens.primaryAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text(stringResource(R.string.delete_account_dialog_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_account_dialog_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccount()
                    }
                ) { Text(stringResource(R.string.delete_account_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(stringResource(R.string.delete_account_cancel))
                }
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.export_options_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Action selection: Download / Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Download Button
                        OutlinedButton(
                            onClick = {
                                pendingExportAction = ExportAction.DOWNLOAD
                                showExportFormatDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = AppTheme.tokens.primaryAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(text = stringResource(R.string.export_download), fontSize = 11.sp)
                        }
                        // Share Button
                        OutlinedButton(
                            onClick = {
                                pendingExportAction = ExportAction.SHARE
                                showExportFormatDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = AppTheme.tokens.primaryAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(text = stringResource(R.string.export_share_action), fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }

    // Format selection dialog
    if (showExportFormatDialog) {
        AlertDialog(
            onDismissRequest = { showExportFormatDialog = false },
            title = { Text(stringResource(R.string.export_select_format), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // CSV Option
                    TextButton(
                        onClick = {
                            showExportFormatDialog = false
                            handleExport(ExportFormat.CSV, "text/csv", "csv")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = AppTheme.tokens.primaryAccent)
                            Spacer(Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(stringResource(R.string.export_format_csv), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    // JSON Option
                    TextButton(
                        onClick = {
                            showExportFormatDialog = false
                            handleExport(ExportFormat.JSON, "application/json", "json")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DataObject, contentDescription = null, tint = AppTheme.tokens.primaryAccent)
                            Spacer(Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(stringResource(R.string.export_format_json), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    // PDF Option
                    TextButton(
                        onClick = {
                            showExportFormatDialog = false
                            handleExport(ExportFormat.PDF, "application/pdf", "pdf")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AppTheme.tokens.primaryAccent)
                            Spacer(Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(stringResource(R.string.export_format_pdf), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportFormatDialog = false }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }

    if (showImportModeDialog) {
        AlertDialog(
            onDismissRequest = { showImportModeDialog = false },
            title = { Text(stringResource(R.string.import_mode_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.import_collection_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            showImportModeDialog = false
                            pendingImportMode = CollectionImportMode.MERGE
                            importDocumentLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "text/csv",
                                    "text/plain",
                                    "application/pdf",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.import_mode_merge))
                    }
                    TextButton(
                        onClick = {
                            showImportModeDialog = false
                            showImportReplaceWarning = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.import_mode_replace),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImportModeDialog = false }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }

    if (showImportReplaceWarning) {
        AlertDialog(
            onDismissRequest = { showImportReplaceWarning = false },
            title = { Text(stringResource(R.string.import_replace_warning_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.import_replace_warning_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportReplaceWarning = false
                        pendingImportMode = CollectionImportMode.REPLACE
                        importDocumentLauncher.launch(
                            arrayOf(
                                "application/json",
                                "text/csv",
                                "text/plain",
                                "application/pdf",
                                "*/*"
                            )
                        )
                    }
                ) {
                    Text(stringResource(R.string.import_replace_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportReplaceWarning = false }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSectionCard(title = stringResource(R.string.privacy_visibility_section)) {
                VisibilitySettingRow(
                    title = stringResource(R.string.collection_visibility_title),
                    subtitle = if (profileUiState.userData?.isCollectionPublic == true) {
                        stringResource(R.string.visibility_public)
                    } else {
                        stringResource(R.string.visibility_private)
                    },
                    checked = profileUiState.userData?.isCollectionPublic == true,
                    onCheckedChange = onUpdateCollectionVisibility
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                VisibilitySettingRow(
                    title = stringResource(R.string.wishlist_visibility_title),
                    subtitle = if (profileUiState.userData?.isWishlistPublic == true) {
                        stringResource(R.string.visibility_public)
                    } else {
                        stringResource(R.string.visibility_private)
                    },
                    checked = profileUiState.userData?.isWishlistPublic == true,
                    onCheckedChange = onUpdateWishlistVisibility
                )
            }

            SettingsSectionCard(title = stringResource(R.string.data_management)) {
                ProfileSettingsListItem(
                    Icons.Default.Sync,
                    stringResource(R.string.pull_new_models),
                    profileUiState.syncStatusResId?.let { stringResource(it) }
                        ?: stringResource(R.string.pull_new_models_desc)
                ) {
                    if (!profileUiState.isSyncing) {
                        profileViewModel.enqueueRemoteSync(androidx.work.WorkManager.getInstance(context))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                ProfileSettingsListItem(
                    Icons.Default.CloudUpload,
                    stringResource(R.string.backup_cloud),
                    stringResource(R.string.cloud_backup_title)
                ) {
                    if (!isAuthenticated) {
                        profileViewModel.setErrorMessage(context.getString(R.string.login_required_backup))
                    } else {
                        onBackup()
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                ProfileSettingsListItem(
                    Icons.Default.CloudDownload,
                    stringResource(R.string.restore_cloud),
                    stringResource(R.string.restore_data_title)
                ) {
                    if (!isAuthenticated) {
                        profileViewModel.setErrorMessage(context.getString(R.string.login_required_backup))
                    } else {
                        onRestore()
                    }
                }
                SettingsDataManagementStatusBanner(profileUiState = profileUiState)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                ProfileSettingsListItem(
                    Icons.Default.CurrencyExchange,
                    stringResource(R.string.excel_export_title),
                    stringResource(R.string.excel_export_desc)
                ) {
                    showExportDialog = true
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                ProfileSettingsListItem(
                    Icons.Filled.FileOpen,
                    stringResource(R.string.import_collection_title),
                    stringResource(R.string.import_collection_desc)
                ) {
                    showImportModeDialog = true
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                ProfileSettingsListItem(
                    Icons.Default.Paid,
                    stringResource(R.string.currency_setting),
                    selectedCurrencyLabel
                ) {
                    showCurrencyDialog = true
                }
            }

            // Admin Panel - sadece admin kullanıcılara göster
            if (profileUiState.userData?.isAdmin == true) {
                SettingsSectionCard(title = stringResource(R.string.admin_management_section)) {
                    ProfileSettingsListItem(
                        Icons.Default.AdminPanelSettings,
                        stringResource(R.string.admin_panel_title),
                        stringResource(R.string.admin_forum_management)
                    ) {
                        onAdminPanelClick()
                    }
                }
            }

            SettingsSectionCard(title = stringResource(R.string.settings_support_title)) {
                ProfileSettingsListItem(
                    Icons.Default.Language,
                    stringResource(R.string.language),
                    selectedLanguageLabel
                ) {
                    showLanguageDialog = true
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                ProfileSettingsListItem(
                    Icons.Default.Palette,
                    stringResource(R.string.theme),
                    selectedThemeLabel
                ) {
                    showThemeDialog = true
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                ProfileSettingsListItem(
                    Icons.Default.TextFields,
                    stringResource(R.string.font_family),
                    selectedFontLabel
                ) {
                    showFontDialog = true
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                ProfileSettingsListItem(
                    Icons.Default.SupportAgent,
                    stringResource(R.string.help_support),
                    stringResource(R.string.settings_subtitle_help_support)
                ) {
                    onOpenSupport()
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                ProfileSettingsListItem(
                    Icons.Default.Shield,
                    stringResource(R.string.privacy_policy),
                    stringResource(R.string.settings_subtitle_privacy_policy)
                ) {
                    onOpenPrivacyPolicy()
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                ProfileSettingsListItem(
                    Icons.Default.Description,
                    stringResource(R.string.terms_of_use),
                    stringResource(R.string.settings_subtitle_terms_of_use)
                ) {
                    onOpenTermsOfUse()
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                ProfileSettingsListItem(
                    Icons.Default.Gavel,
                    stringResource(R.string.community_rules_title),
                    stringResource(R.string.settings_subtitle_community_rules)
                ) {
                    onOpenForumRules()
                }
            }

            SettingsSectionCard(title = stringResource(R.string.community_and_social_section)) {
                ProfileSettingsListItem(
                    icon = Icons.Default.Public,
                    title = stringResource(R.string.settings_official_website),
                    subtitle = "www.basehw.net"
                ) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.basehw.net"))
                    context.startActivity(intent)
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                ProfileSettingsListItem(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.settings_github_repo),
                    subtitle = "github.com/ttimocin/basehw"
                ) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ttimocin/basehw"))
                    context.startActivity(intent)
                }
            }

            SettingsSectionCard(title = stringResource(R.string.account_actions_title)) {
                ProfileSettingsListItem(
                    Icons.Default.Logout,
                    stringResource(R.string.logout),
                    stringResource(R.string.settings_subtitle_logout)
                ) {
                    onBack()
                    onLogout()
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                ProfileSettingsListItem(
                    Icons.Default.DeleteForever,
                    stringResource(R.string.delete_account),
                    stringResource(R.string.delete_account_dialog_title)
                ) {
                    showDeleteAccountDialog = true
                }
            }
             
            Spacer(Modifier.height(120.dp))
        }
    }


    if (profileUiState.isCloudCheckInProgress || profileUiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AppTheme.tokens.primaryAccent)
        }
    }
}

/** Veri yönetimi: bulut yedek/geri yükle durumu ve hatalar (diyalog yok). */
@Composable
private fun SettingsDataManagementStatusBanner(profileUiState: ProfileUiState) {
    val textPair = when {
        profileUiState.error != null -> profileUiState.error!! to true
        profileUiState.isCloudDataOpRunning && profileUiState.syncStatusMsg != null ->
            profileUiState.syncStatusMsg!! to false
        profileUiState.syncSuccess && profileUiState.syncStatusMsg != null ->
            profileUiState.syncStatusMsg!! to false
        else -> null
    } ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = textPair.first,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (textPair.second) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun VisibilitySettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ProfileSettingsListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 18.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        androidx.compose.material3.Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}
