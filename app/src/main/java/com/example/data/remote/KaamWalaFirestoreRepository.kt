package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.*
import com.example.data.preferences.AppPrefsState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class KaamWalaFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun userRef(uid: String) = db.collection("users").document(uid)
    private fun profileRef(uid: String) = db.collection("marketplaceProfiles").document(uid)
    private fun marketOrderRef(orderId: String) = db.collection("marketplaceOrders").document(orderId)

    // =========================================================================
    // WORKSPACE OBSERVATIONS (Preferences & Demo Sandbox)
    // =========================================================================

    fun observePrefs(userId: String): Flow<AppPrefsState> =
        userRef(userId)
            .snapshots()
            .map { snap ->
                snap.toObject(UserWorkspaceDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                    ?.toPrefsState() ?: AppPrefsState()
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.GET, "users/$userId", userId)
            }

    fun observeBookings(userId: String): Flow<List<BookingEntity>> =
        userRef(userId).collection("bookings")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val created = doc.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)
                    doc.toObject(BookingDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        ?.copy(createdAt = created)
                        ?.toEntity()
                }.sortedByDescending { it.createdAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "users/$userId/bookings", userId)
            }

    fun observeWorkers(userId: String): Flow<List<WorkerEntity>> =
        userRef(userId).collection("workers")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(WorkerDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
                }.sortedBy { it.createdAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "users/$userId/workers", userId)
            }

    fun observeBigJobs(userId: String): Flow<List<BigJobEntity>> =
        userRef(userId).collection("bigJobs")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(BigJobDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
                }.sortedByDescending { it.createdAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "users/$userId/bigJobs", userId)
            }

    fun observeBids(userId: String): Flow<List<BidEntity>> =
        userRef(userId).collection("bids")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(BidDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
                }.sortedByDescending { it.createdAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "users/$userId/bids", userId)
            }

    fun observeB2bLeads(userId: String): Flow<List<B2bLeadEntity>> =
        userRef(userId).collection("b2bLeads")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(B2bLeadDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
                }.sortedByDescending { it.createdAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "users/$userId/b2bLeads", userId)
            }

    fun observeTopUpRequests(userId: String): Flow<List<TopUpRequestEntity>> =
        userRef(userId).collection("topUpRequests")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(TopUpRequestDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
                }.sortedByDescending { it.createdAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "users/$userId/topUpRequests", userId)
            }

    fun observeToolLoans(userId: String): Flow<List<ToolLoanEntity>> =
        userRef(userId).collection("toolLoans")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(ToolLoanDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
                }.sortedByDescending { it.createdAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "users/$userId/toolLoans", userId)
            }

    fun observeChatMessages(userId: String): Flow<List<ChatMessageEntity>> =
        userRef(userId).collection("chatMessages")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessageDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
                }.sortedBy { it.timestamp }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "users/$userId/chatMessages", userId)
            }

    fun observeActivePlans(userId: String): Flow<List<ActivePlanEntity>> =
        userRef(userId).collection("activePlans")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(ActivePlanDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
                }.sortedByDescending { it.subscribedAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "users/$userId/activePlans", userId)
            }

    // =========================================================================
    // SHARED MARKETPLACE OBSERVATIONS (Cross-Account Customer <-> Worker <-> Admin)
    // =========================================================================

    fun observeUserProfile(userId: String): Flow<Pair<Boolean, UserProfileEntity?>> =
        profileRef(userId)
            .snapshots()
            .map { snap ->
                if (!snap.exists()) true to null
                else true to snap.toObject(MarketplaceProfileDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.GET, "marketplaceProfiles/$userId", userId)
            }

    fun observeVerifiedWorkers(uid: String): Flow<List<UserProfileEntity>> =
        db.collection("marketplaceProfiles")
            .whereEqualTo("role", "WORKER")
            .whereEqualTo("verificationStatus", "APPROVED")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull {
                    it.toObject(MarketplaceProfileDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
                }.sortedByDescending { it.jobsCompleted }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "marketplaceProfiles", uid)
            }

    fun observeAllMarketplaceProfiles(uid: String): Flow<List<UserProfileEntity>> =
        db.collection("marketplaceProfiles")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull {
                    it.toObject(MarketplaceProfileDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
                }.sortedByDescending { it.createdAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "marketplaceProfiles", uid)
            }

    fun observeMarketplaceConfig(uid: String): Flow<MarketplaceConfigDoc?> =
        db.collection("marketplaceConfig").document("global")
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(MarketplaceConfigDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                else null
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.GET, "marketplaceConfig/global", uid)
            }

    fun observeCustomerMarketplaceOrders(uid: String): Flow<List<BookingEntity>> =
        db.collection("marketplaceOrders")
            .whereEqualTo("customerId", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val created = doc.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)
                    doc.toObject(MarketplaceOrderDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        ?.copy(createdAt = created)
                        ?.toEntity()
                }.sortedByDescending { it.createdAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "marketplaceOrders(customer)", uid)
            }

    fun observePendingDispatchOrders(uid: String): Flow<List<BookingEntity>> =
        db.collection("marketplaceOrders")
            .whereEqualTo("status", "PENDING")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val created = doc.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)
                    doc.toObject(MarketplaceOrderDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        ?.copy(createdAt = created)
                        ?.toEntity()
                }.sortedByDescending { it.createdAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "marketplaceOrders(pending)", uid)
            }

    fun observeWorkerAssignedOrders(uid: String): Flow<List<BookingEntity>> =
        db.collection("marketplaceOrders")
            .whereEqualTo("workerId", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val created = doc.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)
                    doc.toObject(MarketplaceOrderDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        ?.copy(createdAt = created)
                        ?.toEntity()
                }.sortedByDescending { it.createdAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "marketplaceOrders(worker)", uid)
            }

    fun observeAllMarketplaceOrders(uid: String): Flow<List<BookingEntity>> =
        db.collection("marketplaceOrders")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val created = doc.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)
                    doc.toObject(MarketplaceOrderDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        ?.copy(createdAt = created)
                        ?.toEntity()
                }.sortedByDescending { it.createdAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "marketplaceOrders(all)", uid)
            }

    fun observeMarketplaceOrderMessages(
        orderId: String,
        uid: String,
        isAdmin: Boolean,
        isCustomer: Boolean
    ): Flow<List<ChatMessageEntity>> {
        val base = marketOrderRef(orderId).collection("messages")
        val query = when {
            isAdmin -> base
            isCustomer -> base.whereEqualTo("customerId", uid)
            else -> base.whereEqualTo("workerId", uid)
        }
        return query.snapshots()
            .map { snap ->
                snap.documents.mapNotNull {
                    it.toObject(MarketplaceChatMessageDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
                }.sortedBy { it.timestamp }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "marketplaceOrders/$orderId/messages", uid)
            }
    }

    fun observeMarketplaceTopUps(uid: String, isAdmin: Boolean): Flow<List<TopUpRequestEntity>> {
        val query = if (isAdmin) db.collection("marketplaceTopUps")
        else db.collection("marketplaceTopUps").whereEqualTo("workerUid", uid)
        return query.snapshots()
            .map { snap ->
                snap.documents.mapNotNull {
                    it.toObject(MarketplaceTopUpDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
                }.sortedByDescending { it.createdAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "marketplaceTopUps", uid)
            }
    }

    fun observeMarketplaceToolLoans(uid: String, isAdmin: Boolean): Flow<List<ToolLoanEntity>> {
        val query = if (isAdmin) db.collection("marketplaceToolLoans")
        else db.collection("marketplaceToolLoans").whereEqualTo("workerUid", uid)
        return query.snapshots()
            .map { snap ->
                snap.documents.mapNotNull {
                    it.toObject(MarketplaceToolLoanDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
                }.sortedByDescending { it.createdAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "marketplaceToolLoans", uid)
            }
    }

    fun observeAdminLedger(uid: String): Flow<List<AdminLedgerEntity>> =
        db.collection("adminLedger")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull {
                    it.toObject(AdminLedgerDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
                }.sortedByDescending { it.createdAt }
            }
            .catch { error ->
                logAndRethrowIfSignedIn(error, OperationType.LIST, "adminLedger", uid)
            }

    private fun logAndRethrowIfSignedIn(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw firestoreError ?: error
    }

    // =========================================================================
    // INITIALIZATION & REGISTRATION
    // =========================================================================

    suspend fun ensureWorkspaceInitializedAndSeeded(
        displayName: String = auth.currentUser?.displayName ?: "KaamWala User",
        email: String? = auth.currentUser?.email
    ) {
        val uid = requireUserId()
        val ref = userRef(uid)
        try {
            val existing = ref.get().await()
            if (!existing.exists()) {
                val doc = UserWorkspaceDoc(
                    userId = uid,
                    displayName = displayName.ifBlank { "KaamWala User" },
                    email = email,
                    isSeeded = true
                )
                ref.set(doc.toCreateMap()).await()
                seedDefaultCollections(uid)
            } else if (existing.getBoolean("isSeeded") != true) {
                seedDefaultCollections(uid)
                updateWorkspaceFields(mapOf("isSeeded" to true))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }

    suspend fun saveUserProfile(profile: UserProfileEntity, isAdminCaller: Boolean = false) {
        val uid = requireUserId()
        val targetUid = profile.userId.ifBlank { uid }
        val ref = profileRef(targetUid)
        try {
            val snap = ref.get().await()
            if (!snap.exists()) {
                val enforcedStatus = when {
                    isAdminCaller -> profile.verificationStatus
                    profile.role == "WORKER" -> "PENDING"
                    else -> "APPROVED"
                }
                val safeProfile = profile.copy(
                    userId = targetUid,
                    verificationStatus = enforcedStatus,
                    walletBalance = if (isAdminCaller) profile.walletBalance else 0,
                    totalEarnings = if (isAdminCaller) profile.totalEarnings else 0,
                    strikes = if (isAdminCaller) profile.strikes else 0,
                    jobsCompleted = if (isAdminCaller) profile.jobsCompleted else 0
                )
                ref.set(safeProfile.toCreateMap()).await()
            } else {
                val existingRole = snap.getString("role") ?: "CUSTOMER"
                val existingVerif = snap.getString("verificationStatus") ?: "APPROVED"
                val nextVerification = when {
                    isAdminCaller -> profile.verificationStatus
                    profile.role == existingRole -> existingVerif
                    profile.role == "WORKER" -> "PENDING"
                    else -> "APPROVED"
                }
                ref.update(
                    mapOf(
                        "role" to profile.role,
                        "verificationStatus" to nextVerification,
                        "fullName" to profile.fullName.trim().ifBlank { "KaamWala User" }.take(100),
                        "phone" to profile.phone.trim().ifBlank { "03000000000" }.take(30),
                        "email" to profile.email.trim().take(160),
                        "city" to profile.city.trim().ifBlank { "Lahore" }.take(60),
                        "area" to profile.area.trim().take(100),
                        "address" to profile.address.trim().take(300),
                        "skill" to profile.skill.trim().take(120),
                        "experienceYears" to profile.experienceYears.coerceIn(0, 60).toLong(),
                        "cnic" to profile.cnic.trim().take(25),
                        "payoutAccount" to profile.payoutAccount.trim().take(120),
                        "femaleSafe" to profile.femaleSafe,
                        "isOnline" to profile.isOnline,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }

    suspend fun approveWorkerProfile(workerUid: String, bonusWallet: Long = 500L) {
        requireUserId()
        val ref = profileRef(workerUid)
        try {
            val snap = ref.get().await()
            val currentWallet = snap.getLong("walletBalance") ?: 0L
            ref.update(
                mapOf(
                    "verificationStatus" to "APPROVED",
                    "walletBalance" to currentWallet + bonusWallet.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun rejectWorkerProfile(workerUid: String) {
        requireUserId()
        val ref = profileRef(workerUid)
        try {
            ref.update(
                mapOf(
                    "verificationStatus" to "REJECTED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun setMarketplaceWorkerOnline(workerUid: String, isOnline: Boolean) {
        requireUserId()
        val ref = profileRef(workerUid)
        try {
            ref.update(
                mapOf(
                    "isOnline" to isOnline,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun saveMarketplaceConfig(
        brand: String,
        city: String,
        whatsapp: String,
        payAccount: String,
        leadFee: Int,
        commissionPercent: Int
    ) {
        requireUserId()
        val ref = db.collection("marketplaceConfig").document("global")
        try {
            ref.set(
                mapOf(
                    "brand" to brand.trim().ifBlank { "KaamWala" }.take(60),
                    "city" to city.trim().ifBlank { "Lahore" }.take(60),
                    "whatsapp" to whatsapp.trim().take(30),
                    "payAccount" to payAccount.trim().take(120),
                    "leadFee" to leadFee.coerceIn(0, 100000).toLong(),
                    "commissionPercent" to commissionPercent.coerceIn(0, 50).toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }

    private suspend fun seedDefaultCollections(uid: String) {
        val root = userRef(uid)
        defaultWorkers().forEach { w ->
            root.collection("workers").document(w.id).set(w.toCreateMap()).await()
        }
        defaultBookings().forEach { b ->
            root.collection("bookings").document(b.id).set(b.toCreateMap()).await()
        }
        defaultBigJobs().forEach { bj ->
            root.collection("bigJobs").document(bj.id).set(bj.toCreateMap()).await()
        }
        defaultChats().forEach { chat ->
            root.collection("chatMessages").document(chat.id).set(chat.toCreateMap()).await()
        }
        defaultB2bLeads().forEach { lead ->
            root.collection("b2bLeads").document(lead.id).set(lead.toCreateMap()).await()
        }
    }

    suspend fun updateWorkspaceFields(fields: Map<String, Any>) {
        val uid = requireUserId()
        val ref = userRef(uid)
        val payload = fields.toMutableMap().apply {
            put("updatedAt", FieldValue.serverTimestamp())
        }
        try {
            ref.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    // =========================================================================
    // BOOKINGS & SHARED MARKETPLACE ORDERS
    // =========================================================================

    suspend fun insertBooking(
        booking: BookingEntity,
        markFirstPromoUsed: Boolean,
        publishToMarketplace: Boolean = false
    ) {
        val uid = requireUserId()
        val isShared = publishToMarketplace || booking.customerId.isNotBlank()
        val docRef = if (isShared) marketOrderRef(booking.id)
        else userRef(uid).collection("bookings").document(booking.id)
        val payload = if (isShared) booking.toMarketplaceCreateMap(booking.customerId.ifBlank { uid })
        else booking.toCreateMap()
        try {
            docRef.set(payload).await()
            val updates = mutableMapOf<String, Any>()
            if (booking.creditUsed > 0) {
                updates["customerCredit"] = FieldValue.increment(-booking.creditUsed.toLong())
            }
            if (markFirstPromoUsed) {
                updates["usedFirstPromo"] = true
            }
            if (updates.isNotEmpty()) {
                updateWorkspaceFields(updates)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateOrderFields(booking: BookingEntity, fields: Map<String, Any>) {
        val uid = requireUserId()
        require(booking.id.isNotBlank()) { "Booking ID must not be blank" }
        val docRef = if (booking.customerId.isNotBlank()) marketOrderRef(booking.id)
        else userRef(uid).collection("bookings").document(booking.id)
        val payload = fields.toMutableMap().apply {
            put("updatedAt", FieldValue.serverTimestamp())
        }
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateBookingFields(bookingId: String, fields: Map<String, Any>) {
        val uid = requireUserId()
        require(bookingId.isNotBlank()) { "Booking ID must not be blank" }
        val docRef = userRef(uid).collection("bookings").document(bookingId)
        val payload = fields.toMutableMap().apply {
            put("updatedAt", FieldValue.serverTimestamp())
        }
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun getBookingById(bookingId: String): BookingEntity? {
        val uid = requireUserId()
        val docRef = userRef(uid).collection("bookings").document(bookingId)
        return try {
            val localSnap = docRef.get().await()
            if (localSnap.exists()) {
                return localSnap.toObject(BookingDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
            }
            val mktSnap = marketOrderRef(bookingId).get().await()
            if (mktSnap.exists()) {
                mktSnap.toObject(MarketplaceOrderDoc::class.java, ServerTimestampBehavior.ESTIMATE)?.toEntity()
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun acceptJobAsWorker(
        booking: BookingEntity,
        workerUid: String,
        workerName: String,
        workerPhone: String,
        leadFee: Int
    ) {
        val uid = requireUserId()
        val ledgerId = "LED-${UUID.randomUUID().toString().take(8).uppercase()}"
        val ledgerRef = db.collection("adminLedger").document(ledgerId)
        val ledgerEntry = AdminLedgerEntity(
            id = ledgerId,
            entryType = "LEAD_FEE",
            amount = leadFee,
            orderId = booking.id,
            actorName = workerName,
            note = "Lead fee for ${booking.serviceName} (${booking.area})",
            recordedByUid = uid
        )

        if (booking.customerId.isNotBlank()) {
            val orderRef = marketOrderRef(booking.id)
            val wProfileRef = profileRef(workerUid.ifBlank { uid })
            try {
                db.runTransaction { transaction ->
                    val orderSnap = transaction.get(orderRef)
                    val currentStatus = orderSnap.getString("status") ?: ""
                    if (currentStatus != "PENDING") {
                        throw IllegalStateException("Order is already $currentStatus.")
                    }
                    val pSnap = transaction.get(wProfileRef)
                    if (pSnap.exists()) {
                        val currentWallet = pSnap.getLong("walletBalance") ?: 0L
                        if (currentWallet < leadFee.toLong()) {
                            throw IllegalStateException("Insufficient wallet balance. Top up Rs. $leadFee+ first.")
                        }
                        transaction.update(
                            wProfileRef,
                            mapOf(
                                "walletBalance" to currentWallet - leadFee.toLong(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    transaction.update(
                        orderRef,
                        mapOf(
                            "status" to "ACCEPTED",
                            "workerId" to uid,
                            "assignedWorkerName" to workerName.take(100),
                            "assignedWorkerPhone" to workerPhone.take(40),
                            "leadFeePaid" to leadFee.toLong(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    transaction.set(ledgerRef, ledgerEntry.toCreateMap(uid))
                }.await()
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
                throw e
            }
        }

        // Fallback for sandbox / demo workspace bookings
        updateWorkspaceFields(
            mapOf(
                "walletBalance" to FieldValue.increment(-leadFee.toLong()),
                "appRevenue" to FieldValue.increment(leadFee.toLong())
            )
        )
        updateBookingFields(
            booking.id,
            mapOf(
                "status" to "ACCEPTED",
                "assignedWorkerName" to workerName,
                "leadFeePaid" to leadFee.toLong()
            )
        )
        runCatching { ledgerRef.set(ledgerEntry.toCreateMap(uid)).await() }
    }

    suspend fun submitOrderPaymentTid(booking: BookingEntity, tid: String) {
        updateOrderFields(
            booking,
            mapOf(
                "paymentTid" to tid.trim().take(80),
                "paymentStatus" to "PAID_PENDING_VERIFY"
            )
        )
    }

    suspend fun completeOrderAndRecordCommission(
        booking: BookingEntity,
        proofPhotoUri: String,
        commissionPercent: Int,
        actorName: String
    ) {
        val uid = requireUserId()
        val commission = ((booking.total * commissionPercent.coerceIn(0, 50)) / 100).coerceAtLeast(0)
        if (booking.customerId.isNotBlank()) {
            val updates = mutableMapOf<String, Any>(
                "status" to "COMPLETED",
                "proofPhotoUri" to proofPhotoUri.take(1000),
                "commissionAmount" to commission.toLong()
            )
            if (booking.paymentMethod == "CASH") {
                updates["paymentStatus"] = "SETTLED"
            }
            updateOrderFields(booking, updates)
            if (booking.paymentMethod == "CASH" && commission > 0) {
                recordAdminLedgerEntry(
                    AdminLedgerEntity(
                        id = "LED-${UUID.randomUUID().toString().take(8).uppercase()}",
                        entryType = "COMMISSION",
                        amount = commission,
                        orderId = booking.id,
                        actorName = actorName,
                        note = "Cash job $commissionPercent% fee (${booking.serviceName})",
                        recordedByUid = uid
                    )
                )
            }
        } else {
            updateBookingFields(
                booking.id,
                mapOf(
                    "status" to "COMPLETED",
                    "proofPhotoUri" to proofPhotoUri.take(1000)
                )
            )
        }
    }

    suspend fun verifyDigitalOrderPaymentSettled(booking: BookingEntity, commissionPercent: Int) {
        val uid = requireUserId()
        val commission = if (booking.commissionAmount > 0) booking.commissionAmount
        else ((booking.total * commissionPercent.coerceIn(0, 50)) / 100).coerceAtLeast(0)
        updateOrderFields(
            booking,
            mapOf(
                "paymentStatus" to "SETTLED",
                "commissionAmount" to commission.toLong()
            )
        )
        if (commission > 0) {
            recordAdminLedgerEntry(
                AdminLedgerEntity(
                    id = "LED-${UUID.randomUUID().toString().take(8).uppercase()}",
                    entryType = "COMMISSION",
                    amount = commission,
                    orderId = booking.id,
                    actorName = booking.customerName,
                    note = "${booking.paymentMethod} TID ${booking.paymentTid} settled",
                    recordedByUid = uid
                )
            )
        }
    }

    suspend fun reportStrike(booking: BookingEntity, currentStrikes: Int) {
        val nextStrikes = (currentStrikes + 1).coerceAtMost(3).toLong()
        updateWorkspaceFields(mapOf("workerStrikes" to nextStrikes))
        val note = if (booking.reviewNote.isBlank()) "Quality strike reported"
        else "${booking.reviewNote} (Strike reported)"
        updateOrderFields(booking, mapOf("reviewNote" to note.take(500)))
    }

    suspend fun cancelBooking(booking: BookingEntity) {
        updateOrderFields(booking, mapOf("status" to "CANCELLED"))
    }

    suspend fun rescheduleBooking(booking: BookingEntity, newSlot: String, newAddress: String) {
        updateOrderFields(
            booking,
            mapOf(
                "slot" to newSlot.take(60),
                "address" to newAddress.take(300)
            )
        )
    }

    suspend fun updateWorkerLiveLocation(booking: BookingEntity, workerLat: Double, workerLng: Double) {
        updateOrderFields(
            booking,
            mapOf(
                "workerLat" to workerLat,
                "workerLng" to workerLng
            )
        )
    }

    // =========================================================================
    // ADMIN LEDGER & EARNINGS PERSISTENCE
    // =========================================================================

    suspend fun recordAdminLedgerEntry(entry: AdminLedgerEntity) {
        val uid = requireUserId()
        val ledgerId = entry.id.ifBlank { "LED-${UUID.randomUUID().toString().take(8).uppercase()}" }
        val docRef = db.collection("adminLedger").document(ledgerId)
        try {
            docRef.set(entry.toCreateMap(uid)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // =========================================================================
    // SHARED & WORKSPACE SUPPORT ENTITIES (Workers, BigJobs, TopUps, Loans, Chat)
    // =========================================================================

    suspend fun insertWorker(worker: WorkerEntity) {
        val uid = requireUserId()
        val docRef = userRef(uid).collection("workers").document(worker.id)
        try {
            docRef.set(worker.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun insertBigJob(bigJob: BigJobEntity) {
        val uid = requireUserId()
        val docRef = userRef(uid).collection("bigJobs").document(bigJob.id)
        try {
            docRef.set(bigJob.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun acceptBid(bigJobId: String, bidId: String) {
        val uid = requireUserId()
        val docRef = userRef(uid).collection("bigJobs").document(bigJobId)
        try {
            docRef.update(
                mapOf(
                    "status" to "AWARDED",
                    "acceptedBidId" to bidId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun insertBid(bid: BidEntity) {
        val uid = requireUserId()
        val docRef = userRef(uid).collection("bids").document(bid.id)
        try {
            docRef.set(bid.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun insertB2bLead(lead: B2bLeadEntity) {
        val uid = requireUserId()
        val docRef = userRef(uid).collection("b2bLeads").document(lead.id)
        try {
            docRef.set(lead.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun activateB2bLead(leadId: String) {
        val uid = requireUserId()
        val docRef = userRef(uid).collection("b2bLeads").document(leadId)
        try {
            docRef.update(
                mapOf(
                    "status" to "ACTIVE",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun insertTopUpRequest(req: TopUpRequestEntity, useMarketplace: Boolean = false) {
        val uid = requireUserId()
        val docRef = if (useMarketplace || req.workerUid.isNotBlank()) {
            db.collection("marketplaceTopUps").document(req.id)
        } else {
            userRef(uid).collection("topUpRequests").document(req.id)
        }
        val payload = if (useMarketplace || req.workerUid.isNotBlank()) {
            req.toMarketplaceCreateMap(req.workerUid.ifBlank { uid })
        } else {
            req.toCreateMap()
        }
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun approveTopUp(req: TopUpRequestEntity) {
        val uid = requireUserId()
        if (req.workerUid.isNotBlank()) {
            val docRef = db.collection("marketplaceTopUps").document(req.id)
            try {
                docRef.update(
                    mapOf(
                        "status" to "APPROVED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                val wProfile = profileRef(req.workerUid)
                val snap = wProfile.get().await()
                if (snap.exists()) {
                    val cur = snap.getLong("walletBalance") ?: 0L
                    wProfile.update(
                        mapOf(
                            "walletBalance" to cur + req.amount.toLong(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
            return
        }

        val docRef = userRef(uid).collection("topUpRequests").document(req.id)
        try {
            docRef.update(
                mapOf(
                    "status" to "APPROVED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            updateWorkspaceFields(mapOf("walletBalance" to FieldValue.increment(req.amount.toLong())))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun rejectTopUp(req: TopUpRequestEntity) {
        val uid = requireUserId()
        val docRef = if (req.workerUid.isNotBlank()) db.collection("marketplaceTopUps").document(req.id)
        else userRef(uid).collection("topUpRequests").document(req.id)
        try {
            docRef.update(
                mapOf(
                    "status" to "REJECTED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun insertToolLoan(loan: ToolLoanEntity, useMarketplace: Boolean = false) {
        val uid = requireUserId()
        val isMkt = useMarketplace || loan.workerUid.isNotBlank()
        val docRef = if (isMkt) db.collection("marketplaceToolLoans").document(loan.id)
        else userRef(uid).collection("toolLoans").document(loan.id)
        val payload = if (isMkt) loan.toMarketplaceCreateMap(loan.workerUid.ifBlank { uid })
        else loan.toCreateMap()
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun approveToolLoan(loan: ToolLoanEntity) {
        val uid = requireUserId()
        val docRef = if (loan.workerUid.isNotBlank()) db.collection("marketplaceToolLoans").document(loan.id)
        else userRef(uid).collection("toolLoans").document(loan.id)
        try {
            docRef.update(
                mapOf(
                    "status" to "ACTIVE",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun collectDailyQist(loan: ToolLoanEntity) {
        val uid = requireUserId()
        val nextPaid = (loan.paidAmount + loan.dailyQist).coerceAtMost(loan.totalPrice)
        val newStatus = if (nextPaid >= loan.totalPrice) "PAID_OFF" else "ACTIVE"
        val isMkt = loan.workerUid.isNotBlank()
        val docRef = if (isMkt) db.collection("marketplaceToolLoans").document(loan.id)
        else userRef(uid).collection("toolLoans").document(loan.id)
        try {
            docRef.update(
                mapOf(
                    "paidAmount" to nextPaid.toLong(),
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (isMkt) {
                val pRef = profileRef(loan.workerUid)
                val snap = pRef.get().await()
                if (snap.exists()) {
                    val cur = snap.getLong("walletBalance") ?: 0L
                    pRef.update(
                        mapOf(
                            "walletBalance" to (cur - loan.dailyQist.toLong()).coerceAtLeast(0L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else {
                updateWorkspaceFields(
                    mapOf(
                        "walletBalance" to FieldValue.increment(-loan.dailyQist.toLong()),
                        "appRevenue" to FieldValue.increment(loan.dailyQist.toLong())
                    )
                )
            }
            recordAdminLedgerEntry(
                AdminLedgerEntity(
                    id = "LED-${UUID.randomUUID().toString().take(8).uppercase()}",
                    entryType = "TOOL_QIST",
                    amount = loan.dailyQist,
                    orderId = loan.id,
                    actorName = loan.workerName,
                    note = "Tool qist for ${loan.toolName}",
                    recordedByUid = uid
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun insertChatMessage(msg: ChatMessageEntity, isSharedOrder: Boolean = false) {
        val uid = requireUserId()
        val isMkt = isSharedOrder || msg.customerId.isNotBlank()
        val docRef = if (isMkt) {
            marketOrderRef(msg.bookingId).collection("messages").document(msg.id)
        } else {
            userRef(uid).collection("chatMessages").document(msg.id)
        }
        val payload = if (isMkt) msg.toMarketplaceCreateMap(uid) else msg.toCreateMap()
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun insertActivePlan(plan: ActivePlanEntity) {
        val uid = requireUserId()
        val docRef = userRef(uid).collection("activePlans").document(plan.id)
        try {
            docRef.set(plan.toCreateMap()).await()
            updateWorkspaceFields(mapOf("appRevenue" to FieldValue.increment(plan.price.toLong())))
            recordAdminLedgerEntry(
                AdminLedgerEntity(
                    id = "LED-${UUID.randomUUID().toString().take(8).uppercase()}",
                    entryType = "CARE_PLAN",
                    amount = plan.price,
                    orderId = plan.id,
                    actorName = auth.currentUser?.displayName ?: "Customer",
                    note = "Subscribed ${plan.title}",
                    recordedByUid = uid
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun clearDemoOrders() {
        val uid = requireUserId()
        val root = userRef(uid)
        try {
            listOf("bookings", "bigJobs", "b2bLeads").forEach { col ->
                val snap = root.collection(col).whereEqualTo("isDemo", true).get().await()
                snap.documents.forEach { it.reference.delete().await() }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            throw e
        }
    }

    suspend fun resetAllDemo() {
        val uid = requireUserId()
        val root = userRef(uid)
        try {
            listOf(
                "chatMessages", "bids", "bookings", "workers", "bigJobs",
                "b2bLeads", "topUpRequests", "toolLoans", "activePlans"
            ).forEach { col ->
                val snap = root.collection(col).get().await()
                snap.documents.forEach { it.reference.delete().await() }
            }
            updateWorkspaceFields(
                mapOf(
                    "lang" to "en",
                    "brand" to "KaamWala",
                    "city" to "Lahore",
                    "whatsapp" to "923001234567",
                    "payAccount" to "JazzCash / Easypaisa: 0300-1234567",
                    "leadFee" to 50L,
                    "staffPin" to "1234",
                    "liveCustomerMode" to false,
                    "customerCredit" to 0L,
                    "walletBalance" to 1000L,
                    "appRevenue" to 50L,
                    "workerOnline" to true,
                    "workerStrikes" to 0L,
                    "usedFirstPromo" to false,
                    "activeWorkerName" to "Ustad Bilal Ahmed",
                    "isSeeded" to true
                )
            )
            seedDefaultCollections(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            throw e
        }
    }

    private fun defaultWorkers(): List<WorkerEntity> = listOf(
        WorkerEntity(
            id = "w_bilal",
            name = "Ustad Bilal Ahmed",
            skill = "AC & Electrical",
            rating = "4.9",
            jobsCount = "340+",
            femaleSafe = false,
            info = "0300-9911223 | CNIC: 4412",
            avatarUrl = "https://images.unsplash.com/photo-1603415526983-d7c7e6d62c98?auto=format&fit=crop&w=300&q=80",
            isDemo = true
        ),
        WorkerEntity(
            id = "w_zainab",
            name = "Zainab Salon Pro",
            skill = "Female Beauty & Care",
            rating = "5.0",
            jobsCount = "280+",
            femaleSafe = true,
            info = "0301-2233445 | CNIC: 3190",
            avatarUrl = "https://images.unsplash.com/photo-1535295972055-1c762f4483e5?auto=format&fit=crop&w=300&q=80",
            isDemo = true
        ),
        WorkerEntity(
            id = "w_usman",
            name = "Engr. Usman Solar",
            skill = "Solar & Inverters",
            rating = "4.9",
            jobsCount = "165+",
            femaleSafe = false,
            info = "0322-7788990 | CNIC: 5521",
            avatarUrl = "https://images.unsplash.com/photo-1517676109075-9a94d44145d1?auto=format&fit=crop&w=300&q=80",
            isDemo = true
        ),
        WorkerEntity(
            id = "w_rashid",
            name = "Ustad Rashid Ali",
            skill = "Plumber & Geyser",
            rating = "4.8",
            jobsCount = "215+",
            femaleSafe = false,
            info = "0312-8877665 | CNIC: 7731",
            avatarUrl = "https://images.unsplash.com/photo-1595152772835-219674b2a8a6?auto=format&fit=crop&w=300&q=80",
            isDemo = true
        )
    )

    private fun defaultBookings(): List<BookingEntity> = listOf(
        BookingEntity(
            id = "KW-502",
            isDemo = true,
            serviceId = "solar",
            serviceName = "Solar & Inverter",
            fee = 600,
            total = 600,
            slot = "Standard (45 Min)",
            area = "Bahria Town",
            customerName = "Saad Tariq",
            customerPhone = "0321-5550192",
            address = "House 18, Sector C, Bahria Town",
            gpsCoords = "31.3676, 74.1815",
            latitude = 31.3676,
            longitude = 74.1815,
            note = "10 solar panels pressure wash & inverter voltage check",
            otp = "4821",
            status = "PENDING"
        ),
        BookingEntity(
            id = "KW-501",
            isDemo = true,
            serviceId = "ac",
            serviceName = "AC & Cooling",
            fee = 500,
            expressFee = 200,
            total = 700,
            leadFeePaid = 50,
            express = true,
            slot = "Standard (45 Min)",
            area = "DHA Phase 1-8",
            customerName = "Ayesha Malik",
            customerPhone = "0300-7712345",
            address = "Street 9, Phase 5, DHA",
            gpsCoords = "31.4697, 74.4071",
            latitude = 31.4697,
            longitude = 74.4071,
            note = "Split AC E1 error & low cooling",
            otp = "7394",
            status = "ACCEPTED",
            assignedWorkerName = "Ustad Bilal Ahmed"
        )
    )

    private fun defaultBigJobs(): List<BigJobEntity> = listOf(
        BigJobEntity(
            id = "BJ-101",
            category = "Solar Panel Setup (5kW/10kW)",
            area = "Bahria Town",
            description = "5kW Hybrid Inverter & 8 Canadian Solar panels rooftop mounting + earthing.",
            budget = 45000,
            status = "OPEN",
            isDemo = true
        )
    )

    private fun defaultChats(): List<ChatMessageEntity> = listOf(
        ChatMessageEntity(
            id = "chat_seed_1",
            bookingId = "KW-501",
            senderRole = "WORKER",
            senderName = "Ustad Bilal Ahmed",
            text = "Assalam-o-Alaikum! Main 15 min mein DHA Phase 5 pahunch raha hoon."
        )
    )

    private fun defaultB2bLeads(): List<B2bLeadEntity> = listOf(
        B2bLeadEntity(
            id = "B2B-90",
            businessName = "Al-Shifa Dental Clinic",
            phone = "0300-4455667",
            packageName = "Office Care (5 ACs + Weekly Electric/Plumb) — Rs. 8,000/mo",
            status = "NEW",
            isDemo = true
        )
    )
}
