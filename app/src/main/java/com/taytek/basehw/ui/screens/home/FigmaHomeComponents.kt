package com.taytek.basehw.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import com.taytek.basehw.R
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.DiecastNews
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.ui.theme.AppTheme
import com.taytek.basehw.ui.theme.CyberKnockoutIconTint
import com.taytek.basehw.ui.theme.cyberActionGradientBrush
import com.taytek.basehw.ui.theme.CyberNeonIconButton
import com.taytek.basehw.ui.theme.CyberNeonSquareButton
import com.taytek.basehw.ui.theme.LocalThemeVariant
import com.taytek.basehw.ui.theme.NeonCyanNeonIconButton
import com.taytek.basehw.ui.theme.NeonCyanNeonSquareButton
import com.taytek.basehw.ui.theme.NeonCyanKnockoutIconTint
import com.taytek.basehw.ui.theme.ThemeVariant
import com.taytek.basehw.ui.theme.neonCyanActionGradientBrush
import com.taytek.basehw.ui.theme.isDarkThemeUi



@Composable
fun FigmaHomeHeader(
    userName: String,
    onProfileClick: () -> Unit = {},
    onMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (onMenuClick != null) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.nav_open_menu),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Text(
            text = stringResource(R.string.hello_name_template, userName),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun FigmaSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    selectedBrand: Brand?,
    onBrandSelected: (Brand?) -> Unit,
    onClearBrandFilter: () -> Unit,
    onCameraClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBrandMenu by remember { mutableStateOf(false) }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isDark = isDarkThemeUi()
    val placeholderFontSize = (screenWidthDp * 0.034f).coerceIn(11f, 14f).sp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Search Box Content
        Row(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp))
                .background(
                    if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(16.dp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Icon (now with orange background box if needed, but user said "arka planı turuncu olsun" for search icon?
                        // "arama ıkonunu grı yap" was the previous request. Now: "araba ekleme ıkonu arka planı turuncu olsun kamera ıkonunun kendısı turuncu arka planı seffaf olsun"
                        // Wait, "arama ıkonu arka planı turuncu olsun"?
                        // User said: "arama ıkonu arka planı turuncu olsun kamera ıkonunun kendısı turuncu arka planı seffaf olsun"
                        // I will assume "arama ıkonu" means the "Search" (magnifying glass) icon.
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_in_collection),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = placeholderFontSize
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                        if (query.isNotBlank()) {
                            IconButton(onClick = onClearQuery, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.content_desc_clear),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        // Filter Dropdown inside
                        Box {
                            IconButton(
                                onClick = { showBrandMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.filter),
                                    tint = if (selectedBrand == null) MaterialTheme.colorScheme.onSurfaceVariant else AppTheme.tokens.primaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showBrandMenu,
                                onDismissRequest = { showBrandMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.clear)) },
                                    enabled = selectedBrand != null,
                                    onClick = {
                                        onClearBrandFilter()
                                        showBrandMenu = false
                                    }
                                )
                                HorizontalDivider()
                                Brand.entries.forEach { brand ->
                                    DropdownMenuItem(
                                        text = { Text(brand.displayName) },
                                        trailingIcon = {
                                            if (selectedBrand == brand) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = AppTheme.tokens.primaryAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            onBrandSelected(if (selectedBrand == brand) null else brand)
                                            showBrandMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }

        // 2. External Buttons (Camera & Add)
        when (LocalThemeVariant.current) {
            ThemeVariant.Cyber -> {
                CyberNeonIconButton(
                    onClick = onCameraClick,
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = stringResource(R.string.scan)
                )
                CyberNeonSquareButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_new_car),
                        tint = CyberKnockoutIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            ThemeVariant.NeonCyan -> {
                NeonCyanNeonIconButton(
                    onClick = onCameraClick,
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = stringResource(R.string.scan)
                )
                NeonCyanNeonSquareButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_new_car),
                        tint = NeonCyanKnockoutIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            else -> {
            IconButton(
                onClick = onCameraClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = stringResource(R.string.scan),
                    tint = AppTheme.tokens.primaryAccent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                    .background(AppTheme.tokens.primaryAccent, RoundedCornerShape(12.dp))
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_new_car),
                    tint = MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(24.dp)
                )
            }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FigmaStatsSection(
    totalCars: Int,
    monthlyAdded: Int,
    wantedCount: Int,
    sthCount: Int,
    totalValue: Double,
    monthlyValueIncrease: Double = 0.0,
    currencySymbol: String = "TL",
    modifier: Modifier = Modifier,
    initialPagerPage: Int = 0,
    onPagerPageChanged: (Int) -> Unit = {}
) {
    val safeInitial = initialPagerPage.coerceIn(0, 1)
    val pagerState = rememberPagerState(
        initialPage = safeInitial,
        pageCount = { 2 }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { onPagerPageChanged(it) }
    }

    val cardHeight = 90.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 2.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FigmaSmallStatCard(
                        label = stringResource(R.string.total_models_label),
                        value = totalCars.toString(),
                        modifier = Modifier.weight(1f),
                        badgeValue = if (monthlyAdded > 0) "+$monthlyAdded" else null,
                        badgeLabel = stringResource(R.string.this_month_short),
                        cardHeight = cardHeight
                    )
                    FigmaSmallStatCard(
                        label = stringResource(R.string.current_value),
                        value = String.format("%.2f %s", totalValue, currencySymbol),
                        modifier = Modifier.weight(1f),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        badgeValue = if (monthlyValueIncrease > 0) String.format("+%.0f", monthlyValueIncrease) else null,
                        badgeLabel = stringResource(R.string.this_month_short),
                        cardHeight = cardHeight
                    )
                }
                else -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FigmaSmallStatCard(
                        label = stringResource(R.string.wanted_short),
                        value = wantedCount.toString(),
                        modifier = Modifier.weight(1f),
                        cardHeight = cardHeight
                    )
                    FigmaSmallStatCard(
                        label = stringResource(R.string.treasure_short),
                        value = sthCount.toString(),
                        modifier = Modifier.weight(1f),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        cardHeight = cardHeight
                    )
                }
            }
        }

        val hintTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 0.dp),
            contentAlignment = Alignment.Center
        ) {
            @Suppress("DEPRECATION")
            when (pagerState.settledPage) {
                0 -> Icon(
                    imageVector = if (rtl) Icons.Filled.KeyboardArrowLeft else Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = hintTint
                )
                else -> Icon(
                    imageVector = if (rtl) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = hintTint
                )
            }
        }
    }
}

