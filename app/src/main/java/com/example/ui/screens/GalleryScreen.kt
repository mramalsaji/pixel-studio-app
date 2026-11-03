package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.ProcessedImageEntity
import com.example.domain.model.ExifData
import com.example.domain.processing.ExifHelper
import com.example.ui.AppScreen
import com.example.ui.EditorViewModel
import com.example.ui.components.ExifDetailsDialog
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    val images by viewModel.galleryImages.collectAsStateWithLifecycle()
    val selectedItem by viewModel.selectedGalleryItem.collectAsStateWithLifecycle()
    val multiSelectedIds by viewModel.galleryMultiSelectedIds.collectAsStateWithLifecycle()
    val formatFilter by viewModel.galleryFormatFilter.collectAsStateWithLifecycle()

    val filteredImages = remember(images, formatFilter) {
        if (formatFilter == null || formatFilter == "ALL") {
            images
        } else {
            images.filter { it.format.equals(formatFilter, ignoreCase = true) }
        }
    }

    val isMultiSelectMode = multiSelectedIds.isNotEmpty()

    var inspectedExif by remember { mutableStateOf<ExifData?>(null) }

    if (inspectedExif != null) {
        ExifDetailsDialog(
            exifData = inspectedExif!!,
            onDismiss = { inspectedExif = null }
        )
    }

    // Fullscreen Viewer Dialog
    if (selectedItem != null) {
        val item = selectedItem!!
        Dialog(
            onDismissRequest = { viewModel.selectGalleryItem(null) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = File(item.filePath),
                    contentDescription = item.originalFileName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.selectGalleryItem(null) },
                        modifier = Modifier
                            .background(StudioDarkSurfaceCard.copy(alpha = 0.8f), CircleShape)
                            .testTag("gallery_preview_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                val file = File(item.filePath)
                                inspectedExif = ExifHelper.extractExif(file)
                            },
                            modifier = Modifier.background(StudioDarkSurfaceCard.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "EXIF Details", tint = CyanAccent)
                        }

                        IconButton(
                            onClick = {
                                val shareIntent = viewModel.getShareIntent(item)
                                if (shareIntent != null) {
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                                }
                            },
                            modifier = Modifier.background(StudioDarkSurfaceCard.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = TextPrimary)
                        }

                        IconButton(
                            onClick = {
                                viewModel.deleteGalleryItem(item)
                            },
                            modifier = Modifier.background(StudioDarkSurfaceCard.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedAccent)
                        }
                    }
                }

                // Bottom details sheet
                Surface(
                    color = StudioDarkSurfaceCard.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.originalFileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                val sizeKb = item.fileSizeBytes / 1024f
                                val sizeStr = if (sizeKb > 1024) String.format("%.2f MB", sizeKb / 1024f) else String.format("%.0f KB", sizeKb)
                                Text(
                                    text = "${item.width} x ${item.height} px • $sizeStr • ${item.format}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            Button(
                                onClick = { viewModel.exportGalleryItemToPictures(item) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = StudioDarkBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("export_to_pictures_btn")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save to Pictures", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    if (isMultiSelectMode) {
                        Text(
                            text = "${multiSelectedIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    } else {
                        Text(
                            text = "In-App Gallery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isMultiSelectMode) {
                                viewModel.clearGallerySelection()
                            } else {
                                viewModel.navigateTo(AppScreen.HOME)
                            }
                        },
                        modifier = Modifier.testTag("gallery_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = { viewModel.exportSelectedGalleryItems() }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export Selected", tint = CyanAccent)
                        }
                        IconButton(onClick = {
                            val shareMultiple = viewModel.getShareSelectedIntent()
                            if (shareMultiple != null) {
                                context.startActivity(Intent.createChooser(shareMultiple, "Share Photos"))
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share Selected", tint = TextPrimary)
                        }
                        IconButton(onClick = { viewModel.deleteSelectedGalleryItems() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = RedAccent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioDarkBg)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "JPEG", "PNG", "WEBP").forEach { fmt ->
                    FilterChip(
                        selected = formatFilter == fmt,
                        onClick = { viewModel.setGalleryFormatFilter(fmt) },
                        label = { Text(fmt, fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_$fmt")
                    )
                }
            }

            if (filteredImages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "No processed photos found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Edits and batch conversions will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.HOME) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = StudioDarkBg)
                        ) {
                            Text("Go to Editor")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredImages, key = { it.id }) { item ->
                        GalleryItemCard(
                            entity = item,
                            isSelected = multiSelectedIds.contains(item.id),
                            isMultiSelectActive = isMultiSelectMode,
                            onClick = {
                                if (isMultiSelectMode) {
                                    viewModel.toggleGallerySelection(item.id)
                                } else {
                                    viewModel.selectGalleryItem(item)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleGallerySelection(item.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryItemCard(
    entity: ProcessedImageEntity,
    isSelected: Boolean,
    isMultiSelectActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = StudioDarkSurfaceCard,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, CyanAccent) else androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("gallery_item_${entity.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val context = LocalContext.current
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(File(entity.filePath))
                        .crossfade(200)
                        .build(),
                    contentDescription = entity.originalFileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Format badge
                Surface(
                    color = StudioDarkBg.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = entity.format,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Selection checkmark
                if (isMultiSelectActive) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(if (isSelected) CyanAccent else StudioDarkBg.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = StudioDarkBg,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = entity.originalFileName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1
                )
                val sizeKb = entity.fileSizeBytes / 1024f
                val sizeStr = if (sizeKb > 1024) String.format("%.1f MB", sizeKb / 1024f) else "${sizeKb.toInt()} KB"
                Text(
                    text = "${entity.width}x${entity.height} • $sizeStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
