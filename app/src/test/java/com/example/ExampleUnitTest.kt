package com.example

import com.example.domain.model.CropRatio
import com.example.domain.model.ImageFormat
import com.example.domain.model.ResizeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testResizeDimensionsWithAspectRatioLock() {
        val config = ResizeConfig(
            targetWidth = 1000,
            targetHeight = 0,
            maintainAspectRatio = true,
            isPercentageMode = false
        )
        // 2000 x 1000 has 2:1 ratio
        val (calcW, calcH) = config.calculateTargetDimensions(2000, 1000)
        assertEquals(1000, calcW)
        assertEquals(500, calcH)
    }

    @Test
    fun testResizePercentageScaling() {
        val config = ResizeConfig(
            scalePercentage = 50,
            isPercentageMode = true
        )
        val (calcW, calcH) = config.calculateTargetDimensions(1920, 1080)
        assertEquals(960, calcW)
        assertEquals(540, calcH)
    }

    @Test
    fun testCropRatioValues() {
        assertEquals(1f, CropRatio.SQUARE.ratio)
        assertEquals(16f / 9f, CropRatio.SIXTEEN_NINE.ratio ?: 0f, 0.001f)
    }

    @Test
    fun testImageFormatProperties() {
        assertEquals("jpg", ImageFormat.JPEG.extension)
        assertEquals("png", ImageFormat.PNG.extension)
        assertEquals("webp", ImageFormat.WEBP.extension)
    }
}
