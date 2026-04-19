package com.taytek.basehw.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.toColor
import com.taytek.basehw.ui.screens.detail.SavingAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDetailScreen(
    masterId: Long,
    onNavigateBack: () -> Unit,
    onAddCarClick: (Long) -> Unit,
    onNavigateToWishlist: () -> Unit = {},
    fromSth: Boolean = false,
    fromWishlist: Boolean = false,
    viewModel: MasterDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(masterId) {
        viewModel.loadMasterData(masterId)
    }

    LaunchedEffect(uiState.navigateToWishlist) {
        if (uiState.navigateToWishlist) {
            viewModel.clearNavigationEvent()
            onNavigateToWishlist()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.masterData?.modelName ?: stringResource(com.taytek.basehw.R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(com.taytek.basehw.R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.masterData == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(com.taytek.basehw.R.string.car_not_found), color = MaterialTheme.colorScheme.error)
            }
            else -> {
                val master = uiState.masterData!!
                val brandColor = master.brand.toColor()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Hero image removed as per user preference (no stock images)

                     Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                     ) {
                        // Brand and Premium info
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = {},
                                label = { Text(master.brand.displayName) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = brandColor.copy(alpha = 0.15f),
                                    labelColor = brandColor
                                )
                            )
                            if (master.brand == Brand.HOT_WHEELS) {
                                val feature = master.feature?.lowercase()
                                val isSth = feature == "sth"
                                val isChase = feature == "chase"
                                val isTh = feature == "th"
                                val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                                
                                val tierLabel = when {
                                    isSth -> "⭐ STH"
                                    isChase -> "🕶️ CHASE"
                                    isTh -> "🔥 TH"
                                    master.isPremium -> "🏁 " + stringResource(com.taytek.basehw.R.string.premium)
                                    else -> stringResource(com.taytek.basehw.R.string.regular)
                                }
                                val tierColor = when {
                                    isSth -> Color(0xFFB8860B)
                                    isChase -> if (isDarkTheme) Color(0xFFF1F5F9) else Color.Black
                                    isTh -> Color(0xFF71797E)
                                    master.isPremium -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            tierLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = tierColor
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = tierColor.copy(alpha = 0.12f),
                                        labelColor = tierColor
                                    )
                                )
                            }
                        }

                        Text(master.modelName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                        // Data Grid
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (master.colNum.isNotBlank()) MasterDetailRow(stringResource(com.taytek.basehw.R.string.col_num), "#${master.colNum}")
                                if (master.toyNum.isNotBlank()) MasterDetailRow(stringResource(com.taytek.basehw.R.string.toy_num), master.toyNum)
                                MasterDetailRow(stringResource(com.taytek.basehw.R.string.scale_label), master.scale.ifBlank { "1:64" })
                                master.year?.let { MasterDetailRow(stringResource(com.taytek.basehw.R.string.year_label), it.toString()) }
                                if (master.series.isNotBlank()) MasterDetailRow(stringResource(com.taytek.basehw.R.string.series_label), master.series)
                                if (master.seriesNum.isNotBlank()) MasterDetailRow(stringResource(com.taytek.basehw.R.string.series_num_label), master.seriesNum)
                                if (master.brand != Brand.MINI_GT && master.color.isNotBlank()) {
                                    MasterDetailRow(stringResource(com.taytek.basehw.R.string.color_label), master.color)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Add to Collection Button
                        Button(
                            onClick = { onAddCarClick(master.id) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(com.taytek.basehw.R.string.add_to_collection_btn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        if (!fromWishlist) {
                            // Arananlara Ekle button
                            OutlinedButton(
                                onClick = viewModel::addToWishlist,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                enabled = !uiState.isSaving,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.5.dp,
                                    if (!uiState.isSaving) Brand.HOT_WHEELS.toColor()
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Brand.HOT_WHEELS.toColor(),
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                )
                            ) {
                                if (uiState.savingAction == SavingAction.SINGLE) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Brand.HOT_WHEELS.toColor())
                                } else {
                                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(com.taytek.basehw.R.string.add_to_wanted_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Seri olarak Arananlara Ekle button (only if series exists, not from STH)
                            if (master.series.isNotBlank() && !fromSth) {
                                OutlinedButton(
                                    onClick = viewModel::addSeriesToWishlist,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    enabled = !uiState.isSaving,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (!uiState.isSaving) Color(0xFFFF8C00)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFFF8C00),
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                    )
                                ) {
                                    if (uiState.savingAction == SavingAction.SERIES) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color(0xFFFF8C00))
                                    } else {
                                        Text(stringResource(com.taytek.basehw.R.string.add_series_to_wanted), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MasterDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
