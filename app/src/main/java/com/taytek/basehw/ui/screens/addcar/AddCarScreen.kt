package com.taytek.basehw.ui.screens.addcar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import com.taytek.basehw.BuildConfig
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.taytek.basehw.ui.util.UCropContract
import com.taytek.basehw.ui.util.CameraModelOcrHelper
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.ui.theme.HotWheelsRed
import com.taytek.basehw.ui.theme.MatchboxBlue
import com.taytek.basehw.ui.theme.MiniGTSilver
import com.taytek.basehw.ui.theme.MajoretteYellow
import com.taytek.basehw.ui.theme.JadaPurple
import com.taytek.basehw.ui.theme.SikuBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarScreen(
    onNavigateBack: () -> Unit,
    masterDataId: Long = -1L,
    openCameraOnLaunch: Boolean = false,
    onSaveSuccess: (() -> Unit)? = null,
    viewModel: AddCarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    var stableSuggestions by remember { mutableStateOf<List<MasterData>>(emptyList()) }

    LaunchedEffect(masterDataId) {
        if (masterDataId != -1L) {
            viewModel.loadMasterDataById(masterDataId)
        }
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val cropLauncher = rememberLauncherForActivityResult(
        contract = UCropContract()
    ) { croppedUri ->
        if (croppedUri != null) {
            viewModel.onUserPhotoUrlChanged(croppedUri.toString())
            if (openCameraOnLaunch) {
                coroutineScope.launch {
                    val detection = runCatching {
                        CameraModelOcrHelper.detectFromImage(context, croppedUri)
                    }.getOrNull()
                    if (detection != null) {
                        viewModel.applyDetectedCameraRecognition(
                            query = detection.query,
                            detectedBrand = detection.detectedBrand
                        )
                    }
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val dir = File(context.filesDir, "car_photos")
            if (!dir.exists()) dir.mkdirs()
            val destinationUri = Uri.fromFile(File(dir, "cropped_${UUID.randomUUID()}.jpg"))
            cropLauncher.launch(UCropContract.UCropInput(uri, destinationUri))
        }
    }

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            val dir = File(context.filesDir, "car_photos")
            if (!dir.exists()) dir.mkdirs()
            val destinationUri = Uri.fromFile(File(dir, "cropped_${UUID.randomUUID()}.jpg"))
            cropLauncher.launch(UCropContract.UCropInput(cameraImageUri!!, destinationUri))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val dir = File(context.filesDir, "car_photos")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    var hasAutoCameraLaunched by rememberSaveable(openCameraOnLaunch) { mutableStateOf(false) }
    LaunchedEffect(openCameraOnLaunch) {
        if (openCameraOnLaunch && !hasAutoCameraLaunched) {
            hasAutoCameraLaunched = true
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.clearSaveSuccess()
            if (onSaveSuccess != null) onSaveSuccess() else onNavigateBack()
        }
    }

    val showSuggestions = uiState.selectedMasterData == null && uiState.searchQuery.isNotBlank()

    LaunchedEffect(
        searchResults.loadState.refresh,
        searchResults.itemCount,
        uiState.searchQuery,
        showSuggestions
    ) {
        if (!showSuggestions || uiState.searchQuery.isBlank()) {
            stableSuggestions = emptyList()
            return@LaunchedEffect
        }

        val snapshot = searchResults.itemSnapshotList.items.filterNotNull()
        if (snapshot.isNotEmpty()) {
            stableSuggestions = snapshot
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(com.taytek.basehw.R.string.add_new_car),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.onSearchQueryChanged("")
                    }) {
                        Text(
                            text = stringResource(com.taytek.basehw.R.string.reset_btn),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── BRAND + MODEL DETAILS (sabit üst bölüm) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // — BRAND —
                FigmaSectionLabel(stringResource(com.taytek.basehw.R.string.section_brand))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(Brand.entries.toTypedArray()) { brand ->
                        val brandColor = when (brand) {
                            Brand.HOT_WHEELS -> HotWheelsRed
                            Brand.MATCHBOX   -> MatchboxBlue
                            Brand.MINI_GT    -> MiniGTSilver
                            Brand.MAJORETTE  -> MajoretteYellow
                            Brand.JADA       -> JadaPurple
                            Brand.SIKU       -> SikuBlue
                        }
                        val isSelected = uiState.selectedBrand == brand
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.onBrandSelected(brand) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) brandColor else brandColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = brand.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) androidx.compose.ui.graphics.Color.White else brandColor,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold
                                             else androidx.compose.ui.text.font.FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // — MODEL DETAILS —
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FigmaSectionLabel(stringResource(com.taytek.basehw.R.string.section_model_details))
                    
                    val brandColor = when (uiState.selectedBrand) {
                        Brand.HOT_WHEELS -> HotWheelsRed
                        Brand.MATCHBOX   -> MatchboxBlue
                        Brand.MINI_GT    -> MiniGTSilver
                        Brand.MAJORETTE  -> MajoretteYellow
                        Brand.JADA       -> JadaPurple
                        Brand.SIKU       -> SikuBlue
                    }
                    
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.toggleManualMode() },
                        shape = RoundedCornerShape(20.dp),
                        color = if (uiState.isManualMode) brandColor else brandColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = if (uiState.isManualMode) Color.White else brandColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(com.taytek.basehw.R.string.manual_mode_btn),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (uiState.isManualMode) Color.White else brandColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!uiState.isManualMode) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(com.taytek.basehw.R.string.search_model_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingIcon = {
                            if (uiState.selectedMasterData != null) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Search
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // ── Öneriler / Form ──
            if (showSuggestions && !uiState.isManualMode) {
                val isInitialLoading =
                    searchResults.loadState.refresh is LoadState.Loading &&
                        searchResults.itemCount == 0 &&
                        stableSuggestions.isEmpty()

                if (isInitialLoading) {
                    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                } else if (
                    searchResults.itemCount == 0 &&
                    searchResults.loadState.refresh is LoadState.NotLoading &&
                    stableSuggestions.isEmpty()
                ) {
                    Text(
                        text = stringResource(com.taytek.basehw.R.string.no_results_for, uiState.searchQuery),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    Text(
                        text = stringResource(com.taytek.basehw.R.string.matching_models),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val currentItems = searchResults.itemSnapshotList.items.filterNotNull()
                        val displayItems = if (currentItems.isNotEmpty()) currentItems else stableSuggestions

                        items(
                            items = displayItems,
                            key = { it.id }
                        ) { item ->
                            SuggestionItem(masterData = item, onClick = { viewModel.onMasterDataSelected(item) })
                        }
                    }
                }
            } else {
                // ── Detay Formu ──
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // — MANUAL FORM —
                    if (uiState.isManualMode) {
                        OutlinedTextField(
                            value = uiState.manualModelName,
                            onValueChange = viewModel::onManualModelNameChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(com.taytek.basehw.R.string.manual_model_name)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.manualScale,
                                onValueChange = viewModel::onManualScaleChanged,
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(com.taytek.basehw.R.string.manual_scale)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = uiState.manualYear,
                                onValueChange = viewModel::onManualYearChanged,
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(com.taytek.basehw.R.string.manual_year)) },
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.manualSeries,
                                onValueChange = viewModel::onManualSeriesChanged,
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(com.taytek.basehw.R.string.manual_series)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = uiState.manualSeriesNum,
                                onValueChange = viewModel::onManualSeriesNumChanged,
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(com.taytek.basehw.R.string.manual_series_num)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = uiState.manualIsPremium,
                                onCheckedChange = viewModel::onManualIsPremiumChanged
                            )
                            Text(
                                text = stringResource(com.taytek.basehw.R.string.manual_is_premium),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Seçili araç önizleme (Sadece model seçiliyse)
                    if (!uiState.isManualMode) {
                        uiState.selectedMasterData?.let { master ->
                            SelectedCarPreview(masterData = master)
                        }
                    }

                    // — CONDITION | CURRENCY —
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FigmaSectionLabel(stringResource(com.taytek.basehw.R.string.section_condition))
                        FigmaSectionLabel(stringResource(com.taytek.basehw.R.string.section_currency))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mint / Loose pill toggle
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(3.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                val mintSelected = !uiState.isOpened
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (mintSelected) com.taytek.basehw.ui.theme.AppPrimary
                                            else androidx.compose.ui.graphics.Color.Transparent
                                        )
                                        .clickable { viewModel.onIsOpenedChanged(false) }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(com.taytek.basehw.R.string.condition_boxed),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (mintSelected) androidx.compose.ui.graphics.Color.White
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (mintSelected) androidx.compose.ui.text.font.FontWeight.Bold
                                                     else androidx.compose.ui.text.font.FontWeight.Normal
                                    )
                                }
                                val looseSelected = uiState.isOpened
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (looseSelected) com.taytek.basehw.ui.theme.AppPrimary
                                            else androidx.compose.ui.graphics.Color.Transparent
                                        )
                                        .clickable { viewModel.onIsOpenedChanged(true) }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(com.taytek.basehw.R.string.condition_opened),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (looseSelected) androidx.compose.ui.graphics.Color.White
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (looseSelected) androidx.compose.ui.text.font.FontWeight.Bold
                                                     else androidx.compose.ui.text.font.FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Currency dropdown / chip
                        val currentCurrency = uiState.selectedCurrency
                        var showCurrencyMenu by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { showCurrencyMenu = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text(
                                    text = if (currentCurrency != null)
                                        "${currentCurrency.name} (${currentCurrency.symbol})"
                                    else "USD (\$)",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showCurrencyMenu,
                                onDismissRequest = { showCurrencyMenu = false }
                            ) {
                                com.taytek.basehw.domain.model.AppCurrency.entries.forEach { currency ->
                                    DropdownMenuItem(
                                        text = { Text("${currency.name} (${currency.symbol})") },
                                        onClick = {
                                            viewModel.onCurrencySelected(currency)
                                            showCurrencyMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // — PRICE DETAILS —
                    FigmaSectionLabel(stringResource(com.taytek.basehw.R.string.section_price_details))
                    val formCurrencySymbol = uiState.selectedCurrency?.symbol ?: "€"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.purchasePrice,
                            onValueChange = viewModel::onPurchasePriceChanged,
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(com.taytek.basehw.R.string.price_purchase), maxLines = 1) },
                            placeholder = { Text(stringResource(com.taytek.basehw.R.string.price_placeholder)) },
                            prefix = { Text(formCurrencySymbol) },
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = uiState.estimatedValue,
                            onValueChange = viewModel::onEstimatedValueChanged,
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(com.taytek.basehw.R.string.price_market), maxLines = 1) },
                            placeholder = { Text(stringResource(com.taytek.basehw.R.string.price_placeholder)) },
                            prefix = { Text(formCurrencySymbol) },
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // — COLLECTION INFO —
                    FigmaSectionLabel(stringResource(com.taytek.basehw.R.string.section_collection_info))

                    // Storage Location
                    OutlinedTextField(
                        value = uiState.storageLocation,
                        onValueChange = viewModel::onStorageLocationChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(com.taytek.basehw.R.string.storage_location_hint)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        label = { Text(stringResource(com.taytek.basehw.R.string.storage_location)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Personal Notes
                    OutlinedTextField(
                        value = uiState.personalNote,
                        onValueChange = viewModel::onPersonalNoteChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(com.taytek.basehw.R.string.personal_note)) },
                        placeholder = { Text(stringResource(com.taytek.basehw.R.string.notes_placeholder)) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Fotoğraf (isteğe bağlı)
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.togglePhotoOptionMenu(true) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (!uiState.userPhotoUrl.isNullOrEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                                AsyncImage(
                                    model = uiState.userPhotoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                SmallFloatingActionButton(
                                    onClick = { viewModel.onUserPhotoUrlChanged(null) },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(com.taytek.basehw.R.string.add_photo), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // — "+ Add to Collection" tam genişlik mavi buton —
                    Button(
                        onClick = viewModel::addCarToCollection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = (uiState.selectedMasterData != null || uiState.isManualMode) && !uiState.isSaving,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.taytek.basehw.ui.theme.AppPrimary,
                            contentColor = androidx.compose.ui.graphics.Color.White,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = androidx.compose.ui.graphics.Color.White)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(com.taytek.basehw.R.string.add_to_collection_btn),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    uiState.error?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            confirmButton = {
                TextButton(onClick = viewModel::clearError) { Text(stringResource(com.taytek.basehw.R.string.ok)) }
            },
            title = { Text(stringResource(com.taytek.basehw.R.string.error_title)) },
            text = { Text(msg) }
        )
    }

    uiState.ocrHintMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::clearOcrHintMessage,
            confirmButton = {
                TextButton(onClick = viewModel::clearOcrHintMessage) { Text(stringResource(com.taytek.basehw.R.string.ok)) }
            },
            title = { Text("Bilgi") },
            text = { Text(msg) }
        )
    }

    if (uiState.isPhotoOptionMenuVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.togglePhotoOptionMenu(false) },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(com.taytek.basehw.R.string.add_photo_options),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
                ListItem(
                    headlineContent = { Text(stringResource(com.taytek.basehw.R.string.take_photo)) },
                    leadingContent = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                    modifier = Modifier.clickable { permissionLauncher.launch(android.Manifest.permission.CAMERA) }
                )
                ListItem(
                    headlineContent = { Text(stringResource(com.taytek.basehw.R.string.pick_from_gallery)) },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    modifier = Modifier.clickable { galleryLauncher.launch("image/*") }
                )
                ListItem(
                    headlineContent = { Text(stringResource(com.taytek.basehw.R.string.add_from_url)) },
                    leadingContent = { Icon(Icons.Default.Link, contentDescription = null) },
                    modifier = Modifier.clickable { viewModel.toggleUrlInputDialog(true) }
                )
            }
        }
    }

    if (uiState.isUrlInputDialogVisible) {
        var urlText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.toggleUrlInputDialog(false) },
            title = { Text(stringResource(com.taytek.basehw.R.string.enter_image_url)) },
            text = {
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    placeholder = { Text(stringResource(com.taytek.basehw.R.string.url_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onUserPhotoUrlChanged(urlText) },
                    enabled = urlText.isNotBlank()
                ) {
                    Text(stringResource(com.taytek.basehw.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleUrlInputDialog(false) }) {
                    Text(stringResource(com.taytek.basehw.R.string.cancel))
                }
            }
        )
    }
}

/** Figma-style üst başlık etiketi (BRAND, MODEL DETAILS vb.) */
@Composable
private fun FigmaSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    )
}






@Composable
private fun SuggestionItem(masterData: MasterData, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (masterData.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = masterData.imageUrl,
                    contentDescription = masterData.modelName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = masterData.modelName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                masterData.year?.let {
                    Text(it.toString(), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    masterData.scale.ifBlank { "1:64" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (masterData.series.isNotBlank()) {
                    Text(
                        masterData.series,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedCarPreview(masterData: MasterData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (masterData.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = masterData.imageUrl,
                    contentDescription = masterData.modelName,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column {
                Text(
                    text = masterData.modelName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = listOfNotNull(
                        masterData.brand.displayName,
                        masterData.scale.ifBlank { "1:64" },
                        masterData.year?.toString(),
                        masterData.color.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
