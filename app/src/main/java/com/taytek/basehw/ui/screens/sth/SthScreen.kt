package com.taytek.basehw.ui.screens.sth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.taytek.basehw.R
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.ui.theme.AppPrimary
import com.taytek.basehw.ui.theme.DarkNavy

private val SthGold = Color(0xFFE0B94C)
private val SthGoldDeep = Color(0xFFB68A2E)
private val SthCardBlue = Color(0xFF162947)
private val SthCardBlueAlt = Color(0xFF0F1F38)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SthScreen(
    onAddCarClick: () -> Unit,
    onCarClick: (Long) -> Unit,
    viewModel: SthViewModel = hiltViewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val sthCars = viewModel.sthCarsPaged.collectAsLazyPagingItems()
    val chaseCars = viewModel.chaseCarsPaged.collectAsLazyPagingItems()
    val thCars = viewModel.thCarsPaged.collectAsLazyPagingItems()
    
    val cars = when (currentTab) {
        SthViewModel.SthTab.STH -> sthCars
        SthViewModel.SthTab.CHASE -> chaseCars
        SthViewModel.SthTab.TH -> thCars
    }
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    var hasShownContent by rememberSaveable { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background == DarkNavy
    
    LaunchedEffect(cars.itemCount) {
        if (cars.itemCount > 0) hasShownContent = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = when (currentTab) {
                            SthViewModel.SthTab.STH -> stringResource(R.string.sth_title)
                            SthViewModel.SthTab.CHASE -> stringResource(R.string.chase_title)
                            SthViewModel.SthTab.TH -> stringResource(R.string.th_title)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(8.dp))

                    // ── Tabs ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(if (MaterialTheme.colorScheme.background == DarkNavy) 0.dp else 4.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (MaterialTheme.colorScheme.background == DarkNavy) SthCardBlue else MaterialTheme.colorScheme.primaryContainer)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SthTabButton(
                            text = "STH",
                            isSelected = currentTab == SthViewModel.SthTab.STH,
                            onClick = { viewModel.updateTab(SthViewModel.SthTab.STH) },
                            modifier = Modifier.weight(1f),
                            selectedColor = SthGold
                        )
                        SthTabButton(
                            text = "CHASE",
                            isSelected = currentTab == SthViewModel.SthTab.CHASE,
                            onClick = { viewModel.updateTab(SthViewModel.SthTab.CHASE) },
                            modifier = Modifier.weight(1f),
                            selectedColor = if (MaterialTheme.colorScheme.background == DarkNavy) Color.White else Color.Black
                        )
                        SthTabButton(
                            text = "TH",
                            isSelected = currentTab == SthViewModel.SthTab.TH,
                            onClick = { viewModel.updateTab(SthViewModel.SthTab.TH) },
                            modifier = Modifier.weight(1f),
                            selectedColor = if (MaterialTheme.colorScheme.background == DarkNavy) Color(0xFFE5E4E2) else Color(0xFF666666) // Darker gray
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    val searchBorderColor = when (currentTab) {
                        SthViewModel.SthTab.STH -> SthGold
                        SthViewModel.SthTab.CHASE -> if (MaterialTheme.colorScheme.background == DarkNavy) Color.Black else Color.Black
                        else -> if (MaterialTheme.colorScheme.background == DarkNavy) Color.Gray else Color(0xFF666666) // Dark Gray for TH
                    }
                    val placeholderColor = when (currentTab) {
                        SthViewModel.SthTab.STH -> SthGold.copy(alpha = 0.7f)
                        SthViewModel.SthTab.TH -> if (MaterialTheme.colorScheme.background == DarkNavy) Color(0xFFE5E4E2).copy(alpha = 0.7f) else Color(0xFF666666).copy(alpha = 0.8f)
                        else -> if (MaterialTheme.colorScheme.background == DarkNavy) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(if (MaterialTheme.colorScheme.background == DarkNavy) 8.dp else 4.dp, RoundedCornerShape(14.dp))
                            .border(if (MaterialTheme.colorScheme.background == DarkNavy) 2.dp else 1.dp, searchBorderColor, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (MaterialTheme.colorScheme.background == DarkNavy) SthCardBlueAlt else MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = when (currentTab) {
                                        SthViewModel.SthTab.STH -> stringResource(R.string.search_sth)
                                        SthViewModel.SthTab.CHASE -> stringResource(R.string.search_chase)
                                        SthViewModel.SthTab.TH -> stringResource(R.string.search_th)
                                    },
                                    color = placeholderColor
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search, 
                                    null, 
                                    tint = when (currentTab) {
                                        SthViewModel.SthTab.STH -> SthGold
                                        SthViewModel.SthTab.TH -> if (MaterialTheme.colorScheme.background == DarkNavy) Color(0xFFE5E4E2) else Color(0xFF666666)
                                        else -> if (MaterialTheme.colorScheme.background == DarkNavy) Color.White else Color.Black
                                    }
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Year Filter
                    val availableYears by viewModel.availableYears.collectAsState()
                    val selectedYear by viewModel.selectedYear.collectAsState()

                    if (availableYears.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                YearChip(
                                    yearText = stringResource(R.string.all),
                                    isSelected = selectedYear == null,
                                    onClick = { viewModel.updateSelectedYear(null) },
                                    isChase = currentTab == SthViewModel.SthTab.CHASE,
                                    isTh = currentTab == SthViewModel.SthTab.TH
                                )
                            }
                            items(availableYears.size) { index ->
                                val year = availableYears[index]
                                YearChip(
                                    yearText = year.toString(),
                                    isSelected = selectedYear == year,
                                    onClick = { viewModel.updateSelectedYear(year) },
                                    isChase = currentTab == SthViewModel.SthTab.CHASE,
                                    isTh = currentTab == SthViewModel.SthTab.TH
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // ── Cars List ──
            val initialLoading = cars.loadState.refresh is LoadState.Loading && cars.itemCount == 0 && !hasShownContent

            if (initialLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppPrimary)
                    }
                }
            } else if (cars.itemCount == 0 && cars.loadState.refresh is LoadState.NotLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = when (currentTab) {
                                SthViewModel.SthTab.STH -> stringResource(R.string.no_sth_found)
                                SthViewModel.SthTab.CHASE -> stringResource(R.string.no_chase_found)
                                SthViewModel.SthTab.TH -> stringResource(R.string.no_th_found)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(count = cars.itemCount, key = cars.itemKey { it.id }) { index ->
                    val masterData = cars[index]
                    if (masterData != null) {
                        SthCarItem(
                            masterData = masterData,
                            onClick = { onCarClick(masterData.id) },
                            isChase = currentTab == SthViewModel.SthTab.CHASE,
                            isTh = currentTab == SthViewModel.SthTab.TH
                        )
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
        }
    }
}

@Composable
private fun SthTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) selectedColor else {
                    if (MaterialTheme.colorScheme.background == DarkNavy) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                }
            )
        }
    }
}

