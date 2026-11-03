package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.CropRatio
import com.example.ui.AppScreen
import com.example.ui.EditorViewModel
import com.example.ui.components.BatchProgressDialog
import com.example.ui.components.FormatControls
import com.example.ui.components.ResizeControls
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    val batchItems by viewModel.batchItems.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isBatchProcessing.collectAsStateWithLifecycle()
    val showProgressDialog by viewModel.showBatchProgressDialog.collectAsStateWithLifecycle()
    val batchFormat by viewModel.batchFormat.collectAsStateWithLifecycle()
    val batchQuality by viewModel.batchQuality.collectAsStateWithLifecycle()
    val batchCropRatio by viewModel.batchCropRatio.collectAsStateWithLifecycle()
    val batchResizeConfig by viewModel.batchResizeConfig.collectAsStateWithLifecycle()
    val batchStripMetadata by viewModel.batchStripMetadata.collectAsStateWithLifecycle()

    val batchPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.loadBatchImages(uris)
        }
    }

    if (showProgressDialog) {
        BatchProgressDialog(
            items = batchItems,
            isProcessing = isProcessing,
            onCancel = { viewModel.cancelBatchProcessing() },
            onCompleteDismiss = { viewModel.dismissBatchProgressDialog() }
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
                            text = "Batch Processing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${batchItems.size} photos selected",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier.testTag("batch_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            batchPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("batch_add_more_btn")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Photos", color = CyanAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioDarkBg)
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
                    Column {
                        Text(
                            text = "Target Format",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${batchFormat.displayName} (${batchQuality}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }

                    Button(
                        onClick = { viewModel.startBatchProcessing() },
                        enabled = batchItems.isNotEmpty() && !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VioletAccent,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("batch_start_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Process ${batchItems.size} Photos",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Selected Photos Carousel
                Text(
                    text = "Selected Queue (${batchItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            item {
                if (batchItems.isEmpty()) {
                    Surface(
                        color = StudioDarkSurfaceCard,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(36.dp))
                            Text("No photos selected for batch processing.", color = TextSecondary)
                            Button(
                                onClick = {
                                    batchPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = StudioDarkBg)
                            ) {
                                Text("Select Photos")
                            }
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(batchItems) { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StudioDarkSurfaceElevated,
                                modifier = Modifier.size(width = 90.dp, height = 110.dp)
                            ) {
                                Column {
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = item.fileName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(75.dp)
                                    )
                                    Text(
                                        text = item.fileName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Uniform Adjustments Configuration
            item {
                Text(
                    text = "Uniform Adjustments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Uniform Aspect Ratio Crop Preset
            item {
                Surface(
                    color = StudioDarkSurfaceCard,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Batch Centered Crop Preset",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Applies uniform center-focused cropping across all photos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(CropRatio.values()) { ratio ->
                                FilterChip(
                                    selected = batchCropRatio == ratio,
                                    onClick = { viewModel.setBatchCropRatio(ratio) },
                                    label = { Text(ratio.label, fontSize = 12.sp) },
                                    modifier = Modifier.testTag("batch_crop_ratio_${ratio.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Uniform Resize Controls
            item {
                ResizeControls(
                    originalWidth = 1920,
                    originalHeight = 1080,
                    config = batchResizeConfig,
                    onConfigChanged = { viewModel.setBatchResizeConfig(it) }
                )
            }

            // Uniform Format & EXIF Controls
            item {
                FormatControls(
                    selectedFormat = batchFormat,
                    onFormatSelected = { viewModel.setBatchFormat(it) },
                    quality = batchQuality,
                    onQualityChanged = { viewModel.setBatchQuality(it) },
                    stripMetadata = batchStripMetadata,
                    onStripMetadataChanged = { viewModel.setBatchStripMetadata(it) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
