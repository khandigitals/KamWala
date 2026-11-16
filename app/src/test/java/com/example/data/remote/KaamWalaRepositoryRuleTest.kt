package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.AdminLedgerEntity
import com.example.data.local.BookingEntity
import com.example.data.local.UserProfileEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class KaamWalaRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleBooking(id: String) = BookingEntity(
        id = id,
        isDemo = false,
        serviceId = "ac",
        serviceName = "AC & Cooling",
        fee = 500,
        expressFee = 200,
        total = 700,
        express = true,
        femaleSafe = false,
        slot = "Standard (45 Min)",
        area = "Bahria Town",
        customerName = "Hamza Khan",
        customerPhone = "0300-1234567",
        address = "House 14, Block C",
        gpsCoords = "31.3676, 74.1815",
        latitude = 31.3676,
        longitude = 74.1815,
        note = "Cooling low",
        otp = "4821",
        status = "PENDING",
        createdAt = System.currentTimeMillis()
    )

    @Test
    fun ensureWorkspaceInitialized_andInsertBooking_succeedsForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = KaamWalaFirestoreRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureWorkspaceInitializedAndSeeded("Alice Khan", ALICE_EMAIL)
        }

        val bookingId = "KW-${UUID.randomUUID().toString().take(8)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.insertBooking(sampleBooking(bookingId), markFirstPromoUsed = false)
        }

        val loaded = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getBookingById(bookingId)
        }
        assertNotNull(loaded)
        assertEquals(bookingId, loaded?.id)
        assertEquals(31.3676, loaded?.latitude ?: 0.0, 0.0001)

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBookings(uid).first { list -> list.any { it.id == bookingId } }
        }
        assertTrue(emitted.any { it.id == bookingId })
    }

    @Test
    fun updateBookingStatus_terminalLock_preventsRevertingCompletedBooking() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = KaamWalaFirestoreRepository(firestore, auth)

        val bookingId = "KW-${UUID.randomUUID().toString().take(8)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.insertBooking(sampleBooking(bookingId), markFirstPromoUsed = false)
            repository.updateBookingFields(bookingId, mapOf("status" to "COMPLETED"))
        }

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.updateBookingFields(bookingId, mapOf("status" to "PENDING"))
            }
            fail("Expected PERMISSION_DENIED when reverting COMPLETED booking status")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }

        // Rating on completed booking should still succeed
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateBookingFields(bookingId, mapOf("rating" to 5L, "reviewNote" to "Great service!"))
        }
        val updated = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getBookingById(bookingId) }
        assertEquals(5, updated?.rating)
    }

    @Test
    fun observeBookings_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = KaamWalaFirestoreRepository(firestore, auth)
        val bookingId = "KW-${UUID.randomUUID().toString().take(8)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.insertBooking(sampleBooking(bookingId), markFirstPromoUsed = false)
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = KaamWalaFirestoreRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeBookings(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for Bob observing Alice's bookings")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    @Test
    fun observeBookings_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = KaamWalaFirestoreRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeBookings("some_uid_123").first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    @Test
    fun reschedule_workerLocation_andCancelTerminalLock_enforcedByRules() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = KaamWalaFirestoreRepository(firestore, auth)

        val bookingId = "KW-${UUID.randomUUID().toString().take(8)}"
        val booking = sampleBooking(bookingId)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.insertBooking(booking, markFirstPromoUsed = false)
            repository.rescheduleBooking(booking, "Kal Subah 10 AM", "House 99, Street 2")
            repository.updateWorkerLiveLocation(booking, 31.3712, 74.1845)
        }

        val updated = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getBookingById(bookingId) }
        assertEquals("Kal Subah 10 AM", updated?.slot)
        assertEquals("House 99, Street 2", updated?.address)
        assertEquals(31.3712, updated?.workerLat ?: 0.0, 0.0001)
        assertEquals(74.1845, updated?.workerLng ?: 0.0, 0.0001)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.cancelBooking(updated ?: booking)
        }
        val cancelled = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getBookingById(bookingId) }
        assertEquals("CANCELLED", cancelled?.status)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.updateBookingFields(bookingId, mapOf("status" to "ACCEPTED"))
            }
            fail("Expected PERMISSION_DENIED when modifying a CANCELLED booking")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    @Test
    fun marketplaceProfile_workerKyc_adminApproval_andAtomicOrderPickup_succeeds() = runBlocking {
        // 1. Customer (Alice) registers profile and places a shared marketplace order
        val customerUid = signInTestUser(ALICE_EMAIL)
        val customerRepo = KaamWalaFirestoreRepository(firestore, auth)
        val orderId = "KW-MKT-${UUID.randomUUID().toString().take(6)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            customerRepo.saveUserProfile(
                UserProfileEntity(
                    userId = customerUid,
                    role = "CUSTOMER",
                    fullName = "Alice Customer",
                    phone = "0300-1111111",
                    email = ALICE_EMAIL,
                    city = "Lahore",
                    area = "Gulberg",
                    address = "Street 3, Gulberg"
                )
            )
            customerRepo.insertBooking(
                booking = sampleBooking(orderId).copy(customerId = customerUid, paymentMethod = "CASH"),
                markFirstPromoUsed = false,
                publishToMarketplace = true
            )
        }

        // 2. Worker (fresh account) registers KYC -> enforced PENDING with 0 wallet
        val freshWorkerEmail = "bob.${UUID.randomUUID().toString().take(6)}@kaamwala.test"
        val workerUid = signInTestUser(freshWorkerEmail)
        val workerRepo = KaamWalaFirestoreRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            workerRepo.saveUserProfile(
                UserProfileEntity(
                    userId = workerUid,
                    role = "WORKER",
                    fullName = "Ustad Bob",
                    phone = "0300-2222222",
                    email = freshWorkerEmail,
                    city = "Lahore",
                    area = "Gulberg",
                    address = "Workshop 7",
                    skill = "AC & Cooling",
                    experienceYears = 4,
                    cnic = "35202-1234567-1",
                    payoutAccount = "JazzCash 0300-2222222",
                    isOnline = true
                )
            )
        }
        val bobPending = withTimeout(FLOW_TIMEOUT_MS) {
            workerRepo.observeUserProfile(workerUid).first { it.second != null }.second
        }
        assertEquals("PENDING", bobPending?.verificationStatus)
        assertEquals(0, bobPending?.walletBalance)

        // 3. Admin approves Bob with Rs. 500 starter wallet
        signInTestUser(ADMIN_EMAIL)
        val adminRepo = KaamWalaFirestoreRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.approveWorkerProfile(workerUid, bonusWallet = 500L)
        }

        // 4. Bob picks the PENDING marketplace order atomically
        signInTestUser(freshWorkerEmail)
        val loadedOrder = withTimeout(DEFAULT_TIMEOUT_MS) { workerRepo.getBookingById(orderId) }!!
        withTimeout(DEFAULT_TIMEOUT_MS) {
            workerRepo.acceptJobAsWorker(
                booking = loadedOrder,
                workerUid = workerUid,
                workerName = "Ustad Bob",
                workerPhone = "0300-2222222",
                leadFee = 150
            )
        }

        val acceptedOrder = withTimeout(DEFAULT_TIMEOUT_MS) { workerRepo.getBookingById(orderId) }
        assertEquals("ACCEPTED", acceptedOrder?.status)
        assertEquals(workerUid, acceptedOrder?.workerId)
        assertEquals("Ustad Bob", acceptedOrder?.assignedWorkerName)

        val bobAfterAccept = withTimeout(FLOW_TIMEOUT_MS) {
            workerRepo.observeUserProfile(workerUid).first { it.second?.walletBalance == 350 }.second
        }
        assertEquals(350, bobAfterAccept?.walletBalance)

        // 5. Admin ledger records LEAD_FEE and allows Admin WITHDRAWAL
        val adminUid = signInTestUser(ADMIN_EMAIL)
        val ledgerEntries = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAdminLedger(adminUid).first { entries ->
                entries.any { it.orderId == orderId && it.entryType == "LEAD_FEE" }
            }
        }
        assertTrue(ledgerEntries.any { it.amount == 150 && it.entryType == "LEAD_FEE" })

        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.recordAdminLedgerEntry(
                AdminLedgerEntity(
                    id = "LED-W-${UUID.randomUUID().toString().take(6)}",
                    entryType = "WITHDRAWAL",
                    amount = 100,
                    actorName = "Admin",
                    note = "Saved to bank",
                    payoutMethod = "Bank / IBAN",
                    payoutAccount = "PK12MEZN0001",
                    recordedByUid = adminUid
                )
            )
        }
    }

    @Test
    fun marketplaceOrder_digitalPaymentTid_adminSettle_andNonAdminLedgerDenied() = runBlocking {
        val customerUid = signInTestUser(ALICE_EMAIL)
        val customerRepo = KaamWalaFirestoreRepository(firestore, auth)
        val orderId = "KW-PAY-${UUID.randomUUID().toString().take(6)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            customerRepo.insertBooking(
                booking = sampleBooking(orderId).copy(
                    customerId = customerUid,
                    paymentMethod = "JAZZCASH",
                    paymentStatus = "UNPAID"
                ),
                markFirstPromoUsed = false,
                publishToMarketplace = true
            )
            val created = customerRepo.getBookingById(orderId)!!
            customerRepo.submitOrderPaymentTid(created, "JC778899")
        }

        val afterTid = withTimeout(DEFAULT_TIMEOUT_MS) { customerRepo.getBookingById(orderId) }!!
        assertEquals("PAID_PENDING_VERIFY", afterTid.paymentStatus)
        assertEquals("JC778899", afterTid.paymentTid)

        // Non-admin customer must NOT be able to write adminLedger directly
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                customerRepo.recordAdminLedgerEntry(
                    AdminLedgerEntity(
                        id = "LED-FAKE-${UUID.randomUUID().toString().take(6)}",
                        entryType = "WITHDRAWAL",
                        amount = 500,
                        actorName = "Alice",
                        recordedByUid = customerUid
                    )
                )
            }
            fail("Expected PERMISSION_DENIED when non-admin writes adminLedger")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }

        // Admin verifies TID & settles payment
        signInTestUser(ADMIN_EMAIL)
        val adminRepo = KaamWalaFirestoreRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.verifyDigitalOrderPaymentSettled(afterTid, commissionPercent = 10)
        }
        val settled = withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.getBookingById(orderId) }
        assertEquals("SETTLED", settled?.paymentStatus)
    }

    private companion object {
        const val ALICE_EMAIL = "alice.kaamwala@test.com"
        const val BOB_EMAIL = "bob.kaamwala@test.com"
        const val ADMIN_EMAIL = "mnc.sahjavi@gmail.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
