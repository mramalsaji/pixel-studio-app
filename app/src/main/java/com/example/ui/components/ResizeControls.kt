package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PopularResizePresets
import com.example.domain.model.ResizeConfig
import com.example.ui.theme.*

@Composable
fun ResizeControls(
    originalWidth: Int,
    originalHeight: Int,
    config: ResizeConfig,
    onConfigChanged: (ResizeConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var widthText by remember(config.targetWidth, originalWidth) {
        val w = if (config.targetWidth > 0) config.targetWidth else originalWidth
        mutableStateOf(w.toString())
    }
    var heightText by remember(config.targetHeight, originalHeight) {
        val h = if (config.targetHeight > 0) config.targetHeight else originalHeight
        mutableStateOf(h.toString())
    }

    val (currentCalcW, currentCalcH) = remember(config, originalWidth, originalHeight) {
        config.calculateTargetDimensions(originalWidth, originalHeight)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StudioDarkSurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode switch: Pixels vs Percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Resize Dimensions",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = !config.isPercentageMode,
                    onClick = { onConfigChanged(config.copy(isPercentageMode = false)) },
                    label = { Text("Pixels", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("resize_mode_pixels")
                )
                FilterChip(
                    selected = config.isPercentageMode,
                    onClick = { onConfigChanged(config.copy(isPercentageMode = true)) },
                    label = { Text("Scale %", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("resize_mode_percent")
                )
            }
        }

        if (!config.isPercentageMode) {
            // Width and Height inputs with lock toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = widthText,
                    onValueChange = { newText ->
                        widthText = newText.filter { it.isDigit() }
                        val newW = widthText.toIntOrNull() ?: 0
                        if (config.maintainAspectRatio && originalWidth > 0 && originalHeight > 0 && newW > 0) {
                            val ratio = originalWidth.toFloat() / originalHeight.toFloat()
                            val newH = (newW / ratio).toInt().coerceAtLeast(1)
                            heightText = newH.toString()
                            onConfigChanged(config.copy(targetWidth = newW, targetHeight = newH))
                        } else {
                            onConfigChanged(config.copy(targetWidth = newW))
                        }
                    },
                    label = { Text("Width (px)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("resize_width_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderMedium,
                        focusedLabelColor = CyanAccent
                    )
                )

                IconButton(
                    onClick = {
                        onConfigChanged(config.copy(maintainAspectRatio = !config.maintainAspectRatio))
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(StudioDarkSurfaceElevated, RoundedCornerShape(10.dp))
                        .testTag("aspect_ratio_lock_toggle")
                ) {
                    Icon(
                        imageVector = if (config.maintainAspectRatio) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock aspect ratio",
                        tint = if (config.maintainAspectRatio) CyanAccent else TextTertiary
                    )
                }

                OutlinedTextField(
                    value = heightText,
                    onValueChange = { newText ->
                        heightText = newText.filter { it.isDigit() }
                        val newH = heightText.toIntOrNull() ?: 0
                        if (config.maintainAspectRatio && originalWidth > 0 && originalHeight > 0 && newH > 0) {
                            val ratio = originalWidth.toFloat() / originalHeight.toFloat()
                            val newW = (newH * ratio).toInt().coerceAtLeast(1)
                            widthText = newW.toString()
                            onConfigChanged(config.copy(targetWidth = newW, targetHeight = newH))
                        } else {
                            onConfigChanged(config.copy(targetHeight = newH))
                        }
                    },
                    label = { Text("Height (px)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("resize_height_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderMedium,
                        focusedLabelColor = CyanAccent
                    )
                )
            }

            // Quick Resolution Presets
            Text(
                text = "Popular Presets",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(PopularResizePresets) { preset ->
                    SuggestionChip(
                        onClick = {
                            widthText = preset.width.toString()
                            heightText = preset.height.toString()
                            onConfigChanged(
                                config.copy(
                                    targetWidth = preset.width,
                                    targetHeight = preset.height,
                                    maintainAspectRatio = false
                                )
                            )
                        },
                        label = {
                            Text("${preset.title} (${preset.width}x${preset.height})", fontSize = 11.sp)
                        }
                    )
                }
            }
        } else {
            // Percentage Slider & Quick chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scale: ${config.scalePercentage}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanAccent
                )
                Text(
                    text = "${currentCalcW} x ${currentCalcH} px",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Slider(
                value = config.scalePercentage.toFloat(),
                onValueChange = { onConfigChanged(config.copy(scalePercentage = it.toInt())) },
                valueRange = 10f..200f,
                steps = 18,
                colors = SliderDefaults.colors(
                    thumbColor = CyanAccent,
                    activeTrackColor = CyanAccent,
                    inactiveTrackColor = BorderSubtle
                ),
                modifier = Modifier.testTag("scale_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(25, 50, 75, 100, 150, 200).forEach { pct ->
                    FilterChip(
                        selected = config.scalePercentage == pct,
                        onClick = { onConfigChanged(config.copy(scalePercentage = pct)) },
                        label = { Text("${pct}%", fontSize = 11.sp) }
                    )
                }
            }
        }

        // Active output dimension preview badge
        Surface(
            color = StudioDarkSurfaceElevated,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Resulting Dimensions:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(
                    "$currentCalcW x $currentCalcH px",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            }
        }
    }
}
