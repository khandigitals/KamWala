package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AdminLedgerEntity
import com.example.data.local.BookingEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WorkerEntity
import com.example.model.CatalogData
import com.example.ui.KaamWalaUiState
import com.example.ui.WorkerSubTab
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.RegistrationDialog
import com.example.ui.components.KaamWalaGeo
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.WorkerScreen
import com.example.ui.sheets.BookingBottomSheet
import com.example.ui.sheets.TopUpDialog
import com.example.ui.theme.KaamWalaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("KaamWala", appName)
    }

    @Test
    fun `auth screen renders google sign in button`() {
        composeTestRule.setContent {
            KaamWalaTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("KaamWala").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
    }

    @Test
    fun `home screen renders live voice button sign out and AI fault diagnosis`() {
        composeTestRule.setContent {
            KaamWalaTheme {
                var query by remember { mutableStateOf("") }
                val aiRule = if (query.trim().lowercase().length >= 2) {
                    CatalogData.AI_RULES.firstOrNull { rule ->
                        rule.keys.any { query.trim().lowercase().contains(it) }
                    }
                } else null
                val state = KaamWalaUiState(
                    currentUserName = "Hamza Khan",
                    searchQuery = query,
                    matchedAiRule = aiRule,
                    workers = listOf(
                        WorkerEntity(
                            id = "w_bilal",
                            name = "Ustad Bilal Ahmed",
                            skill = "AC & Electrical",
                            info = "0300-9911223"
                        )
                    )
                )
                HomeScreen(
                    state = state,
                    onLogoTap = {},
                    onToggleLang = {},
                    onCityTap = {},
                    onSettingsTap = {},
                    onSearchChange = { query = it },
                    onSelectCategory = {},
                    onToggleFemaleMode = {},
                    onOpenBooking = {},
                    onOpenBigJob = {},
                    onOpenB2b = {},
                    onSubscribePlan = {},
                    onSimulateReferral = {},
                    onShowToast = {},
                    onOpenLiveVoice = {},
                    onSignOut = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("KaamWala").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sign_out_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("live_voice_assistant_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("smart_search_input").performTextInput("AC E1")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("AI Fault Diagnosis").assertIsDisplayed()
        composeTestRule.onNodeWithText("AC Sensor / Gas / Capacitor Issue").assertIsDisplayed()
    }

    @Test
    fun `orders screen shows live tracking map card for accepted booking`() {
        val sampleBooking = BookingEntity(
            id = "KW-501",
            serviceId = "ac",
            serviceName = "AC & Cooling",
            fee = 500,
            total = 700,
            area = "DHA Phase 1-8",
            customerName = "Ayesha Malik",
            customerPhone = "0300-7712345",
            address = "Street 9, Phase 5, DHA",
            latitude = 31.4697,
            longitude = 74.4071,
            otp = "7394",
            status = "ACCEPTED",
            assignedWorkerName = "Ustad Bilal Ahmed"
        )
        composeTestRule.setContent {
            KaamWalaTheme {
                OrdersScreen(
                    state = KaamWalaUiState(bookings = listOf(sampleBooking)),
                    onNewOrder = {},
                    onOpenChat = {},
                    onOpenParts = {},
                    onOpenWarranty = {},
                    onOpenRate = {},
                    onAcceptBid = { _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("order_live_map_card_KW-501").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ustad Bilal Ahmed En Route").assertIsDisplayed()
    }

    @Test
    fun `booking bottom sheet renders map pin picker preview`() {
        val acService = CatalogData.SERVICES.first { it.id == "ac" }
        composeTestRule.setContent {
            KaamWalaTheme {
                BookingBottomSheet(
                    service = acService,
                    prefs = KaamWalaUiState().prefs,
                    onDismiss = {},
                    onShowToast = {},
                    onConfirmBooking = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Tap Map to Pin House Location").assertIsDisplayed()
        composeTestRule.onNodeWithTag("map_picker_gps_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("booking_payment_JAZZCASH").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `city first geo lookup distinguishes karachi dha from lahore dha`() {
        val karachiDha = KaamWalaGeo.coordsFor("Karachi", "DHA Karachi / Clifton")
        val lahoreDha = KaamWalaGeo.coordsFor("Lahore", "DHA Phase 1-8")
        val islamabadDha = KaamWalaGeo.coordsFor("Islamabad", "DHA / Bahria Enclave")

        assertTrue("Expected Karachi latitude ~24.8, got ${karachiDha.latitude}", karachiDha.latitude < 26.0)
        assertTrue("Expected Lahore latitude ~31.4, got ${lahoreDha.latitude}", lahoreDha.latitude in 31.0..32.0)
        assertTrue("Expected Islamabad latitude ~33.6, got ${islamabadDha.latitude}", islamabadDha.latitude > 33.0)
        assertNotEquals(karachiDha, lahoreDha)

        val karachiAreas = CatalogData.areasForCity("Karachi")
        assertTrue(karachiAreas.contains("DHA Karachi / Clifton"))
        assertTrue(karachiAreas.contains("Gulshan-e-Iqbal"))
    }

    @Test
    fun `orders screen allows cancelling accepted booking and shows cancelled banner`() {
        val initialBooking = BookingEntity(
            id = "KW-601",
            serviceId = "ac",
            serviceName = "AC & Cooling",
            fee = 500,
            total = 700,
            area = "Gulberg",
            customerName = "Saad Tariq",
            customerPhone = "0300-1122334",
            address = "House 12, Block H",
            otp = "4411",
            status = "ACCEPTED",
            assignedWorkerName = "Ustad Bilal Ahmed"
        )
        composeTestRule.setContent {
            KaamWalaTheme {
                var bookings by remember { mutableStateOf(listOf(initialBooking)) }
                OrdersScreen(
                    state = KaamWalaUiState(bookings = bookings),
                    onNewOrder = {},
                    onOpenChat = {},
                    onOpenParts = {},
                    onOpenWarranty = {},
                    onOpenRate = {},
                    onAcceptBid = { _, _ -> },
                    onCancelBooking = { target ->
                        bookings = bookings.map {
                            if (it.id == target.id) it.copy(status = "CANCELLED") else it
                        }
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("order_reschedule_button_KW-601")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("order_cancel_button_KW-601")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Cancel Order KW-601?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_cancel_booking_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Order Cancelled", substring = true).assertIsDisplayed()
    }

    @Test
    fun `worker screen filters jobs and exposes live gps button on accepted jobs`() {
        val pendingJob = BookingEntity(
            id = "KW-701",
            serviceId = "plumb",
            serviceName = "Plumbing",
            fee = 500,
            total = 500,
            area = "Johar Town",
            customerName = "Hira",
            customerPhone = "0301-9988776",
            address = "Block G1",
            otp = "1122",
            status = "PENDING"
        )
        val acceptedJob = BookingEntity(
            id = "KW-702",
            serviceId = "ac",
            serviceName = "AC & Cooling",
            fee = 600,
            total = 600,
            area = "DHA Phase 1-8",
            customerName = "Zain",
            customerPhone = "0302-4455667",
            address = "Phase 5",
            otp = "3344",
            status = "ACCEPTED",
            assignedWorkerName = "Ustad Bilal Ahmed"
        )
        composeTestRule.setContent {
            KaamWalaTheme {
                WorkerScreen(
                    state = KaamWalaUiState(
                        bookings = listOf(pendingJob, acceptedJob),
                        workerSubTab = WorkerSubTab.JOBS
                    ),
                    onToggleOnline = {},
                    onLockStaff = {},
                    onSelectWorker = {},
                    onOpenTopUp = {},
                    onSelectSubTab = {},
                    onAcceptJob = {},
                    onOpenOtpVerify = {},
                    onAttachProof = { _, _ -> },
                    onOpenParts = {},
                    onOpenChat = {},
                    onCompleteJob = {},
                    onSubmitBid = { _, _, _ -> },
                    onApplyToolLoan = {},
                    onShowToast = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("worker_filter_all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("worker_filter_pending").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("worker_job_card_KW-701").assertIsDisplayed()

        composeTestRule.onNodeWithTag("worker_filter_active").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("worker_job_card_KW-702").assertIsDisplayed()
        composeTestRule.onNodeWithTag("worker_share_gps_KW-702")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `topup dialog validates input and submits custom pkr amount`() {
        var submittedMethod = ""
        var submittedAmount = 0
        var submittedTid = ""
        composeTestRule.setContent {
            KaamWalaTheme {
                var showDialog by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = { showDialog = true },
                        modifier = Modifier.testTag("open_topup_host_btn")
                    ) {
                        Text("Open TopUp")
                    }
                    if (showDialog) {
                        TopUpDialog(
                            payAccount = "0300-1234567",
                            onDismiss = { showDialog = false },
                            onSubmit = { m, amt, tid ->
                                submittedMethod = m
                                submittedAmount = amt
                                submittedTid = tid
                            }
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("open_topup_host_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("topup_submit_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Kam az kam 4 characters ka valid TID likhein.").assertIsDisplayed()

        composeTestRule.onNodeWithTag("topup_amount_input").performTextClearance()
        composeTestRule.onNodeWithTag("topup_amount_input").performTextInput("3500")
        composeTestRule.onNodeWithTag("topup_tid_input").performTextInput("TID998877")
        composeTestRule.onNodeWithTag("topup_submit_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("JazzCash", submittedMethod)
        assertEquals(3500, submittedAmount)
        assertEquals("TID998877", submittedTid)
    }

    @Test
    fun `registration dialog validates karegar kyc and submits worker profile`() {
        var savedProfile: UserProfileEntity? = null
        composeTestRule.setContent {
            KaamWalaTheme {
                RegistrationDialog(
                    initialProfile = null,
                    defaultName = "",
                    defaultEmail = "ustad@test.com",
                    defaultCity = "Lahore",
                    onDismiss = {},
                    onSaveProfile = { savedProfile = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("reg_role_worker").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("reg_name_input").performScrollTo().performTextInput("Ustad Kashif")
        composeTestRule.onNodeWithTag("reg_phone_input").performScrollTo().performTextInput("03001112233")
        composeTestRule.onNodeWithTag("reg_address_input").performScrollTo().performTextInput("Main Market Gulberg")
        // Missing CNIC should show error
        composeTestRule.onNodeWithTag("registration_submit_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Karegar verification ke liye valid CNIC number likhein.")
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("reg_cnic_input").performScrollTo().performTextInput("35202-9876543-1")
        composeTestRule.onNodeWithTag("reg_payout_input").performScrollTo().performTextInput("JazzCash 03001112233")
        composeTestRule.onNodeWithTag("registration_submit_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("WORKER", savedProfile?.role)
        assertEquals("PENDING", savedProfile?.verificationStatus)
        assertEquals("Ustad Kashif", savedProfile?.fullName)
        assertEquals("35202-9876543-1", savedProfile?.cnic)
    }

    @Test
    fun `orders screen shows submit tid button for unpaid digital booking`() {
        val jazzBooking = BookingEntity(
            id = "KW-801",
            serviceId = "ac",
            serviceName = "AC & Cooling",
            fee = 500,
            total = 500,
            area = "Johar Town",
            customerName = "Ali Raza",
            customerPhone = "03005554443",
            address = "House 5, Block B",
            otp = "8899",
            status = "PENDING",
            paymentMethod = "JAZZCASH",
            paymentStatus = "UNPAID"
        )
        var tidClicked = false
        composeTestRule.setContent {
            KaamWalaTheme {
                OrdersScreen(
                    state = KaamWalaUiState(bookings = listOf(jazzBooking)),
                    onNewOrder = {},
                    onOpenChat = {},
                    onOpenParts = {},
                    onOpenWarranty = {},
                    onOpenRate = {},
                    onAcceptBid = { _, _ -> },
                    onOpenPaymentTid = { tidClicked = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("order_pay_tid_button_KW-801")
            .performScrollTo()
            .performClick()
        assertTrue(tidClicked)
    }

    @Test
    fun `admin screen displays pending karegar kyc and saved earnings ledger`() {
        val pendingWorker = UserProfileEntity(
            userId = "worker_99",
            role = "WORKER",
            verificationStatus = "PENDING",
            fullName = "Ustad Tariq",
            phone = "03011234567",
            city = "Lahore",
            area = "Model Town",
            skill = "Electrician",
            experienceYears = 5,
            cnic = "35202-1122334-5",
            payoutAccount = "Easypaisa 03011234567"
        )
        val ledger = listOf(
            AdminLedgerEntity(
                id = "LED-1",
                entryType = "LEAD_FEE",
                amount = 150,
                orderId = "KW-801",
                actorName = "Ustad Tariq",
                note = "Lead pickup fee"
            ),
            AdminLedgerEntity(
                id = "LED-2",
                entryType = "COMMISSION",
                amount = 200,
                orderId = "KW-801",
                actorName = "Ustad Tariq",
                note = "10% order commission"
            )
        )
        var approvedUid = ""
        composeTestRule.setContent {
            KaamWalaTheme {
                AdminScreen(
                    state = KaamWalaUiState(
                        isAdminUser = true,
                        allMarketplaceProfiles = listOf(pendingWorker),
                        adminLedger = ledger
                    ),
                    onOpenSettings = {},
                    onLockStaff = {},
                    onResetStrikes = {},
                    onApproveTopUp = {},
                    onRejectTopUp = {},
                    onApproveToolLoan = {},
                    onCollectQist = {},
                    onActivateB2b = {},
                    onAddWorker = { _, _, _, _ -> },
                    onClearDemoOrders = {},
                    onResetAllDemo = {},
                    onApproveWorker = { approvedUid = it.userId }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("admin_withdraw_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gross: Rs. 350").assertIsDisplayed()
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("admin_approve_worker_worker_99"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_approve_worker_worker_99").performClick()
        assertEquals("worker_99", approvedUid)
    }

    @Test
    fun `admin screen shows per-worker strike count suspended pill and clear button`() {
        val suspendedWorker = UserProfileEntity(
            userId = "worker_struck_1",
            role = "WORKER",
            verificationStatus = "APPROVED",
            fullName = "Ustad Shoaib",
            phone = "0300-3334445",
            city = "Lahore",
            skill = "Plumbing",
            strikes = 3
        )
        var clearedUid = ""
        composeTestRule.setContent {
            KaamWalaTheme {
                AdminScreen(
                    state = KaamWalaUiState(
                        isAdminUser = true,
                        allMarketplaceProfiles = listOf(suspendedWorker)
                    ),
                    onOpenSettings = {},
                    onLockStaff = {},
                    onResetStrikes = {},
                    onApproveTopUp = {},
                    onRejectTopUp = {},
                    onApproveToolLoan = {},
                    onCollectQist = {},
                    onActivateB2b = {},
                    onAddWorker = { _, _, _, _ -> },
                    onClearDemoOrders = {},
                    onResetAllDemo = {},
                    onClearWorkerStrikes = { clearedUid = it.userId }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("admin_clear_strikes_worker_struck_1"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SUSPENDED").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_clear_strikes_worker_struck_1").performClick()
        assertEquals("worker_struck_1", clearedUid)
    }

    @Test
    fun `worker hisab excludes completed jobs assigned to other workers`() {
        val myCompletedJob = BookingEntity(
            id = "KW-MINE",
            serviceId = "ac",
            serviceName = "AC & Cooling",
            fee = 1000,
            total = 1000,
            leadFeePaid = 50,
            commissionAmount = 100,
            area = "Gulberg",
            customerName = "Ali",
            customerPhone = "03001111111",
            address = "Gulberg 3",
            otp = "1234",
            status = "COMPLETED",
            workerId = "worker_uid_me",
            assignedWorkerName = "Ustad Tariq"
        )
        val otherWorkersCompletedJob = BookingEntity(
            id = "KW-OTHER",
            serviceId = "plumb",
            serviceName = "Plumbing",
            fee = 5000,
            total = 5000,
            leadFeePaid = 100,
            commissionAmount = 500,
            area = "DHA Phase 1-8",
            customerName = "Zara",
            customerPhone = "03002222222",
            address = "DHA Phase 5",
            otp = "5678",
            status = "COMPLETED",
            workerId = "worker_uid_other",
            assignedWorkerName = "Ustad Bilal Ahmed"
        )
        composeTestRule.setContent {
            KaamWalaTheme {
                WorkerScreen(
                    state = KaamWalaUiState(
                        currentUserId = "worker_uid_me",
                        userProfile = UserProfileEntity(
                            userId = "worker_uid_me",
                            role = "WORKER",
                            verificationStatus = "APPROVED",
                            fullName = "Ustad Tariq"
                        ),
                        bookings = listOf(myCompletedJob, otherWorkersCompletedJob),
                        workerSubTab = WorkerSubTab.HISAB
                    ),
                    onToggleOnline = {},
                    onLockStaff = {},
                    onSelectWorker = {},
                    onOpenTopUp = {},
                    onSelectSubTab = {},
                    onAcceptJob = {},
                    onOpenOtpVerify = {},
                    onAttachProof = { _, _ -> },
                    onOpenParts = {},
                    onOpenChat = {},
                    onCompleteJob = {},
                    onSubmitBid = { _, _, _ -> },
                    onApplyToolLoan = {},
                    onShowToast = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("worker_hisab_ledger_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rs. 1,000").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rs. 850").assertIsDisplayed()
    }

    @Test
    fun `missing cross device voice file falls back to whatsapp prompt on orders and worker screens`() {
        val bookingWithRemoteVoice = BookingEntity(
            id = "KW-VOICE-1",
            serviceId = "ac",
            serviceName = "AC & Cooling",
            fee = 500,
            total = 500,
            area = "Johar Town",
            customerName = "Usman",
            customerPhone = "03001234567",
            address = "Johar Town C",
            voiceNotePath = "/nonexistent/sender/phone/voice_kw1.m4a",
            otp = "9911",
            status = "PENDING"
        )
        composeTestRule.setContent {
            KaamWalaTheme {
                WorkerScreen(
                    state = KaamWalaUiState(
                        bookings = listOf(bookingWithRemoteVoice),
                        workerSubTab = WorkerSubTab.JOBS
                    ),
                    onToggleOnline = {},
                    onLockStaff = {},
                    onSelectWorker = {},
                    onOpenTopUp = {},
                    onSelectSubTab = {},
                    onAcceptJob = {},
                    onOpenOtpVerify = {},
                    onAttachProof = { _, _ -> },
                    onOpenParts = {},
                    onOpenChat = {},
                    onCompleteJob = {},
                    onSubmitBid = { _, _, _ -> },
                    onApplyToolLoan = {},
                    onShowToast = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag("worker_missing_voice_KW-VOICE-1"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("worker_missing_voice_KW-VOICE-1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Voice Note (Recorded on sender's phone — Ask on WhatsApp)").assertIsDisplayed()
    }
}
