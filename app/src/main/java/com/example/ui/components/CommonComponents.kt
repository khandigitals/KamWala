package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.preferences.AppPrefsState
import com.example.model.CatalogData
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalKaamWalaExtras
import com.example.ui.theme.tabularMono

val KaamWalaInputShape = RoundedCornerShape(12.dp)
val KaamWalaCardShape = RoundedCornerShape(18.dp)
val KaamWalaDialogShape = RoundedCornerShape(24.dp)

@Composable
fun kaamWalaTextFieldColors(): TextFieldColors {
    val extras = LocalKaamWalaExtras.current
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = extras.inputFill,
        disabledContainerColor = extras.inputFill.copy(alpha = 0.7f),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = extras.inputBorder.copy(alpha = 0.8f),
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = extras.mutedText,
        cursorColor = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun Modifier.tactilePress(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tactile_scale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun FlagshipCard(
    modifier: Modifier = Modifier,
    shape: Shape = KaamWalaCardShape,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = LocalKaamWalaExtras.current.cardBorder,
    shadowElevation: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val extras = LocalKaamWalaExtras.current
    Surface(
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.shadow(
            elevation = shadowElevation,
            shape = shape,
            ambientColor = extras.ambientShadow,
            spotColor = extras.ambientShadow
        )
    ) {
        Column(content = content)
    }
}

@Composable
fun FlagshipPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val gradient = Brush.horizontalGradient(
        listOf(
            Color(0xFF065F46),
            MaterialTheme.colorScheme.primary
        )
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledContentColor = LocalKaamWalaExtras.current.mutedText
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .tactilePress(interactionSource)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) gradient else Brush.horizontalGradient(listOf(Color(0xFFCBD5E1), Color(0xFFCBD5E1))))
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
fun ReceiptRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
    valueColor: Color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
) {
    val extras = LocalKaamWalaExtras.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            color = if (bold) MaterialTheme.colorScheme.onSurface else extras.secondaryText,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
        Text(
            text = value,
            style = (if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium).tabularMono(),
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
fun StatusPill(status: String, isUrgent: Boolean = false) {
    val extras = LocalKaamWalaExtras.current
    val (bg, fg, border) = when {
        isUrgent -> Triple(extras.dangerBg, extras.dangerText, extras.dangerText.copy(alpha = 0.25f))
        status == "PENDING" || status == "UNPAID" -> Triple(extras.amberBadgeBg, extras.amberText, extras.amberBorder.copy(alpha = 0.5f))
        status == "ACCEPTED" || status == "PAID_PENDING_VERIFY" -> Triple(extras.blueBg, extras.blueText, extras.blueText.copy(alpha = 0.22f))
        status == "IN_PROGRESS" -> Triple(extras.purpleBg, extras.purpleText, extras.purpleText.copy(alpha = 0.22f))
        status == "COMPLETED" || status == "APPROVED" || status == "ACTIVE" ||
            status == "AWARDED" || status == "PAID_OFF" || status == "SETTLED" ||
            status == "VERIFIED" || status == "FAMILY_SAFE" ->
            Triple(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
                extras.emeraldBorder.copy(alpha = 0.55f)
            )
        status == "REJECTED" || status == "CANCELLED" || status == "SUSPENDED" ->
            Triple(extras.dangerBg, extras.dangerText, extras.dangerText.copy(alpha = 0.25f))
        else -> Triple(extras.amberBadgeBg, extras.amberText, extras.amberBorder.copy(alpha = 0.5f))
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = if (isUrgent) "URGENT" else status.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.2.sp,
            color = fg,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun BookingProgressStepper(status: String) {
    val extras = LocalKaamWalaExtras.current
    if (status == "CANCELLED") {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = extras.dangerBg,
            border = BorderStroke(1.dp, extras.dangerText.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Text(
                text = "Order Cancelled — No charges billed.",
                style = MaterialTheme.typography.labelMedium,
                color = extras.dangerText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }
        return
    }
    val stepIndex = when (status) {
        "PENDING" -> 0
        "ACCEPTED" -> 1
        "IN_PROGRESS" -> 2
        "COMPLETED" -> 3
        else -> 0
    }
    val labels = listOf("Booked", "Assigned", "Working", "Done")

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 11.dp),
            thickness = 2.dp,
            color = extras.inputBorder
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEachIndexed { idx, label ->
                val done = idx <= stepIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (done) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = 1.dp,
                                color = if (done) MaterialTheme.colorScheme.primary else extras.inputBorder,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        } else {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall.tabularMono(),
                                color = extras.mutedText
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (done) MaterialTheme.colorScheme.primary else extras.mutedText,
                        fontWeight = if (done) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun LiveRadarCard(workerName: String, area: String, status: String) {
    val extras = LocalKaamWalaExtras.current
    val transition = rememberInfiniteTransition(label = "radar")
    val progress by transition.animateFloat(
        initialValue = if (status == "IN_PROGRESS") 0.72f else 0.35f,
        targetValue = if (status == "IN_PROGRESS") 0.92f else 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar_pin"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(listOf(extras.heroStart, extras.heroEnd))
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (status == "IN_PROGRESS") "Kaam Jari Hai • $workerName" else "$workerName Raste Mein Hain",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (status == "IN_PROGRESS") "On Site" else "ETA 12 min",
                style = MaterialTheme.typography.labelSmall.tabularMono(),
                color = extras.emeraldMint,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val trackWidth = maxWidth - 24.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.22f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(extras.emeraldMint)
            )
            Box(
                modifier = Modifier
                    .offset(x = trackWidth * progress)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = "Live worker location",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Live Area: $area",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.78f)
        )
    }
}

@Composable
fun SafetyOtpBanner(otp: String) {
    val extras = LocalKaamWalaExtras.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(extras.amberBadgeBg)
            .border(1.dp, extras.amberBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = extras.amberText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Safety Start OTP",
                style = MaterialTheme.typography.labelMedium,
                color = extras.amberText
            )
        }
        Text(
            text = otp,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = 2.sp,
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
            color = extras.amberText
        )
    }
}

@Composable
fun NetworkImageWithFallback(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var loadError by remember(url) { mutableStateOf(false) }
    val extras = LocalKaamWalaExtras.current

    if (url.isBlank() || loadError) {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(listOf(extras.heroStart, MaterialTheme.colorScheme.primary))
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Build,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(28.dp)
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            onError = { loadError = true },
            modifier = modifier
        )
    }
}

@Composable
fun CrossDevicePhoto(
    model: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val decodedBitmap = remember(model) {
        if (model.startsWith("data:image/", ignoreCase = true) && model.contains(";base64,")) {
            runCatching {
                val base64Part = model.substringAfter(";base64,")
                val bytes = android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        } else {
            null
        }
    }
    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

@Composable
fun StaffPinDialog(
    onDismiss: () -> Unit,
    onSubmitPin: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        title = { Text("Staff / Admin Access", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    "4-Digit Staff PIN enter karein (Default: 1234)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalKaamWalaExtras.current.secondaryText
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text("Staff PIN") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = kaamWalaTextFieldColors(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_pin_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitPin(pin) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("unlock_staff_button")
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CityPickerDialog(
    currentCity: String,
    onDismiss: () -> Unit,
    onSelectCity: (String) -> Unit
) {
    val extras = LocalKaamWalaExtras.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        },
        title = { Text("Select City", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CatalogData.CITIES.forEach { city ->
                    val selected = city == currentCity
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else extras.inputFill,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else extras.inputBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onSelectCity(city) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(city, style = MaterialTheme.typography.titleSmall)
                            if (selected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close") }
        }
    )
}

@Composable
fun SettingsDialog(
    prefs: AppPrefsState,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Int, Int, String, Boolean) -> Unit
) {
    val extras = LocalKaamWalaExtras.current
    var brand by remember { mutableStateOf(prefs.brand) }
    var city by remember { mutableStateOf(prefs.city) }
    var whatsapp by remember { mutableStateOf(prefs.whatsapp) }
    var payAccount by remember { mutableStateOf(prefs.payAccount) }
    var leadFee by remember { mutableStateOf(prefs.leadFee.toString()) }
    var commissionPercent by remember { mutableStateOf(prefs.commissionPercent.toString()) }
    var staffPin by remember { mutableStateOf(prefs.staffPin) }
    var liveCustomerMode by remember { mutableStateOf(prefs.liveCustomerMode) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("App Customize Karein", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it; errorMsg = null },
                    label = { Text("Brand Name") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = kaamWalaTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it; errorMsg = null },
                    label = { Text("City") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = kaamWalaTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("Admin WhatsApp (03xx / 923xx)") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = kaamWalaTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = payAccount,
                    onValueChange = { payAccount = it },
                    label = { Text("JazzCash / Easypaisa Account") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = kaamWalaTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = leadFee,
                        onValueChange = { leadFee = it.filter { c -> c.isDigit() } },
                        label = { Text("Lead Fee") },
                        singleLine = true,
                        shape = KaamWalaInputShape,
                        colors = kaamWalaTextFieldColors(),
                        textStyle = LocalTextStyle.current.tabularMono(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = commissionPercent,
                        onValueChange = { if (it.length <= 2) commissionPercent = it.filter { c -> c.isDigit() } },
                        label = { Text("Comm %") },
                        singleLine = true,
                        shape = KaamWalaInputShape,
                        colors = kaamWalaTextFieldColors(),
                        textStyle = LocalTextStyle.current.tabularMono(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.8f)
                    )
                    OutlinedTextField(
                        value = staffPin,
                        onValueChange = { if (it.length <= 6) staffPin = it.filter { c -> c.isDigit() }; errorMsg = null },
                        label = { Text("PIN") },
                        singleLine = true,
                        shape = KaamWalaInputShape,
                        colors = kaamWalaTextFieldColors(),
                        textStyle = LocalTextStyle.current.tabularMono(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.9f)
                    )
                }
                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = extras.dangerText
                    )
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, extras.emeraldBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Live Customer Mode",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Staff tabs, sample orders & demo buttons hide karein",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Switch(
                            checked = liveCustomerMode,
                            onCheckedChange = { liveCustomerMode = it },
                            modifier = Modifier.testTag("live_mode_switch")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        brand.isBlank() -> errorMsg = "Brand name khali nahi ho sakta."
                        staffPin.trim().length < 4 -> errorMsg = "Staff PIN kam az kam 4 digits ka rakhein."
                        else -> onSave(
                            brand,
                            city,
                            whatsapp,
                            payAccount,
                            leadFee.toIntOrNull() ?: 50,
                            (commissionPercent.toIntOrNull() ?: 10).coerceIn(0, 50),
                            staffPin,
                            liveCustomerMode
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("save_settings_button")
            ) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        }
    )
}
