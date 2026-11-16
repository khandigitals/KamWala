package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.BigJobEntity
import com.example.data.local.BookingEntity
import com.example.model.CatalogData
import com.example.model.ToolCatalogItem
import com.example.ui.KaamWalaUiState
import com.example.ui.WorkerSubTab
import com.example.ui.components.CrossDevicePhoto
import com.example.ui.components.FlagshipCard
import com.example.ui.components.FlagshipPrimaryButton
import com.example.ui.components.KaamWalaCardShape
import com.example.ui.components.KaamWalaGeo
import com.example.ui.components.KaamWalaInputShape
import com.example.ui.components.NetworkImageWithFallback
import com.example.ui.components.ReceiptRow
import com.example.ui.components.StatusPill
import com.example.ui.components.kaamWalaTextFieldColors
import com.example.ui.theme.LocalKaamWalaExtras
import com.example.ui.theme.tabularMono
import com.example.util.PlatformHelpers
import com.example.util.VoiceNoteHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    val coroutineScope = rememberCoroutineScope()
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
            pendingProofBooking = null
            if (uri != null && target != null) {
                coroutineScope.launch {
                    val encoded = withContext(Dispatchers.IO) {
                        PlatformHelpers.encodeCompressedPhotoDataUri(context, uri)
                    }
                    onAttachProof(target, encoded)
                }
            }
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
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // 1. HEADER
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Brush.linearGradient(listOf(extras.heroStart, extras.heroEnd)))
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.workerHdr, style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = onToggleOnline,
                                shape = RoundedCornerShape(999.dp),
                                color = if (effectiveOnline) Color(0xFF065F46) else extras.dangerBg,
                                border = BorderStroke(1.dp, extras.frostedPillBorder),
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .testTag("worker_online_toggle")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = if (effectiveOnline) "Online" else "Offline",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (effectiveOnline) Color.White else extras.dangerText
                                    )
                                }
                            }
                            IconButton(
                                onClick = onLockStaff,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(extras.frostedPill)
                                    .border(1.dp, extras.frostedPillBorder, CircleShape)
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = "Lock Staff", tint = Color.White, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = extras.frostedPill,
                            border = BorderStroke(1.dp, extras.frostedPillBorder)
                        ) {
                            Text(
                                "Lead Fee: Rs. ${state.prefs.leadFee}",
                                style = MaterialTheme.typography.labelSmall.tabularMono(),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = extras.frostedPill,
                            border = BorderStroke(1.dp, extras.frostedPillBorder)
                        ) {
                            Text(
                                "Strikes: $effectiveStrikes/3",
                                style = MaterialTheme.typography.labelSmall.tabularMono(),
                                color = if (effectiveStrikes >= 3) extras.dangerText else Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = extras.ambientShadow,
                        spotColor = extras.ambientShadow
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(extras.darkSurface, extras.darkSurfaceAlt)))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
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
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(2.dp, extras.emeraldMint, CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        val canSwitchWorker = state.userProfile?.role != "WORKER" || state.isAdminUser
                        Box(modifier = Modifier.weight(1f)) {
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = canSwitchWorker) { workerMenuExpanded = true }
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = if (canSwitchWorker) "$activeWorkerName ▾" else activeWorkerName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Verified, contentDescription = null, tint = extras.emeraldMint, modifier = Modifier.size(14.dp))
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
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = extras.frostedPill,
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, extras.frostedPillBorder),
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Test", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("Prepaid Lead Wallet", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = PlatformHelpers.formatPkr(state.effectiveWalletBalance),
                                style = MaterialTheme.typography.headlineMedium.tabularMono(),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = extras.emeraldMint
                            )
                        }
                        Button(
                            onClick = onOpenTopUp,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 46.dp)
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
                FlagshipCard(
                    containerColor = bg as Color,
                    borderColor = border as Color,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg as String,
                            style = MaterialTheme.typography.labelMedium,
                            color = fg as Color,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        )
                        OutlinedButton(
                            onClick = onOpenRegistration,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .testTag("worker_kyc_banner_button")
                        ) {
                            Text(btnLabel as String, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 3. SEGMENTED SUBTABS
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.dp, extras.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 12.dp)
            ) {
                Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        WorkerSubTab.JOBS to "Jobs",
                        WorkerSubTab.BIDS to "Big Bids",
                        WorkerSubTab.HISAB to "Hisab & Tools"
                    ).forEach { (tab, title) ->
                        val active = state.workerSubTab == tab
                        val segBg by animateColorAsState(
                            targetValue = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                            label = "worker_seg_$title"
                        )
                        val segFg by animateColorAsState(
                            targetValue = if (active) MaterialTheme.colorScheme.primary else extras.secondaryText,
                            label = "worker_seg_fg_$title"
                        )
                        Surface(
                            onClick = { onSelectSubTab(tab) },
                            shape = RoundedCornerShape(10.dp),
                            color = segBg,
                            shadowElevation = if (active) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = segFg
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
                        FlagshipCard(
                            containerColor = extras.amberBg,
                            borderColor = extras.amberBorder,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$pendingCount Nayi Job Requests Available! Accept karke Lead pakkein.",
                                style = MaterialTheme.typography.labelLarge.tabularMono(),
                                color = extras.amberText,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to strings.workerFilterAll,
                            "PENDING" to strings.workerFilterPending,
                            "ACTIVE" to strings.workerFilterActive,
                            "DONE" to strings.workerFilterDone
                        ).forEach { (key, label) ->
                            val selected = jobStatusFilter == key
                            FilterChip(
                                selected = selected,
                                onClick = { jobStatusFilter = key },
                                shape = RoundedCornerShape(12.dp),
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .testTag("worker_filter_${key.lowercase()}")
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
                        FlagshipCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Is category mein koi job nahi hai.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = extras.mutedText,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
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
                        FlagshipCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Abhi koi Open Big Job nahi hai.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = extras.mutedText,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                } else {
                    items(openBigJobs, key = { it.id }) { job ->
                        WorkerBigJobBidCard(job = job, onSubmitBid = onSubmitBid)
                    }
                }
            }

            WorkerSubTab.HISAB -> {
                item {
                    val isRegisteredWorker = state.userProfile?.role == "WORKER" && !state.isAdminUser
                    val workerJobs = state.workerVisibleBookings.filter { b ->
                        if (isRegisteredWorker) {
                            b.workerId == state.currentUserId ||
                                (b.workerId.isBlank() && b.assignedWorkerName == activeWorkerName)
                        } else {
                            (state.currentUserId.isNotBlank() && b.workerId == state.currentUserId) ||
                                b.assignedWorkerName == activeWorkerName
                        }
                    }
                    val completedJobs = workerJobs.filter { it.status == "COMPLETED" }
                    val totalEarned = completedJobs.sumOf { it.total }
                    val totalLeadFees = workerJobs.sumOf { it.leadFeePaid }
                    val totalCommission = completedJobs.sumOf { it.commissionAmount }
                    val netEarnings = (totalEarned - totalLeadFees - totalCommission).coerceAtLeast(0)
                    FlagshipCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp)
                            .testTag("worker_hisab_ledger_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "WORKER EARNINGS RECEIPT",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.sp,
                                color = extras.mutedText
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("Worker Earnings Ledger", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(extras.receiptBg)
                                    .border(1.dp, extras.cardBorder, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                ReceiptRow("Active Pro", activeWorkerName)
                                ReceiptRow("Completed Jobs", "${completedJobs.size}")
                                ReceiptRow("Gross Job Earnings", PlatformHelpers.formatPkr(totalEarned))
                                ReceiptRow("Lead Fees Paid", PlatformHelpers.formatPkr(totalLeadFees))
                                ReceiptRow("Platform Commission (${state.prefs.commissionPercent}%)", PlatformHelpers.formatPkr(totalCommission))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = extras.cardBorder)
                                ReceiptRow("Net Karegar Earnings", PlatformHelpers.formatPkr(netEarnings), bold = true)
                            }
                        }
                    }
                }

                item {
                    FlagshipCard(
                        containerColor = extras.emeraldTint,
                        borderColor = extras.emeraldBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Tool Micro-Financing (Asaan Qist)", style = MaterialTheme.typography.titleMedium, color = extras.heroStart)
                            Text(
                                "Rozana qist wallet se kat kar naye tools lein:",
                                style = MaterialTheme.typography.bodySmall,
                                color = extras.secondaryText
                            )
                            Spacer(Modifier.height(10.dp))
                            CatalogData.TOOL_ITEMS.forEach { tool ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, extras.cardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tool.name, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                "Total ${PlatformHelpers.formatPkr(tool.price)} • Rs. ${tool.dailyQist}/day",
                                                style = MaterialTheme.typography.bodySmall.tabularMono(),
                                                color = extras.mutedText
                                            )
                                        }
                                        Button(
                                            onClick = { onApplyToolLoan(tool) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.heightIn(min = 42.dp)
                                        ) {
                                            Text("Apply", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                            if (state.toolLoans.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Text("Your Active Tool Loans:", style = MaterialTheme.typography.labelLarge)
                                state.toolLoans.forEach { loan ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${loan.toolName} (${loan.paidAmount}/${loan.totalPrice})",
                                            style = MaterialTheme.typography.bodySmall.tabularMono()
                                        )
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
    FlagshipCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .testTag("worker_job_card_${booking.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${booking.id} • ${booking.serviceName}",
                    style = MaterialTheme.typography.titleSmall.tabularMono(),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(booking.status, isUrgent = booking.express && booking.status == "PENDING")
            }
            Spacer(Modifier.height(6.dp))
            Text("${booking.customerName} • ${booking.customerPhone}", style = MaterialTheme.typography.bodySmall)
            Text("${booking.address} (${booking.area})", style = MaterialTheme.typography.bodySmall, color = extras.mutedText)
            if (booking.note.isNotBlank()) {
                Text("Masla: ${booking.note}", style = MaterialTheme.typography.bodySmall, color = extras.secondaryText)
            }
            if (booking.photoUri.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                CrossDevicePhoto(
                    model = booking.photoUri,
                    contentDescription = "Customer problem photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            if (booking.proofPhotoUri.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                CrossDevicePhoto(
                    model = booking.proofPhotoUri,
                    contentDescription = "Uploaded proof photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            if (booking.voiceNotePath.isNotBlank()) {
                val localVoiceExists = remember(booking.voiceNotePath) {
                    File(booking.voiceNotePath).exists()
                }
                Spacer(Modifier.height(8.dp))
                if (localVoiceExists) {
                    OutlinedButton(
                        onClick = onToggleVoicePlay,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isPlayingVoice) "Stop Voice Note" else "Play Customer Voice Note",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onWhatsAppCustomer,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("worker_missing_voice_${booking.id}")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Voice Note (Recorded on sender's phone — Ask on WhatsApp)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(extras.receiptBg)
                    .border(1.dp, extras.cardBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Bill: ${PlatformHelpers.formatPkr(booking.total)} • ${booking.slot}",
                        style = MaterialTheme.typography.labelMedium.tabularMono(),
                        fontWeight = FontWeight.Bold,
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
                        style = MaterialTheme.typography.labelSmall.tabularMono(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onCallCustomer,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .testTag("worker_call_customer_${booking.id}")
                ) {
                    Icon(Icons.Filled.Call, contentDescription = "Call Customer", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Call", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onWhatsAppCustomer,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .testTag("worker_whatsapp_customer_${booking.id}")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "WhatsApp Customer", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("WhatsApp", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onChat,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
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
                    FlagshipPrimaryButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("worker_accept_job_${booking.id}")
                    ) {
                        Text("Accept Job (-Rs. $leadFee Lead Fee)", style = MaterialTheme.typography.labelLarge)
                    }
                }
                "ACCEPTED" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlagshipPrimaryButton(
                            onClick = onVerifyOtp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("worker_verify_otp_${booking.id}")
                        ) {
                            Text("Verify OTP & Start", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = onShareGps,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .testTag("worker_share_gps_${booking.id}")
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share Live GPS with Customer", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                "IN_PROGRESS" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = onUploadProof,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Proof", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = onAddParts,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                            ) {
                                Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Parts", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = onComplete,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .testTag("worker_complete_job_${booking.id}")
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Done", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        OutlinedButton(
                            onClick = onShareGps,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
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
    FlagshipCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${job.id} • ${job.category}", style = MaterialTheme.typography.titleSmall.tabularMono())
            Spacer(Modifier.height(2.dp))
            Text(
                "${job.area} • Customer Budget: ${PlatformHelpers.formatPkr(job.budget)}",
                style = MaterialTheme.typography.labelMedium.tabularMono(),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(2.dp))
            Text(job.description, style = MaterialTheme.typography.bodySmall, color = extras.secondaryText)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("Bid (PKR)") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = kaamWalaTextFieldColors(),
                    textStyle = LocalTextStyle.current.tabularMono(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Time") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = kaamWalaTextFieldColors(),
                    modifier = Modifier.weight(1.4f)
                )
            }
            Spacer(Modifier.height(10.dp))
            FlagshipPrimaryButton(
                onClick = {
                    val amt = amount.toIntOrNull()
                    if (amt != null && amt > 0) {
                        onSubmitBid(job, amt, note)
                        amount = ""
                        note = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit Quote Bid", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
