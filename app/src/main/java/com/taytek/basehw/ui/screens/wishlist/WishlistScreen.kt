package com.taytek.basehw.ui.screens.wishlist

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.model.VariantHuntGroupSummary
import com.taytek.basehw.domain.model.VariantHuntMasterRow
import com.taytek.basehw.ui.components.WishlistVariantHuntRowActions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import kotlinx.coroutines.delay
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.ui.res.pluralStringResource
import coil.compose.AsyncImage
import com.taytek.basehw.ui.components.*
import com.taytek.basehw.ui.screens.collection.SelectionHeader
import com.taytek.basehw.ui.theme.*

/** Yıl — kısa tutulur; seri adına maksimum yer kalır. */
private val VARIANT_HUNT_COL_YEAR = 36.dp
/** Eski «No» sütunu kaldırıldı; genişlik buraya aktarıldı. */
private val VARIANT_HUNT_COL_SERIES = 172.dp
/** Durum metni küçük punto ile sığar. */
private val VARIANT_HUNT_COL_STATUS = 86.dp
private val VARIANT_HUNT_TABLE_TOTAL_WIDTH =
    VARIANT_HUNT_COL_YEAR + VARIANT_HUNT_COL_SERIES + VARIANT_HUNT_COL_STATUS

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WishlistScreen(
    onAddCarClick: () -> Unit,
    onCarClick: (Long) -> Unit,
    onAddCarWithMasterId: (Long, Long?) -> Unit = { _, _ -> },
    initialTab: Int = -1,
    onConsumeInitialTab: () -> Unit = {},
    contentPadding: androidx.compose.foundation.layout.PaddingValues? = null,
    viewModel: WishlistViewModel = hiltViewModel()
) {
    val systemNavBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LaunchedEffect(initialTab) {
        if (initialTab in 0..2) {
            viewModel.applyInitialWishlistTab(initialTab)
            onConsumeInitialTab()
        }
    }
    val cars = viewModel.wishlistPaged.collectAsLazyPagingItems()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val subTab by viewModel.subTab.collectAsState()
    val seriesTracking by viewModel.seriesTracking.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedCarIds by viewModel.selectedCarIds.collectAsState()
    val selectedSeriesKeys by viewModel.selectedSeriesKeys.collectAsState()
    val selectionCount by viewModel.selectionCount.collectAsState()
    val selectionText = if (selectedSeriesKeys.isNotEmpty()) {
        pluralStringResource(
            id = com.taytek.basehw.R.plurals.series_selected,
            count = selectedSeriesKeys.size,
            selectedSeriesKeys.size
        )
    } else {
        pluralStringResource(
            id = com.taytek.basehw.R.plurals.cars_selected,
            count = selectedCarIds.size,
            selectedCarIds.size
        )
    }
    val isDarkTheme = isDarkThemeUi()

    val filteredSeriesTracking = remember(seriesTracking, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            seriesTracking
        } else {
            seriesTracking.filter { series ->
                series.seriesName.contains(q, ignoreCase = true) ||
                    series.brand.displayName.contains(q, ignoreCase = true) ||
                    series.items.any { item -> item.masterData.modelName.contains(q, ignoreCase = true) }
            }
        }
    }

    val variantHuntGroups by viewModel.variantHuntGroups.collectAsState()
    val expandedHuntId by viewModel.expandedVariantHuntGroupId.collectAsState()
    val expandedHuntRows by viewModel.expandedVariantHuntRows.collectAsState()

    val filteredVariantHuntGroups = remember(variantHuntGroups, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) variantHuntGroups
        else {
            variantHuntGroups.filter { g ->
                val brandLabel = try {
                    Brand.valueOf(g.brandCode).displayName
                } catch (_: Exception) {
                    g.brandCode
                }
                g.title.contains(q, ignoreCase = true) ||
                    g.keywords.any { it.contains(q, ignoreCase = true) } ||
                    brandLabel.contains(q, ignoreCase = true)
            }
        }
    }

    var variantHuntSeedCar by remember { mutableStateOf<com.taytek.basehw.domain.model.UserCar?>(null) }

    LaunchedEffect(subTab) {
        if (subTab == WishlistSubTab.VariantHunt) {
            viewModel.refreshVariantHuntCompletion()
        }
    }

    val brands = listOf(
        Brand.HOT_WHEELS to com.taytek.basehw.R.drawable.hotwheels,
        Brand.MATCHBOX to com.taytek.basehw.R.drawable.matchbox,
        Brand.MINI_GT to if (isDarkTheme) com.taytek.basehw.R.drawable.minigtdark else com.taytek.basehw.R.drawable.minigt,
        Brand.MAJORETTE to com.taytek.basehw.R.drawable.majorette,
        Brand.GREENLIGHT to com.taytek.basehw.R.drawable.greenlight,
        Brand.SIKU to com.taytek.basehw.R.drawable.siku,
        Brand.KAIDO_HOUSE to com.taytek.basehw.R.drawable.kaido
    )

    val listPadding = contentPadding?.let { cp ->
        PaddingValues(
            start = cp.calculateStartPadding(LayoutDirection.Ltr),
            end = cp.calculateEndPadding(LayoutDirection.Ltr),
            top = 0.dp,
            bottom = cp.calculateBottomPadding() + 56.dp
        )
    } ?: PaddingValues(bottom = 64.dp)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = systemNavBottom)
            .background(cyberRootBackgroundColor())
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = listPadding
        ) {
            // ── Header ──
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Arama Çubuğu
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(3.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isDarkTheme)
                                    MaterialTheme.colorScheme.surface
                                else
                                    MaterialTheme.colorScheme.primaryContainer
                            )
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            decorationBox = { innerTextField ->
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Box(Modifier.weight(1f)) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = stringResource(com.taytek.basehw.R.string.search_in_collection),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 14.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }

                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }, modifier = Modifier.size(32.dp)) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = stringResource(com.taytek.basehw.R.string.clear),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Trend Markalar (arama kutusunun altında)
                    Text(
                        text = stringResource(com.taytek.basehw.R.string.trend_brands),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        brands.take(3).forEach { (brand, resId) ->
                            BrandLogoCard(
                                brand = brand,
                                drawableRes = resId,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Spacer(Modifier.weight(0.5f))
                        brands.drop(3).forEach { (brand, resId) ->
                            BrandLogoCard(
                                brand = brand,
                                drawableRes = resId,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            )
                        }
                        Spacer(Modifier.weight(0.5f))
                    }
                    Spacer(Modifier.height(12.dp))

                    // View Toggle
                    TabRow(
                        selectedTabIndex = subTab.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = AppTheme.tokens.primaryAccent,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[subTab.ordinal]),
                                color = AppTheme.tokens.primaryAccent
                            )
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = subTab == WishlistSubTab.Models,
                            onClick = { viewModel.selectSubTab(WishlistSubTab.Models) },
                            text = {
                                Text(
                                    stringResource(com.taytek.basehw.R.string.tab_models),
                                    fontWeight = if (subTab == WishlistSubTab.Models) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = subTab == WishlistSubTab.Series,
                            onClick = { viewModel.selectSubTab(WishlistSubTab.Series) },
                            text = {
                                Text(
                                    stringResource(com.taytek.basehw.R.string.tab_series),
                                    fontWeight = if (subTab == WishlistSubTab.Series) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = subTab == WishlistSubTab.VariantHunt,
                            onClick = { viewModel.selectSubTab(WishlistSubTab.VariantHunt) },
                            text = {
                                Text(
                                    stringResource(com.taytek.basehw.R.string.tab_variant_hunt),
                                    fontWeight = if (subTab == WishlistSubTab.VariantHunt) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            if (subTab == WishlistSubTab.Models) {
                // ── Aranan Modellerim ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(com.taytek.basehw.R.string.wishlist_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (cars.itemCount > 0) {
                            Text(
                                text = stringResource(com.taytek.basehw.R.string.car_count, cars.itemCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = AppTheme.tokens.primaryAccent
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                when {
                    cars.itemCount == 0 -> {
                        item {
                            EmptyDiscoveryPlaceholder(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                onAddClick = onAddCarClick
                            )
                        }
                    }
                    else -> {
                        items(count = cars.itemCount, key = cars.itemKey { it.id }) { index ->
                            val car = cars[index]
                            if (car != null) {
                                val isSelected = selectedCarIds.contains(car.id)
                                CollectionListItem(
                                    car = car,
                                    onClick = {
                                        if (isSelectionMode) viewModel.toggleCarSelection(car.id)
                                        else onCarClick(car.id)
                                    },
                                    onLongClick = { viewModel.toggleCarSelection(car.id) },
                                    isSelected = isSelected,
                                    wishlistVariantHunt = if (car.masterDataId != null) {
                                        WishlistVariantHuntRowActions(
                                            onOpenVariantHunt = { variantHuntSeedCar = car }
                                        )
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                    }
                }

                if (cars.loadState.append is LoadState.Loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AppTheme.tokens.primaryAccent)
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            } else if (subTab == WishlistSubTab.Series) {
                // ── Seri Bazlı Takip ──
                if (filteredSeriesTracking.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    stringResource(com.taytek.basehw.R.string.no_results_for, searchQuery)
                                } else {
                                    stringResource(com.taytek.basehw.R.string.empty_series_tracking)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredSeriesTracking) { series ->
                        val key = series.brand.name to series.seriesName
                        val isSelected = selectedSeriesKeys.contains(key)
                        SeriesTrackingCard(
                            series = series,
                            isSelected = isSelected,
                            onSelect = { viewModel.toggleSeriesSelection(series.brand, series.seriesName) },
                            onItemClick = onAddCarWithMasterId
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            } else {
                // ── Varyant avı ──
                item {
                    Text(
                        text = stringResource(com.taytek.basehw.R.string.variant_hunt_section_title),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (filteredVariantHuntGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    stringResource(com.taytek.basehw.R.string.no_results_for, searchQuery)
                                } else {
                                    stringResource(com.taytek.basehw.R.string.variant_hunt_empty_groups)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(
                        items = filteredVariantHuntGroups,
                        key = { it.id }
                    ) { group ->
                        Column(Modifier.fillMaxWidth()) {
                            VariantHuntGroupCard(
                                group = group,
                                expanded = expandedHuntId == group.id,
                                rows = if (expandedHuntId == group.id) expandedHuntRows else emptyList(),
                                onExpandToggle = {
                                    viewModel.setVariantHuntExpandedGroup(
                                        if (expandedHuntId == group.id) null else group.id
                                    )
                                },
                                onDelete = { viewModel.deleteVariantHuntGroup(group.id) }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        variantHuntSeedCar?.let { seed ->
            VariantHuntConfirmDialog(
                seedCar = seed,
                viewModel = viewModel,
                onDismiss = { variantHuntSeedCar = null }
            )
        }

        // Floating Selection Header
        AnimatedVisibility(
            visible = isSelectionMode && subTab != WishlistSubTab.VariantHunt,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            ) {
                SelectionHeader(
                    selectedCount = selectionCount,
                    selectionText = selectionText,
                    onClearSelection = { viewModel.clearSelection() },
                    onDeleteSelected = { viewModel.deleteSelected() }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VariantHuntGroupCard(
    group: VariantHuntGroupSummary,
    expanded: Boolean,
    rows: List<VariantHuntMasterRow>,
    onExpandToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember(group.id) { mutableStateOf(false) }
    val brandDisplay = try {
        Brand.valueOf(group.brandCode).displayName
    } catch (_: Exception) {
        group.brandCode
    }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(stringResource(com.taytek.basehw.R.string.variant_hunt_delete_confirm_title))
            },
            text = {
                Text(stringResource(com.taytek.basehw.R.string.variant_hunt_delete_confirm_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        confirmDelete = false
                    }
                ) {
                    Text(stringResource(com.taytek.basehw.R.string.delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(com.taytek.basehw.R.string.cancel))
                }
            }
        )
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .combinedClickable(
                onClick = { },
                onLongClick = { confirmDelete = true }
            ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isDark) AppTheme.tokens.cardBorderMuted else AppTheme.tokens.cardBorderStandard
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${brandDisplay.uppercase()} · ${group.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (group.keywords.isNotEmpty()) {
                        Text(
                            text = group.keywords.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onExpandToggle) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }
            if (expanded && rows.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    // Sabit toplam genişlik: fillMaxWidth yok — aksi halde satır gereksiz genişleyip boşluk oluşuyor.
                    Column(Modifier.width(VARIANT_HUNT_TABLE_TOTAL_WIDTH)) {
                        Row(
                            Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(com.taytek.basehw.R.string.table_header_year),
                                Modifier.width(VARIANT_HUNT_COL_YEAR),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(com.taytek.basehw.R.string.table_header_series),
                                Modifier.width(VARIANT_HUNT_COL_SERIES),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(com.taytek.basehw.R.string.table_header_status),
                                Modifier.width(VARIANT_HUNT_COL_STATUS),
                                textAlign = TextAlign.End,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        rows.forEach { row ->
                            Row(
                                Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    row.year?.toString() ?: "—",
                                    Modifier
                                        .width(VARIANT_HUNT_COL_YEAR)
                                        .padding(top = 2.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Column(Modifier.width(VARIANT_HUNT_COL_SERIES)) {
                                    Text(
                                        text = row.series,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp)
                                    )
                                    if (row.seriesNum.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = row.seriesNum,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                lineHeight = 12.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Box(
                                    Modifier
                                        .width(VARIANT_HUNT_COL_STATUS)
                                        .padding(top = 2.dp),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    Text(
                                        text = if (row.inCollection) {
                                            stringResource(com.taytek.basehw.R.string.status_in_collection)
                                        } else {
                                            stringResource(com.taytek.basehw.R.string.status_wanted)
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            lineHeight = 9.sp
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = if (row.inCollection) Color(0xFF4CAF50) else AppTheme.tokens.primaryAccent,
                                        textAlign = TextAlign.End,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VariantHuntConfirmDialog(
    seedCar: UserCar,
    viewModel: WishlistViewModel,
    onDismiss: () -> Unit
) {
    val masterId = seedCar.masterDataId
    val brandCode = seedCar.masterData?.brand?.name
    if (masterId == null || brandCode == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    var keywordText by remember(masterId) { mutableStateOf("") }
    var matchCount by remember { mutableIntStateOf(0) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val errGeneric = stringResource(com.taytek.basehw.R.string.variant_hunt_error_generic)
    val errTooMany = stringResource(com.taytek.basehw.R.string.variant_hunt_too_many_matches)
    val errNoMatches = stringResource(com.taytek.basehw.R.string.variant_hunt_no_matches)
    val errKeywords = stringResource(com.taytek.basehw.R.string.variant_hunt_keywords_empty)

    fun parseKeywords(raw: String): List<String> =
        raw.split(Regex("[,;\\s]+")).map { it.trim() }.filter { it.isNotEmpty() }

    LaunchedEffect(masterId) {
        val suggested = viewModel.proposeVariantHuntKeywords(masterId)
        keywordText = suggested.joinToString(", ")
    }

    LaunchedEffect(brandCode, keywordText) {
        delay(280)
        val words = parseKeywords(keywordText)
        matchCount = if (words.isEmpty()) 0 else viewModel.countVariantHuntMatches(brandCode, words)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(com.taytek.basehw.R.string.variant_hunt_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = keywordText,
                    onValueChange = {
                        keywordText = it
                        errorText = null
                    },
                    label = { Text(stringResource(com.taytek.basehw.R.string.variant_hunt_keywords_label)) },
                    placeholder = { Text(stringResource(com.taytek.basehw.R.string.variant_hunt_keywords_hint)) },
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                )
                Text(
                    text = stringResource(com.taytek.basehw.R.string.variant_hunt_match_count, matchCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                errorText?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val words = parseKeywords(keywordText)
                    if (words.isEmpty()) {
                        errorText = errKeywords
                        return@TextButton
                    }
                    viewModel.createVariantHunt(masterId, seedCar.id, words) { r ->
                        if (r.isSuccess) {
                            onDismiss()
                        } else {
                            errorText = when (r.exceptionOrNull()?.message) {
                                "too_many" -> errTooMany
                                "no_matches" -> errNoMatches
                                "keywords_empty" -> errKeywords
                                "seed_missing" -> errGeneric
                                else -> errGeneric
                            }
                        }
                    }
                },
                enabled = matchCount in 1..500
            ) {
                Text(stringResource(com.taytek.basehw.R.string.variant_hunt_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(com.taytek.basehw.R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeriesTrackingCard(
    series: com.taytek.basehw.domain.model.SeriesTracking,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onItemClick: (Long, Long?) -> Unit = { _, _ -> }
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val baseColor = MaterialTheme.colorScheme.surfaceContainerLow
    val darkerColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = onSelect
            )
            .shadow(elevation = if (isSelected) 0.dp else 1.dp, shape = RoundedCornerShape(16.dp), clip = true),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) null else BorderStroke(
            1.dp,
            if (isDark) AppTheme.tokens.cardBorderMuted else AppTheme.tokens.cardBorderStandard
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (!isSelected) Modifier.background(Brush.linearGradient(colors = listOf(baseColor, darkerColor)))
                    else Modifier
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = series.seriesName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = series.brand.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // "Table" Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(com.taytek.basehw.R.string.table_header_model),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(com.taytek.basehw.R.string.table_header_no),
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(com.taytek.basehw.R.string.table_header_status),
                    modifier = Modifier.width(100.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            
            series.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !item.isInCollection) {
                            onItemClick(item.masterData.id, item.wishlistId)
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.masterData.modelName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.masterData.seriesNum,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Box(
                        modifier = Modifier.width(100.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        when {
                            item.isInCollection -> {
                                Surface(
                                    color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.width(85.dp)
                                ) {
                                    Text(
                                        text = stringResource(com.taytek.basehw.R.string.status_in_collection),
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            item.isInWishlist -> {
                                Surface(
                                    color = AppTheme.tokens.primaryAccent.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.width(85.dp)
                                ) {
                                    Text(
                                        text = stringResource(com.taytek.basehw.R.string.status_wanted),
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppTheme.tokens.primaryAccent,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = "-",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}


@Composable
private fun BrandLogoCard(
    brand: Brand,
    drawableRes: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(65.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = drawableRes),
            contentDescription = brand.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun EmptyDiscoveryPlaceholder(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = AppTheme.tokens.primaryAccent.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(com.taytek.basehw.R.string.empty_wishlist_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(com.taytek.basehw.R.string.empty_wishlist_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