@Composable
fun FigmaSmallStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
    badgeValue: String? = null,
    badgeLabel: String? = null,
    cardHeight: Dp = 90.dp
) {
    val shellVariant = LocalThemeVariant.current
    val cyberPink = shellVariant == ThemeVariant.Cyber
    val neonCyan = shellVariant == ThemeVariant.NeonCyan
    val labelColor = when {
        cyberPink || neonCyan -> Color.White.copy(alpha = 0.72f)
        MaterialTheme.colorScheme.background.luminance() < 0.5f -> Color.White.copy(alpha = 0.6f)
        else -> Color.Black.copy(alpha = 0.8f)
    }
    val valueResolved = when {
        neonCyan && valueColor == Color.Unspecified -> AppTheme.tokens.primaryAccent
        cyberPink && valueColor == Color.Unspecified -> Color.White
        valueColor != Color.Unspecified -> valueColor
        else -> AppTheme.tokens.primaryAccent
    }
    val badgeCaptionColor = when {
        cyberPink || neonCyan -> Color.White.copy(alpha = 0.78f)
        MaterialTheme.colorScheme.background.luminance() < 0.5f -> Color.White.copy(alpha = 0.7f)
        else -> Color.Black.copy(alpha = 0.85f)
    }

    val corner = 16.dp
    Card(
        modifier = modifier
            .height(cardHeight)
            .border(
                width = 1.dp,
                color = AppTheme.tokens.cardBorderStandard,
                shape = RoundedCornerShape(corner)
            ),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = labelColor,
                    letterSpacing = 0.6.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 24.sp),
                    fontWeight = FontWeight.Black,
                    color = valueResolved,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Sabit alt şerit: rozet yokken boş taşma olmasın, iki kart aynı hizada kalsın
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (badgeValue != null && badgeLabel != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .then(
                                        when (shellVariant) {
                                            ThemeVariant.Cyber -> Modifier.background(
                                                brush = cyberActionGradientBrush(),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            ThemeVariant.NeonCyan -> Modifier.background(
                                                brush = neonCyanActionGradientBrush(),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            else -> Modifier.background(
                                                AppTheme.tokens.primaryAccent,
                                                RoundedCornerShape(4.dp)
                                            )
                                        }
                                    )
                                    .height(16.dp)
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = badgeValue,
                                    color = when (shellVariant) {
                                        ThemeVariant.Cyber -> CyberKnockoutIconTint
                                        ThemeVariant.NeonCyan -> NeonCyanKnockoutIconTint
                                        else -> Color.White
                                    },
                                    fontSize = 9.sp,
                                    lineHeight = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeCaptionColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FigmaRecentlyAddedCardItem(
    car: UserCar,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modelName = car.masterData?.modelName ?: car.manualModelName ?: stringResource(R.string.unknown_model)
    val photoUrl = (car.backupPhotoUrl ?: car.userPhotoUrl)?.takeIf { it != car.masterData?.imageUrl }



    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = AppTheme.tokens.cardBorderStandard,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp, 0.dp, 0.dp, 16.dp))
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }
            }
            Text(
                text = modelName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun FigmaRecentlyAddedVerticalCard(
    car: UserCar,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modelName = car.masterData?.modelName ?: car.manualModelName ?: stringResource(R.string.unknown_model)
    val photoUrl = (car.backupPhotoUrl ?: car.userPhotoUrl)?.takeIf { it != car.masterData?.imageUrl }
    val brand = car.masterData?.brand ?: car.manualBrand
    val seriesName = car.masterData?.series?.takeIf { it.isNotBlank() } ?: car.manualSeries ?: ""


    Card(
        modifier = modifier
            .width(160.dp)
            .border(
                width = 1.dp,
                color = AppTheme.tokens.cardBorderStandard,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            )
        ) {
            // Top: Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp) // Bir tık küçültüldü
                    .clip(RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp))
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp) // Marginler azaltıldı
                    .height(110.dp),
                horizontalAlignment = Alignment.Start, // Kenara yasla
                verticalArrangement = Arrangement.Top
            ) {
                // Middle: Brand Logo
                val logoRes = getBrandLogo(brand)
                if (logoRes != null) {
                    Image(
                        painter = painterResource(logoRes),
                        contentDescription = null,
                        modifier = Modifier.height(38.dp), // Logo bir tık daha büyütüldü
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(6.dp))
                }

                // Model Name
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )

                // Series Name
                if (seriesName.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = seriesName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )
                }
            }
        }
    }
}

