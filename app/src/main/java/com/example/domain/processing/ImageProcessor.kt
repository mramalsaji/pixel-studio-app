package com.example.domain.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.domain.model.CropRatio
import com.example.domain.model.ImageFormat
import com.example.domain.model.ResizeConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageProcessor {
    private const val TAG = "ImageProcessor"

    data class ImageDimensions(val width: Int, val height: Int)

    fun getImageDimensions(context: Context, uri: Uri): ImageDimensions {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val orientation = getExifOrientation(context, uri)
            if (orientation == 90 || orientation == 270) {
                ImageDimensions(options.outHeight, options.outWidth)
            } else {
                ImageDimensions(options.outWidth, options.outHeight)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get dimensions for $uri", e)
            ImageDimensions(0, 0)
        }
    }

    fun getFileNameAndSize(context: Context, uri: Uri): Pair<String, Long> {
        var name = "photo_${System.currentTimeMillis()}"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting file info from $uri", e)
        }
        return Pair(name, size)
    }

    suspend fun loadBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 3000
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // First decode bounds
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            var inSampleSize = 1
            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight
            if (rawWidth > maxDimension || rawHeight > maxDimension) {
                val largest = max(rawWidth, rawHeight)
                inSampleSize = (largest.toFloat() / maxDimension.toFloat()).roundToInt().coerceAtLeast(1)
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            // Apply EXIF orientation rotation
            val rotationDegrees = getExifOrientation(context, uri)
            if (rotationDegrees != 0) {
                rotateBitmap(decodedBitmap, rotationDegrees.toFloat())
            } else {
                decodedBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from $uri", e)
            null
        }
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360 == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated != source && !source.isRecycled) {
            source.recycle()
        }
        return rotated
    }

    fun flipBitmap(source: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        if (!horizontal && !vertical) return source
        val sx = if (horizontal) -1f else 1f
        val sy = if (vertical) -1f else 1f
        val matrix = Matrix().apply { postScale(sx, sy, source.width / 2f, source.height / 2f) }
        val flipped = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (flipped != source && !source.isRecycled) {
            source.recycle()
        }
        return flipped
    }

    fun cropBitmap(source: Bitmap, normalizedRect: RectF): Bitmap {
        val left = (normalizedRect.left.coerceIn(0f, 1f) * source.width).toInt().coerceIn(0, source.width - 1)
        val top = (normalizedRect.top.coerceIn(0f, 1f) * source.height).toInt().coerceIn(0, source.height - 1)
        val right = (normalizedRect.right.coerceIn(0f, 1f) * source.width).toInt().coerceIn(left + 1, source.width)
        val bottom = (normalizedRect.bottom.coerceIn(0f, 1f) * source.height).toInt().coerceIn(top + 1, source.height)

        val cropWidth = max(1, right - left)
        val cropHeight = max(1, bottom - top)

        if (cropWidth >= source.width && cropHeight >= source.height && left == 0 && top == 0) {
            return source
        }

        return Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
    }

    fun calculateBatchCropRect(width: Int, height: Int, ratio: CropRatio): RectF {
        val targetRatio = ratio.ratio ?: return RectF(0f, 0f, 1f, 1f)
        val imageRatio = width.toFloat() / height.toFloat()

        return if (imageRatio > targetRatio) {
            // Image is wider than crop box: crop width centered
            val newWidthNormalized = targetRatio / imageRatio
            val left = (1f - newWidthNormalized) / 2f
            RectF(left, 0f, left + newWidthNormalized, 1f)
        } else {
            // Image is taller than crop box: crop height centered
            val newHeightNormalized = imageRatio / targetRatio
            val top = (1f - newHeightNormalized) / 2f
            RectF(0f, top, 1f, top + newHeightNormalized)
        }
    }

    fun resizeBitmap(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val validWidth = targetWidth.coerceAtLeast(1)
        val validHeight = targetHeight.coerceAtLeast(1)

        if (source.width == validWidth && source.height == validHeight) {
            return source
        }

        return Bitmap.createScaledBitmap(source, validWidth, validHeight, true)
    }

    suspend fun saveBitmapToFile(
        bitmap: Bitmap,
        format: ImageFormat,
        quality: Int,
        destinationFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            destinationFile.parentFile?.mkdirs()
            FileOutputStream(destinationFile).use { outStream ->
                val compressFormat = format.toCompressFormat(lossless = format == ImageFormat.PNG)
                val clampedQuality = quality.coerceIn(10, 100)
                bitmap.compress(compressFormat, clampedQuality, outStream)
                outStream.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving bitmap to file: ${destinationFile.absolutePath}", e)
            false
        }
    }

    suspend fun estimateCompressedSizeBytes(
        source: Bitmap,
        cropRectNormalized: RectF?,
        resizeConfig: ResizeConfig,
        format: ImageFormat,
        quality: Int
    ): Long = withContext(Dispatchers.Default) {
        try {
            if (source.isRecycled) return@withContext 0L

            val cropped = if (cropRectNormalized != null &&
                (cropRectNormalized.left > 0.001f || cropRectNormalized.top > 0.001f ||
                 cropRectNormalized.right < 0.999f || cropRectNormalized.bottom < 0.999f)) {
                cropBitmap(source, cropRectNormalized)
            } else {
                source
            }

            val (targetW, targetH) = resizeConfig.calculateTargetDimensions(cropped.width, cropped.height)
            
            val maxPreviewDim = 1200
            val isLarge = targetW > maxPreviewDim || targetH > maxPreviewDim
            val scaleFactor = if (isLarge) {
                maxPreviewDim.toFloat() / max(targetW, targetH).toFloat()
            } else 1f

            val sampleW = (targetW * scaleFactor).toInt().coerceAtLeast(1)
            val sampleH = (targetH * scaleFactor).toInt().coerceAtLeast(1)
            val resized = resizeBitmap(cropped, sampleW, sampleH)

            val compressFormat = format.toCompressFormat(lossless = format == ImageFormat.PNG)
            val clampedQuality = quality.coerceIn(10, 100)

            val byteStream = java.io.ByteArrayOutputStream(sampleW * sampleH / 4)
            val success = resized.compress(compressFormat, clampedQuality, byteStream)
            val sampleBytes = byteStream.size().toLong()

            if (cropped != source && !cropped.isRecycled) cropped.recycle()
            if (resized != cropped && resized != source && !resized.isRecycled) resized.recycle()

            if (!success || sampleBytes <= 0) return@withContext 0L

            if (isLarge && scaleFactor > 0f) {
                val pixelRatio = (targetW.toDouble() * targetH.toDouble()) / (sampleW.toDouble() * sampleH.toDouble())
                (sampleBytes * Math.pow(pixelRatio, 0.88)).toLong()
            } else {
                sampleBytes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed estimating size", e)
            0L
        }
    }
}
