package com.example.domain.processing

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.example.domain.model.ExifData
import java.io.File
import java.io.InputStream

object ExifHelper {
    private const val TAG = "ExifHelper"

    private val EXIF_TAGS = listOf(
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_ISO_SPEED_RATINGS,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT
    )

    fun extractExif(context: Context, uri: Uri): ExifData {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                parseExif(inputStream)
            } ?: ExifData()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read EXIF from URI: $uri", e)
            ExifData()
        }
    }

    fun extractExif(file: File): ExifData {
        return try {
            file.inputStream().use { inputStream ->
                parseExif(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read EXIF from file: ${file.absolutePath}", e)
            ExifData()
        }
    }

    private fun parseExif(inputStream: InputStream): ExifData {
        val exif = ExifInterface(inputStream)
        val make = exif.getAttribute(ExifInterface.TAG_MAKE)
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)
        val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        val orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION)
        val width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
        val height = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
        val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
        val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
        val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
        val iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
        val flash = exif.getAttribute(ExifInterface.TAG_FLASH)
        val whiteBalance = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)
        val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)

        val latLong = FloatArray(2)
        val hasGps = exif.getLatLong(latLong)
        val altitude = exif.getAltitude(0.0)

        val hasAnyData = make != null || model != null || dateTime != null || hasGps || iso != null

        return ExifData(
            hasExif = hasAnyData,
            make = make,
            model = model,
            dateTime = dateTime,
            orientation = orientation,
            width = width,
            height = height,
            focalLength = focalLength,
            fNumber = fNumber,
            exposureTime = exposureTime,
            iso = iso,
            flash = flash,
            whiteBalance = whiteBalance,
            latitude = if (hasGps) latLong[0].toDouble() else null,
            longitude = if (hasGps) latLong[1].toDouble() else null,
            altitude = if (hasGps && altitude != 0.0) altitude else null,
            software = software
        )
    }

    fun copyExif(context: Context, sourceUri: Uri, destinationFile: File, stripMetadata: Boolean) {
        if (stripMetadata) {
            Log.d(TAG, "Privacy mode: EXIF stripped for ${destinationFile.name}")
            return
        }

        try {
            var sourceExif: ExifInterface? = null
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                sourceExif = ExifInterface(stream)
            }

            val src = sourceExif ?: return
            val destExif = ExifInterface(destinationFile.absolutePath)

            for (tag in EXIF_TAGS) {
                val value = src.getAttribute(tag)
                if (value != null) {
                    destExif.setAttribute(tag, value)
                }
            }
            // Reset orientation to NORMAL because image pixels are already rotated during processing
            destExif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL.toString()
            )
            destExif.saveAttributes()
            Log.d(TAG, "Successfully preserved EXIF tags to ${destinationFile.name}")
        } catch (e: Exception) {
            Log.w(TAG, "Could not copy EXIF metadata (format may not support standard tags): ${e.message}")
        }
    }
}
