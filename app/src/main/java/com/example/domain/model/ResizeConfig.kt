package com.example.domain.model

data class ResizePreset(
    val title: String,
    val width: Int,
    val height: Int
)

val PopularResizePresets = listOf(
    ResizePreset("Full HD (1080p)", 1920, 1080),
    ResizePreset("HD (720p)", 1280, 720),
    ResizePreset("Social Square", 1080, 1080),
    ResizePreset("Instagram Story", 1080, 1920),
    ResizePreset("Avatar / Profile", 512, 512),
    ResizePreset("Small Thumbnail", 256, 256)
)

data class ResizeConfig(
    val targetWidth: Int = 0,
    val targetHeight: Int = 0,
    val maintainAspectRatio: Boolean = true,
    val scalePercentage: Int = 100,
    val isPercentageMode: Boolean = false
) {
    fun calculateTargetDimensions(originalWidth: Int, originalHeight: Int): Pair<Int, Int> {
        if (originalWidth <= 0 || originalHeight <= 0) return Pair(100, 100)
        
        if (isPercentageMode) {
            val scale = (scalePercentage.coerceIn(10, 400)) / 100f
            val w = (originalWidth * scale).toInt().coerceAtLeast(1)
            val h = (originalHeight * scale).toInt().coerceAtLeast(1)
            return Pair(w, h)
        }

        var w = if (targetWidth > 0) targetWidth else originalWidth
        var h = if (targetHeight > 0) targetHeight else originalHeight

        if (maintainAspectRatio) {
            val originalRatio = originalWidth.toFloat() / originalHeight.toFloat()
            if (targetWidth > 0 && targetHeight <= 0) {
                h = (w / originalRatio).toInt().coerceAtLeast(1)
            } else if (targetHeight > 0 && targetWidth <= 0) {
                w = (h * originalRatio).toInt().coerceAtLeast(1)
            }
        }
        return Pair(w.coerceAtLeast(1), h.coerceAtLeast(1))
    }
}
