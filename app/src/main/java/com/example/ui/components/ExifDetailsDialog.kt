package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ExifData
import com.example.ui.theme.*

@Composable
fun ExifDetailsDialog(
    exifData: ExifData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioDarkSurfaceCard,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CyanAccent)
                Text("EXIF Metadata", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!exifData.hasExif) {
                    Surface(
                        color = StudioDarkSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No EXIF metadata was found in this file (or it was stripped).",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    ExifRow(Icons.Default.PhotoCamera, "Camera", exifData.cameraSummary)
                    exifData.dateTime?.let { ExifRow(Icons.Default.CalendarToday, "Date Taken", it) }
                    exifData.iso?.let { ExifRow(Icons.Default.Sensors, "ISO Speed", it) }
                    exifData.fNumber?.let { ExifRow(Icons.Default.Exposure, "Aperture", "f/$it") }
                    exifData.exposureTime?.let { ExifRow(Icons.Default.Timer, "Shutter Speed", "${it}s") }
                    exifData.focalLength?.let { ExifRow(Icons.Default.Straighten, "Focal Length", "${it}mm") }
                    exifData.flash?.let { ExifRow(Icons.Default.FlashOn, "Flash", it) }
                    exifData.software?.let { ExifRow(Icons.Default.Code, "Software", it) }

                    if (exifData.hasGps) {
                        Surface(
                            color = StudioDarkSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                                    Text("GPS Coordinates", color = AmberAccent, fontWeight = FontWeight.SemiBold)
                                }
                                Text("Lat: ${exifData.latitude}, Long: ${exifData.longitude}", color = TextSecondary)
                                exifData.altitude?.let { Text("Altitude: ${it.toInt()}m", color = TextTertiary) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = StudioDarkBg)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ExifRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioDarkSurfaceElevated, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}
