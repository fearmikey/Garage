package com.fearmikey.garage.ui.mod

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.fearmikey.garage.ui.components.verticalScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.fearmikey.garage.data.local.entity.ModificationCategory
import com.fearmikey.garage.data.local.entity.ModificationRecord
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.fromUtcDatePickerMillis
import com.fearmikey.garage.ui.util.toDisplayDate
import com.fearmikey.garage.ui.util.toUtcDatePickerMillis
import java.io.File

@Composable
fun ModsScreen(
    viewModel: ModsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ModsContent(
        uiState = uiState,
        imageFileProvider = viewModel::imageFileFor,
        onToggleViewMode = viewModel::onToggleViewMode,
        onModClicked = viewModel::onModClicked,
        onDismissViewSheet = viewModel::onDismissViewSheet,
        onAddModClicked = viewModel::onAddModClicked,
        onEditModClicked = viewModel::onEditModClicked,
        onDismissSheet = viewModel::onDismissSheet,
        onTitleChanged = viewModel::onTitleChanged,
        onCategoryChanged = viewModel::onCategoryChanged,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onDateChanged = viewModel::onDateChanged,
        onCostChanged = viewModel::onCostChanged,
        onProductUrlChanged = viewModel::onProductUrlChanged,
        onImagesPicked = viewModel::onImagesPicked,
        onReplaceImagePicked = viewModel::onReplaceImagePicked,
        onRemovePhoto = viewModel::onRemovePhoto,
        onSaveMod = viewModel::onSaveMod,
        onDeleteMod = viewModel::onDeleteMod,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModsContent(
    uiState: ModsUiState,
    imageFileProvider: (String) -> File,
    onToggleViewMode: (Boolean) -> Unit,
    onModClicked: (ModificationRecord) -> Unit,
    onDismissViewSheet: () -> Unit,
    onAddModClicked: () -> Unit,
    onEditModClicked: (ModificationRecord) -> Unit,
    onDismissSheet: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onCategoryChanged: (ModificationCategory) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onDateChanged: (Long) -> Unit,
    onCostChanged: (String) -> Unit,
    onProductUrlChanged: (String) -> Unit,
    onImagesPicked: (List<Uri>) -> Unit,
    onReplaceImagePicked: (Int, Uri) -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onSaveMod: () -> Unit,
    onDeleteMod: (ModificationRecord) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddModClicked,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add modification")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.mods.isEmpty()) {
                EmptyState(
                    message = "No modifications logged yet.\nTap + to add a mod.",
                    icon = Icons.Default.Handyman,
                )
            } else if (uiState.isGridView) {
                val gridState = rememberLazyGridState()
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(gridState),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ModsSummaryHeader(
                            count = uiState.mods.size,
                            totalCost = uiState.totalCost,
                            currencySymbol = uiState.currencySymbol,
                            isGridView = true,
                            onToggleViewMode = onToggleViewMode,
                        )
                    }

                    items(uiState.mods, key = { it.id }) { mod ->
                        ModGridCard(
                            mod = mod,
                            currencySymbol = uiState.currencySymbol,
                            imageFileProvider = imageFileProvider,
                            onClick = { onModClicked(mod) },
                        )
                    }
                }
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(listState),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        ModsSummaryHeader(
                            count = uiState.mods.size,
                            totalCost = uiState.totalCost,
                            currencySymbol = uiState.currencySymbol,
                            isGridView = false,
                            onToggleViewMode = onToggleViewMode,
                        )
                    }

                    items(uiState.mods, key = { it.id }) { mod ->
                        ModCard(
                            mod = mod,
                            currencySymbol = uiState.currencySymbol,
                            imageFileProvider = imageFileProvider,
                            onClick = { onModClicked(mod) },
                        )
                    }
                }
            }
        }
    }

    uiState.viewingMod?.let { viewingMod ->
        ViewModSheet(
            mod = viewingMod,
            currencySymbol = uiState.currencySymbol,
            imageFileProvider = imageFileProvider,
            onDismiss = onDismissViewSheet,
            onEdit = onEditModClicked,
            onDelete = onDeleteMod,
        )
    }

    if (uiState.isSheetOpen) {
        AddEditModSheet(
            uiState = uiState,
            onDismiss = onDismissSheet,
            onTitleChanged = onTitleChanged,
            onCategoryChanged = onCategoryChanged,
            onDescriptionChanged = onDescriptionChanged,
            onDateChanged = onDateChanged,
            onCostChanged = onCostChanged,
            onProductUrlChanged = onProductUrlChanged,
            onImagesPicked = onImagesPicked,
            onReplaceImagePicked = onReplaceImagePicked,
            onRemovePhoto = onRemovePhoto,
            onSave = onSaveMod,
            onDelete = {
                uiState.editingModId?.let { id ->
                    uiState.mods.find { it.id == id }?.let(onDeleteMod)
                }
            },
        )
    }
}

