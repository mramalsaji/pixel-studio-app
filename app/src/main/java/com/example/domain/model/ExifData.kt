package com.example.domain.model

data class ExifData(
    val hasExif: Boolean = false,
    val make: String? = null,
    val model: String? = null,
    val dateTime: String? = null,
    val orientation: String? = null,
    val width: String? = null,
    val height: String? = null,
    val focalLength: String? = null,
    val fNumber: String? = null,
    val exposureTime: String? = null,
    val iso: String? = null,
    val flash: String? = null,
    val whiteBalance: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val software: String? = null
) {
    val hasGps: Boolean get() = latitude != null && longitude != null
    val cameraSummary: String get() {
        val parts = listOfNotNull(make, model).filter { it.isNotBlank() }
        return if (parts.isNotEmpty()) parts.joinToString(" ") else "Unknown Camera"
    }
}
