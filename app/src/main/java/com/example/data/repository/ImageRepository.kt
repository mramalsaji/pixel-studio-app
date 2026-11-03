package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ProcessedImageEntity
import com.example.domain.model.CropRatio
import com.example.domain.model.ImageFormat
import com.example.domain.model.ResizeConfig
import com.example.domain.processing.ExifHelper
import com.example.domain.processing.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class ImageRepository(private val context: Context) {
    private val dao = AppDatabase.getInstance(context).processedImageDao()
    private val processedDir = File(context.filesDir, "processed").apply { mkdirs() }

    val allProcessedImages: Flow<List<ProcessedImageEntity>> = dao.getAllImages()

    suspend fun getImageById(id: Long): ProcessedImageEntity? = withContext(Dispatchers.IO) {
        dao.getImageById(id)
    }

    suspend fun processAndSaveImage(
        sourceUri: Uri,
        format: ImageFormat,
        quality: Int,
        cropRectNormalized: android.graphics.RectF?,
        rotationDegrees: Float,
        flipHorizontal: Boolean,
        flipVertical: Boolean,
        resizeConfig: ResizeConfig,
        stripMetadata: Boolean,
        batchId: String? = null
    ): ProcessedImageEntity? = withContext(Dispatchers.IO) {
        try {
            val (originalName, originalSize) = ImageProcessor.getFileNameAndSize(context, sourceUri)
            val baseBitmap = ImageProcessor.loadBitmap(context, sourceUri) ?: return@withContext null

            // 1. Rotation & Flip
            var currentBitmap = if (rotationDegrees != 0f) {
                ImageProcessor.rotateBitmap(baseBitmap, rotationDegrees)
            } else {
                baseBitmap
            }

            if (flipHorizontal || flipVertical) {
                currentBitmap = ImageProcessor.flipBitmap(currentBitmap, flipHorizontal, flipVertical)
            }

            // 2. Crop
            val croppedBitmap = if (cropRectNormalized != null &&
                (cropRectNormalized.left > 0.001f || cropRectNormalized.top > 0.001f ||
                 cropRectNormalized.right < 0.999f || cropRectNormalized.bottom < 0.999f)) {
                ImageProcessor.cropBitmap(currentBitmap, cropRectNormalized)
            } else {
                currentBitmap
            }

            // 3. Resize
            val (targetW, targetH) = resizeConfig.calculateTargetDimensions(
                croppedBitmap.width,
                croppedBitmap.height
            )
            val finalBitmap = ImageProcessor.resizeBitmap(croppedBitmap, targetW, targetH)

            // 4. Save to App Storage
            val cleanBaseName = originalName.substringBeforeLast(".")
            val outputFileName = "PF_${cleanBaseName}_${System.currentTimeMillis()}.${format.extension}"
            val outputFile = File(processedDir, outputFileName)

            val saved = ImageProcessor.saveBitmapToFile(finalBitmap, format, quality, outputFile)
            if (!saved) return@withContext null

            // 5. EXIF Metadata preservation (or strip)
            ExifHelper.copyExif(context, sourceUri, outputFile, stripMetadata)

            val newFileSize = outputFile.length()
            val entity = ProcessedImageEntity(
                filePath = outputFile.absolutePath,
                originalFileName = originalName,
                format = format.name,
                width = finalBitmap.width,
                height = finalBitmap.height,
                fileSizeBytes = newFileSize,
                originalSizeBytes = if (originalSize > 0) originalSize else newFileSize,
                timestamp = System.currentTimeMillis(),
                isMetadataPreserved = !stripMetadata,
                batchId = batchId
            )

            val newId = dao.insert(entity)
            entity.copy(id = newId)
        } catch (e: Exception) {
            Log.e("ImageRepository", "Error processing image: $sourceUri", e)
            null
        }
    }

    suspend fun deleteImage(entity: ProcessedImageEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(entity.filePath)
            if (file.exists()) {
                file.delete()
            }
            dao.deleteById(entity.id)
        } catch (e: Exception) {
            Log.e("ImageRepository", "Error deleting image ${entity.id}", e)
        }
    }

    suspend fun deleteImages(entities: List<ProcessedImageEntity>) = withContext(Dispatchers.IO) {
        val ids = entities.map { it.id }
        for (entity in entities) {
            val file = File(entity.filePath)
            if (file.exists()) file.delete()
        }
        dao.deleteByIds(ids)
    }

    suspend fun exportToGallery(entity: ProcessedImageEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(entity.filePath)
            if (!file.exists()) return@withContext false

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                val mimeType = when (entity.format.uppercase()) {
                    "PNG" -> "image/png"
                    "WEBP" -> "image/webp"
                    else -> "image/jpeg"
                }
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.WIDTH, entity.width)
                put(MediaStore.Images.Media.HEIGHT, entity.height)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + File.separator + "PhotoForge"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val uri = resolver.insert(collection, contentValues) ?: return@withContext false

            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(file).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            true
        } catch (e: Exception) {
            Log.e("ImageRepository", "Failed to export image to device gallery", e)
            false
        }
    }

    fun createShareIntent(entity: ProcessedImageEntity): Intent? {
        return try {
            val file = File(entity.filePath)
            if (!file.exists()) return null

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val mimeType = when (entity.format.uppercase()) {
                "PNG" -> "image/png"
                "WEBP" -> "image/webp"
                else -> "image/jpeg"
            }

            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Log.e("ImageRepository", "Error creating share intent", e)
            null
        }
    }

    fun createShareMultipleIntent(entities: List<ProcessedImageEntity>): Intent? {
        return try {
            val authority = "${context.packageName}.fileprovider"
            val uris = ArrayList<Uri>()
            for (entity in entities) {
                val file = File(entity.filePath)
                if (file.exists()) {
                    uris.add(FileProvider.getUriForFile(context, authority, file))
                }
            }
            if (uris.isEmpty()) return null

            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Log.e("ImageRepository", "Error creating multi share intent", e)
            null
        }
    }
}