@Composable
private fun ModsSummaryHeader(
    count: Int,
    totalCost: Double,
    currencySymbol: String,
    isGridView: Boolean,
    onToggleViewMode: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "$count Modifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (totalCost > 0) {
                    Text(
                        text = "Total Spent: %s%.2f".format(currencySymbol, totalCost),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            IconButton(onClick = { onToggleViewMode(!isGridView) }) {
                Icon(
                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                    contentDescription = if (isGridView) "Switch to list view" else "Switch to grid view",
                )
            }
        }
    }
}

@Composable
private fun ModGridCard(
    mod: ModificationRecord,
    currencySymbol: String,
    imageFileProvider: (String) -> File,
    onClick: () -> Unit,
) {
    val imageFiles = remember(mod.imageUris) {
        mod.imageUris.mapNotNull { uri ->
            val file = imageFileProvider(uri)
            if (file.exists()) file else null
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (imageFiles.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (imageFiles.size == 1) {
                        AsyncImage(
                            model = imageFiles.first(),
                            contentDescription = mod.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        val pagerState = rememberPagerState(pageCount = { imageFiles.size })
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            AsyncImage(
                                model = imageFiles[page],
                                contentDescription = mod.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                                    shape = CircleShape,
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(imageFiles.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(color, shape = CircleShape)
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = mod.category.icon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Text(
                    text = mod.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = mod.date.toDisplayDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )

                Spacer(modifier = Modifier.height(6.dp))

                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(
                            text = mod.category.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = mod.category.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                    },
                )

                if (mod.cost > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "%s%.2f".format(currencySymbol, mod.cost),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (mod.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mod.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModCard(
    mod: ModificationRecord,
    currencySymbol: String,
    imageFileProvider: (String) -> File,
    onClick: () -> Unit,
) {
    val imageFiles = remember(mod.imageUris) {
        mod.imageUris.mapNotNull { uri ->
            val file = imageFileProvider(uri)
            if (file.exists()) file else null
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (imageFiles.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (imageFiles.size == 1) {
                        AsyncImage(
                            model = imageFiles.first(),
                            contentDescription = mod.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        val pagerState = rememberPagerState(pageCount = { imageFiles.size })
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            AsyncImage(
                                model = imageFiles[page],
                                contentDescription = mod.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                                    shape = CircleShape,
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(imageFiles.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(color, shape = CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = mod.category.icon(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = mod.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = mod.date.toDisplayDate(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }

                    if (mod.cost > 0) {
                        Text(
                            text = "%s%.2f".format(currencySymbol, mod.cost),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (mod.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = mod.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (mod.productUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Product Link",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditModSheet(
    uiState: ModsUiState,
    onDismiss: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onCategoryChanged: (ModificationCategory) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onDateChanged: (Long) -> Unit,
    onCostChanged: (String) -> Unit,
    onProductUrlChanged: (String) -> Unit,
    onImagesPicked: (List<Uri>) -> Unit,
    onReplaceImagePicked: (Int, Uri) -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6),
    ) { uris ->
        if (uris.isNotEmpty()) onImagesPicked(uris)
    }

    var replacingIndex by remember { mutableStateOf<Int?>(null) }
    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val index = replacingIndex
        if (uri != null && index != null) {
            onReplaceImagePicked(index, uri)
        }
        replacingIndex = null
    }

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { BottomSheetDefaults.windowInsets },
    ) {
        val addEditScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .verticalScroll(addEditScrollState)
                .verticalScrollbar(addEditScrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (uiState.editingModId != null) "Edit Modification" else "Add Modification",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            // Photo picker / preview area
            if (uiState.photos.isNotEmpty()) {
                Column {
                    val pagerState = rememberPagerState(pageCount = { uiState.photos.size })
                    val currentPage = pagerState.currentPage.coerceIn(0, (uiState.photos.size - 1).coerceAtLeast(0))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            val photoItem = uiState.photos[page]
                            if (photoItem.file?.exists() == true) {
                                AsyncImage(
                                    model = photoItem.file,
                                    contentDescription = "Modification photo ${page + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        if (uiState.photos.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                                        shape = CircleShape,
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                repeat(uiState.photos.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(color, shape = CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (uiState.photos.size > 1) "Photo ${currentPage + 1} of ${uiState.photos.size}" else "Photo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.photos.size < 6) {
                                IconButton(
                                    onClick = {
                                        multiplePhotoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                        )
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.AddAPhoto,
                                        contentDescription = "Add photo",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    replacingIndex = currentPage
                                    singlePhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Change photo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = { onRemovePhoto(currentPage) },
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove photo",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            multiplePhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add Photos (up to 6)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChanged,
                label = { Text("Mod Title *") },
                placeholder = { Text("e.g., 2-inch Lift Kit, Cold Air Intake") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
            ) {
                OutlinedTextField(
                    value = uiState.category.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    leadingIcon = {
                        Icon(
                            imageVector = uiState.category.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                ) {
                    ModificationCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            leadingIcon = {
                                Icon(
                                    imageVector = category.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            onClick = {
                                onCategoryChanged(category)
                                categoryExpanded = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.description,
                onValueChange = onDescriptionChanged,
                label = { Text("Description / Notes") },
                placeholder = { Text("Details, part numbers, installation notes...") },
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.date.toDisplayDate(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Installation Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePickerDialog = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePickerDialog = true },
            )

            OutlinedTextField(
                value = uiState.cost,
                onValueChange = onCostChanged,
                label = { Text("Cost (Optional)") },
                prefix = { Text(uiState.currencySymbol) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.productUrl,
                onValueChange = onProductUrlChanged,
                label = { Text("Product Link / URL (Optional)") },
                placeholder = { Text("e.g. https://example.com/parts/lift-kit") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    capitalization = KeyboardCapitalization.None,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSave,
                enabled = uiState.title.isNotBlank() && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSaving) "Saving..." else "Save Modification")
            }

            if (uiState.editingModId != null) {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Modification")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.date.toUtcDatePickerMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            onDateChanged(utcMillis.fromUtcDatePickerMillis())
                        }
                        showDatePickerDialog = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Modification?") },
            text = { Text("Are you sure you want to delete '${uiState.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

private fun ModificationCategory.icon(): ImageVector = when (this) {
    ModificationCategory.PERFORMANCE -> Icons.Default.Speed
    ModificationCategory.SUSPENSION -> Icons.Default.Tune
    ModificationCategory.EXTERIOR -> Icons.Default.AutoAwesome
    ModificationCategory.INTERIOR -> Icons.Default.AirlineSeatReclineNormal
    ModificationCategory.LIGHTING -> Icons.Default.LightMode
    ModificationCategory.WHEELS_TIRES -> Icons.Default.Autorenew
    ModificationCategory.AUDIO_ELECTRICAL -> Icons.Default.Radio
    ModificationCategory.EXHAUST -> Icons.Default.Air
    ModificationCategory.OTHER -> Icons.Default.Build
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewModSheet(
    mod: ModificationRecord,
    currencySymbol: String,
    imageFileProvider: (String) -> File,
    onDismiss: () -> Unit,
    onEdit: (ModificationRecord) -> Unit,
    onDelete: (ModificationRecord) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    fun copyToClipboard(label: String, text: String) {
        if (text.isNotBlank()) {
            clipboardManager.setText(AnnotatedString(text))
            Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    val imageFiles = remember(mod.imageUris) {
        mod.imageUris.mapNotNull { uri ->
            val file = imageFileProvider(uri)
            if (file.exists()) file else null
        }
    }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { BottomSheetDefaults.windowInsets },
    ) {
        val detailScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .verticalScroll(detailScrollState)
                .verticalScrollbar(detailScrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { copyToClipboard("Title", mod.title) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = mod.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onEdit(mod) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Modification",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (imageFiles.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (imageFiles.size == 1) {
                        AsyncImage(
                            model = imageFiles.first(),
                            contentDescription = mod.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        val pagerState = rememberPagerState(pageCount = { imageFiles.size })
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            AsyncImage(
                                model = imageFiles[page],
                                contentDescription = mod.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                                    shape = CircleShape,
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(imageFiles.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(color, shape = CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssistChip(
                    onClick = { copyToClipboard("Category", mod.category.displayName) },
                    label = { Text(mod.category.displayName) },
                    leadingIcon = {
                        Icon(
                            imageVector = mod.category.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )

                if (mod.cost > 0) {
                    val costText = "%s%.2f".format(currencySymbol, mod.cost)
                    Text(
                        text = costText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { copyToClipboard("Cost", costText) },
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable { copyToClipboard("Installation date", mod.date.toDisplayDate()) },
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Installed: ${mod.date.toDisplayDate()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (mod.productUrl.isNotBlank()) {
                val formattedUrl = remember(mod.productUrl) {
                    if (!mod.productUrl.startsWith("http://") && !mod.productUrl.startsWith("https://")) {
                        "https://${mod.productUrl}"
                    } else {
                        mod.productUrl
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, formattedUrl.toUri())
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                }
                            },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = mod.productUrl,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    IconButton(
                        onClick = { copyToClipboard("Product link", formattedUrl) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Product Link",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            if (mod.description.isNotBlank()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { copyToClipboard("Description", mod.description) },
                ) {
                    Text(
                        text = "Notes / Description",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = mod.description,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }

                Button(
                    onClick = { onEdit(mod) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Modification?") },
            text = { Text("Are you sure you want to delete '${mod.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(mod)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ModsScreenPreview() {
    GarageTheme {
        ModsContent(
            uiState = ModsUiState(
                mods = listOf(
                    ModificationRecord(
                        id = 1,
                        vehicleId = 1,
                        title = "2-inch Lift Kit",
                        category = ModificationCategory.SUSPENSION,
                        description = "Fox 2.0 Performance Series Coilovers and rear shocks",
                        cost = 1450.00,
                    ),
                    ModificationRecord(
                        id = 2,
                        vehicleId = 1,
                        title = "TRD Pro Grille",
                        category = ModificationCategory.EXTERIOR,
                        description = "OEM Toyota TRD Pro grille swap with amber raptor lights",
                        cost = 220.00,
                    ),
                ),
                totalCost = 1670.00,
            ),
            imageFileProvider = { File("") },
            onToggleViewMode = {},
            onModClicked = {},
            onDismissViewSheet = {},
            onAddModClicked = {},
            onEditModClicked = {},
            onDismissSheet = {},
            onTitleChanged = {},
            onCategoryChanged = {},
            onDescriptionChanged = {},
            onDateChanged = {},
            onCostChanged = {},
            onProductUrlChanged = {},
            onImagesPicked = {},
            onReplaceImagePicked = { _, _ -> },
            onRemovePhoto = {},
            onSaveMod = {},
            onDeleteMod = {},
        )
    }
}
