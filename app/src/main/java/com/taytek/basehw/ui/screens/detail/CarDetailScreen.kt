package com.taytek.basehw.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.ui.theme.HotWheelsRed
import com.taytek.basehw.ui.theme.MatchboxBlue
import com.taytek.basehw.ui.theme.MiniGTSilver
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailScreen(
    carId: Long,
    onNavigateBack: () -> Unit,
    viewModel: CarDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale("tr")) }

    LaunchedEffect(carId) { viewModel.loadCar(carId) }
    LaunchedEffect(uiState.isDeleted) { if (uiState.isDeleted) onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.car?.masterData?.modelName ?: "Detay") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
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
            uiState.car == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Araç bulunamadı.", color = MaterialTheme.colorScheme.error)
            }
            else -> {
                val car = uiState.car!!
                val master = car.masterData
                val brandColor = when (master?.brand) {
                    Brand.HOT_WHEELS -> HotWheelsRed
                    Brand.MATCHBOX -> MatchboxBlue
                    Brand.MINI_GT -> MiniGTSilver
                    null -> MaterialTheme.colorScheme.primary
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Hero image
                    if (!master?.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = master!!.imageUrl,
                            contentDescription = master.modelName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Brand chip
                        master?.brand?.let { brand ->
                            AssistChip(
                                onClick = {},
                                label = { Text(brand.displayName) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = brandColor.copy(alpha = 0.15f),
                                    labelColor = brandColor
                                )
                            )
                        }

                        // Model info
                        Text(master?.modelName ?: "", style = MaterialTheme.typography.headlineMedium)

                        // Details grid
                        master?.colNum?.takeIf { it.isNotBlank() }?.let {
                            DetailRow("Koleksiyon No", "#$it")
                        }
                        master?.toyNum?.takeIf { it.isNotBlank() }?.let {
                            DetailRow("Ürün No", it)
                        }
                        DetailRow("Ölçek", master?.scale ?: "1:64")
                        master?.year?.let { DetailRow("Yıl", it.toString()) }
                        master?.series?.takeIf { it.isNotBlank() }?.let {
                            DetailRow("Seri", it)
                        }
                        master?.seriesNum?.takeIf { it.isNotBlank() }?.let {
                            DetailRow("Seri No", it)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                        // User data
                        DetailRow("Durum", if (car.isOpened) "Açık" else "📦 Kutulu")
                        car.storageLocation.takeIf { it.isNotBlank() }?.let {
                            DetailRow("Depo", it)
                        }
                        car.purchaseDate?.let {
                            DetailRow("Alım Tarihi", dateFormat.format(it))
                        }
                        car.personalNote.takeIf { it.isNotBlank() }?.let {
                            Column {
                                Text("Not", style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Card(
                                    Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text(it, modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Arabayı Sil") },
            text = { Text("Bu araç koleksiyonunuzdan kalıcı olarak silinecek. Emin misiniz?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.deleteCar() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("İptal") }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}
