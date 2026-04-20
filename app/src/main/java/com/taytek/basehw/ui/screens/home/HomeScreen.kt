package com.taytek.basehw.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.taytek.basehw.ui.theme.isNeonShellTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCarClick: (Long) -> Unit,
    onProfileClick: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
    onCameraClick: () -> Unit,
    onAddClick: () -> Unit = {},
    onViewAllClick: () -> Unit = {},
    onMasterCarClick: (Long) -> Unit = {},
    onCommunityClick: () -> Unit = {},
    onNewsClick: (String) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val newsItems by viewModel.newsItems.collectAsState()
    val recentlyAddedCars = viewModel.recentlyAddedCars.collectAsLazyPagingItems()
    val masterSearchResults = viewModel.masterSearchResults.collectAsLazyPagingItems()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val isSearching = searchQuery.isNotBlank()

    DisposableEffect(Unit) {
        onDispose { viewModel.clearSearchQuery() }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshUserData()
    }

    // Neon shell themes: gradient is drawn by [MainScreen]; keep this layer transparent.
    val bgModifier =
        if (isNeonShellTheme()) Modifier
        else Modifier.background(MaterialTheme.colorScheme.background)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(bgModifier)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Greeting
            item {
                FigmaHomeHeader(
                    userName = uiState.userName,
                    onProfileClick = onProfileClick,
                    onMenuClick = onMenuClick
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
                                    stringResource(R.string.search_brand_not_found_format, selectedBrand?.displayName ?: "")
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
                if (uiState.isCloudCheckInProgress) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    }
                }
                // 3. Stats Section
                item {
                    FigmaStatsSection(
                        totalCars = uiState.totalCars,
                        monthlyAdded = uiState.monthlyAdded,
                        wantedCount = uiState.wantedCount,
                        sthCount = uiState.sthCount,
                        totalValue = uiState.totalValue,
                        monthlyValueIncrease = uiState.monthlyValueIncrease,
                        currencySymbol = uiState.currencySymbol,
                        initialPagerPage = viewModel.getHomeStatsPagerInitialPage(),
                        onPagerPageChanged = viewModel::onHomeStatsPagerPageChanged
                    )
                }

                // 4. Diecast news (header + horizontal scroll)
                if (newsItems.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.home_news_section),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 0.dp,
                                bottom = 2.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(newsItems, key = { it.id }) { news ->
                                FigmaDiecastNewsCard(
                                    news = news,
                                    onClick = { onNewsClick(news.id) }
                                )
                            }
                        }
                    }
                }

                // 5. Recently Added header (equal vertical padding around title)
                item {
                    val recentlyHeaderV = if (newsItems.isNotEmpty()) 12.dp else 20.dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = recentlyHeaderV, bottom = recentlyHeaderV),
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

                // 6. Recently Added cards (horizontal scroll)
                item {
                    LazyRow(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 4.dp),
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

                if (!uiState.isCloudCheckInProgress && recentlyAddedCars.itemCount == 0) {
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

        // --- Restoration Prompt Dialog ---
        if (uiState.showRestorePrompt) {
            AlertDialog(
                onDismissRequest = viewModel::dismissRestorePrompt,
                title = { Text(stringResource(R.string.restore_prompt_title)) },
                text = { Text(stringResource(R.string.restore_prompt_desc)) },
                confirmButton = {
                    Button(
                        onClick = viewModel::restoreFromCloud,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringResource(R.string.restore_now))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissRestorePrompt) {
                        Text(stringResource(R.string.later))
                    }
                }
            )
        }

        // --- Restoration Loading Overlay ---
        if (uiState.isRestoring) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                    .clickable(enabled = false) {},
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.restoring_data),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