@Composable
fun FigmaDiecastNewsCard(
    news: DiecastNews,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .border(
                width = 1.dp,
                color = AppTheme.tokens.cardBorderStandard,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp))
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (news.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = news.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.ImageNotSupported,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center)
                    )
                }
            }
            Text(
                text = news.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
fun getBrandLogo(brand: Brand?): Int? {
    val isDark = isDarkThemeUi()
    return when (brand) {
        Brand.HOT_WHEELS -> com.taytek.basehw.R.drawable.hotwheels
        Brand.MATCHBOX -> com.taytek.basehw.R.drawable.matchbox
        Brand.MINI_GT -> if (isDark) com.taytek.basehw.R.drawable.minigtdark else com.taytek.basehw.R.drawable.minigt
        Brand.MAJORETTE -> com.taytek.basehw.R.drawable.majorette
        Brand.GREENLIGHT -> com.taytek.basehw.R.drawable.greenlight
        Brand.SIKU -> com.taytek.basehw.R.drawable.siku
        Brand.KAIDO_HOUSE -> com.taytek.basehw.R.drawable.kaido
        else -> null
    }
}

@Composable
fun FigmaRecentlyAddedListItem(
    car: UserCar,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modelName = car.masterData?.modelName ?: car.manualModelName ?: stringResource(R.string.unknown_model)
    val brandName = car.masterData?.brand?.name ?: car.manualBrand?.name ?: ""
    val seriesName = car.masterData?.series?.takeIf { it.isNotBlank() } ?: car.manualSeries ?: ""
    val year = car.masterData?.year ?: car.manualYear
    val photoUrl = (car.backupPhotoUrl ?: car.userPhotoUrl)?.takeIf { it != car.masterData?.imageUrl }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }

                // Tag placeholder if STH/Chase/Premium (Figma has tags on top left)
                val feature = car.masterData?.feature?.lowercase()
                val isSth = feature == "sth"
                val isChase = feature == "chase"
                val isTh = feature == "th"

                if (isSth) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(Color(0xFFB8860B), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.sth_label),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else if (isChase) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(Color.Black, RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.chase_label),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else if (isTh) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(Color(0xFF71797E), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.th_label),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else if (car.masterData?.isPremium == true || car.manualIsPremium == true) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(Color(0xFF3B82F6), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.premium_label_upper),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(brandName, seriesName).joinToString(" / "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (year != null) {
                    Text(
                        text = stringResource(R.string.hot_wheels_year_template, year),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = { /* More options */ }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.content_desc_more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HomeSuggestionItem(
    masterData: MasterData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp) // Increased from 52dp
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (masterData.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = masterData.imageUrl,
                    contentDescription = masterData.modelName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = masterData.modelName,
                style = MaterialTheme.typography.bodyLarge, // Increased from bodyMedium
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                masterData.year?.let {
                    Text(
                        text = it.toString(),
                        style = MaterialTheme.typography.bodySmall, // Increased from labelSmall
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (masterData.series.isNotBlank() || masterData.seriesNum.isNotBlank()) {
                    val seriesText = listOfNotNull(
                        masterData.series.takeIf { it.isNotBlank() },
                        masterData.seriesNum.takeIf { it.isNotBlank() }
                    ).joinToString(" ")
                    Text(
                        text = seriesText,
                        style = MaterialTheme.typography.bodySmall, // Increased from labelSmall
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val feature = masterData.feature?.lowercase()
                
                        if (feature == "sth") {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE0B94C), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.sth_label),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        if (feature == "chase") {
                            Box(
                                modifier = Modifier
                                    .background(Color.Black, RoundedCornerShape(4.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.chase_label),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        if (feature == "th") {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF71797E), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.th_label),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
            }
        }
    }
}
