package com.example.domain.model

import android.graphics.Bitmap
import android.os.Build

enum class ImageFormat(
    val displayName: String,
    val extension: String,
    val mimeType: String,
    val defaultQuality: Int,
    val supportsQuality: Boolean
) {
    JPEG("JPEG", "jpg", "image/jpeg", 90, true),
    PNG("PNG", "png", "image/png", 100, false),
    WEBP("WebP", "webp", "image/webp", 85, true);

    fun toCompressFormat(lossless: Boolean = false): Bitmap.CompressFormat {
        return when (this) {
            JPEG -> Bitmap.CompressFormat.JPEG
            PNG -> Bitmap.CompressFormat.PNG
            WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (lossless) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
        }
    }
}
