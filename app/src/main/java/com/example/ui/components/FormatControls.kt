package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ImageFormat
import com.example.ui.theme.*

@Composable
fun FormatControls(
    selectedFormat: ImageFormat,
    onFormatSelected: (ImageFormat) -> Unit,
    quality: Int,
    onQualityChanged: (Int) -> Unit,
    stripMetadata: Boolean,
    onStripMetadataChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    estimatedSizeBytes: Long? = null,
    originalSizeBytes: Long = 0L,
    isEstimating: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StudioDarkSurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Format & Compression",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = StudioDarkSurfaceElevated,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = selectedFormat.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Format selector cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ImageFormat.values().forEach { format ->
                val isSelected = format == selectedFormat
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) CyanAccent else Color.Transparent,
                    label = "border_${format.name}"
                )
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) StudioDarkSurfaceElevated else StudioDarkSurface,
                    label = "bg_${format.name}"
                )

                Surface(
                    onClick = { onFormatSelected(format) },
                    shape = RoundedCornerShape(12.dp),
                    color = bgColor,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("format_pill_${format.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = format.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) CyanAccent else TextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = when (format) {
                                ImageFormat.JPEG -> "Universal"
                                ImageFormat.PNG -> "Lossless"
                                ImageFormat.WEBP -> "Next-Gen"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) TextPrimary else TextTertiary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Quality slider & Real-time File Size Preview
        if (selectedFormat.supportsQuality) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(StudioDarkSurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Compression Quality",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    val qualityTier = when {
                        quality >= 90 -> "Ultra (${quality}%)"
                        quality >= 75 -> "High (${quality}%)"
                        quality >= 50 -> "Balanced (${quality}%)"
                        else -> "Compact (${quality}%)"
                    }
                    Text(
                        text = qualityTier,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                }

                Slider(
                    value = quality.toFloat(),
                    onValueChange = { onQualityChanged(it.toInt()) },
                    valueRange = 10f..100f,
                    steps = 17,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanAccent,
                        activeTrackColor = CyanAccent,
                        inactiveTrackColor = BorderMedium
                    ),
                    modifier = Modifier.testTag("quality_slider")
                )

                // LIVE FILE SIZE PREVIEW CARD
                if (estimatedSizeBytes != null || isEstimating) {
                    Divider(color = BorderSubtle)
                    LiveSizePreviewBadge(
                        estimatedBytes = estimatedSizeBytes ?: 0L,
                        originalBytes = originalSizeBytes,
                        isEstimating = isEstimating
                    )
                }
            }
        } else {
            // Lossless info
            Surface(
                color = StudioDarkSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Text(
                            text = "PNG is lossless. Quality compression is fixed at 100% for transparency and precision.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (estimatedSizeBytes != null || isEstimating) {
                        LiveSizePreviewBadge(
                            estimatedBytes = estimatedSizeBytes ?: 0L,
                            originalBytes = originalSizeBytes,
                            isEstimating = isEstimating
                        )
                    }
                }
            }
        }

        // Privacy & Metadata Strip Toggle
        Surface(
            color = StudioDarkSurfaceElevated,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = if (stripMetadata) AmberAccent else GreenAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (stripMetadata) "Strip EXIF (Privacy Mode)" else "Preserve EXIF Metadata",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = if (stripMetadata)
                            "GPS coordinates, camera specs & timestamps will be removed."
                        else
                            "Camera tags, capture time & location will be retained.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Switch(
                    checked = stripMetadata,
                    onCheckedChange = onStripMetadataChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AmberAccent,
                        checkedTrackColor = StudioDarkSurfaceCard,
                        uncheckedThumbColor = CyanAccent,
                        uncheckedTrackColor = StudioDarkSurfaceCard
                    ),
                    modifier = Modifier.testTag("strip_metadata_switch")
                )
            }
        }
    }
}

@Composable
private fun LiveSizePreviewBadge(
    estimatedBytes: Long,
    originalBytes: Long,
    isEstimating: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(StudioDarkSurfaceCard)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Speed, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
            Text(
                text = "Estimated Output:",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }

        if (isEstimating) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = CyanAccent
            )
        } else {
            val formattedEst = formatFileSize(estimatedBytes)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "~$formattedEst",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )

                if (originalBytes > 0 && estimatedBytes > 0) {
                    val savingsRatio = (originalBytes - estimatedBytes).toFloat() / originalBytes.toFloat()
                    val pct = (savingsRatio * 100f).toInt()
                    if (pct > 0) {
                        Surface(
                            color = GreenAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "↓ $pct%",
                                color = GreenAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else if (pct < -5) {
                        Surface(
                            color = AmberAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "↑ ${-pct}%",
                                color = AmberAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024f
    return if (kb > 1024) {
        String.format("%.2f MB", kb / 1024f)
    } else {
        String.format("%.0f KB", kb)
    }
}
