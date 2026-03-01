package com.taytek.basehw.ui.screens.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    onSettingsClick: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val cars = viewModel.carsPaged.collectAsLazyPagingItems()
    var selectedFilter by remember { mutableStateOf("Kutulu") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. Header
            item {
                HomeHeader(
                    totalModels = cars.itemCount,
                    onProfileClick = onSettingsClick
                )
            }

            // 2. Featured Card (Using first item as featured for demo, or dummy if empty)
            item {
                if (cars.itemCount > 0) {
                    val firstCar = cars[0]
                    if (firstCar != null) {
                        FeaturedCarCard(
                            car = firstCar,
                            onClick = { onCarClick(firstCar.id) }
                        )
                    }
                } else {
                    // Dummy Featured Card for Preview
                    FeaturedCarCard(
                        car = UserCar(id = 0, masterDataId = 0L, isOpened = false), // Fixed dummy
                        onClick = onAddCarClick
                    )
                }
            }

            // 3. Filter Row
            item {
                FilterChipsRow(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it }
                )
            }

            // 4. Collection List
            when {
                cars.loadState.refresh is LoadState.Loading -> {
                    item {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppPrimary)
                        }
                    }
                }

                cars.itemCount == 0 && cars.loadState.refresh is LoadState.NotLoading -> {
                    item {
                        EmptyCollectionPlaceholder(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                else -> {
                    items(
                        count = cars.itemCount,
                        key = cars.itemKey { it.id }
                    ) { index ->
                        val car = cars[index]
                        if (car != null) {
                            CollectionListItem(
                                car = car,
                                onClick = { onCarClick(car.id) }
                            )
                        }
                    }
                }
            }

            // Append loading
            if (cars.loadState.append is LoadState.Loading) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AppPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCollectionPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = AppPrimary.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Koleksiyonunuz Boş",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
        )
        Text(
            text = "Henüz model eklememişsiniz.\nOrtadaki + butonuna basarak başlayın!",
            style = MaterialTheme.typography.bodyMedium,
            color = AppTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
