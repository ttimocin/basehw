package com.taytek.basehw.ui.screens.statistics

import com.taytek.basehw.domain.model.toColor
import com.taytek.basehw.domain.model.toIcon
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.clipRect
import com.taytek.basehw.ui.theme.AppTheme
import com.taytek.basehw.ui.theme.cyberRootSurfaceColor
import com.taytek.basehw.ui.theme.neonShellChromeIconTint
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues? = null,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    BackHandler { onBack() }
    val totalCars by viewModel.totalCars.collectAsState()
    val boxStatusStats by viewModel.boxStatusStats.collectAsState()
    val brandStats by viewModel.brandStats.collectAsState()
    val hwTierStats by viewModel.hwTierStats.collectAsState()
    val totalPurchasePrice by viewModel.totalPurchasePrice.collectAsState()
    val totalEstimatedValue by viewModel.totalEstimatedValue.collectAsState()
    val customStats by viewModel.customStats.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val collectionHistory by viewModel.collectionHistory.collectAsState()

    var showPieChart by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val systemNavBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    
    val bottomPadding = contentPadding?.let {
        it.calculateBottomPadding() + 48.dp
    } ?: (systemNavBottom + 32.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = systemNavBottom)
            .background(cyberRootSurfaceColor())
    ) {
        Scaffold(
            containerColor = cyberRootSurfaceColor(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.statistics_title), style = MaterialTheme.typography.titleLarge)
                            Text(
                                stringResource(R.string.collection_summary),
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
                                tint = neonShellChromeIconTint()
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = cyberRootSurfaceColor()
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Toplam Araç Sayısı — brand accent (Cyber: turuncu; primary pembe morumsu duruyordu)
                StatCard(
                    title = stringResource(R.string.total_cars_stat),
                    value = totalCars.toString(),
                    color = AppTheme.tokens.primaryAccent
                )

                // Değer Kartları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = stringResource(R.string.stats_purchase_price),
                        value = "${currencySymbol}${"%.2f".format(totalPurchasePrice)}",
                        color = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = stringResource(R.string.stats_current_value),
                        value = "${currencySymbol}${"%.2f".format(totalEstimatedValue)}",
                        color = AppTheme.tokens.primaryAccent,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (collectionHistory.isNotEmpty()) {
                    ValueOverTimeCard(history = collectionHistory, currencySymbol = currencySymbol)
                }

                if (totalCars > 0 && boxStatusStats.isNotEmpty()) {
                    BoxStatusCard(boxStatusStats = boxStatusStats, total = totalCars, showPieChart = showPieChart)
                }

                if (totalCars > 0) {
                    BrandDistributionCard(brandStats = brandStats, total = totalCars, showPieChart = showPieChart)
                }

                val hwTotal = hwTierStats.regularCount + hwTierStats.premiumCount
                if (hwTotal > 0) {
                    HwTierCard(regularCount = hwTierStats.regularCount, premiumCount = hwTierStats.premiumCount, showPieChart = showPieChart)
                }

                if (totalCars > 0) {
                    CustomDistributionCard(
                        originalCount = customStats.originalCount,
                        customCount = customStats.customCount,
                        total = totalCars,
                        showPieChart = showPieChart
                    )
                }

                if (totalCars == 0) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_stats_data),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppTheme.tokens.cardBorderStandard),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .background(brush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceContainerHigh)))
            .padding(20.dp)
        ) {
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

@Composable
private fun BoxStatusCard(boxStatusStats: List<com.taytek.basehw.domain.model.BoxStatusStats>, total: Int, showPieChart: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppTheme.tokens.cardBorderStandard),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .background(brush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceContainerHigh)))
            .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.box_condition),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showPieChart) {
                val slices = boxStatusStats.map { stat ->
                    val conditionObj = com.taytek.basehw.domain.model.VehicleCondition.fromString(stat.condition)
                    PieSliceData(
                        label = stringResource(conditionObj.titleRes),
                        count = stat.count,
                        color = Color(conditionObj.hexColor)
                    )
                }.filter { it.count > 0 }
                ReusablePieChart(slices = slices)
            } else {
                boxStatusStats.forEachIndexed { index, stat ->
                    val conditionObj = com.taytek.basehw.domain.model.VehicleCondition.fromString(stat.condition)
                    val percent = if (total > 0) (stat.count.toFloat() / total) else 0f
                    ProgressBarWithLabel(
                        label = stringResource(conditionObj.titleRes),
                        count = stat.count,
                        percent = percent,
                        color = Color(conditionObj.hexColor)
                    )
                    if (index < boxStatusStats.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandDistributionCard(brandStats: List<com.taytek.basehw.domain.model.BrandStats>, total: Int, showPieChart: Boolean) {
    if (brandStats.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppTheme.tokens.cardBorderStandard),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .background(brush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceContainerHigh)))
            .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (showPieChart) Icons.Default.PieChart else Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.brand_distribution),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val sorted = brandStats.sortedByDescending { it.count }
            
            if (showPieChart) {
                val slices = sorted.map { stat ->
                    val color = stat.brand.toColor()
                    PieSliceData(stat.brand.displayName, stat.count, color)
                }
                ReusablePieChart(slices = slices)
            } else {
                sorted.forEachIndexed { index, stat ->
                    val percent = if (total > 0) (stat.count.toFloat() / total) else 0f
                    val color = stat.brand.toColor()
                    
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
private fun HwTierCard(regularCount: Int, premiumCount: Int, showPieChart: Boolean) {
    val total = regularCount + premiumCount
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppTheme.tokens.cardBorderStandard),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .background(brush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceContainerHigh)))
            .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (showPieChart) Icons.Default.PieChart else Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = Brand.HOT_WHEELS.toColor()
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
                    PieSliceData("Regular", regularCount, Brand.HOT_WHEELS.toColor().copy(alpha = 0.7f)),
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
                    color = Brand.HOT_WHEELS.toColor().copy(alpha = 0.7f)
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

@Composable
private fun CustomDistributionCard(originalCount: Int, customCount: Int, total: Int, showPieChart: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppTheme.tokens.cardBorderStandard),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .background(brush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceContainerHigh)))
            .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (showPieChart) Icons.Default.PieChart else Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.custom_distribution),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showPieChart) {
                val slices = listOf(
                    PieSliceData(stringResource(R.string.original_label), originalCount, MaterialTheme.colorScheme.primary),
                    PieSliceData(stringResource(R.string.custom_label), customCount, Color(0xFF4CAF50))
                ).filter { it.count > 0 }
                ReusablePieChart(slices = slices)
            } else {
                val originalPercent = if (total > 0) originalCount.toFloat() / total else 0f
                val customPercent = if (total > 0) customCount.toFloat() / total else 0f

                ProgressBarWithLabel(
                    label = stringResource(R.string.original_label),
                    count = originalCount,
                    percent = originalPercent,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                ProgressBarWithLabel(
                    label = stringResource(R.string.custom_label),
                    count = customCount,
                    percent = customPercent,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun ValueOverTimeCard(history: List<ValuePoint>, currencySymbol: String) {
    val latestValue = history.lastOrNull()?.cumulativeValue ?: 0.0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppTheme.tokens.cardBorderStandard),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .background(brush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceContainerHigh)))
            .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Koleksiyon Değer Gelişimi",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Text(
                    text = "${currencySymbol}${"%.2f".format(latestValue)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            BezierLineChart(
                points = history,
                currencySymbol = currencySymbol,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = history.firstOrNull()?.label ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = history.lastOrNull()?.label ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BezierLineChart(
    points: List<ValuePoint>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1500),
        label = "chart_reveal"
    )

    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height

        val maxVal = points.maxOf { it.cumulativeValue }.coerceAtLeast(1.0).toFloat()
        val xSpacing = if (points.size > 1) width / (points.size - 1) else width
        
        fun getX(index: Int) = index * xSpacing
        fun getY(value: Double) = height - ((value.toFloat() / maxVal) * (height - 40f)) - 20f

        val strokePath = Path()
        val fillPath = Path()

        points.forEachIndexed { i, point ->
            val x = getX(i)
            val y = getY(point.cumulativeValue)

            if (i == 0) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                val prevX = getX(i - 1)
                val prevY = getY(points[i - 1].cumulativeValue)
                
                val controlX1 = prevX + (x - prevX) / 2f
                val controlY1 = prevY
                val controlX2 = prevX + (x - prevX) / 2f
                val controlY2 = y
                
                strokePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }
            
            if (i == points.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        clipRect(right = width * animationProgress) {
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.3f),
                        primaryColor.copy(alpha = 0.0f)
                    )
                )
            )

            drawPath(
                path = strokePath,
                color = primaryColor,
                style = Stroke(width = 4.dp.toPx())
            )

            val lastX = getX(points.size - 1)
            val lastY = getY(points.last().cumulativeValue)
            
            drawCircle(
                color = primaryColor,
                radius = 6.dp.toPx(),
                center = Offset(lastX, lastY)
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(lastX, lastY)
            )
            
            val paint = android.graphics.Paint().apply {
                color = primaryColor.toArgb()
                textSize = 34f
                textAlign = android.graphics.Paint.Align.RIGHT
                isFakeBoldText = true
            }
            
            val latestValueText = "${currencySymbol}${"%.2f".format(points.last().cumulativeValue)}"
            drawContext.canvas.nativeCanvas.drawText(
                latestValueText,
                lastX,
                lastY - 25f,
                paint
            )
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

data class PieSliceData(val label: String, val count: Int, val color: Color)

@Composable
fun ReusablePieChart(slices: List<PieSliceData>) {
    if (slices.isEmpty()) return
    val total = slices.sumOf { it.count }.toFloat()
    
    Column {
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
            }

            slices.forEach { slice ->
                val sweep = (slice.count / total) * 360f
                
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize
                )
                
                drawArc(
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = 2f)
                )

                if (sweep > 15f) {
                    val midAngle = Math.toRadians((startAngle + sweep / 2.0))
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
