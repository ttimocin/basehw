package com.taytek.basehw.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.ui.theme.*

@Composable
fun HomeHeader(
    title: String = stringResource(com.taytek.basehw.R.string.hello_collector),
    subtitle: String,
    isGridView: Boolean = false,
    onGalleryToggle: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sadece metin - profil ikonu kaldırıldı
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Zil kaldırıldı — Grid Toggle + Settings (Search ekranı tarzı: 52dp yuvarlak)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onGalleryToggle != null) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            CircleShape
                        )
                        .clickable { onGalleryToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGridView)
                            Icons.AutoMirrored.Filled.ViewList
                        else
                            androidx.compose.material.icons.Icons.Default.GridView,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}


/** Figma Hero Card - Gradient mavi, toplam model sayısı + +5% badge */
@Composable
fun CollectionHeroCard(
    totalModels: Int,
    monthlyAddedCount: Int = 0,
    monthlyValueIncrease: Double = 0.0,
    totalEstimatedValue: Double = 0.0,
    currencySymbol: String = "$",
    statIndex: Int = 0,
    isGridView: Boolean = false,
    onGalleryToggle: (() -> Unit)? = null,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isLoading) 0.6f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(110.dp)
            .graphicsLayer { this.alpha = if (isLoading) alpha else 1f }
            .shadow(12.dp, RoundedCornerShape(26.dp), clip = false)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0033CC),
                        Color(0xFF1E90FF)
                    )
                )
            )
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .size(140.dp)
                .offset(x = 220.dp, y = (-50).dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        )

        // Grid Toggle Button (Top Right)
        if (onGalleryToggle != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable { onGalleryToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGridView)
                        Icons.AutoMirrored.Filled.ViewList
                    else
                        androidx.compose.material.icons.Icons.Default.GridView,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp), // Kenar boşluğu
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // Diğer tarafa yasla
        ) {
            // Left Side: Total Models
            Column {
                Text(
                    text = stringResource(com.taytek.basehw.R.string.total_models_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isLoading) "—" else totalModels.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
 
            // Right Side: Rotating Stats (Side by side)
            if (!isLoading) {
                androidx.compose.animation.Crossfade(
                    targetState = statIndex % 3, 
                    label = "stats",
                    modifier = Modifier.padding(end = 8.dp, top = 24.dp)
                ) { index ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (labelRes, value, icon) = when (index) {
                            0 -> Triple(
                                com.taytek.basehw.R.string.hero_stat_this_month,
                                "+$monthlyAddedCount",
                                Icons.AutoMirrored.Filled.TrendingUp
                            )
                            1 -> Triple(
                                com.taytek.basehw.R.string.hero_stat_monthly,
                                "+$currencySymbol${String.format(java.util.Locale.US, "%.0f", monthlyValueIncrease)}",
                                Icons.AutoMirrored.Filled.TrendingUp
                            )
                            else -> Triple(
                                com.taytek.basehw.R.string.hero_stat_total,
                                "$currencySymbol${String.format(java.util.Locale.US, "%.0f", totalEstimatedValue)}",
                                Icons.Default.Star
                            )
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${stringResource(labelRes)}:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            } else {
                // Placeholder for stats when loading
                Box(modifier = Modifier.padding(end = 8.dp, top = 24.dp)) {
                    Text(
                        text = "...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}


@Composable
fun FeaturedCarCard(
    car: UserCar,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 24.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp), clip = false)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageToShow = car.userPhotoUrl ?: car.masterData?.imageUrl
            AsyncImage(
                model = imageToShow,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            startY = 400f
                        )
                    )
            )
            
            // Favorite Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AppPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(com.taytek.basehw.R.string.favorite),
                        color = AppPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Text Details
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Text(
                    text = car.masterData?.modelName ?: car.manualModelName ?: stringResource(com.taytek.basehw.R.string.unknown_model),
                    color = MaterialTheme.colorScheme.surface,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(com.taytek.basehw.R.string.latest_addition),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun SearchBarWithFilter(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onStatsClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .shadow(3.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (MaterialTheme.colorScheme.background == DarkNavy)
                        MaterialTheme.colorScheme.surface
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
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
                            if (query.isEmpty()) {
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
                        
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(com.taytek.basehw.R.string.clear),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IconButton(onClick = onFilterClick, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(com.taytek.basehw.R.string.filter),
                                tint = AppPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )
        }

        IconButton(
            onClick = onStatsClick,
            modifier = Modifier
                .size(40.dp)
                .background(AppPrimary, RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = stringResource(com.taytek.basehw.R.string.statistics_title),
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    onDismiss: () -> Unit,
    currentBrand: String?,
    currentYear: Int?,
    currentSeries: String?,
    currentIsOpened: Boolean?,
    currentSortOrder: com.taytek.basehw.domain.model.SortOrder = com.taytek.basehw.domain.model.SortOrder.DATE_ADDED_DESC,
    onApply: (brand: String?, year: Int?, series: String?, isOpened: Boolean?, sortOrder: com.taytek.basehw.domain.model.SortOrder) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppPrimary.copy(alpha = 0.3f)) }
    ) {
        var brand by remember { mutableStateOf(currentBrand) }
        var yearInput by remember { mutableStateOf(currentYear?.toString() ?: "") }
        var series by remember { mutableStateOf(currentSeries ?: "") }
        var isOpened by remember { mutableStateOf(currentIsOpened) }
        var sortOrder by remember { mutableStateOf(currentSortOrder) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(com.taytek.basehw.R.string.advanced_filters),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Brand Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(com.taytek.basehw.R.string.brand), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 24.dp) // Extra padding for scroll
                ) {
                    items(listOf(null, "HOT_WHEELS", "MATCHBOX", "MINI_GT", "MAJORETTE", "JADA", "SIKU", "KAIDO_HOUSE")) { b ->
                        FilterChip(
                            selected = brand == b,
                            onClick = { brand = b },
                            label = { Text(b?.replace("_", " ") ?: stringResource(com.taytek.basehw.R.string.all)) }
                        )
                    }
                }
            }

            // Year and Series
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = yearInput,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) yearInput = it },
                    label = { Text(stringResource(com.taytek.basehw.R.string.year)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = series,
                    onValueChange = { series = it },
                    label = { Text(stringResource(com.taytek.basehw.R.string.series)) },
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Status
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(com.taytek.basehw.R.string.condition), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 24.dp)
                ) {
                    item { FilterChip(selected = isOpened == null, onClick = { isOpened = null }, label = { Text(stringResource(com.taytek.basehw.R.string.all)) }) }
                    item { FilterChip(selected = isOpened == false, onClick = { isOpened = false }, label = { Text(stringResource(com.taytek.basehw.R.string.boxed)) }) }
                    item { FilterChip(selected = isOpened == true, onClick = { isOpened = true }, label = { Text(stringResource(com.taytek.basehw.R.string.opened)) }) }
                }
            }

            // Sort Order
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(com.taytek.basehw.R.string.sort_order), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 24.dp)
                ) {
                    items(com.taytek.basehw.domain.model.SortOrder.entries) { order ->
                        FilterChip(
                            selected = sortOrder == order,
                            onClick = { sortOrder = order },
                            label = { Text(stringResource(order.titleRes)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onApply(brand, yearInput.toIntOrNull(), series, isOpened, sortOrder)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPrimary)
            ) {
                Text(stringResource(com.taytek.basehw.R.string.apply_filters), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            TextButton(
                onClick = { onApply(null, null, "", null, com.taytek.basehw.domain.model.SortOrder.DATE_ADDED_DESC) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(com.taytek.basehw.R.string.clear_all), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionListItem(
    car: UserCar,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isSelected: Boolean = false
) {
    val modelName = car.masterData?.modelName ?: car.manualModelName ?: stringResource(com.taytek.basehw.R.string.unknown_model)
    val brandName = car.masterData?.brand?.displayName ?: car.manualBrand?.displayName ?: ""
    val seriesName = car.masterData?.series?.takeIf { it.isNotBlank() } ?: car.manualSeries ?: ""
    val year = (car.masterData?.year ?: car.manualYear)?.toString() ?: ""
    val imageToShow = car.userPhotoUrl ?: car.masterData?.imageUrl
    val feature = car.masterData?.feature?.lowercase()
    val isSthCar = feature == "sth"
    val isChaseCar = feature == "chase"
    val isThCar = feature == "th"
    val isDark = MaterialTheme.colorScheme.background == DarkNavy
    val baseColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFFFDFB)
    val darkerColor = if (isDark) Color(0xFF121416) else Color(0xFFFFF7ED)

    val sthBorderColor = if (isSthCar) Color(0xFFB8860B) else if (isChaseCar) Color.Black else if (isThCar) Color(0xFF71797E) else Color.Transparent
    val defaultBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.15f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .shadow(if (isSelected) 8.dp else 4.dp, RoundedCornerShape(16.dp), clip = false)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSthCar || isChaseCar || isThCar) BorderStroke(2.dp, sthBorderColor) else BorderStroke(1.dp, defaultBorderColor),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(
                    brush = if (isSelected) {
                        Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
                    } else {
                        Brush.linearGradient(colors = listOf(baseColor, darkerColor))
                    }
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Sol: Fotoğraf
            Box(
                modifier = Modifier
                    .width(86.dp)
                    .height(86.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            ) {
                if (!imageToShow.isNullOrBlank()) {
                    AsyncImage(
                        model = imageToShow,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Seçim overlay
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Sağ: Bilgiler
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 44.dp, top = 14.dp, bottom = 14.dp)
            ) {
                // Marka + Yıl satırı
                if (brandName.isNotBlank() || year.isNotBlank() || car.masterData?.scale?.isNotBlank() == true || car.manualScale?.isNotBlank() == true) {
                    val scale = car.masterData?.scale?.takeIf { it.isNotBlank() } ?: car.manualScale?.takeIf { it.isNotBlank() } ?: "1:64"
                    Text(
                        text = listOfNotNull(
                            brandName.uppercase().takeIf { it.isNotBlank() },
                            scale,
                            year.takeIf { it.isNotBlank() }
                        ).joinToString(" - "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp,
                        fontSize = 8.5.sp
                    )
                    Spacer(Modifier.height(2.dp))
                }
                // Model adı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = modelName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp
                    )
                }
                // Seri adı
                if (seriesName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = seriesName,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }

            }

            // Ok ikonu
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(20.dp)
            )
            }

            // Favori kalp rozeti (kartın en sağ üst köşesi)
            if (car.isFavorite) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = androidx.compose.ui.graphics.Color(0xFFFF4D6D),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 4.dp, start = 6.dp)
                        .size(14.dp)
                )
            }

            if (isSthCar || isChaseCar || isThCar || !car.isOpened) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSthCar) {
                        Surface(
                            color = Color(0xFF1A1300),
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "STH",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    color = Color(0xFFFFD54F),
                                    shadow = Shadow(
                                        color = Color(0xFFFFD700).copy(alpha = 0.6f),
                                        offset = Offset(0f, 0f),
                                        blurRadius = 8f
                                    )
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isChaseCar) {
                        Surface(
                            color = Color.Black,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 2.dp,
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "CHASE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 8.sp,
                                    color = Color.White
                                ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isThCar) {
                    Surface(
                        color = Color(0xFFE5E4E2),
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 2.dp,
                        border = BorderStroke(0.5.dp, Color.Black.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "TH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp,
                                color = Color.DarkGray
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (!car.isOpened) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "MOC",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (car.quantity > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 40.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "x${car.quantity}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

