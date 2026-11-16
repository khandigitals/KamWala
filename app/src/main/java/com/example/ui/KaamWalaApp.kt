package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CatalogData
import com.example.ui.auth.RegistrationDialog
import com.example.ui.components.CityPickerDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.StaffPinDialog
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.WorkerScreen
import com.example.ui.sheets.*
import com.example.util.PlatformHelpers
import kotlinx.coroutines.delay

@Composable
fun KaamWalaApp(
    viewModel: KaamWalaViewModel,
    onSignOut: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = CatalogData.strings(state.prefs.lang)

    // Back handling when on secondary tabs
    BackHandler(enabled = state.currentTab != AppTab.HOME) {
        viewModel.selectTab(AppTab.HOME)
    }

    val knownPendingOrderIds = remember(state.currentUserId) { mutableSetOf<String>() }
    var alertArmed by remember(state.currentUserId) { mutableStateOf(false) }

    LaunchedEffect(state.currentUserId, state.profileLoaded) {
        if (state.profileLoaded) {
            delay(1200)
            alertArmed = true
        } else {
            alertArmed = false
        }
    }

    LaunchedEffect(
        state.bookings,
        state.profileLoaded,
        alertArmed,
        state.userProfile,
        state.prefs.workerOnline,
        state.isStaffUnlocked,
        state.prefs.liveCustomerMode
    ) {
        val currentPendingIds = state.bookings
            .filter { !it.isDemo && it.status == "PENDING" }
            .map { it.id }
            .toSet()
        if (!state.profileLoaded || !alertArmed) {
            knownPendingOrderIds.addAll(currentPendingIds)
            return@LaunchedEffect
        }
        val newlyArrived = currentPendingIds.filter { it !in knownPendingOrderIds }
        knownPendingOrderIds.addAll(currentPendingIds)

        val profile = state.userProfile
        val canReceiveWorkerAlerts = if (profile?.role == "WORKER") {
            profile.verificationStatus == "APPROVED" && profile.isOnline && profile.strikes < 3
        } else {
            state.prefs.workerOnline && state.isStaffUnlocked && !state.prefs.liveCustomerMode
        }
        if (newlyArrived.isNotEmpty() && canReceiveWorkerAlerts) {
            PlatformHelpers.playAlertTone()
            viewModel.showToast("Nayi Job Aayi Hai (${newlyArrived.first()})! Worker tab check karein.")
        }
    }

    LaunchedEffect(state.toastMessage) {
        val msg = state.toastMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.dismissToast()
        }
    }

    val navItems = buildList {
        add(Triple(AppTab.HOME, strings.navHome, Icons.Filled.Home to Icons.Outlined.Home))
        add(
            Triple(
                AppTab.ORDERS,
                strings.navOrders,
                Icons.AutoMirrored.Filled.ReceiptLong to Icons.AutoMirrored.Outlined.ReceiptLong
            )
        )
        if (!state.prefs.liveCustomerMode || state.userProfile?.role == "WORKER" || state.isAdminUser) {
            add(Triple(AppTab.WORKER, strings.navWorker, Icons.Filled.Engineering to Icons.Outlined.Engineering))
        }
        if (!state.prefs.liveCustomerMode || state.isAdminUser) {
            add(Triple(AppTab.ADMIN, strings.navAdmin, Icons.Filled.AdminPanelSettings to Icons.Outlined.AdminPanelSettings))
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useRail) {
                    Column {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = com.example.ui.theme.LocalKaamWalaExtras.current.cardBorder
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            navItems.forEach { (tab, label, icons) ->
                                val selected = state.currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = com.example.ui.theme.LocalKaamWalaExtras.current.mutedText,
                                        unselectedTextColor = com.example.ui.theme.LocalKaamWalaExtras.current.mutedText
                                    ),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) icons.first else icons.second,
                                            contentDescription = label
                                        )
                                    },
                                    label = {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.ExtraBold else androidx.compose.ui.text.font.FontWeight.SemiBold
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    Row {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.safeDrawing
                        ) {
                            navItems.forEach { (tab, label, icons) ->
                                val selected = state.currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = com.example.ui.theme.LocalKaamWalaExtras.current.mutedText,
                                        unselectedTextColor = com.example.ui.theme.LocalKaamWalaExtras.current.mutedText
                                    ),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) icons.first else icons.second,
                                            contentDescription = label
                                        )
                                    },
                                    label = {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.ExtraBold else androidx.compose.ui.text.font.FontWeight.SemiBold
                                        )
                                    },
                                    modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                        VerticalDivider(
                            thickness = 1.dp,
                            color = com.example.ui.theme.LocalKaamWalaExtras.current.cardBorder
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (state.currentTab) {
                        AppTab.HOME -> HomeScreen(
                            state = state,
                            onLogoTap = { viewModel.onLogoTap() },
                            onToggleLang = { viewModel.toggleLanguage() },
                            onCityTap = { viewModel.openCityPicker() },
                            onSettingsTap = { viewModel.openSettingsDialog() },
                            onSearchChange = { viewModel.updateSearchQuery(it) },
                            onSelectCategory = { viewModel.setCategory(it) },
                            onToggleFemaleMode = { viewModel.toggleFemaleMode() },
                            onOpenBooking = { viewModel.openBookingSheet(it) },
                            onOpenBigJob = { viewModel.openBigJobDialog() },
                            onOpenB2b = { viewModel.openB2bDialog() },
                            onSubscribePlan = { viewModel.subscribePlan(it) },
                            onSimulateReferral = { viewModel.simulateReferralBonus() },
                            onShowToast = { viewModel.showToast(it) },
                            onOpenLiveVoice = { viewModel.openLiveVoiceDialog() },
                            onSignOut = onSignOut,
                            onOpenRegistration = { viewModel.openRegistrationDialog() }
                        )

                        AppTab.ORDERS -> OrdersScreen(
                            state = state,
                            onNewOrder = { viewModel.selectTab(AppTab.HOME) },
                            onOpenChat = { viewModel.openChat(it, "CUSTOMER") },
                            onOpenParts = { viewModel.openPartsDialog(it, allowAdd = false) },
                            onOpenWarranty = { viewModel.openWarrantyDialog(it) },
                            onOpenRate = { viewModel.openRateDialog(it) },
                            onAcceptBid = { job, bid -> viewModel.acceptBid(job, bid) },
                            onRescheduleBooking = { booking, slot, address ->
                                viewModel.rescheduleBooking(booking, slot, address)
                            },
                            onCancelBooking = { booking -> viewModel.cancelBooking(booking) },
                            onOpenPaymentTid = { booking -> viewModel.openPaymentTidDialog(booking) }
                        )

                        AppTab.WORKER -> WorkerScreen(
                            state = state,
                            onToggleOnline = { viewModel.toggleWorkerOnline() },
                            onLockStaff = { viewModel.lockStaff() },
                            onSelectWorker = { viewModel.changeActiveWorker(it) },
                            onOpenTopUp = { viewModel.openTopUpDialog() },
                            onSelectSubTab = { viewModel.selectWorkerSubTab(it) },
                            onAcceptJob = { viewModel.acceptJob(it) },
                            onOpenOtpVerify = { viewModel.openOtpVerifyDialog(it) },
                            onAttachProof = { booking, uri -> viewModel.attachProofPhoto(booking, uri) },
                            onOpenParts = { viewModel.openPartsDialog(it, allowAdd = true) },
                            onOpenChat = { viewModel.openChat(it, "WORKER") },
                            onCompleteJob = { viewModel.completeJob(it) },
                            onSubmitBid = { job, amt, note -> viewModel.submitWorkerBid(job, amt, note) },
                            onApplyToolLoan = { viewModel.applyToolLoan(it) },
                            onShowToast = { viewModel.showToast(it) },
                            onShareLiveLocation = { booking, lat, lng ->
                                viewModel.updateWorkerLocation(booking, lat, lng)
                            },
                            onOpenRegistration = { viewModel.openRegistrationDialog() }
                        )

                        AppTab.ADMIN -> AdminScreen(
                            state = state,
                            onOpenSettings = { viewModel.openSettingsDialog() },
                            onLockStaff = { viewModel.lockStaff() },
                            onResetStrikes = { viewModel.resetStrikes() },
                            onApproveTopUp = { viewModel.approveTopUp(it) },
                            onRejectTopUp = { viewModel.rejectTopUp(it) },
                            onApproveToolLoan = { viewModel.approveToolLoan(it) },
                            onCollectQist = { viewModel.collectDailyQist(it) },
                            onActivateB2b = { viewModel.activateB2bLead(it) },
                            onAddWorker = { n, s, i, f -> viewModel.addNewWorker(n, s, i, f) },
                            onClearDemoOrders = { viewModel.clearDemoOrders() },
                            onResetAllDemo = { viewModel.resetAllDemo() },
                            onApproveWorker = { viewModel.approveWorkerProfile(it) },
                            onRejectWorker = { viewModel.rejectWorkerProfile(it) },
                            onVerifyPaymentSettled = { viewModel.verifyDigitalOrderPayment(it) },
                            onOpenWithdrawDialog = { viewModel.openAdminWithdrawDialog() },
                            onClearWorkerStrikes = { viewModel.clearWorkerStrikes(it) }
                        )
                    }
                }
            }
        }
    }

    // Bottom Sheets & Dialogs
    if (state.showLiveVoiceDialog) {
        LiveVoiceAssistantDialog(
            city = state.prefs.city,
            onDismiss = { viewModel.closeLiveVoiceDialog() },
            onBookRecommendedService = { serviceId ->
                viewModel.openBookingFromVoice(serviceId)
            }
        )
    }

    state.bookingSheetService?.let { svc ->
        BookingBottomSheet(
            service = svc,
            prefs = state.prefs,
            userProfile = state.userProfile,
            onDismiss = { viewModel.closeBookingSheet() },
            onShowToast = { viewModel.showToast(it) },
            onConfirmBooking = { service, slot, express, femaleSafe, area, name, phone, address, gps, note, photo, voice, promo, paymentMethod, lat, lng ->
                viewModel.submitBooking(
                    service = service,
                    slot = slot,
                    express = express,
                    femaleSafe = femaleSafe,
                    area = area,
                    customerName = name,
                    customerPhone = phone,
                    address = address,
                    gpsCoords = gps,
                    note = note,
                    photoUri = photo,
                    voiceNotePath = voice,
                    promoCode = promo,
                    paymentMethod = paymentMethod,
                    latitude = lat,
                    longitude = lng
                )
            }
        )
    }

    state.confirmedBooking?.let { booking ->
        BookingConfirmedDialog(
            booking = booking,
            adminWhatsApp = state.prefs.whatsapp,
            onDismiss = { viewModel.dismissConfirmedBooking() },
            onGoToOrders = {
                viewModel.dismissConfirmedBooking()
                viewModel.selectTab(AppTab.ORDERS)
            }
        )
    }

    if (state.pendingProtectedTarget != null) {
        StaffPinDialog(
            onDismiss = { viewModel.dismissPinDialog() },
            onSubmitPin = { viewModel.submitStaffPin(it) }
        )
    }

    if (state.showCityPickerDialog) {
        CityPickerDialog(
            currentCity = state.prefs.city,
            onDismiss = { viewModel.closeCityPicker() },
            onSelectCity = { viewModel.selectCity(it) }
        )
    }

    if (state.showSettingsDialog) {
        SettingsDialog(
            prefs = state.prefs,
            onDismiss = { viewModel.closeSettingsDialog() },
            onSave = { brand, city, wa, pay, fee, commPct, pin, live ->
                viewModel.saveSettings(brand, city, wa, pay, fee, commPct, pin, live)
            }
        )
    }

    if (state.showRegistrationDialog) {
        RegistrationDialog(
            initialProfile = state.userProfile,
            defaultName = state.currentUserName,
            defaultEmail = state.currentUserEmail,
            defaultCity = state.prefs.city,
            onDismiss = { viewModel.closeRegistrationDialog() },
            onSaveProfile = { profile -> viewModel.saveRegistrationProfile(profile) }
        )
    }

    state.activePaymentTidBooking?.let { booking ->
        PaymentTidDialog(
            booking = booking,
            payAccount = state.prefs.payAccount,
            onDismiss = { viewModel.closePaymentTidDialog() },
            onSubmitTid = { tid -> viewModel.submitPaymentTid(booking, tid) }
        )
    }

    if (state.showAdminWithdrawDialog) {
        AdminWithdrawalDialog(
            availableBalance = state.netAdminBalance,
            defaultAccount = state.prefs.payAccount,
            onDismiss = { viewModel.closeAdminWithdrawDialog() },
            onConfirmWithdraw = { amt, method, account, note ->
                viewModel.recordAdminWithdrawal(amt, method, account, note)
            }
        )
    }

    if (state.showBigJobDialog) {
        BigJobDialog(
            city = state.prefs.city,
            onDismiss = { viewModel.closeBigJobDialog() },
            onSubmit = { cat, area, desc, budget -> viewModel.submitBigJob(cat, area, desc, budget) }
        )
    }

    if (state.showB2bDialog) {
        B2bDialog(
            onDismiss = { viewModel.closeB2bDialog() },
            onSubmit = { biz, phone, pkg -> viewModel.submitB2bLead(biz, phone, pkg) }
        )
    }

    state.activeChatBookingId?.let { chatId ->
        val msgs = state.chatMessages.filter { it.bookingId == chatId }
        ChatDialog(
            bookingId = chatId,
            currentRole = state.activeChatRole,
            messages = msgs,
            onDismiss = { viewModel.closeChat() },
            onSend = { viewModel.sendChatMessage(it) }
        )
    }

    state.activePartsBookingId?.let { partsId ->
        val booking = state.bookings.firstOrNull { it.id == partsId }
        if (booking != null) {
            HardwarePartsDialog(
                booking = booking,
                allowAdd = state.partsAllowAdd,
                onAddPart = { part -> viewModel.addHardwarePartToBooking(booking, part) },
                onDismiss = { viewModel.closePartsDialog() }
            )
        }
    }

    state.activeWarrantyBooking?.let { booking ->
        WarrantyCertificateDialog(
            booking = booking,
            brandName = state.prefs.brand,
            onDismiss = { viewModel.closeWarrantyDialog() }
        )
    }

    if (state.showTopUpDialog) {
        TopUpDialog(
            payAccount = state.prefs.payAccount,
            onDismiss = { viewModel.closeTopUpDialog() },
            onSubmit = { method, amt, tid -> viewModel.submitTopUp(method, amt, tid) }
        )
    }

    state.activeOtpVerifyBooking?.let { booking ->
        OtpVerifyDialog(
            booking = booking,
            onDismiss = { viewModel.closeOtpVerifyDialog() },
            onVerify = { otp -> viewModel.verifyOtpAndStartJob(booking, otp) }
        )
    }

    state.activeRateBooking?.let { booking ->
        RateReviewDialog(
            booking = booking,
            onDismiss = { viewModel.closeRateDialog() },
            onSubmitRating = { r, note -> viewModel.submitRating(booking, r, note) },
            onReportStrike = { viewModel.reportStrike(booking) }
        )
    }
}
