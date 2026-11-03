package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_images")
data class ProcessedImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val originalFileName: String,
    val format: String, // "JPEG", "PNG", "WEBP"
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val originalSizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isMetadataPreserved: Boolean = true,
    val batchId: String? = null
)
