package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.BatchItem
import com.example.domain.model.BatchItemStatus
import com.example.ui.theme.*

@Composable
fun BatchProgressDialog(
    items: List<BatchItem>,
    isProcessing: Boolean,
    onCancel: () -> Unit,
    onCompleteDismiss: () -> Unit
) {
    val totalCount = items.size
    val completedCount = items.count { it.status == BatchItemStatus.SUCCESS || it.status == BatchItemStatus.ERROR }
    val progressFloat = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val isFinished = !isProcessing && completedCount == totalCount && totalCount > 0

    AlertDialog(
        onDismissRequest = { if (isFinished) onCompleteDismiss() },
        containerColor = StudioDarkSurfaceCard,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isFinished) "Batch Completed!" else "Batch Processing...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isFinished) GreenAccent else CyanAccent
                )
                Text(
                    text = "$completedCount of $totalCount items processed",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progressFloat },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .testTag("batch_linear_progress"),
                    color = if (isFinished) GreenAccent else CyanAccent,
                    trackColor = BorderSubtle,
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StudioDarkSurfaceElevated, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    color = TextPrimary
                                )
                                item.errorMessage?.let { err ->
                                    Text(
                                        text = err,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RedAccent
                                    )
                                }
                            }

                            when (item.status) {
                                BatchItemStatus.PENDING -> {
                                    Icon(
                                        Icons.Default.HourglassEmpty,
                                        contentDescription = "Pending",
                                        tint = TextTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                BatchItemStatus.PROCESSING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = CyanAccent
                                    )
                                }
                                BatchItemStatus.SUCCESS -> {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = GreenAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                BatchItemStatus.ERROR -> {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = RedAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isFinished) {
                Button(
                    onClick = onCompleteDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = StudioDarkBg),
                    modifier = Modifier.testTag("batch_complete_dismiss_btn")
                ) {
                    Text("View Results", fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAccent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RedAccent),
                    modifier = Modifier.testTag("batch_cancel_btn")
                ) {
                    Text("Cancel Batch")
                }
            }
        }
    )
}
