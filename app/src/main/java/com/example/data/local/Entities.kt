package com.example.data.local

data class BookingEntity(
    val id: String,
    val isDemo: Boolean = false,
    val customerId: String = "",
    val workerId: String = "",
    val city: String = "Lahore",
    val serviceId: String,
    val serviceName: String,
    val fee: Int,
    val expressFee: Int = 0,
    val promoDisc: Int = 0,
    val creditUsed: Int = 0,
    val partsTotal: Int = 0,
    val addedPartsSummary: String = "",
    val total: Int,
    val leadFeePaid: Int = 0,
    val commissionAmount: Int = 0,
    val express: Boolean = false,
    val femaleSafe: Boolean = false,
    val slot: String = "Standard (45 Min)",
    val area: String,
    val customerName: String,
    val customerPhone: String,
    val address: String,
    val gpsCoords: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val workerLat: Double = 0.0,
    val workerLng: Double = 0.0,
    val note: String = "",
    val photoUri: String = "",
    val voiceNotePath: String = "",
    val proofPhotoUri: String = "",
    val otp: String,
    val status: String = "PENDING", // PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
    val assignedWorkerName: String = "",
    val assignedWorkerPhone: String = "",
    val paymentMethod: String = "CASH", // CASH, JAZZCASH, EASYPAISA, RAAST
    val paymentStatus: String = "UNPAID", // UNPAID, PAID_PENDING_VERIFY, SETTLED
    val paymentTid: String = "",
    val rating: Int = 0,
    val reviewNote: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class UserProfileEntity(
    val userId: String,
    val role: String = "CUSTOMER", // CUSTOMER, WORKER, ADMIN
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val city: String = "Lahore",
    val area: String = "",
    val address: String = "",
    val skill: String = "",
    val experienceYears: Int = 0,
    val cnic: String = "",
    val payoutAccount: String = "",
    val femaleSafe: Boolean = false,
    val verificationStatus: String = "APPROVED", // PENDING, APPROVED, REJECTED
    val isOnline: Boolean = true,
    val walletBalance: Int = 0,
    val totalEarnings: Int = 0,
    val strikes: Int = 0,
    val rating: Double = 5.0,
    val jobsCompleted: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class AdminLedgerEntity(
    val id: String,
    val entryType: String, // LEAD_FEE, COMMISSION, CARE_PLAN, TOOL_QIST, WITHDRAWAL
    val amount: Int,
    val orderId: String = "",
    val actorName: String = "",
    val note: String = "",
    val payoutMethod: String = "",
    val payoutAccount: String = "",
    val recordedByUid: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class WorkerEntity(
    val id: String,
    val name: String,
    val skill: String,
    val rating: String = "4.9",
    val jobsCount: String = "New",
    val femaleSafe: Boolean = false,
    val info: String,
    val avatarUrl: String = "",
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class BigJobEntity(
    val id: String,
    val customerId: String = "",
    val category: String,
    val area: String,
    val description: String,
    val budget: Int,
    val status: String = "OPEN", // OPEN, AWARDED
    val acceptedBidId: String = "",
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class BidEntity(
    val id: String,
    val bigJobId: String,
    val customerId: String = "",
    val workerUid: String = "",
    val workerName: String,
    val amount: Int,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class B2bLeadEntity(
    val id: String,
    val submittedByUid: String = "",
    val businessName: String,
    val phone: String,
    val packageName: String,
    val status: String = "NEW", // NEW, ACTIVE
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class TopUpRequestEntity(
    val id: String,
    val workerUid: String = "",
    val workerName: String,
    val method: String,
    val amount: Int,
    val tid: String,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val createdAt: Long = System.currentTimeMillis()
)

data class ToolLoanEntity(
    val id: String,
    val workerUid: String = "",
    val workerName: String,
    val toolName: String,
    val totalPrice: Int,
    val dailyQist: Int,
    val paidAmount: Int = 0,
    val status: String = "PENDING", // PENDING, ACTIVE, PAID_OFF
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatMessageEntity(
    val id: String,
    val bookingId: String,
    val customerId: String = "",
    val workerId: String = "",
    val senderUid: String = "",
    val senderRole: String, // CUSTOMER, WORKER, ADMIN
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ActivePlanEntity(
    val id: String,
    val planId: String,
    val title: String,
    val desc: String,
    val price: Int,
    val subscribedAt: Long = System.currentTimeMillis()
)
