package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.B2bLeadEntity
import com.example.data.local.BookingEntity
import com.example.data.local.ToolLoanEntity
import com.example.data.local.TopUpRequestEntity
import com.example.data.local.UserProfileEntity
import com.example.model.CatalogData
import com.example.ui.KaamWalaUiState
import com.example.ui.components.FlagshipCard
import com.example.ui.components.KaamWalaInputShape
import com.example.ui.components.StatusPill
import com.example.ui.components.kaamWalaTextFieldColors
import com.example.ui.theme.LocalKaamWalaExtras
import com.example.ui.theme.tabularMono
import com.example.util.PlatformHelpers

@Composable
fun AdminScreen(
    state: KaamWalaUiState,
    onOpenSettings: () -> Unit,
    onLockStaff: () -> Unit,
    onResetStrikes: () -> Unit,
    onApproveTopUp: (TopUpRequestEntity) -> Unit,
    onRejectTopUp: (TopUpRequestEntity) -> Unit,
    onApproveToolLoan: (ToolLoanEntity) -> Unit,
    onCollectQist: (ToolLoanEntity) -> Unit,
    onActivateB2b: (B2bLeadEntity) -> Unit,
    onAddWorker: (String, String, String, Boolean) -> Unit,
    onClearDemoOrders: () -> Unit,
    onResetAllDemo: () -> Unit,
    onApproveWorker: (UserProfileEntity) -> Unit = {},
    onRejectWorker: (UserProfileEntity) -> Unit = {},
    onVerifyPaymentSettled: (BookingEntity) -> Unit = {},
    onOpenWithdrawDialog: () -> Unit = {},
    onClearWorkerStrikes: (UserProfileEntity) -> Unit = {}
) {
    val extras = LocalKaamWalaExtras.current
    val strings = CatalogData.strings(state.prefs.lang)

    var newWName by remember { mutableStateOf("") }
    var newWSkill by remember { mutableStateOf("") }
    var newWInfo by remember { mutableStateOf("") }
    var newWFemale by remember { mutableStateOf(false) }
    var addWorkerError by remember { mutableStateOf<String?>(null) }

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
                        Text(strings.admHdr, style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(
                                onClick = onOpenSettings,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = extras.frostedPill,
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, extras.frostedPillBorder),
                                modifier = Modifier.heightIn(min = 38.dp)
                            ) {
                                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Settings", style = MaterialTheme.typography.labelSmall)
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
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = extras.frostedPill,
                        border = BorderStroke(1.dp, extras.frostedPillBorder)
                    ) {
                        Text(
                            "Heatmap, Workers, B2B & Approvals",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFECFDF5),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // 2. KPI GRID
        item {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        value = PlatformHelpers.formatPkr(state.grossAdminEarnings),
                        label = "Gross Admin Earnings",
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        value = PlatformHelpers.formatPkr(state.netAdminBalance),
                        label = "Net Available Balance",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        value = PlatformHelpers.formatPkr(state.totalGmv),
                        label = "Total Order GMV",
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        value = "${state.totalOrdersAndBidsCount} / ${state.b2bLeads.size} B2B",
                        label = "Orders + Bids",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2B. SAVED ADMIN EARNINGS LEDGER & WITHDRAWALS
        item {
            FlagshipCard(
                containerColor = extras.emeraldTint,
                borderColor = extras.emeraldBorder,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Admin Earnings Ledger & Payouts",
                                style = MaterialTheme.typography.titleMedium,
                                color = extras.heroStart
                            )
                            Text(
                                "Commission ${state.prefs.commissionPercent}% + Lead Fee Rs. ${state.prefs.leadFee}",
                                style = MaterialTheme.typography.labelSmall.tabularMono(),
                                color = extras.secondaryText
                            )
                        }
                        Button(
                            onClick = onOpenWithdrawDialog,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .testTag("admin_withdraw_button")
                        ) {
                            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Save / Withdraw", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, extras.cardBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Gross: ${PlatformHelpers.formatPkr(state.grossAdminEarnings)}",
                            style = MaterialTheme.typography.labelMedium.tabularMono(),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Saved/Withdrawn: ${PlatformHelpers.formatPkr(state.totalAdminWithdrawn)}",
                            style = MaterialTheme.typography.labelMedium.tabularMono(),
                            color = extras.secondaryText
                        )
                    }
                    if (state.adminLedger.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = extras.emeraldBorder.copy(alpha = 0.45f))
                        state.adminLedger.take(8).forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Text(
                                        "${entry.entryType} • ${entry.actorName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val detail = buildString {
                                        if (entry.orderId.isNotBlank()) append("${entry.orderId} ")
                                        if (entry.payoutMethod.isNotBlank()) append("(${entry.payoutMethod} ${entry.payoutAccount}) ")
                                        if (entry.note.isNotBlank()) append(entry.note)
                                    }
                                    if (detail.isNotBlank()) {
                                        Text(detail, style = MaterialTheme.typography.labelSmall, color = extras.mutedText)
                                    }
                                }
                                Text(
                                    text = if (entry.entryType == "WITHDRAWAL") "-${PlatformHelpers.formatPkr(entry.amount)}" else "+${PlatformHelpers.formatPkr(entry.amount)}",
                                    style = MaterialTheme.typography.labelMedium.tabularMono(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (entry.entryType == "WITHDRAWAL") extras.dangerText else extras.heroStart
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Abhi tak koi cloud ledger entry record nahi hui (har accepted lead aur completed order khud yahan save hoga).",
                            style = MaterialTheme.typography.labelSmall,
                            color = extras.mutedText
                        )
                    }
                }
            }
        }

        // 2C. PENDING KAREGAR KYC APPROVALS
        item {
            SectionTitle("Pending Karegar KYC Approvals (${state.pendingWorkerRegistrations.size})")
        }
        if (state.pendingWorkerRegistrations.isEmpty()) {
            item { EmptyAdminNote("Koi pending Karegar KYC request nahi hai.") }
        } else {
            items(state.pendingWorkerRegistrations, key = { "kyc_${it.userId}" }) { profile ->
                FlagshipCard(
                    borderColor = extras.amberBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 5.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${profile.fullName} • ${profile.skill}", style = MaterialTheme.typography.titleSmall)
                            StatusPill(profile.verificationStatus)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Phone: ${profile.phone} • ${profile.city} (${profile.area})",
                            style = MaterialTheme.typography.bodySmall,
                            color = extras.secondaryText
                        )
                        Text(
                            "CNIC: ${profile.cnic} • Exp: ${profile.experienceYears} yrs • Payout: ${profile.payoutAccount}",
                            style = MaterialTheme.typography.labelSmall.tabularMono(),
                            color = extras.mutedText
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onApproveWorker(profile) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .testTag("admin_approve_worker_${profile.userId}")
                            ) {
                                Text("Approve (+Rs. 500)", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { onRejectWorker(profile) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .testTag("admin_reject_worker_${profile.userId}")
                            ) {
                                Text("Reject", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // 2D. DIGITAL PAYMENT TID VERIFICATION QUEUE
        item {
            SectionTitle("Digital Payment TID Verification (${state.pendingDigitalPayments.size})")
        }
        if (state.pendingDigitalPayments.isEmpty()) {
            item { EmptyAdminNote("Koi digital order payment verify hone ke liye pending nahi hai.") }
        } else {
            items(state.pendingDigitalPayments, key = { "pay_${it.id}" }) { b ->
                FlagshipCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 5.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${b.id} • ${PlatformHelpers.formatPkr(b.total)}",
                                style = MaterialTheme.typography.titleSmall.tabularMono()
                            )
                            StatusPill(b.paymentStatus)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${b.customerName} • ${b.paymentMethod} • TID: ${b.paymentTid}",
                            style = MaterialTheme.typography.bodySmall.tabularMono(),
                            color = extras.secondaryText
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onVerifyPaymentSettled(b) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .testTag("admin_verify_payment_${b.id}")
                        ) {
                            Text("Verify TID & Mark SETTLED", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 3. AREA DEMAND HEATMAP
        item {
            FlagshipCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Area Demand Heatmap (${state.prefs.city})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    val activeBookings = state.bookings.filter { it.status != "CANCELLED" }
                    val allAreas = (CatalogData.areasForCity(state.prefs.city) + activeBookings.map { it.area })
                        .filter { it.isNotBlank() }
                        .distinct()
                    val areaCounts = allAreas.map { area ->
                        area to activeBookings.count { it.area.equals(area, ignoreCase = true) }
                    }
                    val maxCount = (areaCounts.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                    areaCounts.forEach { (area, count) ->
                        val ratio = ((count.toFloat() / maxCount.toFloat()).coerceIn(0.08f, 1f))
                        Column(modifier = Modifier.padding(bottom = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(area, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "$count orders",
                                    style = MaterialTheme.typography.labelSmall.tabularMono(),
                                    color = extras.mutedText
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(extras.inputFill)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(MaterialTheme.colorScheme.primary, extras.amberAccent)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. QUALITY CONTROL
        item {
            val registeredWorkers = state.allMarketplaceProfiles.filter { it.role == "WORKER" }
            FlagshipCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quality Control & Strikes", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (registeredWorkers.isNotEmpty()) {
                        registeredWorkers.forEach { profile ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Text(
                                        "${profile.fullName} (${profile.city})",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        "${profile.skill} • Strikes: ${profile.strikes}/3",
                                        style = MaterialTheme.typography.labelSmall.tabularMono(),
                                        color = if (profile.strikes >= 3) extras.dangerText else extras.secondaryText
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (profile.strikes >= 3) {
                                        StatusPill("SUSPENDED")
                                    }
                                    if (profile.strikes > 0) {
                                        OutlinedButton(
                                            onClick = { onClearWorkerStrikes(profile) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .heightIn(min = 42.dp)
                                                .testTag("admin_clear_strikes_${profile.userId}")
                                        ) {
                                            Text("Clear Strikes", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = extras.cardBorder)
                    }
                    Text(
                        "Sandbox / Demo Strikes: ${state.prefs.workerStrikes}/3",
                        style = MaterialTheme.typography.bodySmall.tabularMono(),
                        color = extras.secondaryText
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onResetStrikes,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                    ) {
                        Text("Reset Demo Strikes / Unsuspend")
                    }
                }
            }
        }

        // 5. WALLET RECHARGE REQUESTS
        item {
            SectionTitle("Wallet Recharge Requests (TID)")
        }
        if (state.topUpRequests.isEmpty()) {
            item { EmptyAdminNote("Koi pending recharge request nahi hai.") }
        } else {
            items(state.topUpRequests, key = { it.id }) { req ->
                FlagshipCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 5.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${req.workerName} • ${PlatformHelpers.formatPkr(req.amount)}",
                                style = MaterialTheme.typography.titleSmall.tabularMono()
                            )
                            StatusPill(req.status)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${req.method} • TID: ${req.tid}",
                            style = MaterialTheme.typography.bodySmall.tabularMono(),
                            color = extras.mutedText
                        )
                        if (req.status == "PENDING") {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onApproveTopUp(req) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                ) {
                                    Text("Approve")
                                }
                                OutlinedButton(
                                    onClick = { onRejectTopUp(req) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                ) {
                                    Text("Reject")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. TOOL LOANS & QIST RECOVERY
        item {
            SectionTitle("Tool Loans & Qist Recovery")
        }
        if (state.toolLoans.isEmpty()) {
            item { EmptyAdminNote("Koi tool loan request nahi hai.") }
        } else {
            items(state.toolLoans, key = { it.id }) { loan ->
                FlagshipCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 5.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(loan.toolName, style = MaterialTheme.typography.titleSmall)
                            StatusPill(loan.status)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${loan.workerName} • Paid: ${PlatformHelpers.formatPkr(loan.paidAmount)} / ${PlatformHelpers.formatPkr(loan.totalPrice)}",
                            style = MaterialTheme.typography.bodySmall.tabularMono(),
                            color = extras.mutedText
                        )
                        Spacer(Modifier.height(8.dp))
                        when (loan.status) {
                            "PENDING" -> Button(
                                onClick = { onApproveToolLoan(loan) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                            ) {
                                Text("Approve Tool Loan")
                            }
                            "ACTIVE" -> OutlinedButton(
                                onClick = { onCollectQist(loan) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                            ) {
                                Text("Collect Daily Qist (-Rs. ${loan.dailyQist})")
                            }
                        }
                    }
                }
            }
        }

        // 7. B2B CORPORATE LEADS
        item {
            SectionTitle("B2B Corporate Leads")
        }
        if (state.b2bLeads.isEmpty()) {
            item { EmptyAdminNote("Koi B2B inquiry nahi hai.") }
        } else {
            items(state.b2bLeads, key = { it.id }) { lead ->
                FlagshipCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(lead.businessName, style = MaterialTheme.typography.titleSmall)
                            Text(lead.phone, style = MaterialTheme.typography.bodySmall.tabularMono(), color = MaterialTheme.colorScheme.primary)
                            Text(lead.packageName, style = MaterialTheme.typography.bodySmall, color = extras.mutedText)
                        }
                        if (lead.status == "NEW") {
                            Button(
                                onClick = { onActivateB2b(lead) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.heightIn(min = 42.dp)
                            ) {
                                Text("Activate", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            StatusPill(lead.status)
                        }
                    }
                }
            }
        }

        // 8. ADD WORKER FORM
        item {
            FlagshipCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Naya Hunarmand Add Karein", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newWName,
                        onValueChange = { newWName = it; addWorkerError = null },
                        label = { Text("Worker Name") },
                        singleLine = true,
                        shape = KaamWalaInputShape,
                        colors = kaamWalaTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newWSkill,
                        onValueChange = { newWSkill = it; addWorkerError = null },
                        label = { Text("Skill Category") },
                        singleLine = true,
                        shape = KaamWalaInputShape,
                        colors = kaamWalaTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newWInfo,
                        onValueChange = { newWInfo = it; addWorkerError = null },
                        label = { Text("Phone & CNIC") },
                        singleLine = true,
                        shape = KaamWalaInputShape,
                        colors = kaamWalaTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Checkbox(checked = newWFemale, onCheckedChange = { newWFemale = it })
                        Text("Female / Family-Safe Verified Pro", style = MaterialTheme.typography.bodySmall)
                    }
                    if (addWorkerError != null) {
                        Text(
                            text = addWorkerError ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = extras.dangerText,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newWName.isNotBlank() && newWSkill.isNotBlank() && newWInfo.isNotBlank()) {
                                onAddWorker(newWName, newWSkill, newWInfo, newWFemale)
                                newWName = ""
                                newWSkill = ""
                                newWInfo = ""
                                newWFemale = false
                                addWorkerError = null
                            } else {
                                addWorkerError = "Name, Skill aur Phone/CNIC sab fill karein."
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("admin_add_worker_button")
                    ) {
                        Text("Verify & Add Worker")
                    }
                }
            }
        }

        // 9. REGISTERED WORKERS & ALL BOOKINGS LOG
        item {
            SectionTitle("Registered Workers (${state.workers.size})")
        }
        items(state.workers, key = { it.id }) { worker ->
            FlagshipCard(
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(worker.name, style = MaterialTheme.typography.titleSmall)
                        Text("${worker.skill} • ${worker.info}", style = MaterialTheme.typography.bodySmall, color = extras.mutedText)
                    }
                    StatusPill(if (worker.femaleSafe) "FAMILY_SAFE" else "VERIFIED")
                }
            }
        }

        item {
            SectionTitle("All Bookings Log (${state.bookings.size})")
        }
        items(state.bookings, key = { "adm_${it.id}" }) { b ->
            FlagshipCard(
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${b.id} • ${b.serviceName}", style = MaterialTheme.typography.titleSmall.tabularMono())
                        Text(
                            "${b.customerName} (${b.area}) • ${PlatformHelpers.formatPkr(b.total)} (${b.paymentMethod})",
                            style = MaterialTheme.typography.bodySmall.tabularMono(),
                            color = extras.mutedText
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusPill(b.status)
                        StatusPill(b.paymentStatus)
                    }
                }
            }
        }

        // 10. FRESH START / RESET DEMO
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onClearDemoOrders,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = extras.amberBadgeBg,
                        contentColor = extras.amberText
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Clear Sample Orders", style = MaterialTheme.typography.labelSmall)
                }
                FilledTonalButton(
                    onClick = onResetAllDemo,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = extras.dangerBg,
                        contentColor = extras.dangerText
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Reset All Demo", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun KpiCard(value: String, label: String, modifier: Modifier = Modifier) {
    val extras = LocalKaamWalaExtras.current
    FlagshipCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.tabularMono(),
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = extras.mutedText
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun EmptyAdminNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = LocalKaamWalaExtras.current.mutedText,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
    )
}
