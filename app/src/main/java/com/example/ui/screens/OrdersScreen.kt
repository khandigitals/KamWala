package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.BidEntity
import com.example.data.local.BigJobEntity
import com.example.data.local.BookingEntity
import com.example.model.CatalogData
import com.example.ui.KaamWalaUiState
import com.example.ui.components.BookingProgressStepper
import com.example.ui.components.OrderLiveTrackingMapCard
import com.example.ui.components.SafetyOtpBanner
import com.example.ui.components.StatusPill
import com.example.ui.sheets.RescheduleBookingDialog
import com.example.ui.theme.LocalKaamWalaExtras
import com.example.util.PlatformHelpers
import com.example.util.VoiceNoteHelper

@Composable
fun OrdersScreen(
    state: KaamWalaUiState,
    onNewOrder: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenParts: (String) -> Unit,
    onOpenWarranty: (BookingEntity) -> Unit,
    onOpenRate: (BookingEntity) -> Unit,
    onAcceptBid: (BigJobEntity, BidEntity) -> Unit,
    onRescheduleBooking: (BookingEntity, String, String) -> Unit = { _, _, _ -> },
    onCancelBooking: (BookingEntity) -> Unit = {},
    onOpenPaymentTid: (BookingEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val extras = LocalKaamWalaExtras.current
    val strings = CatalogData.strings(state.prefs.lang)
    val voiceHelper = remember { VoiceNoteHelper(context) }
    var playingBookingId by remember { mutableStateOf<String?>(null) }
    var rescheduleTarget by remember { mutableStateOf<BookingEntity?>(null) }
    var cancelTarget by remember { mutableStateOf<BookingEntity?>(null) }

    DisposableEffect(Unit) {
        onDispose { voiceHelper.release() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(Brush.linearGradient(listOf(extras.heroStart, extras.heroEnd)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.ordersHdr,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        FilledTonalButton(
                            onClick = onNewOrder,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.16f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .testTag("orders_new_order_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.ordersNewOrder, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = strings.ordersSub,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFECFDF5),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Active Monthly Care Plans
        if (state.activePlans.isNotEmpty()) {
            item {
                Text(
                    text = strings.ordersCarePlans,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)
                )
            }
            items(state.activePlans, key = { it.id }) { plan ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = extras.emeraldTint,
                    border = BorderStroke(1.dp, extras.emeraldBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(plan.title, style = MaterialTheme.typography.titleSmall, color = extras.heroStart)
                            Text(plan.desc, style = MaterialTheme.typography.bodySmall, color = extras.secondaryText)
                        }
                        Text(
                            text = "${PlatformHelpers.formatPkr(plan.price)}/mo",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Big Jobs & Bids
        if (state.visibleBigJobs.isNotEmpty()) {
            item {
                Text(
                    text = strings.ordersBigJobs,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)
                )
            }
            items(state.visibleBigJobs, key = { it.id }) { job ->
                val jobBids = state.bids.filter { it.bigJobId == job.id }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, extras.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                ) {
                    Column(modifier = Modifier.padding(13.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(job.category, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            StatusPill(job.status)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${job.area} • Budget: ${PlatformHelpers.formatPkr(job.budget)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(job.description, style = MaterialTheme.typography.bodySmall, color = extras.secondaryText)

                        if (jobBids.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Worker Quotes (${jobBids.size}):", style = MaterialTheme.typography.labelMedium)
                            jobBids.forEach { bid ->
                                val isAccepted = job.acceptedBidId == bid.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isAccepted) extras.emeraldTint else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (isAccepted) extras.emeraldBorder else extras.cardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(bid.workerName, style = MaterialTheme.typography.titleSmall)
                                            if (bid.note.isNotBlank()) {
                                                Text(bid.note, style = MaterialTheme.typography.bodySmall, color = extras.mutedText)
                                            }
                                            Text(
                                                PlatformHelpers.formatPkr(bid.amount),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (job.status == "OPEN") {
                                            Button(
                                                onClick = { onAcceptBid(job, bid) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.heightIn(min = 38.dp)
                                            ) {
                                                Text("Accept", style = MaterialTheme.typography.labelSmall)
                                            }
                                        } else if (isAccepted) {
                                            StatusPill("AWARDED")
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Workers ko bid ke liye bhej diya gaya hai...",
                                style = MaterialTheme.typography.bodySmall,
                                color = extras.mutedText
                            )
                        }
                    }
                }
            }
        }

        // Customer Bookings List
        val visibleBookings = state.customerVisibleBookings

        if (visibleBookings.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, extras.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_orders),
                            contentDescription = "No orders yet",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(strings.ordersEmptyTitle, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            strings.ordersEmptyDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = extras.mutedText
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onNewOrder) {
                            Text(strings.ordersEmptyBtn)
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "${strings.ordersServiceBookings} (${visibleBookings.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)
                )
            }
            items(visibleBookings, key = { it.id }) { booking ->
                CustomerBookingCard(
                    booking = booking,
                    city = state.prefs.city,
                    isPlayingVoice = playingBookingId == booking.id,
                    onPlayVoice = { path ->
                        if (playingBookingId == booking.id) {
                            voiceHelper.stopPlayback()
                            playingBookingId = null
                        } else {
                            playingBookingId = booking.id
                            voiceHelper.playFile(path) { playingBookingId = null }
                        }
                    },
                    onChat = { onOpenChat(booking.id) },
                    onParts = { onOpenParts(booking.id) },
                    onWarranty = { onOpenWarranty(booking) },
                    onRate = { onOpenRate(booking) },
                    onReschedule = { rescheduleTarget = booking },
                    onCancel = { cancelTarget = booking },
                    onPayTid = { onOpenPaymentTid(booking) },
                    onCallWorker = {
                        PlatformHelpers.dialPhone(
                            context,
                            booking.assignedWorkerPhone.ifBlank { state.prefs.whatsapp }
                        )
                    },
                    onWhatsApp = {
                        val targetWa = booking.assignedWorkerPhone.ifBlank { state.prefs.whatsapp }
                        PlatformHelpers.openWhatsApp(
                            context,
                            targetWa,
                            "KaamWala Order ${booking.id}: ${booking.serviceName} (${booking.area}) - Status: ${booking.status}, Safety OTP: ${booking.otp}, Total: ${PlatformHelpers.formatPkr(booking.total)}"
                        )
                    }
                )
            }
        }
    }

    rescheduleTarget?.let { target ->
        RescheduleBookingDialog(
            booking = target,
            onDismiss = { rescheduleTarget = null },
            onConfirm = { newSlot, newAddress ->
                onRescheduleBooking(target, newSlot, newAddress)
                rescheduleTarget = null
            }
        )
    }

    cancelTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Cancel Order ${target.id}?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "Kya aap waqai ${target.serviceName} (${target.area}) ki booking cancel karna chahte hain?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelBooking(target)
                        cancelTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extras.dangerText,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_cancel_booking_button")
                ) {
                    Text("Cancel Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) { Text("Keep Order") }
            }
        )
    }
}

@Composable
private fun CustomerBookingCard(
    booking: BookingEntity,
    city: String,
    isPlayingVoice: Boolean,
    onPlayVoice: (String) -> Unit,
    onChat: () -> Unit,
    onParts: () -> Unit,
    onWarranty: () -> Unit,
    onRate: () -> Unit,
    onReschedule: () -> Unit,
    onCancel: () -> Unit,
    onPayTid: () -> Unit,
    onCallWorker: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val extras = LocalKaamWalaExtras.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("order_card_${booking.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${booking.id} • ${booking.serviceName}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${booking.area} • ${booking.slot}",
                        style = MaterialTheme.typography.bodySmall,
                        color = extras.mutedText
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (booking.express) StatusPill("URGENT", isUrgent = true)
                    StatusPill(booking.status)
                }
            }

            Spacer(Modifier.height(6.dp))
            BookingProgressStepper(status = booking.status)

            if (booking.status == "ACCEPTED" || booking.status == "IN_PROGRESS") {
                Spacer(Modifier.height(6.dp))
                OrderLiveTrackingMapCard(
                    bookingId = booking.id,
                    workerName = booking.assignedWorkerName.ifBlank { "Verified Ustad" },
                    city = city,
                    area = booking.area,
                    status = booking.status,
                    latitude = booking.latitude,
                    longitude = booking.longitude,
                    gpsCoords = booking.gpsCoords,
                    workerLat = booking.workerLat,
                    workerLng = booking.workerLng
                )
            }

            if (booking.status != "COMPLETED" && booking.status != "CANCELLED") {
                Spacer(Modifier.height(8.dp))
                SafetyOtpBanner(otp = booking.otp)
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payable: ${PlatformHelpers.formatPkr(booking.total)} (${booking.paymentMethod})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(booking.paymentStatus)
            }
            if (booking.paymentTid.isNotBlank()) {
                Text(
                    text = "Payment TID: ${booking.paymentTid}",
                    style = MaterialTheme.typography.labelSmall,
                    color = extras.secondaryText
                )
            }
            if (booking.assignedWorkerName.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ustad: ${booking.assignedWorkerName}" +
                            if (booking.assignedWorkerPhone.isNotBlank()) " (${booking.assignedWorkerPhone})" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = extras.secondaryText,
                        modifier = Modifier.weight(1f)
                    )
                    if (booking.assignedWorkerPhone.isNotBlank() && booking.status != "CANCELLED") {
                        TextButton(
                            onClick = onCallWorker,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .testTag("order_call_worker_${booking.id}")
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "Call Ustad", modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Call", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (booking.note.isNotBlank()) {
                Text(
                    text = "Note: ${booking.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = extras.secondaryText
                )
            }
            if (booking.gpsCoords.isNotBlank()) {
                Text(
                    text = "GPS Pin: ${booking.gpsCoords}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (booking.addedPartsSummary.isNotBlank()) {
                Text(
                    text = "Parts Added: ${booking.addedPartsSummary}",
                    style = MaterialTheme.typography.labelSmall,
                    color = extras.amberText
                )
            }

            // Photos or Voice preview
            if (booking.photoUri.isNotBlank() || booking.proofPhotoUri.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (booking.photoUri.isNotBlank()) {
                        Column {
                            Text("Problem Photo", style = MaterialTheme.typography.labelSmall, color = extras.mutedText)
                            AsyncImage(
                                model = booking.photoUri,
                                contentDescription = "Problem photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                    }
                    if (booking.proofPhotoUri.isNotBlank()) {
                        Column {
                            Text("Work Proof", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            AsyncImage(
                                model = booking.proofPhotoUri,
                                contentDescription = "Work proof photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                    }
                }
            }

            if (booking.voiceNotePath.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { onPlayVoice(booking.voiceNotePath) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 38.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isPlayingVoice) "Stop Voice Note" else "Play Voice Note", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (booking.rating > 0) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = extras.amberAccent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Rated ${booking.rating}/5 ${if (booking.reviewNote.isNotBlank()) "• ${booking.reviewNote}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = extras.amberText
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onChat,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .testTag("order_chat_button_${booking.id}")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Chat", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onParts,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                ) {
                    Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Parts", style = MaterialTheme.typography.labelSmall)
                }
                if (booking.status == "COMPLETED") {
                    Button(
                        onClick = onWarranty,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .testTag("order_warranty_button_${booking.id}")
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Warranty", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    FilledTonalButton(
                        onClick = onWhatsApp,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("WhatsApp", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (booking.paymentMethod != "CASH" && booking.paymentStatus != "SETTLED" && booking.status != "CANCELLED") {
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onPayTid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extras.amberAccent,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp)
                        .testTag("order_pay_tid_button_${booking.id}")
                ) {
                    Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (booking.paymentStatus == "PAID_PENDING_VERIFY") "Update Payment TID (${booking.paymentMethod})"
                        else "Pay via ${booking.paymentMethod} / Submit TID",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (booking.status == "PENDING" || booking.status == "ACCEPTED") {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onReschedule,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .testTag("order_reschedule_button_${booking.id}")
                    ) {
                        Icon(Icons.Filled.EditCalendar, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reschedule", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onCancel,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = extras.dangerText),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .testTag("order_cancel_button_${booking.id}")
                    ) {
                        Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (booking.status == "COMPLETED" && booking.rating == 0) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onRate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp)
                        .testTag("order_rate_button_${booking.id}")
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Rate Ustad / Report Issue", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
