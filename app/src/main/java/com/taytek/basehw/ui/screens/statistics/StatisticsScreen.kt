package com.taytek.basehw.ui.screens.statistics

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.taytek.basehw.domain.model.BadgeType
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.ui.theme.HotWheelsRed
import com.taytek.basehw.ui.theme.MatchboxBlue
import com.taytek.basehw.ui.theme.MiniGTSilver
import com.taytek.basehw.ui.theme.MajoretteYellow
import com.taytek.basehw.ui.theme.JadaPurple
import com.taytek.basehw.ui.theme.SikuBlue
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val totalCars by viewModel.totalCars.collectAsState()
    val boxStatusStats by viewModel.boxStatusStats.collectAsState()
    val brandStats by viewModel.brandStats.collectAsState()
    val hwTierStats by viewModel.hwTierStats.collectAsState()
    val totalPurchasePrice by viewModel.totalPurchasePrice.collectAsState()
    val totalEstimatedValue by viewModel.totalEstimatedValue.collectAsState()
    val earnedBadges by viewModel.earnedBadges.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    var showPieChart by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(com.taytek.basehw.R.string.statistics_title), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(com.taytek.basehw.R.string.collection_summary),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showPieChart = !showPieChart }) {
                        Icon(
                            imageVector = if (showPieChart) Icons.Default.BarChart else Icons.Default.PieChart,
                            contentDescription = if (showPieChart) "Bar grafik" else "Pasta grafik",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Toplam Araç Sayısı (Hero stat — ikonun kaldırıldı)
            StatCard(
                title = stringResource(com.taytek.basehw.R.string.total_cars_stat),
                value = totalCars.toString(),
                color = MaterialTheme.colorScheme.primary
            )

            // Collection Values (Değer Kartları)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = stringResource(com.taytek.basehw.R.string.stats_purchase_price),
                    value = "${currencySymbol}${"%.2f".format(totalPurchasePrice)}",
                    color = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(com.taytek.basehw.R.string.stats_current_value),
                    value = "${currencySymbol}${"%.2f".format(totalEstimatedValue)}",
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            // Kutu Durumu Kartı
            val openedCount = boxStatusStats.find { it.isOpened }?.count ?: 0
            val boxedCount = boxStatusStats.find { !it.isOpened }?.count ?: 0
            if (totalCars > 0) {
                BoxStatusCard(openedCount = openedCount, boxedCount = boxedCount, total = totalCars, showPieChart = showPieChart)
            }

            // Marka Dağılımı Kartı
            if (totalCars > 0) {
                BrandDistributionCard(brandStats = brandStats, total = totalCars, showPieChart = showPieChart)
            }

            // Hot Wheels Regular / Premium kırılımı
            val hwTotal = hwTierStats.regularCount + hwTierStats.premiumCount
            if (hwTotal > 0) {
                HwTierCard(regularCount = hwTierStats.regularCount, premiumCount = hwTierStats.premiumCount, showPieChart = showPieChart)
            }

            // Earned Badges
            if (earnedBadges.isNotEmpty()) {
                BadgesSection(badges = earnedBadges)
            }

            // Boş Koleksiyon Uyarı
            if (totalCars == 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(com.taytek.basehw.R.string.no_stats_data),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BadgesSection(badges: List<BadgeType>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(com.taytek.basehw.R.string.earned_badges_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(com.taytek.basehw.R.string.earned_badges_count, badges.size, BadgeType.entries.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(badges) { badge ->
                BadgeCard(badge = badge)
            }
        }
        // Locked badges (greyed out)
        val lockedBadges = BadgeType.entries.filter { it !in badges }
        if (lockedBadges.isNotEmpty()) {
            Text(
                text = stringResource(com.taytek.basehw.R.string.locked_badges_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(lockedBadges) { badge ->
                    BadgeCard(badge = badge, locked = true)
                }
            }
        }
    }
}

@Composable
fun BadgeCard(badge: BadgeType, locked: Boolean = false) {
    val badgeColor = Color(badge.color)
    Card(
        modifier = androidx.compose.ui.Modifier.width(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (locked)
                MaterialTheme.colorScheme.surfaceVariant
            else
                badgeColor.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = badge.emoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(badge.titleRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (locked)
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else
                    badgeColor
            )
            Text(
                text = if (locked) "🔒 ${stringResource(badge.descRes)}" else stringResource(badge.descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (locked) 0.4f else 0.8f
                )
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
            }
        }
    }
}

@Composable
fun BoxStatusCard(openedCount: Int, boxedCount: Int, total: Int, showPieChart: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(com.taytek.basehw.R.string.box_condition),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val openedPercent = if (total > 0) (openedCount.toFloat() / total) else 0f
            val boxedPercent = if (total > 0) (boxedCount.toFloat() / total) else 0f

            if (showPieChart) {
                val slices = listOf(
                    PieSliceData(stringResource(com.taytek.basehw.R.string.boxed_moc), boxedCount, MaterialTheme.colorScheme.primary),
                    PieSliceData(stringResource(com.taytek.basehw.R.string.opened_unboxed), openedCount, MaterialTheme.colorScheme.secondary)
                ).filter { it.count > 0 }
                ReusablePieChart(slices = slices)
            } else {
                ProgressBarWithLabel(label = stringResource(com.taytek.basehw.R.string.boxed_moc), count = boxedCount, percent = boxedPercent, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                ProgressBarWithLabel(label = stringResource(com.taytek.basehw.R.string.opened_unboxed), count = openedCount, percent = openedPercent, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun BrandDistributionCard(brandStats: List<com.taytek.basehw.domain.model.BrandStats>, total: Int, showPieChart: Boolean) {
    if (brandStats.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (showPieChart) Icons.Default.PieChart else Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(com.taytek.basehw.R.string.brand_distribution),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val sorted = brandStats.sortedByDescending { it.count }
            
            if (showPieChart) {
                val slices = sorted.map { stat ->
                    val color = when (stat.brand) {
                        Brand.HOT_WHEELS -> HotWheelsRed
                        Brand.MATCHBOX   -> MatchboxBlue
                        Brand.MINI_GT    -> MiniGTSilver
                        Brand.MAJORETTE  -> MajoretteYellow
                        Brand.JADA       -> JadaPurple
                        Brand.SIKU       -> SikuBlue
                    }
                    PieSliceData(stat.brand.displayName, stat.count, color)
                }
                ReusablePieChart(slices = slices)
            } else {
                sorted.forEachIndexed { index, stat ->
                    val percent = if (total > 0) (stat.count.toFloat() / total) else 0f
                    val color = when (stat.brand) {
                        Brand.HOT_WHEELS -> HotWheelsRed
                        Brand.MATCHBOX   -> MatchboxBlue
                        Brand.MINI_GT    -> MiniGTSilver
                        Brand.MAJORETTE  -> MajoretteYellow
                        Brand.JADA       -> JadaPurple
                        Brand.SIKU       -> SikuBlue
                    }
                    
                    ProgressBarWithLabel(
                        label = stat.brand.displayName,
                        count = stat.count,
                        percent = percent,
                        color = color
                    )
                    
                    if (index < brandStats.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressBarWithLabel(label: String, count: Int, percent: Float, color: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = percent,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "$count (${(percent * 100).roundToInt()}%)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}

@Composable
fun HwTierCard(regularCount: Int, premiumCount: Int, showPieChart: Boolean) {
    val total = regularCount + premiumCount
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = HotWheelsRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hot Wheels — Regular / Premium",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showPieChart) {
                val slices = listOf(
                    PieSliceData("Regular", regularCount, HotWheelsRed.copy(alpha = 0.7f)),
                    PieSliceData("🏁 Premium", premiumCount, MaterialTheme.colorScheme.tertiary)
                ).filter { it.count > 0 }
                ReusablePieChart(slices = slices)
            } else {
                val regularPercent = if (total > 0) regularCount.toFloat() / total else 0f
                val premiumPercent = if (total > 0) premiumCount.toFloat() / total else 0f

                ProgressBarWithLabel(
                    label = "Regular",
                    count = regularCount,
                    percent = regularPercent,
                    color = HotWheelsRed.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                ProgressBarWithLabel(
                    label = "🏁 Premium",
                    count = premiumCount,
                    percent = premiumPercent,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

data class PieSliceData(val label: String, val count: Int, val color: Color)

@Composable
fun ReusablePieChart(slices: List<PieSliceData>) {
    if (slices.isEmpty()) return
    val total = slices.sumOf { it.count }.toFloat()
    
    Column {
        // Canvas pie chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val diameter = minOf(size.width, size.height) * 0.9f
            val radius = diameter / 2f
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )
            val centerNode = Offset(size.width / 2f, size.height / 2f)
            val arcSize = Size(diameter, diameter)
            var startAngle = -90f

            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 36f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
            }

            slices.forEach { slice ->
                val sweep = (slice.count / total) * 360f
                
                // Draw arc
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize
                )
                // Draw divider
                drawArc(
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = 2f)
                )

                // Draw text label inside slice if slice is large enough
                if (sweep > 15f) {
                    val midAngle = Math.toRadians((startAngle + sweep / 2.0))
                    // position text at 65% of radius
                    val textRadius = radius * 0.65f 
                    val textX = centerNode.x + (textRadius * Math.cos(midAngle)).toFloat()
                    val textY = centerNode.y + (textRadius * Math.sin(midAngle)).toFloat()
                    
                    val pct = (slice.count / total * 100).roundToInt()
                    drawContext.canvas.nativeCanvas.drawText("$pct%", textX, textY - (paint.descent() + paint.ascent()) / 2, paint)
                }
                
                startAngle += sweep
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        slices.forEach { slice ->
            val pct = if (total > 0) (slice.count / total * 100).roundToInt() else 0
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(slice.color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = slice.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${slice.count} ($pct%)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
