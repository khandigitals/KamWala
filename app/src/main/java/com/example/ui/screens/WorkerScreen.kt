package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.ui.components.KaamWalaGeo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BigJobEntity
import com.example.data.local.BookingEntity
import com.example.model.CatalogData
import com.example.model.ToolCatalogItem
import com.example.ui.KaamWalaUiState
import com.example.ui.WorkerSubTab
import com.example.ui.components.NetworkImageWithFallback
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalKaamWalaExtras
import com.example.util.PlatformHelpers
import com.example.util.VoiceNoteHelper

@Composable
fun WorkerScreen(
    state: KaamWalaUiState,
    onToggleOnline: () -> Unit,
    onLockStaff: () -> Unit,
    onSelectWorker: (String) -> Unit,
    onOpenTopUp: () -> Unit,
    onSelectSubTab: (WorkerSubTab) -> Unit,
    onAcceptJob: (BookingEntity) -> Unit,
    onOpenOtpVerify: (BookingEntity) -> Unit,
    onAttachProof: (BookingEntity, String) -> Unit,
    onOpenParts: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onCompleteJob: (BookingEntity) -> Unit,
    onSubmitBid: (BigJobEntity, Int, String) -> Unit,
    onApplyToolLoan: (ToolCatalogItem) -> Unit,
    onShowToast: (String) -> Unit,
    onShareLiveLocation: (BookingEntity, Double, Double) -> Unit = { _, _, _ -> },
    onOpenRegistration: () -> Unit = {}
) {
    val context = LocalContext.current
    val extras = LocalKaamWalaExtras.current
    val strings = CatalogData.strings(state.prefs.lang)
    val activeWorkerName = state.effectiveWorkerName
    val activeWorker = state.workers.firstOrNull { it.name == activeWorkerName }
        ?: state.workers.firstOrNull()
    val effectiveOnline = if (state.userProfile?.role == "WORKER") state.userProfile.isOnline else state.prefs.workerOnline
    val effectiveStrikes = if (state.userProfile?.role == "WORKER") state.userProfile.strikes else state.prefs.workerStrikes

    val voiceHelper = remember { VoiceNoteHelper(context) }
    var playingBookingId by remember { mutableStateOf<String?>(null) }
    var workerMenuExpanded by remember { mutableStateOf(false) }
    var pendingProofBooking by remember { mutableStateOf<BookingEntity?>(null) }
    var pendingLocationBooking by remember { mutableStateOf<BookingEntity?>(null) }
    var jobStatusFilter by remember { mutableStateOf("ALL") }

    DisposableEffect(Unit) {
        onDispose { voiceHelper.release() }
    }

    val proofPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            val target = pendingProofBooking
            if (uri != null && target != null) {
                onAttachProof(target, uri.toString())
            }
            pendingProofBooking = null
        }
    )

    fun dispatchWorkerLocation(target: BookingEntity) {
        PlatformHelpers.captureApproximateLocation(context) { coords ->
            val parsed = coords?.let { KaamWalaGeo.parseCoords(it) }
            if (parsed != null) {
                onShareLiveLocation(target, parsed.latitude, parsed.longitude)
            } else {
                val baseLat = if (target.latitude != 0.0) target.latitude else KaamWalaGeo.coordsFor(state.prefs.city, target.area).latitude
                val baseLng = if (target.longitude != 0.0) target.longitude else KaamWalaGeo.coordsFor(state.prefs.city, target.area).longitude
                val offsetLat = if (target.status == "IN_PROGRESS") baseLat + 0.0002 else baseLat + 0.0042
                val offsetLng = if (target.status == "IN_PROGRESS") baseLng - 0.0002 else baseLng - 0.0038
                onShareLiveLocation(target, offsetLat, offsetLng)
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            val target = pendingLocationBooking
            pendingLocationBooking = null
            if (target != null) {
                if (granted) {
                    dispatchWorkerLocation(target)
                } else {
                    onShowToast("Location permission denied.")
                }
            }
        }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. HEADER
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
                        Text(strings.workerHdr, style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                onClick = onToggleOnline,
                                shape = RoundedCornerShape(20.dp),
                                color = if (effectiveOnline) Color(0xFF065F46) else extras.dangerBg,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier.testTag("worker_online_toggle")
                            ) {
                                Text(
                                    text = if (effectiveOnline) "Online" else "Offline",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (effectiveOnline) Color.White else extras.dangerText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            IconButton(
                                onClick = onLockStaff,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = "Lock Staff", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.14f)) {
                            Text(
                                "Lead Fee: Rs. ${state.prefs.leadFee}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.14f)) {
                            Text(
                                "Strikes: $effectiveStrikes/3",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (effectiveStrikes >= 3) extras.dangerText else Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. PREPAID LEAD WALLET CARD
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(extras.darkSurface, extras.darkSurfaceAlt)))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NetworkImageWithFallback(
                            url = activeWorker?.avatarUrl ?: "",
                            contentDescription = activeWorkerName,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(2.dp, extras.emeraldMint, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { workerMenuExpanded = true }
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = "$activeWorkerName ▾",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Verified, contentDescription = null, tint = extras.emeraldMint, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    val skillLabel = state.userProfile?.takeIf { it.role == "WORKER" }?.skill?.ifBlank { null }
                                        ?: activeWorker?.skill ?: "Pro"
                                    Text(
                                        text = "CNIC Verified • $skillLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = workerMenuExpanded,
                                onDismissRequest = { workerMenuExpanded = false }
                            ) {
                                state.workers.forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text("${w.name} (${w.skill})") },
                                        onClick = {
                                            workerMenuExpanded = false
                                            onSelectWorker(w.name)
                                        }
                                    )
                                }
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                PlatformHelpers.playAlertTone()
                                onShowToast("Incoming Job Alert Ringtone Tested!")
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.14f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.heightIn(min = 36.dp)
                        ) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Test", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("Prepaid Lead Wallet", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                            Text(
                                text = PlatformHelpers.formatPkr(state.effectiveWalletBalance),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = extras.emeraldMint
                            )
                        }
                        Button(
                            onClick = onOpenTopUp,
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .testTag("worker_recharge_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Recharge", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 2B. WORKER KYC REGISTRATION STATUS BANNER
        if (state.userProfile?.role == "WORKER" || (state.profileLoaded && state.userProfile == null)) {
            item {
                val profile = state.userProfile
                val (bg, border, fg, msg, btnLabel) = when (profile?.verificationStatus) {
                    "APPROVED" -> listOf(
                        extras.emeraldTint,
                        extras.emeraldBorder,
                        extras.heroStart,
                        "Verified Karegar • ${profile.skill} (${profile.city}) • Payout: ${profile.payoutAccount}",
                        "Profile"
                    )
                    "REJECTED" -> listOf(
                        extras.dangerBg,
                        extras.dangerText.copy(alpha = 0.4f),
                        extras.dangerText,
                        "KYC Rejected: Apni CNIC aur Skill details update karein.",
                        "Update KYC"
                    )
                    "PENDING" -> listOf(
                        extras.amberBg,
                        extras.amberBorder,
                        extras.amberText,
                        "Karegar KYC Pending Verification — Admin approval ke baad orders pick ho sakein ge.",
                        "View / Edit"
                    )
                    else -> listOf(
                        extras.amberBg,
                        extras.amberBorder,
                        extras.amberText,
                        "Karegar Account Register karein (CNIC, Skill & JazzCash/Easypaisa).",
                        "Register"
                    )
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = bg as Color,
                    border = BorderStroke(1.dp, border as Color),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg as String,
                            style = MaterialTheme.typography.labelMedium,
                            color = fg as Color,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        OutlinedButton(
                            onClick = onOpenRegistration,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .testTag("worker_kyc_banner_button")
                        ) {
                            Text(btnLabel as String, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 3. SUBTABS
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp)
            ) {
                Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        WorkerSubTab.JOBS to "Jobs",
                        WorkerSubTab.BIDS to "Big Bids",
                        WorkerSubTab.HISAB to "Hisab & Tools"
                    ).forEach { (tab, title) ->
                        val active = state.workerSubTab == tab
                        Surface(
                            onClick = { onSelectSubTab(tab) },
                            shape = RoundedCornerShape(9.dp),
                            color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) MaterialTheme.colorScheme.primary else extras.secondaryText
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. SUBTAB CONTENT
        when (state.workerSubTab) {
            WorkerSubTab.JOBS -> {
                val visibleJobs = state.workerVisibleBookings
                val pendingCount = visibleJobs.count { it.status == "PENDING" }
                if (pendingCount > 0) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = extras.amberBg,
                            border = BorderStroke(1.dp, extras.amberBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$pendingCount Nayi Job Requests Available! Accept karke Lead pakkein.",
                                style = MaterialTheme.typography.labelLarge,
                                color = extras.amberText,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to strings.workerFilterAll,
                            "PENDING" to strings.workerFilterPending,
                            "ACTIVE" to strings.workerFilterActive,
                            "DONE" to strings.workerFilterDone
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = jobStatusFilter == key,
                                onClick = { jobStatusFilter = key },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("worker_filter_${key.lowercase()}")
                            )
                        }
                    }
                }
                val filteredJobs = visibleJobs.filter { b ->
                    when (jobStatusFilter) {
                        "PENDING" -> b.status == "PENDING"
                        "ACTIVE" -> b.status == "ACCEPTED" || b.status == "IN_PROGRESS"
                        "DONE" -> b.status == "COMPLETED" || b.status == "CANCELLED"
                        else -> true
                    }
                }
                if (filteredJobs.isEmpty()) {
                    item {
                        Text(
                            text = "Is category mein koi job nahi hai.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extras.mutedText,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    items(filteredJobs, key = { it.id }) { booking ->
                        WorkerJobCard(
                            booking = booking,
                            leadFee = state.prefs.leadFee,
                            isPlayingVoice = playingBookingId == booking.id,
                            onToggleVoicePlay = {
                                if (playingBookingId == booking.id) {
                                    voiceHelper.stopPlayback()
                                    playingBookingId = null
                                } else {
                                    playingBookingId = booking.id
                                    voiceHelper.playFile(booking.voiceNotePath) {
                                        playingBookingId = null
                                    }
                                }
                            },
                            onCallCustomer = { PlatformHelpers.dialPhone(context, booking.customerPhone) },
                            onWhatsAppCustomer = {
                                PlatformHelpers.openWhatsApp(
                                    context,
                                    booking.customerPhone,
                                    "Assalam-o-Alaikum ${booking.customerName}, main KaamWala Ustad hoon (${booking.id}: ${booking.serviceName})."
                                )
                            },
                            onAccept = { onAcceptJob(booking) },
                            onVerifyOtp = { onOpenOtpVerify(booking) },
                            onShareGps = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    dispatchWorkerLocation(booking)
                                } else {
                                    pendingLocationBooking = booking
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            },
                            onUploadProof = {
                                pendingProofBooking = booking
                                proofPhotoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onAddParts = { onOpenParts(booking.id) },
                            onChat = { onOpenChat(booking.id) },
                            onComplete = { onCompleteJob(booking) }
                        )
                    }
                }
            }

            WorkerSubTab.BIDS -> {
                val openBigJobs = state.bigJobs.filter { it.status == "OPEN" }
                if (openBigJobs.isEmpty()) {
                    item {
                        Text(
                            "Abhi koi Open Big Job nahi hai.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extras.mutedText,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    items(openBigJobs, key = { it.id }) { job ->
                        WorkerBigJobBidCard(job = job, onSubmitBid = onSubmitBid)
                    }
                }
            }

            WorkerSubTab.HISAB -> {
                item {
                    val workerJobs = state.workerVisibleBookings.filter {
                        (state.currentUserId.isNotBlank() && it.workerId == state.currentUserId) ||
                            it.assignedWorkerName == activeWorkerName ||
                            it.status == "COMPLETED"
                    }
                    val completedJobs = workerJobs.filter { it.status == "COMPLETED" }
                    val totalEarned = completedJobs.sumOf { it.total }
                    val totalLeadFees = workerJobs.sumOf { it.leadFeePaid }
                    val totalCommission = completedJobs.sumOf { it.commissionAmount }
                    val netEarnings = (totalEarned - totalLeadFees - totalCommission).coerceAtLeast(0)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, extras.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Worker Earnings Ledger", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            LedgerRow("Active Pro", activeWorkerName)
                            LedgerRow("Completed Jobs", "${completedJobs.size}")
                            LedgerRow("Gross Job Earnings", PlatformHelpers.formatPkr(totalEarned))
                            LedgerRow("Lead Fees Paid", PlatformHelpers.formatPkr(totalLeadFees))
                            LedgerRow("Platform Commission (${state.prefs.commissionPercent}%)", PlatformHelpers.formatPkr(totalCommission))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            LedgerRow("Net Karegar Earnings", PlatformHelpers.formatPkr(netEarnings), bold = true)
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = extras.emeraldTint,
                        border = BorderStroke(1.dp, extras.emeraldBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Tool Micro-Financing (Asaan Qist)", style = MaterialTheme.typography.titleMedium, color = extras.heroStart)
                            Text(
                                "Rozana qist wallet se kat kar naye tools lein:",
                                style = MaterialTheme.typography.bodySmall,
                                color = extras.secondaryText
                            )
                            Spacer(Modifier.height(10.dp))
                            CatalogData.TOOL_ITEMS.forEach { tool ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, extras.cardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tool.name, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                "Total ${PlatformHelpers.formatPkr(tool.price)} • Rs. ${tool.dailyQist}/day",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = extras.mutedText
                                            )
                                        }
                                        Button(
                                            onClick = { onApplyToolLoan(tool) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.heightIn(min = 38.dp)
                                        ) {
                                            Text("Apply", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                            if (state.toolLoans.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Your Active Tool Loans:", style = MaterialTheme.typography.labelLarge)
                                state.toolLoans.forEach { loan ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${loan.toolName} (${loan.paidAmount}/${loan.totalPrice})", style = MaterialTheme.typography.bodySmall)
                                        StatusPill(loan.status)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun WorkerJobCard(
    booking: BookingEntity,
    leadFee: Int,
    isPlayingVoice: Boolean,
    onToggleVoicePlay: () -> Unit,
    onCallCustomer: () -> Unit,
    onWhatsAppCustomer: () -> Unit,
    onAccept: () -> Unit,
    onVerifyOtp: () -> Unit,
    onShareGps: () -> Unit,
    onUploadProof: () -> Unit,
    onAddParts: () -> Unit,
    onChat: () -> Unit,
    onComplete: () -> Unit
) {
    val extras = LocalKaamWalaExtras.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .testTag("worker_job_card_${booking.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${booking.id} • ${booking.serviceName}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                StatusPill(booking.status, isUrgent = booking.express && booking.status == "PENDING")
            }
            Spacer(Modifier.height(4.dp))
            Text("${booking.customerName} • ${booking.customerPhone}", style = MaterialTheme.typography.bodySmall)
            Text("${booking.address} (${booking.area})", style = MaterialTheme.typography.bodySmall, color = extras.mutedText)
            if (booking.note.isNotBlank()) {
                Text("Masla: ${booking.note}", style = MaterialTheme.typography.bodySmall, color = extras.secondaryText)
            }
            if (booking.photoUri.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                AsyncImage(
                    model = booking.photoUri,
                    contentDescription = "Customer problem photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }
            if (booking.voiceNotePath.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onToggleVoicePlay,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 36.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isPlayingVoice) "Stop Voice Note" else "Play Customer Voice Note",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bill: ${PlatformHelpers.formatPkr(booking.total)} • ${booking.slot}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusPill(booking.paymentStatus)
            }
            val payHint = when {
                booking.paymentMethod == "CASH" -> "Cash wasool karein kaam mukammal hone par"
                booking.paymentStatus == "SETTLED" -> "Digital (${booking.paymentMethod}) Verified & Settled"
                else -> "Digital (${booking.paymentMethod}) • Admin verification/escrow"
            }
            Text(payHint, style = MaterialTheme.typography.labelSmall, color = extras.mutedText)

            if (booking.workerLat != 0.0 || booking.workerLng != 0.0) {
                Text(
                    "Live GPS Shared: ${KaamWalaGeo.formatLatLng(booking.workerLat, booking.workerLng)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))
            // Direct customer contact row (Call + WhatsApp + Chat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onCallCustomer,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp)
                        .testTag("worker_call_customer_${booking.id}")
                ) {
                    Icon(Icons.Filled.Call, contentDescription = "Call Customer", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Call", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onWhatsAppCustomer,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp)
                        .testTag("worker_whatsapp_customer_${booking.id}")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "WhatsApp Customer", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("WhatsApp", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onChat,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp)
                        .testTag("worker_chat_customer_${booking.id}")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Chat", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(8.dp))

            when (booking.status) {
                "PENDING" -> {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("worker_accept_job_${booking.id}")
                    ) {
                        Text("Accept Job (-Rs. $leadFee Lead Fee)")
                    }
                }
                "ACCEPTED" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onVerifyOtp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .testTag("worker_verify_otp_${booking.id}")
                        ) {
                            Text("Verify OTP & Start")
                        }
                        OutlinedButton(
                            onClick = onShareGps,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp)
                                .testTag("worker_share_gps_${booking.id}")
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share Live GPS with Customer", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                "IN_PROGRESS" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = onUploadProof,
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 42.dp)
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Proof", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = onAddParts,
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 42.dp)
                            ) {
                                Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Parts", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = onComplete,
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 42.dp)
                                    .testTag("worker_complete_job_${booking.id}")
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Done", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        OutlinedButton(
                            onClick = onShareGps,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp)
                                .testTag("worker_share_gps_${booking.id}")
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Update Live GPS Pin", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                "COMPLETED" -> {
                    Text(
                        "Completed • 7-Day Warranty Active",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                "CANCELLED" -> {
                    Text(
                        "Cancelled by Customer",
                        style = MaterialTheme.typography.labelMedium,
                        color = extras.dangerText
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerBigJobBidCard(
    job: BigJobEntity,
    onSubmitBid: (BigJobEntity, Int, String) -> Unit
) {
    val extras = LocalKaamWalaExtras.current
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("${job.id} • ${job.category}", style = MaterialTheme.typography.titleSmall)
            Text(
                "${job.area} • Customer Budget: ${PlatformHelpers.formatPkr(job.budget)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(job.description, style = MaterialTheme.typography.bodySmall, color = extras.secondaryText)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("Bid (PKR)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Time") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val amt = amount.toIntOrNull()
                    if (amt != null && amt > 0) {
                        onSubmitBid(job, amt, note)
                        amount = ""
                        note = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp)
            ) {
                Text("Submit Quote Bid")
            }
        }
    }
}
