package com.taytek.basehw.ui.screens.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.ui.theme.HotWheelsRed
import com.taytek.basehw.ui.theme.MatchboxBlue
import com.taytek.basehw.ui.theme.MiniGTSilver
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val totalCars by viewModel.totalCars.collectAsState()
    val boxStatusStats by viewModel.boxStatusStats.collectAsState()
    val brandStats by viewModel.brandStats.collectAsState()

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("İstatistikler", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Koleksiyon Özeti",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            // Toplam Araç Sayısı (Hero stat)
            StatCard(
                title = "Toplam Araç",
                value = totalCars.toString(),
                icon = Icons.Default.DirectionsCar,
                color = MaterialTheme.colorScheme.primary
            )

            // Kutu Durumu Kartı
            val openedCount = boxStatusStats.find { it.isOpened }?.count ?: 0
            val boxedCount = boxStatusStats.find { !it.isOpened }?.count ?: 0
            if (totalCars > 0) {
                BoxStatusCard(openedCount = openedCount, boxedCount = boxedCount, total = totalCars)
            }

            // Marka Dağılımı Kartı
            if (totalCars > 0) {
                BrandDistributionCard(brandStats = brandStats, total = totalCars)
            }

            // Boş Koleksiyon Uyarı
            if (totalCars == 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Henüz istatistik için veri yok.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun BoxStatusCard(openedCount: Int, boxedCount: Int, total: Int) {
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
                    text = "Kutu Durumu",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val openedPercent = if (total > 0) (openedCount.toFloat() / total) else 0f
            val boxedPercent = if (total > 0) (boxedCount.toFloat() / total) else 0f

            ProgressBarWithLabel(label = "Kutulu (MOC)", count = boxedCount, percent = boxedPercent, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            ProgressBarWithLabel(label = "Açık", count = openedCount, percent = openedPercent, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun BrandDistributionCard(brandStats: List<com.taytek.basehw.domain.model.BrandStats>, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Marka Dağılımı",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            brandStats.sortedByDescending { it.count }.forEachIndexed { index, stat ->
                val percent = if (total > 0) (stat.count.toFloat() / total) else 0f
                val color = when (stat.brand) {
                    Brand.HOT_WHEELS -> HotWheelsRed
                    Brand.MATCHBOX -> MatchboxBlue
                    Brand.MINI_GT -> MiniGTSilver
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
