package com.taytek.basehw.ui.screens.addcar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.toColor
import com.taytek.basehw.ui.theme.HotWheelsRed
import com.taytek.basehw.domain.model.MasterData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWantedCarScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddWantedCarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()

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
                title = {
                    Text(
                        text = stringResource(com.taytek.basehw.R.string.add_to_wanted_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.onSearchQueryChanged("")
                    }) {
                        Text(
                            text = stringResource(com.taytek.basehw.R.string.reset_btn),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FigmaSectionLabel(stringResource(com.taytek.basehw.R.string.section_brand))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(Brand.entries.toTypedArray()) { brand ->
                        val brandColor = brand.toColor()
                        val isSelected = uiState.selectedBrand == brand
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.onBrandSelected(brand) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) brandColor else brandColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = brand.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) androidx.compose.ui.graphics.Color.White else brandColor,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold
                                             else androidx.compose.ui.text.font.FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FigmaSectionLabel(stringResource(com.taytek.basehw.R.string.section_model_details))
                    
                    val brandColor = uiState.selectedBrand?.toColor() ?: HotWheelsRed
                    
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.toggleManualMode() },
                        shape = RoundedCornerShape(20.dp),
                        color = if (uiState.isManualMode) brandColor else brandColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = if (uiState.isManualMode) Color.White else brandColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(com.taytek.basehw.R.string.manual_mode_btn),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (uiState.isManualMode) Color.White else brandColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!uiState.isManualMode) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(com.taytek.basehw.R.string.search_model_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingIcon = {
                            if (uiState.selectedMasterData != null) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Search
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            if (showSuggestions && !uiState.isManualMode) {
                if (searchResults.loadState.refresh is LoadState.Loading) {
                    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                } else if (searchResults.itemCount == 0 && searchResults.loadState.refresh is LoadState.NotLoading) {
                    Text(
                        text = stringResource(com.taytek.basehw.R.string.no_results_for, uiState.searchQuery),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    Text(
                        text = stringResource(com.taytek.basehw.R.string.matching_models),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(count = searchResults.itemCount, key = searchResults.itemKey { it.id }) { index ->
                            val item = searchResults[index]
                            if (item != null) {
                                SuggestionItem(masterData = item, onClick = { viewModel.onMasterDataSelected(item) })
                            }
                        }
                    }
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.isManualMode) {
                        OutlinedTextField(
                            value = uiState.manualModelName,
                            onValueChange = viewModel::onManualModelNameChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(com.taytek.basehw.R.string.manual_model_name)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.manualScale,
                                onValueChange = viewModel::onManualScaleChanged,
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(com.taytek.basehw.R.string.manual_scale)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = uiState.manualYear,
                                onValueChange = viewModel::onManualYearChanged,
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(com.taytek.basehw.R.string.manual_year)) },
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.manualSeries,
                                onValueChange = viewModel::onManualSeriesChanged,
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(com.taytek.basehw.R.string.manual_series)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = uiState.manualSeriesNum,
                                onValueChange = viewModel::onManualSeriesNumChanged,
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(com.taytek.basehw.R.string.manual_series_num)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = uiState.manualIsPremium,
                                onCheckedChange = viewModel::onManualIsPremiumChanged
                            )
                            Text(
                                text = stringResource(com.taytek.basehw.R.string.manual_is_premium),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.personalNote,
                        onValueChange = viewModel::onPersonalNoteChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(com.taytek.basehw.R.string.personal_note)) },
                        placeholder = { Text(stringResource(com.taytek.basehw.R.string.notes_placeholder)) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = viewModel::addCarToWishlist,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = (uiState.selectedMasterData != null || uiState.isManualMode) && !uiState.isSaving,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, if ((uiState.selectedMasterData != null || uiState.isManualMode) && !uiState.isSaving) Brand.HOT_WHEELS.toColor() else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Brand.HOT_WHEELS.toColor(),
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Brand.HOT_WHEELS.toColor())
                        } else {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(com.taytek.basehw.R.string.add_to_wanted_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                        }
                    }

                    if (uiState.selectedMasterData?.series?.isNotBlank() == true) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = viewModel::addSeriesToWishlist,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !uiState.isSaving,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, if (!uiState.isSaving) Color(0xFFFF8C00) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF8C00),
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color(0xFFFF8C00))
                            } else {
                                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(com.taytek.basehw.R.string.add_series_to_wanted),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    uiState.error?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            confirmButton = {
                TextButton(onClick = viewModel::clearError) { Text(stringResource(com.taytek.basehw.R.string.ok)) }
            },
            title = { Text(stringResource(com.taytek.basehw.R.string.error_title)) },
            text = { Text(msg) }
        )
    }
}

@Composable
private fun FigmaSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    )
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
                Text(
                    masterData.scale.ifBlank { "1:64" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
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
                        masterData.scale.ifBlank { "1:64" },
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
