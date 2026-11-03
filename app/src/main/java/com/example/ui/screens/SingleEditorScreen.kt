package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.CropRatio
import com.example.ui.AppScreen
import com.example.ui.EditorTab
import com.example.ui.EditorViewModel
import com.example.ui.components.CropCanvas
import com.example.ui.components.ExifDetailsDialog
import com.example.ui.components.FormatControls
import com.example.ui.components.ResizeControls
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleEditorScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    val bitmap by viewModel.singleBitmap.collectAsStateWithLifecycle()
    val fileName by viewModel.singleFileName.collectAsStateWithLifecycle()
    val singleFileSize by viewModel.singleFileSize.collectAsStateWithLifecycle()
    val cropRatio by viewModel.cropRatio.collectAsStateWithLifecycle()
    val cropRect by viewModel.cropRect.collectAsStateWithLifecycle()
    val resizeConfig by viewModel.resizeConfig.collectAsStateWithLifecycle()
    val selectedFormat by viewModel.selectedFormat.collectAsStateWithLifecycle()
    val quality by viewModel.quality.collectAsStateWithLifecycle()
    val stripMetadata by viewModel.stripMetadata.collectAsStateWithLifecycle()
    val exifData by viewModel.exifData.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeEditorTab.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isSingleProcessing.collectAsStateWithLifecycle()
    val originalDimensions by viewModel.originalDimensions.collectAsStateWithLifecycle()
    val estimatedSizeBytes by viewModel.estimatedOutputSizeBytes.collectAsStateWithLifecycle()
    val isEstimatingSize by viewModel.isEstimatingSize.collectAsStateWithLifecycle()

    var showExifDialog by remember { mutableStateOf(false) }

    if (showExifDialog) {
        ExifDetailsDialog(
            exifData = exifData,
            onDismiss = { showExifDialog = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = fileName.ifBlank { "Photo Editor" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "${originalDimensions.first} x ${originalDimensions.second} px • ${formatFileSizeShort(singleFileSize)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier.testTag("single_editor_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showExifDialog = true },
                        modifier = Modifier.testTag("single_editor_exif_action_btn")
                    ) {
                        Badge(
                            containerColor = if (exifData.hasExif) CyanAccent else StudioDarkSurfaceElevated,
                            contentColor = StudioDarkBg
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "EXIF Info",
                                tint = if (exifData.hasExif) StudioDarkBg else TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioDarkBg
                )
            )
        },
        bottomBar = {
            Surface(
                color = StudioDarkSurfaceCard,
                tonalElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val (calcW, calcH) = resizeConfig.calculateTargetDimensions(
                        originalDimensions.first,
                        originalDimensions.second
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Save as ${selectedFormat.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            if (estimatedSizeBytes != null) {
                                Surface(
                                    color = StudioDarkSurfaceElevated,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "~${formatFileSizeShort(estimatedSizeBytes!!)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "$calcW x $calcH px",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.saveSingleImage {
                                viewModel.navigateTo(AppScreen.GALLERY)
                            }
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = StudioDarkBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_and_process_btn")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = StudioDarkBg
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Process & Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Interactive Crop View Canvas (takes upper portion of screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
                    .background(Color.Black)
            ) {
                CropCanvas(
                    bitmap = bitmap,
                    cropRatio = cropRatio,
                    normalizedCropRect = cropRect,
                    onCropRectChanged = { viewModel.updateCropRect(it) }
                )
            }

            // Tab bar selector with AMOLED styling
            SecondaryTabRow(
                selectedTabIndex = activeTab.ordinal,
                containerColor = StudioDarkSurface,
                contentColor = CyanAccent
            ) {
                Tab(
                    selected = activeTab == EditorTab.CROP,
                    onClick = { viewModel.setActiveEditorTab(EditorTab.CROP) },
                    text = { Text("Crop") },
                    icon = { Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_crop")
                )
                Tab(
                    selected = activeTab == EditorTab.RESIZE,
                    onClick = { viewModel.setActiveEditorTab(EditorTab.RESIZE) },
                    text = { Text("Resize") },
                    icon = { Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_resize")
                )
                Tab(
                    selected = activeTab == EditorTab.FORMAT,
                    onClick = { viewModel.setActiveEditorTab(EditorTab.FORMAT) },
                    text = { Text("Format") },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_format")
                )
            }

            // Animated Tab Content (smooth 120Hz spring motion)
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f),
                label = "editor_tab_animation"
            ) { tab ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (tab) {
                        EditorTab.CROP -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Rotation and Flip Controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.rotate90() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Rotate 90°", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.toggleFlipHorizontal() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Flip H", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.toggleFlipVertical() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Flip V", fontSize = 12.sp)
                                    }
                                }

                                // Aspect Ratio chips
                                Text("Aspect Ratio", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(CropRatio.values()) { ratio ->
                                        FilterChip(
                                            selected = cropRatio == ratio,
                                            onClick = { viewModel.setCropRatio(ratio) },
                                            label = { Text(ratio.label, fontSize = 12.sp) },
                                            modifier = Modifier.testTag("crop_ratio_${ratio.name.lowercase()}")
                                        )
                                    }
                                }

                                Surface(
                                    color = StudioDarkSurfaceCard,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Drag the Amber corner handles or inside the frame to adjust your crop zone.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                        EditorTab.RESIZE -> {
                            ResizeControls(
                                originalWidth = originalDimensions.first,
                                originalHeight = originalDimensions.second,
                                config = resizeConfig,
                                onConfigChanged = { viewModel.setResizeConfig(it) }
                            )
                        }
                        EditorTab.FORMAT -> {
                            FormatControls(
                                selectedFormat = selectedFormat,
                                onFormatSelected = { viewModel.setSelectedFormat(it) },
                                quality = quality,
                                onQualityChanged = { viewModel.setQuality(it) },
                                stripMetadata = stripMetadata,
                                onStripMetadataChanged = { viewModel.setStripMetadata(it) },
                                estimatedSizeBytes = estimatedSizeBytes,
                                originalSizeBytes = singleFileSize,
                                isEstimating = isEstimatingSize
                            )
                        }
                        EditorTab.EXIF -> {}
                    }
                }
            }
        }
    }
}

private fun formatFileSizeShort(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024f
    return if (kb > 1024) String.format("%.2f MB", kb / 1024f) else "${kb.toInt()} KB"
}
