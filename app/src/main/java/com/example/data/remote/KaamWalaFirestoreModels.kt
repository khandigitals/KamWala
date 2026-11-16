package com.example.data.remote

import com.example.data.local.*
import com.example.data.preferences.AppPrefsState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserWorkspaceDoc(
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val lang: String = "en",
    val brand: String = "KaamWala",
    val city: String = "Lahore",
    val whatsapp: String = "923001234567",
    val payAccount: String = "JazzCash / Easypaisa: 0300-1234567",
    val leadFee: Long = 50L,
    val staffPin: String = "1234",
    val liveCustomerMode: Boolean = false,
    val customerCredit: Long = 0L,
    val walletBalance: Long = 1000L,
    val appRevenue: Long = 50L,
    val workerOnline: Boolean = true,
    val workerStrikes: Long = 0L,
    val usedFirstPromo: Boolean = false,
    val activeWorkerName: String = "Ustad Bilal Ahmed",
    val isSeeded: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toPrefsState(): AppPrefsState = AppPrefsState(
        lang = lang,
        brand = brand,
        city = city,
        whatsapp = whatsapp,
        payAccount = payAccount,
        leadFee = leadFee.toInt(),
        staffPin = staffPin,
        liveCustomerMode = liveCustomerMode,
        customerCredit = customerCredit.toInt(),
        walletBalance = walletBalance.toInt(),
        appRevenue = appRevenue.toInt(),
        workerOnline = workerOnline,
        workerStrikes = workerStrikes.toInt(),
        usedFirstPromo = usedFirstPromo,
        activeWorkerName = activeWorkerName,
        isSeeded = isSeeded
    )

    fun toCreateMap(): Map<String, Any> {
        val raw: Map<String, Any?> = mapOf(
            "userId" to userId,
            "displayName" to displayName.take(100),
            "email" to email?.take(160),
            "lang" to if (lang == "ur") "ur" else "en",
            "brand" to brand.ifBlank { "KaamWala" }.take(60),
            "city" to city.ifBlank { "Lahore" }.take(60),
            "whatsapp" to whatsapp.take(30),
            "payAccount" to payAccount.take(120),
            "leadFee" to leadFee,
            "staffPin" to staffPin.ifBlank { "1234" }.take(8),
            "liveCustomerMode" to liveCustomerMode,
            "customerCredit" to customerCredit,
            "walletBalance" to walletBalance,
            "appRevenue" to appRevenue,
            "workerOnline" to workerOnline,
            "workerStrikes" to workerStrikes,
            "usedFirstPromo" to usedFirstPromo,
            "activeWorkerName" to activeWorkerName.take(100),
            "isSeeded" to isSeeded,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class BookingDoc(
    @DocumentId val id: String = "",
    val isDemo: Boolean = false,
    val serviceId: String = "",
    val serviceName: String = "",
    val fee: Long = 0L,
    val expressFee: Long = 0L,
    val promoDisc: Long = 0L,
    val creditUsed: Long = 0L,
    val partsTotal: Long = 0L,
    val addedPartsSummary: String = "",
    val total: Long = 0L,
    val leadFeePaid: Long = 0L,
    val express: Boolean = false,
    val femaleSafe: Boolean = false,
    val slot: String = "Standard (45 Min)",
    val area: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val address: String = "",
    val gpsCoords: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val workerLat: Double = 0.0,
    val workerLng: Double = 0.0,
    val note: String = "",
    val photoUri: String = "",
    val voiceNotePath: String = "",
    val proofPhotoUri: String = "",
    val otp: String = "0000",
    val status: String = "PENDING",
    val assignedWorkerName: String = "",
    val rating: Long = 0L,
    val reviewNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): BookingEntity = BookingEntity(
        id = id,
        isDemo = isDemo,
        serviceId = serviceId,
        serviceName = serviceName,
        fee = fee.toInt(),
        expressFee = expressFee.toInt(),
        promoDisc = promoDisc.toInt(),
        creditUsed = creditUsed.toInt(),
        partsTotal = partsTotal.toInt(),
        addedPartsSummary = addedPartsSummary,
        total = total.toInt(),
        leadFeePaid = leadFeePaid.toInt(),
        express = express,
        femaleSafe = femaleSafe,
        slot = slot,
        area = area,
        customerName = customerName,
        customerPhone = customerPhone,
        address = address,
        gpsCoords = gpsCoords,
        latitude = latitude,
        longitude = longitude,
        workerLat = workerLat,
        workerLng = workerLng,
        note = note,
        photoUri = photoUri,
        voiceNotePath = voiceNotePath,
        proofPhotoUri = proofPhotoUri,
        otp = otp,
        status = status,
        assignedWorkerName = assignedWorkerName,
        rating = rating.toInt(),
        reviewNote = reviewNote,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun BookingEntity.toCreateMap(): Map<String, Any> = mapOf(
    "isDemo" to isDemo,
    "serviceId" to serviceId.take(40),
    "serviceName" to serviceName.take(100),
    "fee" to fee.toLong(),
    "expressFee" to expressFee.toLong(),
    "promoDisc" to promoDisc.toLong(),
    "creditUsed" to creditUsed.toLong(),
    "partsTotal" to partsTotal.toLong(),
    "addedPartsSummary" to addedPartsSummary.take(500),
    "total" to total.toLong(),
    "leadFeePaid" to leadFeePaid.toLong(),
    "express" to express,
    "femaleSafe" to femaleSafe,
    "slot" to slot.take(60),
    "area" to area.take(100),
    "customerName" to customerName.take(100),
    "customerPhone" to customerPhone.take(40),
    "address" to address.take(300),
    "gpsCoords" to gpsCoords.take(80),
    "latitude" to latitude,
    "longitude" to longitude,
    "workerLat" to workerLat,
    "workerLng" to workerLng,
    "note" to note.take(1000),
    "photoUri" to photoUri.take(1000),
    "voiceNotePath" to voiceNotePath.take(500),
    "proofPhotoUri" to proofPhotoUri.take(1000),
    "otp" to otp.take(10),
    "status" to status,
    "assignedWorkerName" to assignedWorkerName.take(100),
    "rating" to rating.toLong(),
    "reviewNote" to reviewNote.take(500),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class WorkerDoc(
    @DocumentId val id: String = "",
    val name: String = "",
    val skill: String = "",
    val rating: String = "4.9",
    val jobsCount: String = "New",
    val femaleSafe: Boolean = false,
    val info: String = "",
    val avatarUrl: String = "",
    val isDemo: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): WorkerEntity = WorkerEntity(
        id = id,
        name = name,
        skill = skill,
        rating = rating,
        jobsCount = jobsCount,
        femaleSafe = femaleSafe,
        info = info,
        avatarUrl = avatarUrl,
        isDemo = isDemo,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun WorkerEntity.toCreateMap(): Map<String, Any> = mapOf(
    "name" to name.take(100),
    "skill" to skill.take(120),
    "rating" to rating.take(10),
    "jobsCount" to jobsCount.take(20),
    "femaleSafe" to femaleSafe,
    "info" to info.take(200),
    "avatarUrl" to avatarUrl.take(500),
    "isDemo" to isDemo,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class BigJobDoc(
    @DocumentId val id: String = "",
    val category: String = "",
    val area: String = "",
    val description: String = "",
    val budget: Long = 0L,
    val status: String = "OPEN",
    val acceptedBidId: String = "",
    val isDemo: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): BigJobEntity = BigJobEntity(
        id = id,
        category = category,
        area = area,
        description = description,
        budget = budget.toInt(),
        status = status,
        acceptedBidId = acceptedBidId,
        isDemo = isDemo,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun BigJobEntity.toCreateMap(): Map<String, Any> = mapOf(
    "category" to category.take(120),
    "area" to area.take(100),
    "description" to description.take(1000),
    "budget" to budget.toLong(),
    "status" to status,
    "acceptedBidId" to acceptedBidId.take(128),
    "isDemo" to isDemo,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class BidDoc(
    @DocumentId val id: String = "",
    val bigJobId: String = "",
    val workerName: String = "",
    val amount: Long = 0L,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): BidEntity = BidEntity(
        id = id,
        bigJobId = bigJobId,
        workerName = workerName,
        amount = amount.toInt(),
        note = note,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun BidEntity.toCreateMap(): Map<String, Any> = mapOf(
    "bigJobId" to bigJobId.take(128),
    "workerName" to workerName.take(100),
    "amount" to amount.toLong(),
    "note" to note.take(500),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class B2bLeadDoc(
    @DocumentId val id: String = "",
    val businessName: String = "",
    val phone: String = "",
    val packageName: String = "",
    val status: String = "NEW",
    val isDemo: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): B2bLeadEntity = B2bLeadEntity(
        id = id,
        businessName = businessName,
        phone = phone,
        packageName = packageName,
        status = status,
        isDemo = isDemo,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun B2bLeadEntity.toCreateMap(): Map<String, Any> = mapOf(
    "businessName" to businessName.take(120),
    "phone" to phone.take(40),
    "packageName" to packageName.take(200),
    "status" to status,
    "isDemo" to isDemo,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class TopUpRequestDoc(
    @DocumentId val id: String = "",
    val workerName: String = "",
    val method: String = "",
    val amount: Long = 0L,
    val tid: String = "",
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): TopUpRequestEntity = TopUpRequestEntity(
        id = id,
        workerName = workerName,
        method = method,
        amount = amount.toInt(),
        tid = tid,
        status = status,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun TopUpRequestEntity.toCreateMap(): Map<String, Any> = mapOf(
    "workerName" to workerName.take(100),
    "method" to method.take(40),
    "amount" to amount.toLong(),
    "tid" to tid.take(80),
    "status" to status,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class ToolLoanDoc(
    @DocumentId val id: String = "",
    val workerName: String = "",
    val toolName: String = "",
    val totalPrice: Long = 0L,
    val dailyQist: Long = 0L,
    val paidAmount: Long = 0L,
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): ToolLoanEntity = ToolLoanEntity(
        id = id,
        workerName = workerName,
        toolName = toolName,
        totalPrice = totalPrice.toInt(),
        dailyQist = dailyQist.toInt(),
        paidAmount = paidAmount.toInt(),
        status = status,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun ToolLoanEntity.toCreateMap(): Map<String, Any> = mapOf(
    "workerName" to workerName.take(100),
    "toolName" to toolName.take(120),
    "totalPrice" to totalPrice.toLong(),
    "dailyQist" to dailyQist.toLong(),
    "paidAmount" to paidAmount.toLong(),
    "status" to status,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class ChatMessageDoc(
    @DocumentId val id: String = "",
    val bookingId: String = "",
    val senderRole: String = "CUSTOMER",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): ChatMessageEntity = ChatMessageEntity(
        id = id,
        bookingId = bookingId,
        senderRole = senderRole,
        senderName = senderName,
        text = text,
        timestamp = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun ChatMessageEntity.toCreateMap(): Map<String, Any> = mapOf(
    "bookingId" to bookingId.take(128),
    "senderRole" to senderRole,
    "senderName" to senderName.take(100),
    "text" to text.take(1000),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class ActivePlanDoc(
    @DocumentId val id: String = "",
    val planId: String = "",
    val title: String = "",
    val desc: String = "",
    val price: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): ActivePlanEntity = ActivePlanEntity(
        id = id,
        planId = planId,
        title = title,
        desc = desc,
        price = price.toInt(),
        subscribedAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun ActivePlanEntity.toCreateMap(): Map<String, Any> = mapOf(
    "planId" to planId.take(40),
    "title" to title.take(100),
    "desc" to desc.take(300),
    "price" to price.toLong(),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

// =========================================================================
// SHARED MARKETPLACE DTOs (Cross-Account Customer <-> Worker <-> Admin)
// =========================================================================

@IgnoreExtraProperties
data class MarketplaceProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "CUSTOMER",
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val city: String = "Lahore",
    val area: String = "",
    val address: String = "",
    val skill: String = "",
    val experienceYears: Long = 0L,
    val cnic: String = "",
    val payoutAccount: String = "",
    val femaleSafe: Boolean = false,
    val verificationStatus: String = "APPROVED",
    val isOnline: Boolean = true,
    val walletBalance: Long = 0L,
    val totalEarnings: Long = 0L,
    val strikes: Long = 0L,
    val rating: Double = 5.0,
    val jobsCompleted: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): UserProfileEntity = UserProfileEntity(
        userId = userId.ifBlank { id },
        role = role,
        fullName = fullName,
        phone = phone,
        email = email,
        city = city,
        area = area,
        address = address,
        skill = skill,
        experienceYears = experienceYears.toInt(),
        cnic = cnic,
        payoutAccount = payoutAccount,
        femaleSafe = femaleSafe,
        verificationStatus = verificationStatus,
        isOnline = isOnline,
        walletBalance = walletBalance.toInt(),
        totalEarnings = totalEarnings.toInt(),
        strikes = strikes.toInt(),
        rating = rating,
        jobsCompleted = jobsCompleted.toInt(),
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun UserProfileEntity.toCreateMap(): Map<String, Any> = mapOf(
    "userId" to userId.take(128),
    "role" to role,
    "fullName" to fullName.trim().ifBlank { "KaamWala User" }.take(100),
    "phone" to phone.trim().ifBlank { "03000000000" }.take(30),
    "email" to email.trim().take(160),
    "city" to city.trim().ifBlank { "Lahore" }.take(60),
    "area" to area.trim().take(100),
    "address" to address.trim().take(300),
    "skill" to skill.trim().take(120),
    "experienceYears" to experienceYears.coerceIn(0, 60).toLong(),
    "cnic" to cnic.trim().take(25),
    "payoutAccount" to payoutAccount.trim().take(120),
    "femaleSafe" to femaleSafe,
    "verificationStatus" to verificationStatus,
    "isOnline" to isOnline,
    "walletBalance" to walletBalance.toLong(),
    "totalEarnings" to totalEarnings.toLong(),
    "strikes" to strikes.toLong(),
    "rating" to rating.coerceIn(0.0, 5.0),
    "jobsCompleted" to jobsCompleted.toLong(),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class MarketplaceOrderDoc(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val workerId: String = "",
    val city: String = "Lahore",
    val serviceId: String = "",
    val serviceName: String = "",
    val fee: Long = 0L,
    val expressFee: Long = 0L,
    val promoDisc: Long = 0L,
    val creditUsed: Long = 0L,
    val partsTotal: Long = 0L,
    val addedPartsSummary: String = "",
    val total: Long = 0L,
    val leadFeePaid: Long = 0L,
    val commissionAmount: Long = 0L,
    val express: Boolean = false,
    val femaleSafe: Boolean = false,
    val slot: String = "Standard (45 Min)",
    val area: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val address: String = "",
    val gpsCoords: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val workerLat: Double = 0.0,
    val workerLng: Double = 0.0,
    val note: String = "",
    val photoUri: String = "",
    val voiceNotePath: String = "",
    val proofPhotoUri: String = "",
    val otp: String = "0000",
    val status: String = "PENDING",
    val assignedWorkerName: String = "",
    val assignedWorkerPhone: String = "",
    val paymentMethod: String = "CASH",
    val paymentStatus: String = "UNPAID",
    val paymentTid: String = "",
    val rating: Long = 0L,
    val reviewNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): BookingEntity = BookingEntity(
        id = id,
        isDemo = false,
        customerId = customerId,
        workerId = workerId,
        city = city,
        serviceId = serviceId,
        serviceName = serviceName,
        fee = fee.toInt(),
        expressFee = expressFee.toInt(),
        promoDisc = promoDisc.toInt(),
        creditUsed = creditUsed.toInt(),
        partsTotal = partsTotal.toInt(),
        addedPartsSummary = addedPartsSummary,
        total = total.toInt(),
        leadFeePaid = leadFeePaid.toInt(),
        commissionAmount = commissionAmount.toInt(),
        express = express,
        femaleSafe = femaleSafe,
        slot = slot,
        area = area,
        customerName = customerName,
        customerPhone = customerPhone,
        address = address,
        gpsCoords = gpsCoords,
        latitude = latitude,
        longitude = longitude,
        workerLat = workerLat,
        workerLng = workerLng,
        note = note,
        photoUri = photoUri,
        voiceNotePath = voiceNotePath,
        proofPhotoUri = proofPhotoUri,
        otp = otp,
        status = status,
        assignedWorkerName = assignedWorkerName,
        assignedWorkerPhone = assignedWorkerPhone,
        paymentMethod = paymentMethod,
        paymentStatus = paymentStatus,
        paymentTid = paymentTid,
        rating = rating.toInt(),
        reviewNote = reviewNote,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun BookingEntity.toMarketplaceCreateMap(customerUid: String): Map<String, Any> = mapOf(
    "customerId" to customerUid.take(128),
    "workerId" to workerId.take(128),
    "city" to city.ifBlank { "Lahore" }.take(60),
    "serviceId" to serviceId.take(40),
    "serviceName" to serviceName.take(100),
    "fee" to fee.toLong(),
    "expressFee" to expressFee.toLong(),
    "promoDisc" to promoDisc.toLong(),
    "creditUsed" to creditUsed.toLong(),
    "partsTotal" to partsTotal.toLong(),
    "addedPartsSummary" to addedPartsSummary.take(500),
    "total" to total.toLong(),
    "leadFeePaid" to leadFeePaid.toLong(),
    "commissionAmount" to commissionAmount.toLong(),
    "express" to express,
    "femaleSafe" to femaleSafe,
    "slot" to slot.take(60),
    "area" to area.take(100),
    "customerName" to customerName.take(100),
    "customerPhone" to customerPhone.take(40),
    "address" to address.take(300),
    "gpsCoords" to gpsCoords.take(80),
    "latitude" to latitude,
    "longitude" to longitude,
    "workerLat" to workerLat,
    "workerLng" to workerLng,
    "note" to note.take(1000),
    "photoUri" to photoUri.take(1000),
    "voiceNotePath" to voiceNotePath.take(500),
    "proofPhotoUri" to proofPhotoUri.take(1000),
    "otp" to otp.take(10),
    "status" to status,
    "assignedWorkerName" to assignedWorkerName.take(100),
    "assignedWorkerPhone" to assignedWorkerPhone.take(40),
    "paymentMethod" to paymentMethod,
    "paymentStatus" to paymentStatus,
    "paymentTid" to paymentTid.take(80),
    "rating" to rating.toLong(),
    "reviewNote" to reviewNote.take(500),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class MarketplaceChatMessageDoc(
    @DocumentId val id: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val workerId: String = "",
    val senderUid: String = "",
    val senderRole: String = "CUSTOMER",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): ChatMessageEntity = ChatMessageEntity(
        id = id,
        bookingId = bookingId,
        customerId = customerId,
        workerId = workerId,
        senderUid = senderUid,
        senderRole = senderRole,
        senderName = senderName,
        text = text,
        timestamp = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun ChatMessageEntity.toMarketplaceCreateMap(senderUid: String): Map<String, Any> = mapOf(
    "bookingId" to bookingId.take(128),
    "customerId" to customerId.take(128),
    "workerId" to workerId.take(128),
    "senderUid" to senderUid.take(128),
    "senderRole" to senderRole,
    "senderName" to senderName.take(100),
    "text" to text.take(1000),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class MarketplaceTopUpDoc(
    @DocumentId val id: String = "",
    val workerUid: String = "",
    val workerName: String = "",
    val method: String = "",
    val amount: Long = 0L,
    val tid: String = "",
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): TopUpRequestEntity = TopUpRequestEntity(
        id = id,
        workerUid = workerUid,
        workerName = workerName,
        method = method,
        amount = amount.toInt(),
        tid = tid,
        status = status,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun TopUpRequestEntity.toMarketplaceCreateMap(uid: String): Map<String, Any> = mapOf(
    "workerUid" to uid.take(128),
    "workerName" to workerName.take(100),
    "method" to method.take(40),
    "amount" to amount.toLong(),
    "tid" to tid.take(80),
    "status" to status,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class MarketplaceToolLoanDoc(
    @DocumentId val id: String = "",
    val workerUid: String = "",
    val workerName: String = "",
    val toolName: String = "",
    val totalPrice: Long = 0L,
    val dailyQist: Long = 0L,
    val paidAmount: Long = 0L,
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): ToolLoanEntity = ToolLoanEntity(
        id = id,
        workerUid = workerUid,
        workerName = workerName,
        toolName = toolName,
        totalPrice = totalPrice.toInt(),
        dailyQist = dailyQist.toInt(),
        paidAmount = paidAmount.toInt(),
        status = status,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun ToolLoanEntity.toMarketplaceCreateMap(uid: String): Map<String, Any> = mapOf(
    "workerUid" to uid.take(128),
    "workerName" to workerName.take(100),
    "toolName" to toolName.take(120),
    "totalPrice" to totalPrice.toLong(),
    "dailyQist" to dailyQist.toLong(),
    "paidAmount" to paidAmount.toLong(),
    "status" to status,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class MarketplaceBigJobDoc(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val category: String = "",
    val area: String = "",
    val description: String = "",
    val budget: Long = 0L,
    val status: String = "OPEN",
    val acceptedBidId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): BigJobEntity = BigJobEntity(
        id = id,
        customerId = customerId,
        category = category,
        area = area,
        description = description,
        budget = budget.toInt(),
        status = status,
        acceptedBidId = acceptedBidId,
        isDemo = false,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun BigJobEntity.toMarketplaceCreateMap(uid: String): Map<String, Any> = mapOf(
    "customerId" to uid.take(128),
    "category" to category.take(120),
    "area" to area.take(100),
    "description" to description.take(1000),
    "budget" to budget.toLong(),
    "status" to status,
    "acceptedBidId" to acceptedBidId.take(128),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class MarketplaceBidDoc(
    @DocumentId val id: String = "",
    val bigJobId: String = "",
    val customerId: String = "",
    val workerUid: String = "",
    val workerName: String = "",
    val amount: Long = 0L,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): BidEntity = BidEntity(
        id = id,
        bigJobId = bigJobId,
        customerId = customerId,
        workerUid = workerUid,
        workerName = workerName,
        amount = amount.toInt(),
        note = note,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun BidEntity.toMarketplaceCreateMap(uid: String): Map<String, Any> = mapOf(
    "bigJobId" to bigJobId.take(128),
    "customerId" to customerId.take(128),
    "workerUid" to uid.take(128),
    "workerName" to workerName.take(100),
    "amount" to amount.toLong(),
    "note" to note.take(500),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class MarketplaceB2bDoc(
    @DocumentId val id: String = "",
    val submittedByUid: String = "",
    val businessName: String = "",
    val phone: String = "",
    val packageName: String = "",
    val status: String = "NEW",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): B2bLeadEntity = B2bLeadEntity(
        id = id,
        submittedByUid = submittedByUid,
        businessName = businessName,
        phone = phone,
        packageName = packageName,
        status = status,
        isDemo = false,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun B2bLeadEntity.toMarketplaceCreateMap(uid: String): Map<String, Any> = mapOf(
    "submittedByUid" to uid.take(128),
    "businessName" to businessName.take(120),
    "phone" to phone.take(40),
    "packageName" to packageName.take(200),
    "status" to status,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class MarketplaceConfigDoc(
    val brand: String = "KaamWala",
    val city: String = "Lahore",
    val whatsapp: String = "923001234567",
    val payAccount: String = "JazzCash / Easypaisa: 0300-1234567",
    val leadFee: Long = 50L,
    val commissionPercent: Long = 10L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminLedgerDoc(
    @DocumentId val id: String = "",
    val entryType: String = "LEAD_FEE",
    val amount: Long = 0L,
    val orderId: String = "",
    val actorName: String = "",
    val note: String = "",
    val payoutMethod: String = "",
    val payoutAccount: String = "",
    val recordedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity(): AdminLedgerEntity = AdminLedgerEntity(
        id = id,
        entryType = entryType,
        amount = amount.toInt(),
        orderId = orderId,
        actorName = actorName,
        note = note,
        payoutMethod = payoutMethod,
        payoutAccount = payoutAccount,
        recordedByUid = recordedByUid,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun AdminLedgerEntity.toCreateMap(uid: String): Map<String, Any> = mapOf(
    "entryType" to entryType,
    "amount" to amount.toLong(),
    "orderId" to orderId.take(128),
    "actorName" to actorName.take(100),
    "note" to note.take(300),
    "payoutMethod" to payoutMethod.take(40),
    "payoutAccount" to payoutAccount.take(120),
    "recordedByUid" to uid.take(128),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)
