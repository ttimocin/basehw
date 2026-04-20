package com.taytek.basehw.ui.screens.detail

import com.taytek.basehw.ui.screens.community.CommunityRulesDialog

import com.taytek.basehw.R
import androidx.compose.ui.res.stringResource
import com.taytek.basehw.ui.theme.AppTheme
import com.taytek.basehw.ui.theme.DarkNavy
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.HwCardType
import com.taytek.basehw.domain.model.HwCardTypeRules
import com.taytek.basehw.domain.model.toColor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import com.taytek.basehw.ui.theme.LocalThemeVariant
import com.taytek.basehw.ui.theme.ThemeVariant
import com.taytek.basehw.ui.theme.neonCyanActionGradientBrush
import com.taytek.basehw.ui.theme.cyberActionGradientBrush
import com.taytek.basehw.ui.theme.CyberKnockoutIconTint
import com.taytek.basehw.ui.theme.NeonCyanKnockoutIconTint
import androidx.compose.material3.Surface
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.util.Date
import android.net.Uri
import java.util.UUID
import com.taytek.basehw.ui.util.UCropContract
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailScreen(
    carId: Long,
    onNavigateBack: () -> Unit,
    onMoveToCollection: (Long, Long) -> Unit = { _, _ -> },
    fromWishlist: Boolean = false,
    viewModel: CarDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val conversionRate by viewModel.conversionRate.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditStorageDialog by remember { mutableStateOf(false) }
    var showEditPriceDialog by remember { mutableStateOf(false) }
    var showEditValueDialog by remember { mutableStateOf(false) }
    var showEditPurchaseDateDialog by remember { mutableStateOf(false) }
    var showEditNoteDialog by remember { mutableStateOf(false) }
    var showEditHwCardDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareCaption by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var isAddingAdditional by remember { mutableStateOf(false) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = UCropContract()
    ) { croppedUri ->
        if (croppedUri != null) {
            if (isAddingAdditional) {
                viewModel.addAdditionalPhoto(croppedUri.toString())
            } else {
                viewModel.onUserPhotoUrlChanged(croppedUri.toString())
            }
        }
    }

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
    
    var editValueText by remember { mutableStateOf("") }
    
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault()) }

    LaunchedEffect(carId) { viewModel.loadCar(carId) }
    LaunchedEffect(uiState.isDeleted) { if (uiState.isDeleted) onNavigateBack() }

    if (uiState.showVerificationDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissVerificationDialog,
            title = { Text(stringResource(R.string.share_verification_required_title)) },
            text = { Text(stringResource(R.string.share_verification_required_desc)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissVerificationDialog) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    if (uiState.showRulesDialog) {
        CommunityRulesDialog(
            onAccept = viewModel::acceptRules,
            onDismiss = viewModel::dismissRulesDialog
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.car?.masterData?.modelName ?: uiState.car?.manualModelName ?: stringResource(com.taytek.basehw.R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(com.taytek.basehw.R.string.back))
                    }
                },
                actions = {
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.car == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(com.taytek.basehw.R.string.car_not_found), color = MaterialTheme.colorScheme.error)
            }
            else -> {
                val car = uiState.car!!
                val master = car.masterData
                val brand = master?.brand ?: car.manualBrand
                val brandColor = brand?.toColor() ?: MaterialTheme.colorScheme.primary

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(8.dp))

                    // Hero section with Pager and Overlay Buttons
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        val car = uiState.car!!
                        val master = car.masterData
                        val allPhotos = remember(car.userPhotoUrl, car.additionalPhotos) {
                            mutableListOf<String>().apply {
                                car.userPhotoUrl?.let { if (it == master?.imageUrl) null else it }?.let { add(it) }
                                addAll(car.additionalPhotos.filter { it != master?.imageUrl })
                            }
                        }

                        if (allPhotos.isNotEmpty()) {
                            val pagerState = rememberPagerState(pageCount = { allPhotos.size })
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { viewModel.togglePhotoOptionMenu(true) }
                                ) {
                                    AsyncImage(
                                        model = allPhotos[page],
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                            
                            // Page indicator
                            if (allPhotos.size > 1) {
                                Row(
                                    Modifier
                                        .height(30.dp)
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    repeat(allPhotos.size) { iteration ->
                                        val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(color.copy(alpha = 0.7f))
                                                .size(6.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Placeholder if no photo
                            val baseColor = MaterialTheme.colorScheme.surfaceContainerLow
                            val darkerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(colors = listOf(baseColor, darkerColor)))
                                    .clickable { viewModel.togglePhotoOptionMenu(true) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.AddAPhoto,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        stringResource(com.taytek.basehw.R.string.add_photo),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        // Change Photo Camera Icon
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(40.dp)
                                .clickable { viewModel.togglePhotoOptionMenu(true) },
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = stringResource(com.taytek.basehw.R.string.change_photo),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Favorite Heart
                        IconButton(
                            onClick = { viewModel.toggleFavorite() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Icon(
                                imageVector = if (car.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(com.taytek.basehw.R.string.favorite),
                                tint = if (car.isFavorite) Color(0xFFFF4D6D) else Color.White
                            )
                        }
                    }


                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Brand chip + Premium/Regular tag
                        brand?.let { b ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(text = b.displayName) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = brandColor.copy(alpha = 0.15f),
                                        labelColor = brandColor
                                    )
                                )
                                val isPremium = master?.isPremium ?: car.manualIsPremium ?: false
                                val feature = master?.feature?.lowercase()
                                val isSth = feature == "sth"
                                val isChase = feature == "chase"
                                val isTh = feature == "th"
                                val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                                if (b == Brand.HOT_WHEELS) {
                                    val tierLabel = when {
                                        isSth -> "⭐ STH"
                                        isChase -> "CHASE"
                                        isTh -> "🔥 TH"
                                        isPremium -> "🏁 " + stringResource(com.taytek.basehw.R.string.premium)
                                        else -> stringResource(com.taytek.basehw.R.string.regular)
                                    }
                                    val tierColor = when {
                                        isSth -> Color(0xFFB8860B)
                                        isChase -> if (isDarkTheme) Color(0xFFF1F5F9) else Color.Black
                                        isTh -> Color(0xFF71797E)
                                        isPremium -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                text = tierLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = tierColor
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = tierColor.copy(alpha = 0.12f),
                                            labelColor = tierColor
                                        )
                                    )
                                }
                            }
                        }

                        // Model info
                        Text(master?.modelName ?: car.manualModelName ?: "", style = MaterialTheme.typography.headlineMedium)

                        // Details grid
                        master?.colNum?.takeIf { it.isNotBlank() }?.let {
                            DetailRow(stringResource(com.taytek.basehw.R.string.col_num), "#$it")
                        }
                        master?.toyNum?.takeIf { it.isNotBlank() }?.let {
                            DetailRow(stringResource(com.taytek.basehw.R.string.toy_num), it)
                        }
                        val displayScale = master?.scale?.takeIf { it.isNotBlank() } ?: car.manualScale?.takeIf { it.isNotBlank() } ?: "1:64"
                        DetailRow(stringResource(com.taytek.basehw.R.string.scale_label), displayScale)
                        val displayYear = master?.year ?: car.manualYear
                        displayYear?.let { DetailRow(stringResource(com.taytek.basehw.R.string.year_label), it.toString()) }
                        val displayColor = master?.color?.takeIf { it.isNotBlank() }
                        displayColor?.let {
                            DetailRow(stringResource(com.taytek.basehw.R.string.color_label), it)
                        }
                        val displaySeries = master?.series?.takeIf { it.isNotBlank() } ?: car.manualSeries
                        displaySeries?.takeIf { it.isNotBlank() }?.let {
                            DetailRow(stringResource(com.taytek.basehw.R.string.series_label), it)
                        }
                        val displaySeriesNum = master?.seriesNum?.takeIf { it.isNotBlank() } ?: car.manualSeriesNum
                        displaySeriesNum?.takeIf { it.isNotBlank() }?.let {
                            DetailRow(stringResource(com.taytek.basehw.R.string.series_num_label), it)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                        // User data
                        val conditionObj = car.condition
                        DetailRow(
                            label = stringResource(com.taytek.basehw.R.string.condition_label),
                            value = stringResource(conditionObj.titleRes),
                            valueColor = Color(conditionObj.hexColor)
                        )

                        if (HwCardTypeRules.showForUserCar(car)) {
                            val cardLabel = stringResource(com.taytek.basehw.R.string.hw_card_type_label)
                            val cardValue = when (car.hwCardType) {
                                HwCardType.SHORT -> stringResource(com.taytek.basehw.R.string.hw_card_short)
                                HwCardType.LONG -> stringResource(com.taytek.basehw.R.string.hw_card_long)
                                null -> stringResource(com.taytek.basehw.R.string.not_specified)
                            }
                            DetailRow(
                                label = cardLabel,
                                value = cardValue,
                                onEdit = { showEditHwCardDialog = true }
                            )
                        }
                        
                        if (car.isCustom) {
                            DetailRow(stringResource(com.taytek.basehw.R.string.custom_label), stringResource(com.taytek.basehw.R.string.ok))
                        }
                        
                        DetailRow(
                            label = stringResource(com.taytek.basehw.R.string.storage_label), 
                            value = car.storageLocation.ifBlank { stringResource(com.taytek.basehw.R.string.not_specified) },
                            onEdit = { 
                                editValueText = car.storageLocation
                                showEditStorageDialog = true 
                            }
                        )
                        
                        DetailRow(
                            label = stringResource(com.taytek.basehw.R.string.purchase_price_label), 
                            value = if (car.purchasePrice != null) {
                                val converted = car.purchasePrice * conversionRate
                                "$currencySymbol${"%.2f".format(converted)}"
                            } else stringResource(com.taytek.basehw.R.string.not_specified),
                            onEdit = { 
                                val converted = car.purchasePrice?.let { it * conversionRate }
                                editValueText = converted?.let { "%.2f".format(it) } ?: ""
                                showEditPriceDialog = true 
                            }
                        )
                        
                        DetailRow(
                            label = stringResource(com.taytek.basehw.R.string.estimated_value_label), 
                            value = if (car.estimatedValue != null) {
                                val converted = car.estimatedValue * conversionRate
                                "$currencySymbol${"%.2f".format(converted)}"
                            } else stringResource(com.taytek.basehw.R.string.not_specified),
                            onEdit = { 
                                val converted = car.estimatedValue?.let { it * conversionRate }
                                editValueText = converted?.let { "%.2f".format(it) } ?: ""
                                showEditValueDialog = true 
                            }
                        )
                        
                        DetailRow(
                            label = stringResource(com.taytek.basehw.R.string.purchase_date_label),
                            value = car.purchaseDate?.let(dateFormat::format) ?: stringResource(com.taytek.basehw.R.string.not_specified),
                            onEdit = { showEditPurchaseDateDialog = true }
                        )
                        
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(com.taytek.basehw.R.string.note_label), style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                               IconButton(onClick = { 
                                    editValueText = car.personalNote
                                    showEditNoteDialog = true 
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(com.taytek.basehw.R.string.edit), modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            val baseColorNote = MaterialTheme.colorScheme.surfaceContainerLow
                            val darkerColorNote = MaterialTheme.colorScheme.surfaceContainerHigh

                            Card(
                                Modifier.fillMaxWidth().clickable {
                                    editValueText = car.personalNote
                                    showEditNoteDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) AppTheme.tokens.cardBorderMuted else AppTheme.tokens.cardBorderStandard
                                ),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Brush.linearGradient(colors = listOf(baseColorNote, darkerColorNote)))
                                ) {
                                    Text(
                                        text = car.personalNote.ifBlank { stringResource(com.taytek.basehw.R.string.tap_to_add_note) }, 
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (car.personalNote.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        if (!fromWishlist && !displaySeries.isNullOrBlank() && (!uiState.isSeriesInWishlist || uiState.seriesJustAdded)) {
                            val successGreen = Color(0xFF4CAF50)
                            val shell = LocalThemeVariant.current
                            when {
                                uiState.seriesJustAdded -> {
                                    OutlinedButton(
                                        onClick = {},
                                        enabled = false,
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.5.dp, successGreen),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = successGreen,
                                            disabledContentColor = successGreen
                                        )
                                    ) {
                                        Text(
                                            stringResource(com.taytek.basehw.R.string.series_added_to_wanted),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                shell == ThemeVariant.Cyber -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .alpha(if (!uiState.isSavingSeries) 1f else 0.65f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(cyberWideNeonFillBrush())
                                            .clickable(
                                                enabled = !uiState.isSavingSeries,
                                                onClick = viewModel::addSeriesToWishlist
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            uiState.isSavingSeries -> {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(22.dp),
                                                    strokeWidth = 2.dp,
                                                    color = CyberKnockoutIconTint
                                                )
                                            }
                                            else -> {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = null,
                                                        tint = CyberKnockoutIconTint
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(
                                                        stringResource(com.taytek.basehw.R.string.add_series_to_wanted),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = CyberKnockoutIconTint
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                shell == ThemeVariant.NeonCyan -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .alpha(if (!uiState.isSavingSeries) 1f else 0.65f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(neonCyanActionGradientBrush())
                                            .clickable(
                                                enabled = !uiState.isSavingSeries,
                                                onClick = viewModel::addSeriesToWishlist
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            uiState.isSavingSeries -> {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(22.dp),
                                                    strokeWidth = 2.dp,
                                                    color = NeonCyanKnockoutIconTint
                                                )
                                            }
                                            else -> {
                                                Text(
                                                    stringResource(com.taytek.basehw.R.string.add_series_to_wanted),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = NeonCyanKnockoutIconTint
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    val outlineColor = MaterialTheme.colorScheme.primary
                                    OutlinedButton(
                                        onClick = viewModel::addSeriesToWishlist,
                                        enabled = !uiState.isSavingSeries,
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.5.dp, outlineColor),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = outlineColor,
                                            disabledContentColor = outlineColor.copy(alpha = 0.6f)
                                        )
                                    ) {
                                        when {
                                            uiState.isSavingSeries -> {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(22.dp),
                                                    strokeWidth = 2.dp,
                                                    color = outlineColor
                                                )
                                            }
                                            else -> {
                                                Text(
                                                    stringResource(com.taytek.basehw.R.string.add_series_to_wanted),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (car.isWishlist) {
                            when (LocalThemeVariant.current) {
                                ThemeVariant.Cyber -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(cyberWideNeonFillBrush())
                                            .clickable {
                                                onMoveToCollection(master?.id ?: -1L, car.id)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.AddHome,
                                                contentDescription = null,
                                                tint = CyberKnockoutIconTint
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                stringResource(com.taytek.basehw.R.string.add_to_collection_btn),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CyberKnockoutIconTint
                                            )
                                        }
                                    }
                                }
                                ThemeVariant.NeonCyan -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(neonCyanActionGradientBrush())
                                            .clickable {
                                                onMoveToCollection(master?.id ?: -1L, car.id)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.AddHome,
                                                contentDescription = null,
                                                tint = NeonCyanKnockoutIconTint
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                stringResource(com.taytek.basehw.R.string.add_to_collection_btn),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = NeonCyanKnockoutIconTint
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Button(
                                        onClick = {
                                            onMoveToCollection(master?.id ?: -1L, car.id)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.tokens.primaryAccent)
                                    ) {
                                        Icon(Icons.Default.AddHome, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(com.taytek.basehw.R.string.add_to_collection_btn),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(if (car.isWishlist) 8.dp else 16.dp))

                        // Share to Community button
                        when {
                            uiState.isShared -> {
                                OutlinedButton(
                                    onClick = {
                                        shareCaption = ""
                                        showShareDialog = true
                                    },
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.5.dp, Color(0xFF4CAF50)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF4CAF50),
                                        disabledContentColor = Color(0xFF4CAF50)
                                    )
                                ) {
                                    Text(
                                        stringResource(com.taytek.basehw.R.string.shared_successfully),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            LocalThemeVariant.current == ThemeVariant.Cyber && !uiState.isShared -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .alpha(if (!uiState.isSharing) 1f else 0.65f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(AppTheme.tokens.selectionIconTint)
                                        .padding(1.5.dp)
                                        .clip(RoundedCornerShape(12.5.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable(
                                            enabled = !uiState.isSharing,
                                            onClick = {
                                                shareCaption = ""
                                                showShareDialog = true
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        uiState.isSharing -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                strokeWidth = 2.dp,
                                                color = AppTheme.tokens.selectionIconTint
                                            )
                                        }
                                        else -> {
                                            Text(
                                                stringResource(com.taytek.basehw.R.string.share_to_community),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                val shareOutline = when (LocalThemeVariant.current) {
                                    ThemeVariant.NeonCyan -> AppTheme.tokens.selectionIconTint
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                OutlinedButton(
                                    onClick = {
                                        shareCaption = ""
                                        showShareDialog = true
                                    },
                                    enabled = !uiState.isSharing && !uiState.isShared,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.5.dp, shareOutline),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = shareOutline,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    when {
                                        uiState.isSharing -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                strokeWidth = 2.dp,
                                                color = shareOutline
                                            )
                                        }
                                        else -> {
                                            Text(
                                                stringResource(com.taytek.basehw.R.string.share_to_community),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        TextButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(com.taytek.basehw.R.string.delete_car), style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(com.taytek.basehw.R.string.delete_car_dialog_title)) },
            text = { Text(stringResource(com.taytek.basehw.R.string.delete_car_dialog_text)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.deleteCar() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(com.taytek.basehw.R.string.delete_car))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(com.taytek.basehw.R.string.cancel)) }
            }
        )
    }

    if (showEditHwCardDialog && uiState.car != null) {
        val carForCard = uiState.car!!
        AlertDialog(
            onDismissRequest = { showEditHwCardDialog = false },
            title = { Text(stringResource(com.taytek.basehw.R.string.hw_card_type_label)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HwCardType.entries.forEach { type ->
                        val label = when (type) {
                            HwCardType.SHORT -> stringResource(com.taytek.basehw.R.string.hw_card_short)
                            HwCardType.LONG -> stringResource(com.taytek.basehw.R.string.hw_card_long)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateHwCardType(type)
                                    showEditHwCardDialog = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = carForCard.hwCardType == type,
                                onClick = {
                                    viewModel.updateHwCardType(type)
                                    showEditHwCardDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEditHwCardDialog = false }) {
                    Text(stringResource(com.taytek.basehw.R.string.cancel))
                }
            }
        )
    }

    if (showEditStorageDialog) {
        AlertDialog(
            onDismissRequest = { showEditStorageDialog = false },
            title = { Text(stringResource(com.taytek.basehw.R.string.edit_storage_title)) },
            text = {
                OutlinedTextField(
                    value = editValueText,
                    onValueChange = { editValueText = it },
                    label = { Text(stringResource(com.taytek.basehw.R.string.storage_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.updateStorageLocation(editValueText)
                    showEditStorageDialog = false 
                }) { Text(stringResource(com.taytek.basehw.R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditStorageDialog = false }) { Text(stringResource(com.taytek.basehw.R.string.cancel)) }
            }
        )
    }

    if (showEditPriceDialog) {
        AlertDialog(
            onDismissRequest = { showEditPriceDialog = false },
            title = { Text(stringResource(com.taytek.basehw.R.string.edit_price_title)) },
            text = {
                OutlinedTextField(
                    value = editValueText,
                    onValueChange = { editValueText = it },
                    label = { Text("${stringResource(com.taytek.basehw.R.string.purchase_price_label)} ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.updatePurchasePrice(editValueText)
                    showEditPriceDialog = false 
                }) { Text(stringResource(com.taytek.basehw.R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditPriceDialog = false }) { Text(stringResource(com.taytek.basehw.R.string.cancel)) }
            }
        )
    }

    if (showEditValueDialog) {
        AlertDialog(
            onDismissRequest = { showEditValueDialog = false },
            title = { Text(stringResource(com.taytek.basehw.R.string.edit_value_title)) },
            text = {
                OutlinedTextField(
                    value = editValueText,
                    onValueChange = { editValueText = it },
                    label = { Text("${stringResource(com.taytek.basehw.R.string.estimated_value_label)} ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.updateEstimatedValue(editValueText)
                    showEditValueDialog = false 
                }) { Text(stringResource(com.taytek.basehw.R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditValueDialog = false }) { Text(stringResource(com.taytek.basehw.R.string.cancel)) }
            }
        )
    }

    if (showEditPurchaseDateDialog) {
        val initialDateMillis = uiState.car?.purchaseDate?.time ?: System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

        DatePickerDialog(
            onDismissRequest = { showEditPurchaseDateDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.updatePurchaseDate(Date(it))
                        }
                        showEditPurchaseDateDialog = false
                    }
                ) {
                    Text(stringResource(com.taytek.basehw.R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPurchaseDateDialog = false }) {
                    Text(stringResource(com.taytek.basehw.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEditNoteDialog) {
        AlertDialog(
            onDismissRequest = { showEditNoteDialog = false },
            title = { Text(stringResource(com.taytek.basehw.R.string.edit_note_title)) },
            text = {
                OutlinedTextField(
                    value = editValueText,
                    onValueChange = { editValueText = it },
                    label = { Text(stringResource(com.taytek.basehw.R.string.note_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.updatePersonalNote(editValueText)
                    showEditNoteDialog = false 
                }) { Text(stringResource(com.taytek.basehw.R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditNoteDialog = false }) { Text(stringResource(com.taytek.basehw.R.string.cancel)) }
            }
        )
    }

    // Share to Community dialog
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text(stringResource(com.taytek.basehw.R.string.share_to_community)) },
            text = {
                OutlinedTextField(
                    value = shareCaption,
                    onValueChange = { shareCaption = it },
                    placeholder = { Text(stringResource(com.taytek.basehw.R.string.share_caption_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                when (LocalThemeVariant.current) {
                    ThemeVariant.Cyber -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(cyberActionGradientBrush())
                                .clickable {
                                    viewModel.shareToFeed(shareCaption.trim())
                                    showShareDialog = false
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(com.taytek.basehw.R.string.share_button),
                                color = CyberKnockoutIconTint,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    ThemeVariant.NeonCyan -> {
                        Button(
                            onClick = {
                                viewModel.shareToFeed(shareCaption.trim())
                                showShareDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.tokens.primaryAccent,
                                contentColor = NeonCyanKnockoutIconTint
                            )
                        ) {
                            Text(stringResource(com.taytek.basehw.R.string.share_button))
                        }
                    }
                    else -> {
                        Button(
                            onClick = {
                                viewModel.shareToFeed(shareCaption.trim())
                                showShareDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.tokens.primaryAccent)
                        ) {
                            Text(stringResource(com.taytek.basehw.R.string.share_button))
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text(stringResource(com.taytek.basehw.R.string.cancel))
                }
            }
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
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(com.taytek.basehw.R.string.main_photo),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                ListItem(
                    headlineContent = { Text(stringResource(com.taytek.basehw.R.string.take_photo)) },
                    leadingContent = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                    modifier = Modifier.clickable { 
                        isAddingAdditional = false
                        permissionLauncher.launch(android.Manifest.permission.CAMERA) 
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(com.taytek.basehw.R.string.pick_from_gallery)) },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    modifier = Modifier.clickable { 
                        isAddingAdditional = false
                        galleryLauncher.launch("image/*") 
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = stringResource(com.taytek.basehw.R.string.additional_photos),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                ListItem(
                    headlineContent = { Text(stringResource(com.taytek.basehw.R.string.add_additional_photo)) },
                    leadingContent = { Icon(Icons.Default.AddAPhoto, contentDescription = null) },
                    modifier = Modifier.clickable { 
                        isAddingAdditional = true
                        galleryLauncher.launch("image/*") 
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(com.taytek.basehw.R.string.take_additional_photo)) },
                    leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    modifier = Modifier.clickable { 
                        isAddingAdditional = true
                        permissionLauncher.launch(android.Manifest.permission.CAMERA) 
                    }
                )
                
                if (uiState.car?.userPhotoUrl != null || (uiState.car?.additionalPhotos?.isNotEmpty() == true)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ListItem(
                        headlineContent = { Text(stringResource(com.taytek.basehw.R.string.remove_all_photos)) },
                        leadingContent = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable { 
                            viewModel.onUserPhotoUrlChanged(null)
                            // We can also clear additional photos here if we want
                            uiState.car?.additionalPhotos?.indices?.forEach { _ -> viewModel.removeAdditionalPhoto(0) }
                            viewModel.togglePhotoOptionMenu(false)
                        }
                    )
                }
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
                    onClick = { 
                        if (isAddingAdditional) {
                            viewModel.addAdditionalPhoto(urlText)
                        } else {
                            viewModel.onUserPhotoUrlChanged(urlText)
                        }
                    },
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

/** Synthwave: anasayfa kamera / wishlist “Koleksiyona ekle” ile aynı geniş neon dolgu. */
@Composable
@ReadOnlyComposable
private fun cyberWideNeonFillBrush(): Brush = Brush.linearGradient(
    colors = listOf(
        AppTheme.tokens.selectionIconTint,
        AppTheme.tokens.primaryAccent
    ),
    start = Offset(0f, 0f),
    end = Offset(1000f, 0f)
)

@Composable
private fun DetailRow(
    label: String, 
    value: String, 
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    onEdit: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                fontWeight = if (valueColor != MaterialTheme.colorScheme.onSurface) FontWeight.Bold else FontWeight.Normal
            )
            if (onEdit != null) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.Edit, 
                    contentDescription = stringResource(com.taytek.basehw.R.string.edit),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
    }
}
