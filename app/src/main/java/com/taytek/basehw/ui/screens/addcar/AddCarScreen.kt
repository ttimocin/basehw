package com.taytek.basehw.ui.screens.addcar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.ui.theme.HotWheelsRed
import com.taytek.basehw.ui.theme.MatchboxBlue
import com.taytek.basehw.ui.theme.MiniGTSilver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddCarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()

    // Navigate back on success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.clearSaveSuccess()
            onNavigateBack()
        }
    }

    val showSuggestions = uiState.selectedMasterData == null && uiState.searchQuery.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yeni Araba Ekle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
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
        ) {
            // ── Top section: brand + search ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Marka Seçimi - Filter Chips
                Text(
                    text = "Marka",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Brand.entries.forEach { brand ->
                        val brandColor = when (brand) {
                            Brand.HOT_WHEELS -> HotWheelsRed
                            Brand.MATCHBOX -> MatchboxBlue
                            Brand.MINI_GT -> MiniGTSilver
                        }
                        FilterChip(
                            selected = uiState.selectedBrand == brand,
                            onClick = { viewModel.onBrandSelected(brand) },
                            label = { Text(brand.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = brandColor.copy(alpha = 0.2f),
                                selectedLabelColor = brandColor
                            )
                        )
                    }
                }

                // Dinamik arama alanı
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Model Ara…") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.selectedMasterData != null) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Seçildi",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Search
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ── Suggestions (only when no master selected) ──
            if (showSuggestions) {
                if (searchResults.loadState.refresh is LoadState.Loading) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                } else if (searchResults.itemCount == 0 &&
                    searchResults.loadState.refresh is LoadState.NotLoading
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "\"${uiState.searchQuery}\" için sonuç bulunamadı.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Eşleşen Modeller",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            count = searchResults.itemCount,
                            key = searchResults.itemKey { it.id }
                        ) { index ->
                            val item = searchResults[index]
                            if (item != null) {
                                SuggestionItem(
                                    masterData = item,
                                    onClick = { viewModel.onMasterDataSelected(item) }
                                )
                            }
                        }
                    }
                }
            } else {
                // ── Detail form (shown after selection or when searching clears) ──
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Selected car preview
                    uiState.selectedMasterData?.let { master ->
                        SelectedCarPreview(masterData = master)
                    }

                    // Kutu / Açık Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Kutu Durumu", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (uiState.isOpened) "Açık (Kutusu Yok)" else "Kutulu (MOC)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isOpened,
                            onCheckedChange = viewModel::onIsOpenedChanged
                        )
                    }

                    // Depo Konumu
                    OutlinedTextField(
                        value = uiState.storageLocation,
                        onValueChange = viewModel::onStorageLocationChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Depo Konumu") },
                        placeholder = { Text("Örn: Raf A-3, Vitrinde…") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Kişisel Not
                    OutlinedTextField(
                        value = uiState.personalNote,
                        onValueChange = viewModel::onPersonalNoteChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        label = { Text("Kişisel Not") },
                        placeholder = { Text("Bu araba hakkında notlarınız…") },
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Ekle Butonları
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = viewModel::addCarToWishlist,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            enabled = uiState.selectedMasterData != null && !uiState.isSaving,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Arananlar", maxLines = 1)
                        }

                        Button(
                            onClick = viewModel::addCarToCollection,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            enabled = uiState.selectedMasterData != null && !uiState.isSaving,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Ekle", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    // Error snack
    uiState.error?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            confirmButton = {
                TextButton(onClick = viewModel::clearError) { Text("Tamam") }
            },
            title = { Text("Hata") },
            text = { Text(msg) }
        )
    }
}

@Composable
private fun SuggestionItem(masterData: MasterData, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(50.dp)
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

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = masterData.modelName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                masterData.year?.let {
                    Text(it.toString(), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (masterData.series.isNotBlank()) {
                    Text(
                        masterData.series,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedCarPreview(masterData: MasterData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (masterData.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = masterData.imageUrl,
                    contentDescription = masterData.modelName,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column {
                Text(
                    text = masterData.modelName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = listOfNotNull(
                        masterData.brand.displayName,
                        masterData.year?.toString(),
                        masterData.color.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
