package com.taytek.basehw.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.taytek.basehw.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCarClick: (Long) -> Unit,
    onProfileClick: () -> Unit,
    onCameraClick: () -> Unit,
    onAddClick: () -> Unit = {},
    onViewAllClick: () -> Unit = {},
    onMasterCarClick: (Long) -> Unit = {},
    onCommunityClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentlyAddedCars = viewModel.recentlyAddedCars.collectAsLazyPagingItems()
    val masterSearchResults = viewModel.masterSearchResults.collectAsLazyPagingItems()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val isSearching = searchQuery.isNotBlank()

    DisposableEffect(Unit) {
        onDispose { viewModel.clearSearchQuery() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. Greeting
            item {
                FigmaHomeHeader(
                    userName = uiState.userName,
                    onProfileClick = onProfileClick
                )
            }

            // 2. Search Bar
            item {
                FigmaSearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    onClearQuery = viewModel::clearSearchQuery,
                    selectedBrand = selectedBrand,
                    onBrandSelected = viewModel::updateSelectedBrand,
                    onClearBrandFilter = { viewModel.updateSelectedBrand(null) },
                    onCameraClick = onCameraClick,
                    onAddClick = onAddClick
                )
            }

            if (isSearching) {
                // — Master Catalog Search (same system as AddCar screen) —
                if (masterSearchResults.itemCount == 0) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    stringResource(R.string.no_results_for, searchQuery)
                                } else {
                                    "${selectedBrand?.displayName ?: ""} için sonuç bulunamadı"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(32.dp)
                            )
                        }
                    }
                } else {
                    items(masterSearchResults.itemCount) { index ->
                        val item = masterSearchResults[index]
                        if (item != null) {
                            HomeSuggestionItem(
                                masterData = item,
                                onClick = { onMasterCarClick(item.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            } else {
                // 3. Stats Section
                item {
                    FigmaStatsSection(
                        totalCars = uiState.totalCars,
                        monthlyAdded = uiState.monthlyAdded,
                        wantedCount = uiState.wantedCount,
                        sthCount = uiState.sthCount,
                        totalValue = uiState.totalValue,
                        monthlyValueIncrease = uiState.monthlyValueIncrease,
                        currencySymbol = uiState.currencySymbol
                    )
                }

                // 4. Recently Added Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.recently_added),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
// View All text removed
                    }
                }

                // 5. Recently Added Cards (horizontal scroll)
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentlyAddedCars.itemCount) { index ->
                            val car = recentlyAddedCars[index]
                            if (car != null) {
                                FigmaRecentlyAddedVerticalCard(
                                    car = car,
                                    onClick = { onCarClick(car.id) }
                                )
                            }
                        }
                    }
                }

                // Empty state
                if (recentlyAddedCars.itemCount == 0) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.empty_collection_desc),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
