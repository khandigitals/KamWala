package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalKaamWalaExtras
import com.example.ui.theme.tabularMono
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.Locale

object KaamWalaGeo {
    fun coordsFor(city: String, area: String): LatLng {
        val c = city.trim().lowercase(Locale.US)
        val a = area.trim().lowercase(Locale.US)
        return when (c) {
            "karachi" -> when {
                a.contains("dha") || a.contains("clifton") -> LatLng(24.8047, 67.0342)
                a.contains("gulshan") -> LatLng(24.9180, 67.0971)
                a.contains("bahria") -> LatLng(25.0040, 67.3198)
                a.contains("pechs") || a.contains("tariq") -> LatLng(24.8719, 67.0598)
                a.contains("nazimabad") -> LatLng(24.9408, 67.0398)
                else -> LatLng(24.8607, 67.0011)
            }
            "islamabad" -> when {
                a.contains("f-6") || a.contains("f-7") || a.contains("f-8") -> LatLng(33.7215, 73.0561)
                a.contains("g-9") || a.contains("g-10") || a.contains("g-11") -> LatLng(33.6881, 73.0149)
                a.contains("dha") || a.contains("bahria") -> LatLng(33.6897, 73.2041)
                a.contains("e-11") || a.contains("d-12") -> LatLng(33.7026, 72.9718)
                a.contains("blue") -> LatLng(33.7104, 73.0595)
                else -> LatLng(33.6844, 73.0479)
            }
            "rawalpindi" -> when {
                a.contains("bahria") -> LatLng(33.5326, 73.1167)
                a.contains("chaklala") -> LatLng(33.5886, 73.0956)
                a.contains("saddar") || a.contains("cantt") -> LatLng(33.5973, 73.0510)
                a.contains("satellite") -> LatLng(33.6352, 73.0715)
                a.contains("peshawar") -> LatLng(33.6095, 73.0046)
                else -> LatLng(33.5651, 73.0169)
            }
            "faisalabad" -> when {
                a.contains("peoples") -> LatLng(31.4106, 73.1105)
                a.contains("madina") -> LatLng(31.4227, 73.1228)
                a.contains("canal") -> LatLng(31.4480, 73.1490)
                a.contains("d ground") -> LatLng(31.4101, 73.1066)
                a.contains("jaranwala") -> LatLng(31.4024, 73.1422)
                else -> LatLng(31.4504, 73.1350)
            }
            "multan" -> when {
                a.contains("dha") -> LatLng(30.2562, 71.5030)
                a.contains("model") -> LatLng(30.2194, 71.4820)
                a.contains("cantt") -> LatLng(30.1846, 71.4465)
                a.contains("bosan") -> LatLng(30.2330, 71.4882)
                a.contains("gulgasht") -> LatLng(30.2215, 71.4718)
                else -> LatLng(30.1575, 71.5249)
            }
            else -> when {
                a.contains("bahria") -> LatLng(31.3676, 74.1815)
                a.contains("dha") -> LatLng(31.4697, 74.4071)
                a.contains("gulberg") -> LatLng(31.5117, 74.3510)
                a.contains("johar") -> LatLng(31.4697, 74.2728)
                a.contains("cantt") -> LatLng(31.5497, 74.3742)
                else -> LatLng(31.5204, 74.3587)
            }
        }
    }

    fun formatLatLng(lat: Double, lng: Double): String =
        String.format(Locale.US, "%.4f, %.4f", lat, lng)

    fun parseCoords(raw: String): LatLng? {
        val parts = raw.split(",").mapNotNull { it.trim().toDoubleOrNull() }
        return if (parts.size == 2) LatLng(parts[0], parts[1]) else null
    }

    val isRobolectric: Boolean
        get() = Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

    val hasConfiguredMapsKey: Boolean
        get() = BuildConfig.MAPS_API_KEY.isNotBlank() &&
            !BuildConfig.MAPS_API_KEY.contains("YOUR_GOOGLE_MAPS_API_KEY")
}

