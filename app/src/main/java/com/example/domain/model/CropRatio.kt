package com.example.domain.model

enum class CropRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    THREE_FOUR("3:4", 3f / 4f),
    SIXTEEN_NINE("16:9", 16f / 9f),
    NINE_SIXTEEN("9:16", 9f / 16f),
    THREE_TWO("3:2", 3f / 2f),
    TWO_THREE("2:3", 2f / 3f)
}
