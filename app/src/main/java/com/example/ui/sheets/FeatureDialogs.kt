package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.BookingEntity
import com.example.data.local.ChatMessageEntity
import com.example.model.CatalogData
import com.example.model.HardwarePart
import com.example.ui.components.KaamWalaDialogShape
import com.example.ui.components.KaamWalaInputShape
import com.example.ui.components.SafetyOtpBanner
import com.example.ui.components.kaamWalaTextFieldColors
import com.example.ui.theme.LocalKaamWalaExtras
import com.example.ui.theme.tabularMono
import com.example.util.PlatformHelpers

@Composable
fun BookingConfirmedDialog(
    booking: BookingEntity,
    adminWhatsApp: String,
    onDismiss: () -> Unit,
    onGoToOrders: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        },
        title = {
            Text(
                "Confirm Ho Gaya! (${booking.id})",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "${booking.serviceName} • ${booking.area}\nTotal: ${PlatformHelpers.formatPkr(booking.total)}",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.tabularMono()
                )
                SafetyOtpBanner(otp = booking.otp)
                Button(
                    onClick = {
                        PlatformHelpers.openWhatsApp(
                            context,
                            adminWhatsApp,
                            "New KaamWala Booking ${booking.id}: ${booking.serviceName} for ${booking.customerName} (${booking.customerPhone}) at ${booking.address}, ${booking.area}. Total: ${PlatformHelpers.formatPkr(booking.total)}. OTP: ${booking.otp}"
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Admin ko WhatsApp Bhejein")
                }
                FilledTonalButton(
                    onClick = onGoToOrders,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("go_to_orders_after_booking")
                ) {
                    Text("Live Tracking Dekhein")
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun BigJobDialog(
    city: String = "Lahore",
    onDismiss: () -> Unit,
    onSubmit: (category: String, area: String, desc: String, budget: Int) -> Unit
) {
    val extras = LocalKaamWalaExtras.current
    val fieldColors = kaamWalaTextFieldColors()
    val categories = listOf(
        "Solar Panel Setup (5kW/10kW)",
        "Home Painting & Seepage",
        "Carpenter Kitchen Cabinets",
        "Full House Plumbing/Wiring"
    )
    var selectedCat by remember { mutableStateOf(categories.first()) }
    var catMenu by remember { mutableStateOf(false) }
    var area by remember(city) { mutableStateOf(CatalogData.areasForCity(city).first()) }
    var desc by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Big Job Post Karein", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Solar, Paint, Woodwork — workers ki bids payein:", style = MaterialTheme.typography.bodySmall)
                Box {
                    OutlinedTextField(
                        value = selectedCat,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        shape = KaamWalaInputShape,
                        colors = fieldColors,
                        trailingIcon = { Text("▾", modifier = Modifier.padding(end = 10.dp)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(Modifier.matchParentSize().clickable { catMenu = true })
                    DropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { selectedCat = cat; catMenu = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Area ($city)") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it; errorMsg = null },
                    label = { Text("Detail") },
                    minLines = 2,
                    shape = KaamWalaInputShape,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it.filter { c -> c.isDigit() }; errorMsg = null },
                    label = { Text("Budget (PKR)") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = fieldColors,
                    textStyle = LocalTextStyle.current.tabularMono(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg ?: "", style = MaterialTheme.typography.labelSmall, color = extras.dangerText)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val b = budget.toIntOrNull() ?: 0
                    when {
                        area.isBlank() -> errorMsg = "Area enter karein."
                        desc.isBlank() -> errorMsg = "Kaam ki detail likhein."
                        b <= 0 -> errorMsg = "Valid budget (PKR) enter karein."
                        else -> onSubmit(selectedCat, area, desc, b)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("submit_big_job_button")
            ) { Text("Post for Bids") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        }
    )
}

@Composable
fun B2bDialog(
    onDismiss: () -> Unit,
    onSubmit: (biz: String, phone: String, pkg: String) -> Unit
) {
    val extras = LocalKaamWalaExtras.current
    val fieldColors = kaamWalaTextFieldColors()
    val packages = listOf(
        "Office Care (5 ACs + Weekly Electric/Plumb) — Rs. 8,000/mo",
        "Restaurant Deep Clean + Exhaust — Rs. 12,000/mo",
        "Society Multi-Tech Retainer — Rs. 25,000/mo"
    )
    var biz by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pkg by remember { mutableStateOf(packages.first()) }
    var pkgMenu by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("B2B Office & Shop Contract", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = biz,
                    onValueChange = { biz = it; errorMsg = null },
                    label = { Text("Business Name") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMsg = null },
                    label = { Text("Phone") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = fieldColors,
                    textStyle = LocalTextStyle.current.tabularMono(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    OutlinedTextField(
                        value = pkg,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Monthly Plan") },
                        shape = KaamWalaInputShape,
                        colors = fieldColors,
                        trailingIcon = { Text("▾", modifier = Modifier.padding(end = 10.dp)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(Modifier.matchParentSize().clickable { pkgMenu = true })
                    DropdownMenu(expanded = pkgMenu, onDismissRequest = { pkgMenu = false }) {
                        packages.forEach { p ->
                            DropdownMenuItem(text = { Text(p) }, onClick = { pkg = p; pkgMenu = false })
                        }
                    }
                }
                if (errorMsg != null) {
                    Text(errorMsg ?: "", style = MaterialTheme.typography.labelSmall, color = extras.dangerText)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        biz.isBlank() -> errorMsg = "Business ka naam likhein."
                        phone.filter { it.isDigit() }.length < 7 -> errorMsg = "Valid phone number likhein."
                        else -> onSubmit(biz, phone, pkg)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("submit_b2b_button")
            ) { Text("Request Quote") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        }
    )
}

@Composable
fun ChatDialog(
    bookingId: String,
    currentRole: String,
    messages: List<ChatMessageEntity>,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    val extras = LocalKaamWalaExtras.current
    val fieldColors = kaamWalaTextFieldColors()
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Live Safe Chat ($bookingId)", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = extras.inputFill,
                    border = BorderStroke(1.dp, extras.inputBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    if (messages.isEmpty()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("Abhi koi message nahi hai.", style = MaterialTheme.typography.bodySmall, color = extras.mutedText)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                val isMine = msg.senderRole == currentRole
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        border = if (isMine) null else BorderStroke(1.dp, extras.cardBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                                            Text(
                                                msg.senderName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isMine) Color.White.copy(alpha = 0.8f) else extras.mutedText
                                            )
                                            Text(
                                                msg.text,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CatalogData.QUICK_CHAT_REPLIES.forEach { reply ->
                        AssistChip(
                            onClick = { onSend(reply) },
                            label = { Text(reply, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Message likhein...") },
                        singleLine = true,
                        shape = KaamWalaInputShape,
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (input.isNotBlank()) {
                                onSend(input)
                                input = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", tint = Color.White)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close") } }
    )
}

@Composable
fun HardwarePartsDialog(
    booking: BookingEntity,
    allowAdd: Boolean,
    onAddPart: (HardwarePart) -> Unit,
    onDismiss: () -> Unit
) {
    val extras = LocalKaamWalaExtras.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Hardware Partner Parts", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (booking.addedPartsSummary.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = extras.emeraldTint,
                        border = BorderStroke(1.dp, extras.emeraldBorder)
                    ) {
                        Text(
                            "Added to ${booking.id}: ${booking.addedPartsSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                CatalogData.HARDWARE_PARTS.forEach { part ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = extras.inputFill,
                        border = BorderStroke(1.dp, extras.inputBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(part.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    PlatformHelpers.formatPkr(part.price),
                                    style = MaterialTheme.typography.labelMedium.tabularMono(),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (allowAdd) {
                                Button(
                                    onClick = { onAddPart(part) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.heightIn(min = 42.dp)
                                ) {
                                    Text("Add", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close") } }
    )
}

@Composable
fun WarrantyCertificateDialog(
    booking: BookingEntity,
    brandName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val extras = LocalKaamWalaExtras.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = extras.emeraldTint,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_warranty_seal),
                        contentDescription = "7-Day Warranty Seal",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(36.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$brandName 7-Day Warranty",
                        style = MaterialTheme.typography.titleLarge,
                        color = extras.heroStart,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Order ID: ${booking.id}\nService: ${booking.serviceName}\nCustomer: ${booking.customerName}\nVerified Pro: ${booking.assignedWorkerName.ifBlank { "KaamWala Pro" }}\nPaid Total: ${PlatformHelpers.formatPkr(booking.total)}",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall.tabularMono(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Free re-visit coverage for 7 days on workmanship.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    PlatformHelpers.shareText(
                        context,
                        "$brandName Digital Warranty Certificate\nOrder: ${booking.id}\nService: ${booking.serviceName}\nCustomer: ${booking.customerName}\nPro: ${booking.assignedWorkerName}\nAmount Paid: ${PlatformHelpers.formatPkr(booking.total)}\nStatus: 7-Day Workmanship Warranty Active"
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Share / Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close") }
        }
    )
}

@Composable
fun TopUpDialog(
    payAccount: String,
    onDismiss: () -> Unit,
    onSubmit: (method: String, amount: Int, tid: String) -> Unit
) {
    val extras = LocalKaamWalaExtras.current
    val fieldColors = kaamWalaTextFieldColors()
    val methods = listOf("JazzCash", "Easypaisa", "Raast")
    val amounts = listOf(500, 1000, 2000, 5000)
    var selectedMethod by remember { mutableStateOf(methods.first()) }
    var customAmount by remember { mutableStateOf("1000") }
    var tid by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .widthIn(max = 420.dp),
        title = { Text("Wallet Recharge", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Account par bhej kar TID likhein:", style = MaterialTheme.typography.bodySmall)
                Text(
                    payAccount,
                    style = MaterialTheme.typography.labelLarge.tabularMono(),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    methods.forEach { m ->
                        FilterChip(
                            selected = selectedMethod == m,
                            onClick = { selectedMethod = m },
                            label = { Text(m, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    amounts.forEach { amt ->
                        FilterChip(
                            selected = customAmount.toIntOrNull() == amt,
                            onClick = { customAmount = amt.toString(); errorMsg = null },
                            label = {
                                Text(
                                    PlatformHelpers.formatPkr(amt),
                                    style = MaterialTheme.typography.labelSmall.tabularMono()
                                )
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = customAmount,
                    onValueChange = { customAmount = it.filter { c -> c.isDigit() }; errorMsg = null },
                    label = { Text("Amount (PKR)") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = fieldColors,
                    textStyle = LocalTextStyle.current.tabularMono(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("topup_amount_input")
                )
                OutlinedTextField(
                    value = tid,
                    onValueChange = { tid = it; errorMsg = null },
                    label = { Text("Transaction ID (TID)") },
                    placeholder = { Text("08472910384") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = fieldColors,
                    textStyle = LocalTextStyle.current.tabularMono(),
                    modifier = Modifier.fillMaxWidth().testTag("topup_tid_input")
                )
                if (errorMsg != null) {
                    Text(errorMsg ?: "", style = MaterialTheme.typography.labelSmall, color = extras.dangerText)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = customAmount.toIntOrNull() ?: 0
                    when {
                        amt < 100 -> errorMsg = "Kam az kam Rs. 100 recharge amount likhein."
                        tid.trim().length < 4 -> errorMsg = "Kam az kam 4 characters ka valid TID likhein."
                        else -> onSubmit(selectedMethod, amt, tid)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("topup_submit_button")
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        }
    )
}

@Composable
fun OtpVerifyDialog(
    booking: BookingEntity,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit
) {
    var entered by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Verify Safety Start OTP", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text("Customer se 4-digit Safety OTP pooch kar enter karein (${booking.id}):", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = entered,
                    onValueChange = { if (it.length <= 4) entered = it.filter { c -> c.isDigit() } },
                    label = { Text("4-Digit OTP") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = kaamWalaTextFieldColors(),
                    textStyle = LocalTextStyle.current.tabularMono(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("worker_otp_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerify(entered) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("worker_otp_confirm_button")
            ) { Text("Start Work") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        }
    )
}

@Composable
fun RateReviewDialog(
    booking: BookingEntity,
    onDismiss: () -> Unit,
    onSubmitRating: (Int, String) -> Unit,
    onReportStrike: () -> Unit
) {
    val extras = LocalKaamWalaExtras.current
    var rating by remember { mutableStateOf(5) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Rate ${booking.assignedWorkerName.ifBlank { "Ustad" }}", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "$star stars",
                                tint = extras.amberAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Feedback / Comment") },
                    shape = KaamWalaInputShape,
                    colors = kaamWalaTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = onReportStrike,
                    colors = ButtonDefaults.textButtonColors(contentColor = extras.dangerText),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Report Quality Issue (Log Strike)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitRating(rating, note) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Submit Rating") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        }
    )
}

@Composable
fun RescheduleBookingDialog(
    booking: BookingEntity,
    onDismiss: () -> Unit,
    onConfirm: (newSlot: String, newAddress: String) -> Unit
) {
    val extras = LocalKaamWalaExtras.current
    val slots = listOf("Standard (45 Min)", "Aaj Shaam 4-7 PM", "Kal Subah 10 AM")
    var selectedSlot by remember { mutableStateOf(booking.slot) }
    var address by remember { mutableStateOf(booking.address) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Reschedule Order ${booking.id}", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${booking.serviceName} • Naya Time Slot aur Address select karein:", style = MaterialTheme.typography.bodySmall)
                slots.forEach { slot ->
                    FilterChip(
                        selected = selectedSlot == slot,
                        onClick = { selectedSlot = slot },
                        label = { Text(slot, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it; errorMsg = null },
                    label = { Text("Address") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = kaamWalaTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg ?: "", style = MaterialTheme.typography.labelSmall, color = extras.dangerText)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (address.isBlank()) {
                        errorMsg = "Address likhna zaroori hai."
                    } else {
                        onConfirm(selectedSlot, address)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("confirm_reschedule_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        }
    )
}

@Composable
fun PaymentTidDialog(
    booking: BookingEntity,
    payAccount: String,
    onDismiss: () -> Unit,
    onSubmitTid: (String) -> Unit
) {
    val context = LocalContext.current
    val extras = LocalKaamWalaExtras.current
    var tid by remember { mutableStateOf(booking.paymentTid) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .widthIn(max = 420.dp),
        title = {
            Text(
                "Digital Payment (${booking.paymentMethod})",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Order ${booking.id} (${booking.serviceName}) ke liye ${PlatformHelpers.formatPkr(booking.total)} is official account par bhej kar TID likhein:",
                    style = MaterialTheme.typography.bodySmall
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = extras.emeraldTint,
                    border = BorderStroke(1.dp, extras.emeraldBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = payAccount,
                            style = MaterialTheme.typography.labelLarge.tabularMono(),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                PlatformHelpers.copyToClipboard(context, "KaamWala Pay Account", payAccount)
                            }
                        ) {
                            Text("Copy")
                        }
                    }
                }
                OutlinedTextField(
                    value = tid,
                    onValueChange = { tid = it; errorMsg = null },
                    label = { Text("Transaction ID / Reference (TID)") },
                    placeholder = { Text("e.g. JC99887766") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = kaamWalaTextFieldColors(),
                    textStyle = LocalTextStyle.current.tabularMono(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_payment_tid_input")
                )
                if (errorMsg != null) {
                    Text(errorMsg ?: "", style = MaterialTheme.typography.labelSmall, color = extras.dangerText)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tid.trim().length < 4) {
                        errorMsg = "Kam az kam 4 characters ka valid TID likhein."
                    } else {
                        onSubmitTid(tid.trim())
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("order_payment_tid_submit")
            ) {
                Text("Submit Payment Proof")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminWithdrawalDialog(
    availableBalance: Int,
    defaultAccount: String,
    onDismiss: () -> Unit,
    onConfirmWithdraw: (amount: Int, method: String, account: String, note: String) -> Unit
) {
    val extras = LocalKaamWalaExtras.current
    val fieldColors = kaamWalaTextFieldColors()
    val methods = listOf("JazzCash", "Easypaisa", "Bank / IBAN", "Raast")
    var selectedMethod by remember { mutableStateOf(methods.first()) }
    var account by remember { mutableStateOf(defaultAccount) }
    var amountText by remember { mutableStateOf(availableBalance.coerceAtLeast(0).toString()) }
    var note by remember { mutableStateOf("Admin earnings saved to personal account") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .widthIn(max = 420.dp),
        title = { Text("Save / Withdraw Earnings", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Net Available Platform Balance: ${PlatformHelpers.formatPkr(availableBalance)}",
                    style = MaterialTheme.typography.labelLarge.tabularMono(),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    methods.forEach { m ->
                        FilterChip(
                            selected = selectedMethod == m,
                            onClick = { selectedMethod = m },
                            label = { Text(m, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() }; errorMsg = null },
                    label = { Text("Withdraw Amount (PKR)") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = fieldColors,
                    textStyle = LocalTextStyle.current.tabularMono(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_withdraw_amount_input")
                )
                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it; errorMsg = null },
                    label = { Text("Destination Account / IBAN") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_withdraw_account_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ledger Note / Reference") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg ?: "", style = MaterialTheme.typography.labelSmall, color = extras.dangerText)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toIntOrNull() ?: 0
                    when {
                        amt <= 0 -> errorMsg = "Valid amount enter karein."
                        amt > availableBalance -> errorMsg = "Available balance (${PlatformHelpers.formatPkr(availableBalance)}) se zyada withdraw nahi ho sakta."
                        account.trim().length < 5 -> errorMsg = "Valid destination account / IBAN likhein."
                        else -> onConfirmWithdraw(amt, selectedMethod, account.trim(), note.trim())
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("admin_withdraw_submit_button")
            ) {
                Text("Record & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        }
    )
}