@Composable
fun ServicePinMapPicker(
    city: String,
    area: String,
    selectedLat: Double,
    selectedLng: Double,
    onLocationPicked: (lat: Double, lng: Double, formatted: String) -> Unit,
    onRequestGpsCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = LocalKaamWalaExtras.current
    val fallback = remember(city, area) { KaamWalaGeo.coordsFor(city, area) }
    val currentPin = remember(selectedLat, selectedLng, fallback) {
        if (selectedLat != 0.0 || selectedLng != 0.0) LatLng(selectedLat, selectedLng) else fallback
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.emeraldBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(extras.inputFill)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Tap Map to Pin House Location",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = KaamWalaGeo.formatLatLng(currentPin.latitude, currentPin.longitude),
                            style = MaterialTheme.typography.labelSmall.tabularMono(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                TextButton(
                    onClick = onRequestGpsCenter,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("map_picker_gps_button")
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("My GPS", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (KaamWalaGeo.isRobolectric) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .background(extras.emeraldTint),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Map Preview (${KaamWalaGeo.formatLatLng(currentPin.latitude, currentPin.longitude)})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(currentPin, 14.5f)
                }
                val markerState = remember(currentPin) { MarkerState(position = currentPin) }

                LaunchedEffect(currentPin) {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(currentPin, 14.5f))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                ) {
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("booking_google_map"),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            myLocationButtonEnabled = false,
                            mapToolbarEnabled = false
                        ),
                        onMapClick = { latLng ->
                            onLocationPicked(
                                latLng.latitude,
                                latLng.longitude,
                                KaamWalaGeo.formatLatLng(latLng.latitude, latLng.longitude)
                            )
                        }
                    ) {
                        Marker(
                            state = markerState,
                            title = "Service Location",
                            snippet = "$area • $city"
                        )
                    }

                    if (!KaamWalaGeo.hasConfiguredMapsKey) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = extras.amberBadgeBg.copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, extras.amberAccent),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Add MAPS_API_KEY in AI Studio Secrets for full map tiles",
                                style = MaterialTheme.typography.labelSmall,
                                color = extras.amberText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderLiveTrackingMapCard(
    bookingId: String,
    workerName: String,
    city: String,
    area: String,
    status: String,
    latitude: Double,
    longitude: Double,
    gpsCoords: String,
    workerLat: Double = 0.0,
    workerLng: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extras = LocalKaamWalaExtras.current
    val customerLatLng = remember(latitude, longitude, gpsCoords, city, area) {
        when {
            latitude != 0.0 || longitude != 0.0 -> LatLng(latitude, longitude)
            else -> KaamWalaGeo.parseCoords(gpsCoords) ?: KaamWalaGeo.coordsFor(city, area)
        }
    }
    val hasLiveWorkerGps = workerLat != 0.0 || workerLng != 0.0
    val workerLatLng = remember(customerLatLng, status, workerLat, workerLng) {
        when {
            hasLiveWorkerGps -> LatLng(workerLat, workerLng)
            status == "IN_PROGRESS" -> LatLng(customerLatLng.latitude + 0.0006, customerLatLng.longitude - 0.0005)
            else -> LatLng(customerLatLng.latitude + 0.0085, customerLatLng.longitude - 0.0070)
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.emeraldBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("order_live_map_card_$bookingId")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(extras.heroStart)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (status == "IN_PROGRESS") "$workerName On Site" else "$workerName En Route",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$area • ${if (hasLiveWorkerGps) "Live Worker GPS" else "Estimated Route"} (${KaamWalaGeo.formatLatLng(workerLatLng.latitude, workerLatLng.longitude)})",
                        style = MaterialTheme.typography.labelSmall.tabularMono(),
                        color = Color(0xFFA7F3D0)
                    )
                }
                FilledTonalButton(
                    onClick = {
                        val uri = Uri.parse(
                            "geo:${customerLatLng.latitude},${customerLatLng.longitude}?q=${customerLatLng.latitude},${customerLatLng.longitude}(${Uri.encode("KaamWala: $area")})"
                        )
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // No Maps app installed
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 34.dp)
                ) {
                    Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Maps", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (!KaamWalaGeo.isRobolectric) {
                val midPoint = LatLng(
                    (customerLatLng.latitude + workerLatLng.latitude) / 2.0,
                    (customerLatLng.longitude + workerLatLng.longitude) / 2.0
                )
                val cameraState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(midPoint, if (status == "IN_PROGRESS") 15.5f else 13.8f)
                }
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp),
                    cameraPositionState = cameraState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        mapToolbarEnabled = false
                    )
                ) {
                    Marker(
                        state = remember(customerLatLng) { MarkerState(position = customerLatLng) },
                        title = "Your Home Pin",
                        snippet = area,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                    Marker(
                        state = remember(workerLatLng) { MarkerState(position = workerLatLng) },
                        title = workerName,
                        snippet = if (status == "IN_PROGRESS") "Working at location" else "ETA ~12 min",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                    Polyline(
                        points = listOf(workerLatLng, customerLatLng),
                        color = MaterialTheme.colorScheme.primary,
                        width = 8f
                    )
                }
            }
        }
    }
}
