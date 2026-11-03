package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.domain.model.CropRatio
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import kotlin.math.max
import kotlin.math.min

private enum class DragHandle {
    NONE, INSIDE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    EDGE_LEFT, EDGE_TOP, EDGE_RIGHT, EDGE_BOTTOM
}

@Composable
fun CropCanvas(
    bitmap: Bitmap?,
    cropRatio: CropRatio,
    normalizedCropRect: RectF,
    onCropRectChanged: (RectF) -> Unit,
    modifier: Modifier = Modifier
) {
    if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) return

    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        if (containerWidth <= 0 || containerHeight <= 0) return@BoxWithConstraints

        // Calculate fitted image rect inside container
        val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val containerAspect = containerWidth / containerHeight

        val fittedWidth: Float
        val fittedHeight: Float
        val imageLeft: Float
        val imageTop: Float

        if (imageAspect > containerAspect) {
            fittedWidth = containerWidth
            fittedHeight = containerWidth / imageAspect
            imageLeft = 0f
            imageTop = (containerHeight - fittedHeight) / 2f
        } else {
            fittedHeight = containerHeight
            fittedWidth = containerHeight * imageAspect
            imageTop = 0f
            imageLeft = (containerWidth - fittedWidth) / 2f
        }

        var activeHandle by remember { mutableStateOf(DragHandle.NONE) }
        val touchTolerance = 64f // px for easy touch targeting

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cropRatio, bitmap, normalizedCropRect) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val currentPxLeft = imageLeft + normalizedCropRect.left * fittedWidth
                            val currentPxTop = imageTop + normalizedCropRect.top * fittedHeight
                            val currentPxRight = imageLeft + normalizedCropRect.right * fittedWidth
                            val currentPxBottom = imageTop + normalizedCropRect.bottom * fittedHeight

                            val isNear = { x: Float, y: Float ->
                                (offset.x - x) * (offset.x - x) + (offset.y - y) * (offset.y - y) <= touchTolerance * touchTolerance
                            }

                            activeHandle = when {
                                isNear(currentPxLeft, currentPxTop) -> DragHandle.TOP_LEFT
                                isNear(currentPxRight, currentPxTop) -> DragHandle.TOP_RIGHT
                                isNear(currentPxLeft, currentPxBottom) -> DragHandle.BOTTOM_LEFT
                                isNear(currentPxRight, currentPxBottom) -> DragHandle.BOTTOM_RIGHT
                                offset.x in (currentPxLeft - touchTolerance)..(currentPxLeft + touchTolerance) &&
                                        offset.y in currentPxTop..currentPxBottom -> DragHandle.EDGE_LEFT
                                offset.x in (currentPxRight - touchTolerance)..(currentPxRight + touchTolerance) &&
                                        offset.y in currentPxTop..currentPxBottom -> DragHandle.EDGE_RIGHT
                                offset.y in (currentPxTop - touchTolerance)..(currentPxTop + touchTolerance) &&
                                        offset.x in currentPxLeft..currentPxRight -> DragHandle.EDGE_TOP
                                offset.y in (currentPxBottom - touchTolerance)..(currentPxBottom + touchTolerance) &&
                                        offset.x in currentPxLeft..currentPxRight -> DragHandle.EDGE_BOTTOM
                                offset.x in currentPxLeft..currentPxRight && offset.y in currentPxTop..currentPxBottom -> DragHandle.INSIDE
                                else -> DragHandle.NONE
                            }
                        },
                        onDragEnd = { activeHandle = DragHandle.NONE },
                        onDragCancel = { activeHandle = DragHandle.NONE },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (activeHandle == DragHandle.NONE) return@detectDragGestures

                            val dxNorm = dragAmount.x / fittedWidth
                            val dyNorm = dragAmount.y / fittedHeight

                            var l = normalizedCropRect.left
                            var t = normalizedCropRect.top
                            var r = normalizedCropRect.right
                            var b = normalizedCropRect.bottom

                            val minSizeNorm = 0.08f

                            when (activeHandle) {
                                DragHandle.INSIDE -> {
                                    val width = r - l
                                    val height = b - t
                                    var newL = l + dxNorm
                                    var newT = t + dyNorm
                                    var newR = newL + width
                                    var newB = newT + height

                                    if (newL < 0f) {
                                        newL = 0f
                                        newR = width
                                    } else if (newR > 1f) {
                                        newR = 1f
                                        newL = 1f - width
                                    }

                                    if (newT < 0f) {
                                        newT = 0f
                                        newB = height
                                    } else if (newB > 1f) {
                                        newB = 1f
                                        newT = 1f - height
                                    }

                                    l = newL
                                    t = newT
                                    r = newR
                                    b = newB
                                }
                                DragHandle.TOP_LEFT -> {
                                    l = (l + dxNorm).coerceIn(0f, r - minSizeNorm)
                                    t = (t + dyNorm).coerceIn(0f, b - minSizeNorm)
                                }
                                DragHandle.TOP_RIGHT -> {
                                    r = (r + dxNorm).coerceIn(l + minSizeNorm, 1f)
                                    t = (t + dyNorm).coerceIn(0f, b - minSizeNorm)
                                }
                                DragHandle.BOTTOM_LEFT -> {
                                    l = (l + dxNorm).coerceIn(0f, r - minSizeNorm)
                                    b = (b + dyNorm).coerceIn(t + minSizeNorm, 1f)
                                }
                                DragHandle.BOTTOM_RIGHT -> {
                                    r = (r + dxNorm).coerceIn(l + minSizeNorm, 1f)
                                    b = (b + dyNorm).coerceIn(t + minSizeNorm, 1f)
                                }
                                DragHandle.EDGE_LEFT -> {
                                    l = (l + dxNorm).coerceIn(0f, r - minSizeNorm)
                                }
                                DragHandle.EDGE_TOP -> {
                                    t = (t + dyNorm).coerceIn(0f, b - minSizeNorm)
                                }
                                DragHandle.EDGE_RIGHT -> {
                                    r = (r + dxNorm).coerceIn(l + minSizeNorm, 1f)
                                }
                                DragHandle.EDGE_BOTTOM -> {
                                    b = (b + dyNorm).coerceIn(t + minSizeNorm, 1f)
                                }
                                DragHandle.NONE -> {}
                            }

                            // If aspect ratio is locked and not INSIDE drag:
                            val targetRatio = cropRatio.ratio
                            if (targetRatio != null && activeHandle != DragHandle.INSIDE) {
                                val currentWidthPx = (r - l) * bitmap.width
                                val currentHeightPx = (b - t) * bitmap.height
                                val requiredHeightPx = currentWidthPx / targetRatio
                                val requiredHeightNorm = requiredHeightPx / bitmap.height

                                if (t + requiredHeightNorm <= 1f) {
                                    b = t + requiredHeightNorm
                                } else if (b - requiredHeightNorm >= 0f) {
                                    t = b - requiredHeightNorm
                                }
                            }

                            onCropRectChanged(RectF(l, t, r, b))
                        }
                    )
                }
        ) {
            // Draw background image
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(imageLeft.toInt(), imageTop.toInt()),
                dstSize = IntSize(fittedWidth.toInt(), fittedHeight.toInt())
            )

            val pxLeft = imageLeft + normalizedCropRect.left * fittedWidth
            val pxTop = imageTop + normalizedCropRect.top * fittedHeight
            val pxRight = imageLeft + normalizedCropRect.right * fittedWidth
            val pxBottom = imageTop + normalizedCropRect.bottom * fittedHeight
            val cropWidth = max(1f, pxRight - pxLeft)
            val cropHeight = max(1f, pxBottom - pxTop)

            // Scrim (dark overlay outside crop rectangle)
            val scrimColor = Color(0x99000000)
            // Top scrim
            drawRect(scrimColor, Offset(0f, 0f), Size(size.width, pxTop))
            // Bottom scrim
            drawRect(scrimColor, Offset(0f, pxBottom), Size(size.width, size.height - pxBottom))
            // Left scrim
            drawRect(scrimColor, Offset(0f, pxTop), Size(pxLeft, cropHeight))
            // Right scrim
            drawRect(scrimColor, Offset(pxRight, pxTop), Size(size.width - pxRight, cropHeight))

            // Crop border outline
            drawRect(
                color = CyanAccent,
                topLeft = Offset(pxLeft, pxTop),
                size = Size(cropWidth, cropHeight),
                style = Stroke(width = 2.dp.toPx())
            )

            // Rule of thirds grid lines
            val oneThirdW = cropWidth / 3f
            val oneThirdH = cropHeight / 3f
            val gridColor = Color(0x40FFFFFF)
            drawLine(gridColor, Offset(pxLeft + oneThirdW, pxTop), Offset(pxLeft + oneThirdW, pxBottom), 1.dp.toPx())
            drawLine(gridColor, Offset(pxLeft + oneThirdW * 2f, pxTop), Offset(pxLeft + oneThirdW * 2f, pxBottom), 1.dp.toPx())
            drawLine(gridColor, Offset(pxLeft, pxTop + oneThirdH), Offset(pxRight, pxTop + oneThirdH), 1.dp.toPx())
            drawLine(gridColor, Offset(pxLeft, pxTop + oneThirdH * 2f), Offset(pxRight, pxTop + oneThirdH * 2f), 1.dp.toPx())

            // Corner handles (accentuate with AmberAccent corners)
            val handleLength = 22.dp.toPx()
            val handleThickness = 4.dp.toPx()
            val cornerColor = AmberAccent

            // Top-left
            drawLine(cornerColor, Offset(pxLeft - 2, pxTop), Offset(pxLeft + handleLength, pxTop), handleThickness)
            drawLine(cornerColor, Offset(pxLeft, pxTop - 2), Offset(pxLeft, pxTop + handleLength), handleThickness)

            // Top-right
            drawLine(cornerColor, Offset(pxRight + 2, pxTop), Offset(pxRight - handleLength, pxTop), handleThickness)
            drawLine(cornerColor, Offset(pxRight, pxTop - 2), Offset(pxRight, pxTop + handleLength), handleThickness)

            // Bottom-left
            drawLine(cornerColor, Offset(pxLeft - 2, pxBottom), Offset(pxLeft + handleLength, pxBottom), handleThickness)
            drawLine(cornerColor, Offset(pxLeft, pxBottom + 2), Offset(pxLeft, pxBottom - handleLength), handleThickness)

            // Bottom-right
            drawLine(cornerColor, Offset(pxRight + 2, pxBottom), Offset(pxRight - handleLength, pxBottom), handleThickness)
            drawLine(cornerColor, Offset(pxRight, pxBottom + 2), Offset(pxRight, pxBottom - handleLength), handleThickness)
        }
    }
}
