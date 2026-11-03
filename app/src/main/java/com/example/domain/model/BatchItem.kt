package com.example.domain.model

import android.net.Uri
import com.example.data.local.ProcessedImageEntity

enum class BatchItemStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    ERROR
}

data class BatchItem(
    val id: String,
    val uri: Uri,
    val fileName: String,
    val originalSizeBytes: Long = 0L,
    val status: BatchItemStatus = BatchItemStatus.PENDING,
    val progress: Float = 0f,
    val resultEntity: ProcessedImageEntity? = null,
    val errorMessage: String? = null
)
