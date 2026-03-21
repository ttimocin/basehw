package com.taytek.basehw.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.taytek.basehw.R
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.ui.theme.AppPrimary
import com.taytek.basehw.ui.theme.DarkNavy



@Composable
fun FigmaHomeHeader(
    userName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp) // Adjusted padding for new position
    ) {
        Text(
            text = stringResource(R.string.hello_name_template, userName),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
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
    val isDark = MaterialTheme.colorScheme.background == DarkNavy
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = placeholderFontSize),
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
                                    tint = if (selectedBrand == null) MaterialTheme.colorScheme.onSurfaceVariant else AppPrimary,
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
                                                    tint = AppPrimary,
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
        // Camera (Transparent bg, Orange icon)
        IconButton(
            onClick = onCameraClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = stringResource(R.string.scan),
                tint = AppPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Add (Orange bg, White icon)
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                .background(AppPrimary, RoundedCornerShape(12.dp))
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

@Composable
fun FigmaStatsSection(
    totalCars: Int,
    monthlyAdded: Int,
    wantedCount: Int,
    sthCount: Int,
    totalValue: Double,
    monthlyValueIncrease: Double = 0.0,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Total Models & Current Value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FigmaSmallStatCard(
                label = stringResource(R.string.total_models_label),
                value = totalCars.toString(),
                modifier = Modifier.weight(1f),
                badgeValue = if (monthlyAdded > 0) "+$monthlyAdded" else null,
                badgeLabel = stringResource(R.string.this_month_short)
            )
            FigmaSmallStatCard(
                label = stringResource(R.string.current_value),
                value = String.format("%.2f TL", totalValue),
                modifier = Modifier.weight(1f),
                valueColor = AppPrimary,
                badgeValue = if (monthlyValueIncrease > 0) String.format("+%.0f", monthlyValueIncrease) else null,
                badgeLabel = stringResource(R.string.this_month_short)
            )
        }

        // Row 2: Wanted & Treasure
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FigmaSmallStatCard(
                label = stringResource(R.string.wanted_short),
                value = wantedCount.toString(),
                modifier = Modifier.weight(1f)
            )
            FigmaSmallStatCard(
                label = stringResource(R.string.treasure_short),
                value = sthCount.toString(),
                modifier = Modifier.weight(1f),
                valueColor = AppPrimary
            )
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
    badgeLabel: String? = null
) {
    val isDark = MaterialTheme.colorScheme.background == DarkNavy
    val baseColor = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
    val darkerColor = if (isDark) Color(0xFF121416) else Color(0xFFE2E8F0)

    Card(
        modifier = modifier.border(
            width = 1.dp,
            color = AppPrimary.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp)
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(baseColor, darkerColor)
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Black,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppPrimary
                    )

                    if (badgeValue != null && badgeLabel != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(AppPrimary, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeValue,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = badgeLabel,
                                color = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
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
    val photoUrl = car.userPhotoUrl ?: car.masterData?.imageUrl

    val isDark = MaterialTheme.colorScheme.background == DarkNavy
    val baseColor = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
    val darkerColor = if (isDark) Color(0xFF121416) else Color(0xFFE2E8F0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = AppPrimary.copy(alpha = 0.6f),
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
                    colors = listOf(baseColor, darkerColor)
                )
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
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
    val photoUrl = car.userPhotoUrl ?: car.masterData?.imageUrl
    val brand = car.masterData?.brand ?: car.manualBrand
    val seriesName = car.masterData?.series?.takeIf { it.isNotBlank() } ?: car.manualSeries ?: ""
    val isDark = MaterialTheme.colorScheme.background == DarkNavy
    val baseColor = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
    val darkerColor = if (isDark) Color(0xFF121416) else Color(0xFFE2E8F0)

    Card(
        modifier = modifier
            .width(160.dp)
            .border(
                width = 1.dp,
                color = AppPrimary.copy(alpha = 0.6f),
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
                    colors = listOf(baseColor, darkerColor)
                )
            )
        ) {
            // Top: Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp) // Bir tık küçültüldü
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
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
fun getBrandLogo(brand: Brand?): Int? {
    val isDark = MaterialTheme.colorScheme.background == DarkNavy
    return when (brand) {
        Brand.HOT_WHEELS -> com.taytek.basehw.R.drawable.hotwheels
        Brand.MATCHBOX -> com.taytek.basehw.R.drawable.matchbox
        Brand.MINI_GT -> if (isDark) com.taytek.basehw.R.drawable.minigtdark else com.taytek.basehw.R.drawable.minigt
        Brand.MAJORETTE -> com.taytek.basehw.R.drawable.majorette
        Brand.JADA -> com.taytek.basehw.R.drawable.jada
        Brand.SIKU -> com.taytek.basehw.R.drawable.siku
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
    val photoUrl = car.userPhotoUrl ?: car.masterData?.imageUrl

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
                            text = "CHASE",
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
                            text = "TH",
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
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (masterData.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = masterData.imageUrl,
                    contentDescription = masterData.modelName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = masterData.modelName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                masterData.year?.let {
                    Text(
                        text = it.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (masterData.series.isNotBlank()) {
                    Text(
                        text = masterData.series,
                        style = MaterialTheme.typography.labelSmall,
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
                            text = "STH",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
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
                            text = "CHASE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
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
                            text = "TH",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
