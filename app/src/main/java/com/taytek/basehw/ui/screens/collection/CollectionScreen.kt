package com.taytek.basehw.ui.screens.collection

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.ui.components.*
import com.taytek.basehw.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onAddCarClick: () -> Unit,
    onCarClick: (Long) -> Unit,
    onStatisticsClick: () -> Unit = {},
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val cars = viewModel.carsPaged.collectAsLazyPagingItems()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedSeries by viewModel.selectedSeries.collectAsState()
    val selectedIsOpened by viewModel.selectedIsOpened.collectAsState()
    val selectedSortOrder by viewModel.sortOrder.collectAsState()

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(false) }

    val columns = if (isGridView) 3 else 1

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    SelectionHeader(
                        selectedCount = selectedIds.size,
                        onClearSelection = { viewModel.clearSelection() },
                        onDeleteSelected = { viewModel.deleteSelected() }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = if (isGridView)
                    PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 100.dp)
                else
                    PaddingValues(bottom = 100.dp),
                horizontalArrangement = if (isGridView) Arrangement.spacedBy(6.dp) else Arrangement.Start,
                verticalArrangement = if (isGridView) Arrangement.spacedBy(6.dp) else Arrangement.Top
            ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(com.taytek.basehw.R.string.nav_home),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(com.taytek.basehw.R.string.collection_screen_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    SearchBarWithFilter(
                        query = searchQuery ?: "",
                        onQueryChange = viewModel::updateSearchQuery,
                        onFilterClick = { showFilterSheet = true },
                        onStatsClick = onStatisticsClick,
                        contentPadding = PaddingValues(0.dp)
                    )
                }
            }

            // 4. Cars
            when {
                cars.itemCount == 0 -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        val configuration = LocalConfiguration.current
                        val screenHeight = configuration.screenHeightDp.dp
                        // Approximate height of Hero Card (200dp) + Search Bar (80dp) + Header (60dp) + Bottom Nav (100dp)
                        val occupiedHeight = 440.dp 
                        val minHeight = if (screenHeight > occupiedHeight) screenHeight - occupiedHeight else 300.dp

                        EmptyCollectionPlaceholder(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = minHeight),
                            onAddClick = onAddCarClick
                        )
                    }
                }

                else -> {
                    items(
                        count = cars.itemCount,
                        span = { if (isGridView) GridItemSpan(1) else GridItemSpan(maxLineSpan) },
                        key = cars.itemKey { it.id }
                    ) { index ->
                        val car = cars[index]
                        if (car != null) {
                            val isSelected = selectedIds.contains(car.id)
                            if (isGridView) {
                                GalleryGridItem(
                                    car = car,
                                    onClick = { 
                                        if (isSelectionMode) viewModel.toggleSelection(car.id)
                                        else onCarClick(car.id) 
                                    },
                                    onLongClick = { viewModel.toggleSelection(car.id) },
                                    isSelected = isSelected,
                                    modifier = Modifier.padding(2.dp)
                                )
                            } else {
                                CollectionListItem(
                                    car = car,
                                    onClick = { 
                                        if (isSelectionMode) viewModel.toggleSelection(car.id)
                                        else onCarClick(car.id) 
                                    },
                                    onLongClick = { viewModel.toggleSelection(car.id) },
                                    isSelected = isSelected
                                )
                            }
                        }
                    }
                }
            }

            // 5. Append loading
            if (cars.loadState.append is LoadState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AppPrimary)
                    }
                }
            }
        }

        if (showFilterSheet) {
            FilterBottomSheet(
                onDismiss = { showFilterSheet = false },
                currentBrand = selectedBrand,
                currentYear = selectedYear,
                currentSeries = selectedSeries,
                currentIsOpened = selectedIsOpened,
                currentSortOrder = selectedSortOrder,
                onApply = { b, y, s, o, sort ->
                    viewModel.updateFilters(b, y, s, o, sort)
                    showFilterSheet = false
                }
            )
        }
        }
    }
}

@Composable
fun SelectionHeader(
    selectedCount: Int,
    selectionText: String? = null,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.AddCircle, contentDescription = "Cancel", modifier = Modifier.size(28.dp).rotate(45f)) // Using AddCircle rotated as X
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = selectionText
                    ?: pluralStringResource(id = com.taytek.basehw.R.plurals.cars_selected, count = selectedCount, selectedCount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }

        IconButton(onClick = onDeleteSelected) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun EmptyCollectionPlaceholder(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        ) {

            Text(
                text = stringResource(com.taytek.basehw.R.string.empty_collection_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(com.taytek.basehw.R.string.empty_collection_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

        }
    }
}
