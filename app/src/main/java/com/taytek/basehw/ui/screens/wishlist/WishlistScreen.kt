package com.taytek.basehw.ui.screens.wishlist

import androidx.compose.animation.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.ui.res.pluralStringResource
import coil.compose.AsyncImage
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.ui.components.*
import com.taytek.basehw.ui.screens.collection.SelectionHeader
import com.taytek.basehw.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WishlistScreen(
    onAddCarClick: () -> Unit,
    onCarClick: (Long) -> Unit,
    onAddCarWithMasterId: (Long, Long?) -> Unit = { _, _ -> },
    initialTab: Int = -1,
    onConsumeInitialTab: () -> Unit = {},
    viewModel: WishlistViewModel = hiltViewModel()
) {
    LaunchedEffect(initialTab) {
        if (initialTab == 1) {
            viewModel.setSeriesView(true)
            onConsumeInitialTab()
        } else if (initialTab == 0) {
            viewModel.setSeriesView(false)
            onConsumeInitialTab()
        }
    }
    val cars = viewModel.wishlistPaged.collectAsLazyPagingItems()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSeriesView by viewModel.isSeriesView.collectAsState()
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
    val isDarkTheme = MaterialTheme.colorScheme.background == DarkNavy

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

    val brands = listOf(
        Brand.HOT_WHEELS to com.taytek.basehw.R.drawable.hotwheels,
        Brand.MATCHBOX to com.taytek.basehw.R.drawable.matchbox,
        Brand.MINI_GT to if (isDarkTheme) com.taytek.basehw.R.drawable.minigtdark else com.taytek.basehw.R.drawable.minigt,
        Brand.MAJORETTE to com.taytek.basehw.R.drawable.majorette,
        Brand.GREENLIGHT to com.taytek.basehw.R.drawable.greenlight,
        Brand.SIKU to com.taytek.basehw.R.drawable.siku,
        Brand.KAIDO_HOUSE to com.taytek.basehw.R.drawable.kaido
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { scaffoldPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Header ──
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(com.taytek.basehw.R.string.search_screen_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

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
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 14.sp,
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
                        selectedTabIndex = if (isSeriesView) 1 else 0,
                        containerColor = Color.Transparent,
                        contentColor = AppPrimary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[if (isSeriesView) 1 else 0]),
                                color = AppPrimary
                            )
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = !isSeriesView,
                            onClick = { if (isSeriesView) viewModel.toggleView() },
                            text = { Text(stringResource(com.taytek.basehw.R.string.tab_models), fontWeight = if (!isSeriesView) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = isSeriesView,
                            onClick = { if (!isSeriesView) viewModel.toggleView() },
                            text = { Text(stringResource(com.taytek.basehw.R.string.tab_series), fontWeight = if (isSeriesView) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            if (!isSeriesView) {
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
                                color = AppPrimary
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
                                    isSelected = isSelected
                                )
                            }
                        }
                    }
                }

                if (cars.loadState.append is LoadState.Loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AppPrimary)
                        }
                    }
                }
            } else {
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
                }
            }
        }

        // Floating Selection Header
        AnimatedVisibility(
            visible = isSelectionMode,
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
    } // closes Scaffold
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeriesTrackingCard(
    series: com.taytek.basehw.domain.model.SeriesTracking,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onItemClick: (Long, Long?) -> Unit = { _, _ -> }
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = onSelect
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) 
                            Color.Transparent 
                        else 
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            )
            .shadow(elevation = if (isSelected) 0.dp else 1.dp, shape = RoundedCornerShape(16.dp), clip = true),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
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
                                    color = AppPrimary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.width(85.dp)
                                ) {
                                    Text(
                                        text = stringResource(com.taytek.basehw.R.string.status_wanted),
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppPrimary,
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
            tint = AppPrimary.copy(alpha = 0.3f)
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
