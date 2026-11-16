package com.example.ui.sheets

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.UserProfileEntity
import com.example.data.preferences.AppPrefsState
import com.example.model.CatalogData
import com.example.model.ServiceItem
import com.example.ui.components.KaamWalaGeo
import com.example.ui.components.NetworkImageWithFallback
import com.example.ui.components.ServicePinMapPicker
import com.example.ui.theme.LocalKaamWalaExtras
import com.example.util.PlatformHelpers
import com.example.util.VoiceNoteHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingBottomSheet(
    service: ServiceItem,
    prefs: AppPrefsState,
    userProfile: UserProfileEntity? = null,
    onDismiss: () -> Unit,
    onShowToast: (String) -> Unit,
    onConfirmBooking: (
        service: ServiceItem,
        slot: String,
        express: Boolean,
        femaleSafe: Boolean,
        area: String,
        customerName: String,
        customerPhone: String,
        address: String,
        gpsCoords: String,
        note: String,
        photoUri: String,
        voiceNotePath: String,
        promoCode: String,
        paymentMethod: String,
        latitude: Double,
        longitude: Double
    ) -> Unit
) {
    val context = LocalContext.current
    val extras = LocalKaamWalaExtras.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val voiceHelper = remember { VoiceNoteHelper(context) }

    DisposableEffect(Unit) {
        onDispose { voiceHelper.release() }
    }

    val strings = CatalogData.strings(prefs.lang)
    val cityAreas = remember(prefs.city) { CatalogData.areasForCity(prefs.city) }

    var selectedSlot by remember { mutableStateOf("Standard (45 Min)") }
    var express by remember { mutableStateOf(false) }
    var femaleSafe by remember { mutableStateOf(service.femaleSafe) }
    var area by remember(cityAreas, userProfile) {
        mutableStateOf(
            userProfile?.area?.takeIf { it in cityAreas } ?: cityAreas.first()
        )
    }
    var areaMenuExpanded by remember { mutableStateOf(false) }
    var customerName by remember(userProfile) { mutableStateOf(userProfile?.fullName.orEmpty()) }
    var customerPhone by remember(userProfile) { mutableStateOf(userProfile?.phone.orEmpty()) }
    var address by remember(userProfile) { mutableStateOf(userProfile?.address.orEmpty()) }
    var paymentMethod by remember { mutableStateOf("CASH") }
    val initialCoords = remember(prefs.city, area) { KaamWalaGeo.coordsFor(prefs.city, area) }
    var latitude by remember { mutableStateOf(initialCoords.latitude) }
    var longitude by remember { mutableStateOf(initialCoords.longitude) }
    var hasCustomPin by remember { mutableStateOf(false) }
    LaunchedEffect(prefs.city, area) {
        if (!hasCustomPin) {
            val c = KaamWalaGeo.coordsFor(prefs.city, area)
            latitude = c.latitude
            longitude = c.longitude
        }
    }
    var gpsCoords by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf("") }
    var voiceNotePath by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var isPlayingVoice by remember { mutableStateOf(false) }
    var promoCode by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    fun applyGpsCoords(coords: String) {
        gpsCoords = coords
        KaamWalaGeo.parseCoords(coords)?.let {
            latitude = it.latitude
            longitude = it.longitude
            hasCustomPin = true
        }
        if (address.isBlank()) address = "Pinned Location ($coords)"
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                PlatformHelpers.captureApproximateLocation(context) { coords ->
                    if (coords != null) {
                        applyGpsCoords(coords)
                        onShowToast("GPS location attached!")
                    } else {
                        onShowToast("Location unavailable right now. Please tap the map or enter address manually.")
                    }
                }
            } else {
                onShowToast("Location permission denied. Tap the map or enter address manually.")
            }
        }
    )

    fun requestGpsCapture() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            PlatformHelpers.captureApproximateLocation(context) { coords ->
                if (coords != null) {
                    applyGpsCoords(coords)
                    onShowToast("GPS location attached!")
                } else {
                    onShowToast("Location unavailable. Please tap on map or type address.")
                }
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                val started = voiceHelper.startRecording()
                if (started != null) {
                    isRecording = true
                    onShowToast("Recording voice note... Tap Stop when done.")
                } else {
                    onShowToast("Microphone unavailable on this device.")
                }
            } else {
                onShowToast("Microphone permission denied.")
            }
        }
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                photoUri = uri.toString()
                onShowToast("Problem photo attached!")
            }
        }
    )

    // Price calculations
    val expressFee = if (express) 200 else 0
    val cleanPromo = promoCode.trim().uppercase()
    val promoDiscount = when {
        cleanPromo == "FIRST200" && !prefs.usedFirstPromo -> 200
        cleanPromo == "REF200" -> 100
        else -> 0
    }
    val afterPromo = (service.fee + expressFee - promoDiscount).coerceAtLeast(0)
    val creditUsed = minOf(prefs.customerCredit, afterPromo)
    val payableTotal = (afterPromo - creditUsed).coerceAtLeast(0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.bookBtnPrefix} ${service.localizedName(prefs.lang)}",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close booking sheet")
                }
            }

            // Service Preview Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, extras.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NetworkImageWithFallback(
                        url = service.imageUrl,
                        contentDescription = service.nameEn,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Visit Fee: ${PlatformHelpers.formatPkr(service.fee)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "7-Day Warranty • Transparent Rate Card",
                            style = MaterialTheme.typography.bodySmall,
                            color = extras.mutedText
                        )
                    }
                }
            }

            // Standard Rate Card
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, extras.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Standard Rate Card", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    service.rates.forEach { rate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(rate.label, style = MaterialTheme.typography.bodySmall)
                            Text(rate.price, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Time Slot
            Spacer(Modifier.height(12.dp))
            Text(strings.timeSlotLabel, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "Standard (45 Min)" to "45 Mins",
                    "Aaj Shaam 4-7 PM" to "Shaam",
                    "Kal Subah 10 AM" to "Kal Subah"
                ).forEach { (value, label) ->
                    val active = selectedSlot == value
                    Surface(
                        onClick = { selectedSlot = value },
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else extras.cardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Express & Family-Safe toggles
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = extras.amberBg,
                border = BorderStroke(1.dp, extras.amberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Express 20-Min Emergency (+Rs. 200)", style = MaterialTheme.typography.labelMedium, color = extras.amberText)
                    Checkbox(
                        checked = express,
                        onCheckedChange = { express = it },
                        modifier = Modifier.testTag("booking_express_checkbox")
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = extras.roseBg,
                border = BorderStroke(1.dp, extras.roseBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Female / Family-Safe Pro Only", style = MaterialTheme.typography.labelMedium, color = extras.roseAccent)
                    Checkbox(checked = femaleSafe, onCheckedChange = { femaleSafe = it })
                }
            }

            // Society / Area dropdown
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = area,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("${strings.societyAreaLabel} (${prefs.city})") },
                    trailingIcon = { Text("▾", modifier = Modifier.padding(end = 12.dp)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { areaMenuExpanded = true }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { areaMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = areaMenuExpanded,
                    onDismissRequest = { areaMenuExpanded = false }
                ) {
                    cityAreas.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = {
                                area = opt
                                areaMenuExpanded = false
                                if (!hasCustomPin) {
                                    val areaCoords = KaamWalaGeo.coordsFor(prefs.city, opt)
                                    latitude = areaCoords.latitude
                                    longitude = areaCoords.longitude
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Aapka Naam") },
                placeholder = { Text("Hamza Khan") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("booking_name_input")
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = customerPhone,
                onValueChange = { customerPhone = it },
                label = { Text("WhatsApp Number") },
                placeholder = { Text("0300-1234567") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("booking_phone_input")
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address + Live GPS") },
                    placeholder = { Text("House #14, Street 3, Block C") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("booking_address_input")
                )
                OutlinedButton(
                    onClick = { requestGpsCapture() },
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .testTag("booking_gps_button")
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = "Capture GPS", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("GPS", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (gpsCoords.isNotBlank()) {
                Text(
                    "GPS Attached: $gpsCoords",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(10.dp))
            ServicePinMapPicker(
                city = prefs.city,
                area = area,
                selectedLat = latitude,
                selectedLng = longitude,
                onLocationPicked = { lat, lng, formatted ->
                    latitude = lat
                    longitude = lng
                    hasCustomPin = true
                    gpsCoords = formatted
                    if (address.isBlank()) {
                        address = "$area Pin ($formatted)"
                    }
                    onShowToast("Map pin updated: $formatted")
                },
                onRequestGpsCenter = { requestGpsCapture() }
            )

            // Photo Picker + Voice Note
            Spacer(Modifier.height(10.dp))
            Text("Problem Photo & Voice Note", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("booking_photo_button")
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (photoUri.isNotBlank()) "Photo Added" else "Pick Photo", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = {
                        if (isRecording) {
                            val saved = voiceHelper.stopRecording()
                            isRecording = false
                            if (saved != null) {
                                voiceNotePath = saved
                                onShowToast("Voice note saved!")
                            }
                        } else {
                            val hasAudio = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasAudio) {
                                val started = voiceHelper.startRecording()
                                if (started != null) {
                                    isRecording = true
                                    onShowToast("Recording... Tap Stop when finished.")
                                } else {
                                    onShowToast("Microphone unavailable on this device.")
                                }
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) extras.dangerText else MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("booking_voice_button")
                ) {
                    Icon(
                        if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        when {
                            isRecording -> "Stop Rec"
                            voiceNotePath.isNotBlank() -> "Voice Ready"
                            else -> "Record Voice"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (photoUri.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Selected problem photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    OutlinedButton(
                        onClick = { photoUri = "" },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 38.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove Photo", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (voiceNotePath.isNotBlank() && !isRecording) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = extras.emeraldTint,
                    border = BorderStroke(1.dp, extras.emeraldBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (isPlayingVoice) {
                                    voiceHelper.stopPlayback()
                                    isPlayingVoice = false
                                } else {
                                    isPlayingVoice = true
                                    voiceHelper.playFile(voiceNotePath) { isPlayingVoice = false }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(
                                if (isPlayingVoice) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (isPlayingVoice) "Stop Preview" else "Play Voice Note",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        IconButton(
                            onClick = {
                                voiceHelper.stopPlayback()
                                isPlayingVoice = false
                                voiceNotePath = ""
                                onShowToast("Voice note removed.")
                            }
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete voice note",
                                tint = extras.dangerText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Masla / Tafseel") },
                placeholder = { Text("Masla likhein...") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = promoCode,
                onValueChange = { promoCode = it },
                label = { Text("Promo Code (FIRST200 / REF200)") },
                placeholder = { Text("FIRST200") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("booking_promo_input")
            )

            // Payment Method Selection
            Spacer(Modifier.height(10.dp))
            Text("Payment ka Tarika (Method)", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "CASH" to "Cash",
                    "JAZZCASH" to "JazzCash",
                    "EASYPAISA" to "Easypaisa",
                    "RAAST" to "Raast"
                ).forEach { (methodKey, label) ->
                    val selected = paymentMethod == methodKey
                    Surface(
                        onClick = { paymentMethod = methodKey },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else extras.cardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .testTag("booking_payment_$methodKey")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            if (paymentMethod != "CASH") {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = extras.amberBg,
                    border = BorderStroke(1.dp, extras.amberBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${prefs.payAccount} — Booking ke baad Orders tab mein TID submit karein.",
                        style = MaterialTheme.typography.labelSmall,
                        color = extras.amberText,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Price Breakdown
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = extras.emeraldTint,
                border = BorderStroke(1.dp, extras.emeraldBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SummaryRow("Visit Fee:", PlatformHelpers.formatPkr(service.fee))
                    SummaryRow("Express Fee:", PlatformHelpers.formatPkr(expressFee))
                    SummaryRow("Promo Code Discount:", "- ${PlatformHelpers.formatPkr(promoDiscount)}")
                    SummaryRow("Referral Wallet Used:", "- ${PlatformHelpers.formatPkr(creditUsed)}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Payable:", style = MaterialTheme.typography.titleSmall)
                        Text(
                            PlatformHelpers.formatPkr(payableTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (formError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = formError ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = extras.dangerText
                )
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    if (customerName.isBlank() || customerPhone.isBlank() || address.isBlank()) {
                        formError = "Naam, WhatsApp Number aur Address fill karein."
                        onShowToast("Naam, Phone aur Address fill karein!")
                        return@Button
                    }
                    formError = null
                    if (isRecording) {
                        val saved = voiceHelper.stopRecording()
                        isRecording = false
                        if (saved != null) voiceNotePath = saved
                    }
                    onConfirmBooking(
                        service,
                        selectedSlot,
                        express,
                        femaleSafe,
                        area,
                        customerName,
                        customerPhone,
                        address,
                        gpsCoords,
                        note,
                        photoUri,
                        voiceNotePath,
                        promoCode,
                        paymentMethod,
                        latitude,
                        longitude
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("confirm_booking_button")
            ) {
                Text(strings.confirmBookingBtn, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.labelMedium)
    }
}