@Composable
private fun SthCarItem(
    masterData: MasterData, 
    onClick: () -> Unit, 
    isChase: Boolean = false,
    isTh: Boolean = false
) {
    val borderColor = when {
        isChase -> Color.Black
        isTh -> if (MaterialTheme.colorScheme.background == DarkNavy) Color(0xFF71797E) else Color(0xFF666666)
        else -> SthGold
    }
    val isDark = MaterialTheme.colorScheme.background == DarkNavy
    val baseColor = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
    val darkerColor = if (isDark) Color(0xFF121416) else Color(0xFFE2E8F0)
    
    val bgColor = Brush.linearGradient(listOf(baseColor, darkerColor))
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .shadow(if (isDark) 8.dp else 4.dp, RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(60.dp)
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

        Spacer(Modifier.width(16.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = masterData.modelName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                masterData.year?.let {
                    Text(
                        text = it.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (masterData.series.isNotBlank()) {
                    Text(
                        text = masterData.series,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (masterData.colNum.isNotBlank() || masterData.toyNum.isNotBlank()) {
                    Text(
                        text = if (masterData.colNum.isNotBlank()) masterData.colNum else masterData.toyNum,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when {
                    isChase -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CHASE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    isTh -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE5E4E2))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TH",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    else -> { // STH
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppPrimary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "STH",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Show Case Badge if available (for any special edition)
                if (masterData.caseNum.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Gray.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.case_label, masterData.caseNum),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearChip(
    yearText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isChase: Boolean = false,
    isTh: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background == DarkNavy
    
    val borderColor = when {
        isChase -> Color.Black
        isTh -> Color.Gray
        else -> SthGold
    }
    val selectedBgColor = when {
        isChase -> if (!isDark) Color.Black else Color.White
        isTh -> Color(0xFFE5E4E2)
        else -> SthGoldDeep
    }
    val unselectedBgColor = if (isDark) SthCardBlue else MaterialTheme.colorScheme.surface
    val textColor = if (isSelected) {
        if (isChase && !isDark) Color.White else Color.Black
    } else {
        when {
            isChase -> if (isDark) Color.White else Color.Black
            isTh -> if (isDark) Color(0xFFE5E4E2) else Color(0xFF666666)
            else -> SthGold
        }
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) selectedBgColor else unselectedBgColor,
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Text(
            text = yearText,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
