package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.preferences.AppPrefsState
import com.example.data.remote.KaamWalaFirestoreRepository
import com.example.data.remote.MarketplaceConfigDoc
import com.example.model.AiFaultRule
import com.example.model.CarePlan
import com.example.model.CatalogData
import com.example.model.HardwarePart
import com.example.model.ServiceCategory
import com.example.model.ServiceItem
import com.example.model.ToolCatalogItem
import com.example.ui.components.KaamWalaGeo
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AppTab { HOME, ORDERS, WORKER, ADMIN }
enum class WorkerSubTab { JOBS, BIDS, HISAB }
enum class ProtectedTarget { WORKER, ADMIN, SETTINGS }

data class KaamWalaUiState(
    val currentUserId: String = "",
    val currentUserName: String = "",
    val currentUserEmail: String = "",
    val isAdminUser: Boolean = false,
    val prefs: AppPrefsState = AppPrefsState(),
    val userProfile: UserProfileEntity? = null,
    val profileLoaded: Boolean = false,
    val showRegistrationDialog: Boolean = false,
    val currentTab: AppTab = AppTab.HOME,
    val workerSubTab: WorkerSubTab = WorkerSubTab.JOBS,
    val selectedCategory: ServiceCategory = ServiceCategory.ALL,
    val femaleMode: Boolean = false,
    val searchQuery: String = "",
    val matchedAiRule: AiFaultRule? = null,
    val isStaffUnlocked: Boolean = false,
    val pendingProtectedTarget: ProtectedTarget? = null,
    val bookings: List<BookingEntity> = emptyList(),
    val workers: List<WorkerEntity> = emptyList(),
    val allMarketplaceProfiles: List<UserProfileEntity> = emptyList(),
    val adminLedger: List<AdminLedgerEntity> = emptyList(),
    val bigJobs: List<BigJobEntity> = emptyList(),
    val bids: List<BidEntity> = emptyList(),
    val b2bLeads: List<B2bLeadEntity> = emptyList(),
    val topUpRequests: List<TopUpRequestEntity> = emptyList(),
    val toolLoans: List<ToolLoanEntity> = emptyList(),
    val chatMessages: List<ChatMessageEntity> = emptyList(),
    val activePlans: List<ActivePlanEntity> = emptyList(),
    val bookingSheetService: ServiceItem? = null,
    val confirmedBooking: BookingEntity? = null,
    val showBigJobDialog: Boolean = false,
    val showB2bDialog: Boolean = false,
    val showTopUpDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showCityPickerDialog: Boolean = false,
    val showLiveVoiceDialog: Boolean = false,
    val showAdminWithdrawDialog: Boolean = false,
    val activePaymentTidBooking: BookingEntity? = null,
    val activeChatBookingId: String? = null,
    val activeChatRole: String = "CUSTOMER",
    val activePartsBookingId: String? = null,
    val partsAllowAdd: Boolean = false,
    val activeWarrantyBooking: BookingEntity? = null,
    val activeOtpVerifyBooking: BookingEntity? = null,
    val activeRateBooking: BookingEntity? = null,
    val toastMessage: String? = null
) {
    val effectiveWalletBalance: Int
        get() = if (userProfile?.role == "WORKER" && !isAdminUser) userProfile.walletBalance else prefs.walletBalance

    val effectiveWorkerName: String
        get() = userProfile?.takeIf { it.role == "WORKER" && it.fullName.isNotBlank() }?.fullName
            ?: prefs.activeWorkerName

    val isWorkerVerifiedOrSandbox: Boolean
        get() = isAdminUser ||
            userProfile == null ||
            (userProfile.role == "WORKER" && userProfile.verificationStatus == "APPROVED") ||
            isStaffUnlocked

    val customerVisibleBookings: List<BookingEntity>
        get() = bookings.filter { b ->
            val ownerOk = b.customerId.isBlank() || b.customerId == currentUserId || isAdminUser
            val liveOk = !prefs.liveCustomerMode || !b.isDemo
            ownerOk && liveOk
        }

    val workerVisibleBookings: List<BookingEntity>
        get() = bookings.filter { b ->
            b.status == "PENDING" ||
                b.workerId == currentUserId ||
                b.customerId.isBlank() ||
                isAdminUser
        }

    val pendingWorkerRegistrations: List<UserProfileEntity>
        get() = allMarketplaceProfiles.filter { it.role == "WORKER" && it.verificationStatus == "PENDING" }

    val pendingDigitalPayments: List<BookingEntity>
        get() = bookings.filter { it.paymentMethod != "CASH" && it.paymentStatus == "PAID_PENDING_VERIFY" }

    val grossAdminEarnings: Int
        get() = if (adminLedger.isNotEmpty()) {
            adminLedger.filter { it.entryType != "WITHDRAWAL" }.sumOf { it.amount }
        } else {
            prefs.appRevenue
        }

    val totalAdminWithdrawn: Int
        get() = adminLedger.filter { it.entryType == "WITHDRAWAL" }.sumOf { it.amount }

    val netAdminBalance: Int
        get() = (grossAdminEarnings - totalAdminWithdrawn).coerceAtLeast(0)

    val filteredServices: List<ServiceItem>
        get() {
            val q = searchQuery.trim().lowercase()
            return CatalogData.SERVICES.filter { svc ->
                val catOk = selectedCategory == ServiceCategory.ALL || svc.category == selectedCategory
                val femaleOk = !femaleMode || svc.femaleSafe
                val searchOk = q.isEmpty() ||
                    svc.nameEn.lowercase().contains(q) ||
                    svc.nameUr.lowercase().contains(q) ||
                    svc.desc.lowercase().contains(q) ||
                    (matchedAiRule != null && svc.id == matchedAiRule.serviceId)
                catOk && femaleOk && searchOk
            }
        }

    val filteredWorkers: List<WorkerEntity>
        get() = if (femaleMode) workers.filter { it.femaleSafe } else workers

    val visibleBigJobs: List<BigJobEntity>
        get() = if (prefs.liveCustomerMode) bigJobs.filter { !it.isDemo } else bigJobs

    val totalGmv: Int
        get() = bookings.filter { it.status != "CANCELLED" }.sumOf { it.total } + activePlans.sumOf { it.price }

    val totalOrdersAndBidsCount: Int
        get() = bookings.size + bigJobs.size
}

private data class WorkspaceDataSlice(
    val prefs: AppPrefsState = AppPrefsState(),
    val bookings: List<BookingEntity> = emptyList(),
    val workers: List<WorkerEntity> = emptyList(),
    val bigJobs: List<BigJobEntity> = emptyList(),
    val bids: List<BidEntity> = emptyList(),
    val b2bLeads: List<B2bLeadEntity> = emptyList(),
    val topUps: List<TopUpRequestEntity> = emptyList(),
    val toolLoans: List<ToolLoanEntity> = emptyList(),
    val chats: List<ChatMessageEntity> = emptyList(),
    val plans: List<ActivePlanEntity> = emptyList()
)

private data class MarketplaceDataSlice(
    val profileLoaded: Boolean = false,
    val userProfile: UserProfileEntity? = null,
    val config: MarketplaceConfigDoc? = null,
    val verifiedWorkers: List<UserProfileEntity> = emptyList(),
    val allProfiles: List<UserProfileEntity> = emptyList(),
    val marketplaceBookings: List<BookingEntity> = emptyList(),
    val marketplaceTopUps: List<TopUpRequestEntity> = emptyList(),
    val marketplaceLoans: List<ToolLoanEntity> = emptyList(),
    val adminLedger: List<AdminLedgerEntity> = emptyList(),
    val sharedChatMessages: List<ChatMessageEntity> = emptyList()
)

private data class LocalUiControl(
    val currentTab: AppTab = AppTab.HOME,
    val workerSubTab: WorkerSubTab = WorkerSubTab.JOBS,
    val selectedCategory: ServiceCategory = ServiceCategory.ALL,
    val femaleMode: Boolean = false,
    val searchQuery: String = "",
    val isStaffUnlocked: Boolean = false,
    val pendingProtectedTarget: ProtectedTarget? = null,
    val showRegistrationDialog: Boolean = false,
    val bookingSheetService: ServiceItem? = null,
    val confirmedBooking: BookingEntity? = null,
    val showBigJobDialog: Boolean = false,
    val showB2bDialog: Boolean = false,
    val showTopUpDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showCityPickerDialog: Boolean = false,
    val showLiveVoiceDialog: Boolean = false,
    val showAdminWithdrawDialog: Boolean = false,
    val activePaymentTidBooking: BookingEntity? = null,
    val activeChatBookingId: String? = null,
    val activeChatRole: String = "CUSTOMER",
    val activePartsBookingId: String? = null,
    val partsAllowAdd: Boolean = false,
    val activeWarrantyBooking: BookingEntity? = null,
    val activeOtpVerifyBooking: BookingEntity? = null,
    val activeRateBooking: BookingEntity? = null,
    val toastMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class KaamWalaViewModel(
    private val repository: KaamWalaFirestoreRepository,
    private val currentUserId: String,
    currentUserName: String? = null,
    currentUserEmail: String? = null
) : ViewModel() {

    private val resolvedUserName = currentUserName?.trim().orEmpty()
    private val resolvedUserEmail = currentUserEmail?.trim().orEmpty()
    private val isAdminUser = resolvedUserEmail.equals("mnc.sahjavi@gmail.com", ignoreCase = true)
    private val uiControl = MutableStateFlow(LocalUiControl(isStaffUnlocked = isAdminUser))
    private var logoTapCount = 0
    private var lastLogoTapMs = 0L

    init {
        launchSafely {
            repository.ensureWorkspaceInitializedAndSeeded(
                displayName = resolvedUserName.ifBlank { "KaamWala User" },
                email = resolvedUserEmail.ifBlank { null }
            )
        }
    }

    private fun <T> Flow<T>.safeCatch(fallback: T): Flow<T> = catch { error ->
        val root = unwrapFirestoreError(error)
        if (root is CancellationException && error is CancellationException) throw error
        emit(fallback)
    }

    private val workspaceFlow = combine(
        repository.observePrefs(currentUserId).safeCatch(AppPrefsState()),
        repository.observeBookings(currentUserId).safeCatch(emptyList()),
        repository.observeWorkers(currentUserId).safeCatch(emptyList()),
        repository.observeBigJobs(currentUserId).safeCatch(emptyList()),
        repository.observeBids(currentUserId).safeCatch(emptyList())
    ) { prefs, bookings, workers, bigJobs, bids ->
        WorkspaceDataSlice(
            prefs = prefs,
            bookings = bookings,
            workers = workers,
            bigJobs = bigJobs,
            bids = bids
        )
    }.combine(
        combine(
            repository.observeB2bLeads(currentUserId).safeCatch(emptyList()),
            repository.observeTopUpRequests(currentUserId).safeCatch(emptyList()),
            repository.observeToolLoans(currentUserId).safeCatch(emptyList()),
            repository.observeChatMessages(currentUserId).safeCatch(emptyList()),
            repository.observeActivePlans(currentUserId).safeCatch(emptyList())
        ) { b2b, topUps, loans, chats, plans ->
            listOf(b2b, topUps, loans, chats, plans)
        }
    ) { snap, extra ->
        @Suppress("UNCHECKED_CAST")
        snap.copy(
            b2bLeads = extra[0] as List<B2bLeadEntity>,
            topUps = extra[1] as List<TopUpRequestEntity>,
            toolLoans = extra[2] as List<ToolLoanEntity>,
            chats = extra[3] as List<ChatMessageEntity>,
            plans = extra[4] as List<ActivePlanEntity>
        )
    }

    private val marketplaceOrdersFlow: Flow<List<BookingEntity>> = combine(
        repository.observeCustomerMarketplaceOrders(currentUserId).safeCatch(emptyList()),
        repository.observePendingDispatchOrders(currentUserId).safeCatch(emptyList()),
        repository.observeWorkerAssignedOrders(currentUserId).safeCatch(emptyList()),
        if (isAdminUser) repository.observeAllMarketplaceOrders(currentUserId).safeCatch(emptyList())
        else flowOf(emptyList())
    ) { cust, pending, worker, adminAll ->
        (adminAll + cust + worker + pending)
            .associateBy { it.id }
            .values
            .sortedByDescending { it.createdAt }
    }

    private val activeOrderChatMessagesFlow: Flow<List<ChatMessageEntity>> =
        uiControl
            .map { Triple(it.activeChatBookingId, it.activeChatRole, it.bookingSheetService) }
            .distinctUntilChanged()
            .flatMapLatest { (chatBookingId, role, _) ->
                if (chatBookingId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    repository.observeMarketplaceOrderMessages(
                        orderId = chatBookingId,
                        uid = currentUserId,
                        isAdmin = isAdminUser,
                        isCustomer = role != "WORKER"
                    ).safeCatch(emptyList())
                }
            }

    private val marketplaceCoreFlow = combine(
        repository.observeUserProfile(currentUserId).safeCatch(true to null),
        repository.observeMarketplaceConfig(currentUserId).safeCatch(null),
        repository.observeVerifiedWorkers(currentUserId).safeCatch(emptyList()),
        if (isAdminUser) repository.observeAllMarketplaceProfiles(currentUserId).safeCatch(emptyList())
        else flowOf(emptyList()),
        marketplaceOrdersFlow
    ) { profilePair, config, verifiedWorkers, allProfiles, orders ->
        MarketplaceDataSlice(
            profileLoaded = profilePair.first,
            userProfile = profilePair.second,
            config = config,
            verifiedWorkers = verifiedWorkers,
            allProfiles = allProfiles,
            marketplaceBookings = orders
        )
    }.combine(
        combine(
            repository.observeMarketplaceTopUps(currentUserId, isAdminUser).safeCatch(emptyList()),
            repository.observeMarketplaceToolLoans(currentUserId, isAdminUser).safeCatch(emptyList()),
            if (isAdminUser) repository.observeAdminLedger(currentUserId).safeCatch(emptyList())
            else flowOf(emptyList()),
            activeOrderChatMessagesFlow
        ) { topUps, loans, ledger, sharedChats ->
            listOf(topUps, loans, ledger, sharedChats)
        }
    ) { core, extra ->
        @Suppress("UNCHECKED_CAST")
        core.copy(
            marketplaceTopUps = extra[0] as List<TopUpRequestEntity>,
            marketplaceLoans = extra[1] as List<ToolLoanEntity>,
            adminLedger = extra[2] as List<AdminLedgerEntity>,
            sharedChatMessages = extra[3] as List<ChatMessageEntity>
        )
    }

    val uiState: StateFlow<KaamWalaUiState> = combine(
        workspaceFlow,
        marketplaceCoreFlow,
        uiControl
    ) { ws, mkt, ctrl ->
        val q = ctrl.searchQuery.trim().lowercase()
        val aiRule = if (q.length >= 2) {
            CatalogData.AI_RULES.firstOrNull { rule -> rule.keys.any { q.contains(it) } }
        } else null

        val mergedPrefs = mkt.config?.let { cfg ->
            ws.prefs.copy(
                whatsapp = cfg.whatsapp.ifBlank { ws.prefs.whatsapp },
                payAccount = cfg.payAccount.ifBlank { ws.prefs.payAccount },
                leadFee = cfg.leadFee.toInt().takeIf { it > 0 } ?: ws.prefs.leadFee,
                commissionPercent = cfg.commissionPercent.toInt().coerceIn(0, 50)
            )
        } ?: ws.prefs

        val verifiedProfileWorkers = mkt.verifiedWorkers.map { p ->
            WorkerEntity(
                id = "mkt_${p.userId}",
                name = p.fullName,
                skill = p.skill.ifBlank { "Verified Pro" },
                rating = String.format("%.1f", p.rating.coerceIn(1.0, 5.0)),
                jobsCount = "${p.jobsCompleted.coerceAtLeast(1)}+",
                femaleSafe = p.femaleSafe,
                info = "${p.phone} | ${p.city} (${p.area.ifBlank { "Verified" }})",
                avatarUrl = "",
                isDemo = false,
                createdAt = p.createdAt
            )
        }
        val mergedWorkers = (verifiedProfileWorkers + ws.workers)
            .distinctBy { it.name.lowercase() }

        val mergedBookings = (mkt.marketplaceBookings + ws.bookings)
            .associateBy { it.id }
            .values
            .sortedByDescending { it.createdAt }

        val mergedTopUps = (mkt.marketplaceTopUps + ws.topUps)
            .associateBy { it.id }
            .values
            .sortedByDescending { it.createdAt }

        val mergedLoans = (mkt.marketplaceLoans + ws.toolLoans)
            .associateBy { it.id }
            .values
            .sortedByDescending { it.createdAt }

        val mergedChats = (mkt.sharedChatMessages + ws.chats)
            .associateBy { it.id }
            .values
            .sortedBy { it.timestamp }

        KaamWalaUiState(
            currentUserId = currentUserId,
            currentUserName = resolvedUserName,
            currentUserEmail = resolvedUserEmail,
            isAdminUser = isAdminUser,
            prefs = mergedPrefs,
            userProfile = mkt.userProfile,
            profileLoaded = mkt.profileLoaded,
            showRegistrationDialog = ctrl.showRegistrationDialog,
            currentTab = ctrl.currentTab,
            workerSubTab = ctrl.workerSubTab,
            selectedCategory = ctrl.selectedCategory,
            femaleMode = ctrl.femaleMode,
            searchQuery = ctrl.searchQuery,
            matchedAiRule = aiRule,
            isStaffUnlocked = ctrl.isStaffUnlocked || isAdminUser,
            pendingProtectedTarget = ctrl.pendingProtectedTarget,
            bookings = mergedBookings,
            workers = mergedWorkers,
            allMarketplaceProfiles = mkt.allProfiles,
            adminLedger = mkt.adminLedger,
            bigJobs = ws.bigJobs,
            bids = ws.bids,
            b2bLeads = ws.b2bLeads,
            topUpRequests = mergedTopUps,
            toolLoans = mergedLoans,
            chatMessages = mergedChats,
            activePlans = ws.plans,
            bookingSheetService = ctrl.bookingSheetService,
            confirmedBooking = ctrl.confirmedBooking,
            showBigJobDialog = ctrl.showBigJobDialog,
            showB2bDialog = ctrl.showB2bDialog,
            showTopUpDialog = ctrl.showTopUpDialog,
            showSettingsDialog = ctrl.showSettingsDialog,
            showCityPickerDialog = ctrl.showCityPickerDialog,
            showLiveVoiceDialog = ctrl.showLiveVoiceDialog,
            showAdminWithdrawDialog = ctrl.showAdminWithdrawDialog,
            activePaymentTidBooking = ctrl.activePaymentTidBooking,
            activeChatBookingId = ctrl.activeChatBookingId,
            activeChatRole = ctrl.activeChatRole,
            activePartsBookingId = ctrl.activePartsBookingId,
            partsAllowAdd = ctrl.partsAllowAdd,
            activeWarrantyBooking = ctrl.activeWarrantyBooking,
            activeOtpVerifyBooking = ctrl.activeOtpVerifyBooking,
            activeRateBooking = ctrl.activeRateBooking,
            toastMessage = ctrl.toastMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KaamWalaUiState(
            currentUserId = currentUserId,
            currentUserName = resolvedUserName,
            currentUserEmail = resolvedUserEmail,
            isAdminUser = isAdminUser,
            isStaffUnlocked = isAdminUser
        )
    )

    private fun unwrapFirestoreError(error: Throwable): Throwable {
        val causes = generateSequence(error) { it.cause }
        return causes.firstOrNull { it is FirebaseFirestoreException }
            ?: causes.firstOrNull { it !is CancellationException }
            ?: error
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val root = unwrapFirestoreError(e)
                showToast(root.localizedMessage ?: "Action failed. Check your connection.")
            }
        }
    }

    fun showToast(message: String) {
        uiControl.value = uiControl.value.copy(toastMessage = message)
    }

    fun dismissToast() {
        uiControl.value = uiControl.value.copy(toastMessage = null)
    }

    // Registration & Profile Actions
    fun openRegistrationDialog() {
        uiControl.value = uiControl.value.copy(showRegistrationDialog = true)
    }

    fun closeRegistrationDialog() {
        uiControl.value = uiControl.value.copy(showRegistrationDialog = false)
    }

    fun saveRegistrationProfile(profile: UserProfileEntity) {
        launchSafely {
            repository.saveUserProfile(
                profile = profile.copy(userId = currentUserId, email = resolvedUserEmail),
                isAdminCaller = isAdminUser
            )
            repository.updateWorkspaceFields(
                mapOf(
                    "city" to profile.city,
                    "displayName" to profile.fullName.take(100),
                    "activeWorkerName" to if (profile.role == "WORKER") profile.fullName.take(100)
                    else uiState.value.prefs.activeWorkerName
                )
            )
            uiControl.value = uiControl.value.copy(
                showRegistrationDialog = false,
                toastMessage = if (profile.role == "WORKER") {
                    "Karegar KYC submitted! Admin approval ke baad orders milna shuru ho jayein ge."
                } else {
                    "Customer profile save ho gayi! Ab aap service book kar sakte hain."
                }
            )
        }
    }

    fun selectTab(tab: AppTab) {
        if (tab == AppTab.WORKER || tab == AppTab.ADMIN) {
            val target = if (tab == AppTab.WORKER) ProtectedTarget.WORKER else ProtectedTarget.ADMIN
            openProtected(target)
        } else {
            uiControl.value = uiControl.value.copy(currentTab = tab)
        }
    }

    fun openProtected(target: ProtectedTarget) {
        val currentState = uiState.value
        val workerDirect = target == ProtectedTarget.WORKER && currentState.userProfile?.role == "WORKER"
        if (isAdminUser || uiControl.value.isStaffUnlocked || workerDirect) {
            when (target) {
                ProtectedTarget.WORKER -> uiControl.value = uiControl.value.copy(currentTab = AppTab.WORKER)
                ProtectedTarget.ADMIN -> uiControl.value = uiControl.value.copy(currentTab = AppTab.ADMIN)
                ProtectedTarget.SETTINGS -> uiControl.value = uiControl.value.copy(showSettingsDialog = true)
            }
        } else {
            uiControl.value = uiControl.value.copy(pendingProtectedTarget = target)
        }
    }

    fun onLogoTap() {
        val now = System.currentTimeMillis()
        if (now - lastLogoTapMs > 1500L) {
            logoTapCount = 0
        }
        lastLogoTapMs = now
        logoTapCount++
        if (logoTapCount >= 3) {
            logoTapCount = 0
            openProtected(ProtectedTarget.ADMIN)
        }
    }

    fun submitStaffPin(enteredPin: String) {
        val correctPin = uiState.value.prefs.staffPin
        if (enteredPin.trim() == correctPin) {
            val target = uiControl.value.pendingProtectedTarget
            uiControl.value = uiControl.value.copy(
                isStaffUnlocked = true,
                pendingProtectedTarget = null,
                currentTab = when (target) {
                    ProtectedTarget.WORKER -> AppTab.WORKER
                    ProtectedTarget.ADMIN -> AppTab.ADMIN
                    else -> uiControl.value.currentTab
                },
                showSettingsDialog = target == ProtectedTarget.SETTINGS,
                toastMessage = "Staff Access Unlocked"
            )
        } else {
            showToast("Galat PIN! Default PIN 1234 try karein.")
        }
    }

    fun dismissPinDialog() {
        uiControl.value = uiControl.value.copy(pendingProtectedTarget = null)
    }

    fun lockStaff() {
        uiControl.value = uiControl.value.copy(
            isStaffUnlocked = false,
            currentTab = AppTab.HOME,
            showSettingsDialog = false,
            toastMessage = "Staff Mode Locked"
        )
    }

    fun toggleLanguage() {
        val next = if (uiState.value.prefs.lang == "en") "ur" else "en"
        launchSafely {
            repository.updateWorkspaceFields(mapOf("lang" to next))
        }
    }

    fun openCityPicker() {
        uiControl.value = uiControl.value.copy(showCityPickerDialog = true)
    }

    fun closeCityPicker() {
        uiControl.value = uiControl.value.copy(showCityPickerDialog = false)
    }

    fun selectCity(city: String) {
        launchSafely {
            repository.updateWorkspaceFields(mapOf("city" to city))
            uiControl.value = uiControl.value.copy(
                showCityPickerDialog = false,
                toastMessage = "City set to $city"
            )
        }
    }

    fun openLiveVoiceDialog() {
        uiControl.value = uiControl.value.copy(showLiveVoiceDialog = true)
    }

    fun closeLiveVoiceDialog() {
        uiControl.value = uiControl.value.copy(showLiveVoiceDialog = false)
    }

    fun openBookingFromVoice(serviceId: String) {
        val svc = CatalogData.SERVICES.firstOrNull { it.id == serviceId }
        uiControl.value = uiControl.value.copy(
            showLiveVoiceDialog = false,
            bookingSheetService = svc ?: uiControl.value.bookingSheetService
        )
    }

    fun setCategory(category: ServiceCategory) {
        uiControl.value = uiControl.value.copy(selectedCategory = category)
    }

    fun toggleFemaleMode() {
        val next = !uiControl.value.femaleMode
        uiControl.value = uiControl.value.copy(
            femaleMode = next,
            toastMessage = if (next) "Family-Safe Pro filter ON" else "Showing all pros"
        )
    }

    fun updateSearchQuery(query: String) {
        uiControl.value = uiControl.value.copy(searchQuery = query)
    }

    fun openBookingSheet(serviceId: String) {
        val svc = CatalogData.SERVICES.firstOrNull { it.id == serviceId } ?: return
        uiControl.value = uiControl.value.copy(bookingSheetService = svc)
    }

    fun closeBookingSheet() {
        uiControl.value = uiControl.value.copy(bookingSheetService = null)
    }

    fun submitBooking(
        service: ServiceItem,
        slot: String,
        express: Boolean,
        femaleSafe: Boolean,
        area: String,
        customerName: String,
        customerPhone: String,
        address: String,
        gpsCoords: String,
        note: String,
        photoUri: String,
        voiceNotePath: String,
        promoCode: String,
        paymentMethod: String = "CASH",
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ) {
        val prefs = uiState.value.prefs
        val expressFee = if (express) 200 else 0
        val cleanPromo = promoCode.trim().uppercase()
        val promoDisc = when {
            cleanPromo == "FIRST200" && !prefs.usedFirstPromo -> 200
            cleanPromo == "REF200" -> 100
            else -> 0
        }
        val afterPromo = (service.fee + expressFee - promoDisc).coerceAtLeast(0)
        val creditUsed = minOf(prefs.customerCredit, afterPromo)
        val total = (afterPromo - creditUsed).coerceAtLeast(0)
        val otp = Random.nextInt(1000, 9999).toString()
        val id = "KW-${Random.nextInt(600, 999)}-${System.currentTimeMillis() % 100}"

        val resolvedLatLng = when {
            latitude != 0.0 || longitude != 0.0 -> latitude to longitude
            gpsCoords.isNotBlank() -> KaamWalaGeo.parseCoords(gpsCoords)?.let { it.latitude to it.longitude }
                ?: KaamWalaGeo.coordsFor(prefs.city, area).let { it.latitude to it.longitude }
            else -> KaamWalaGeo.coordsFor(prefs.city, area).let { it.latitude to it.longitude }
        }
        val finalGps = gpsCoords.ifBlank {
            KaamWalaGeo.formatLatLng(resolvedLatLng.first, resolvedLatLng.second)
        }

        val booking = BookingEntity(
            id = id,
            isDemo = false,
            customerId = currentUserId,
            workerId = "",
            city = prefs.city,
            serviceId = service.id,
            serviceName = service.nameEn,
            fee = service.fee,
            expressFee = expressFee,
            promoDisc = promoDisc,
            creditUsed = creditUsed,
            total = total,
            express = express,
            femaleSafe = femaleSafe,
            slot = slot,
            area = area,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            address = address.trim(),
            gpsCoords = finalGps,
            latitude = resolvedLatLng.first,
            longitude = resolvedLatLng.second,
            note = note.trim(),
            photoUri = photoUri,
            voiceNotePath = voiceNotePath,
            otp = otp,
            status = "PENDING",
            paymentMethod = paymentMethod,
            paymentStatus = "UNPAID",
            createdAt = System.currentTimeMillis()
        )

        launchSafely {
            repository.insertBooking(
                booking = booking,
                markFirstPromoUsed = (cleanPromo == "FIRST200" && promoDisc > 0),
                publishToMarketplace = true
            )
            uiControl.value = uiControl.value.copy(
                bookingSheetService = null,
                confirmedBooking = booking
            )
        }
    }

    fun dismissConfirmedBooking() {
        uiControl.value = uiControl.value.copy(confirmedBooking = null)
    }

    fun openPaymentTidDialog(booking: BookingEntity) {
        uiControl.value = uiControl.value.copy(activePaymentTidBooking = booking)
    }

    fun closePaymentTidDialog() {
        uiControl.value = uiControl.value.copy(activePaymentTidBooking = null)
    }

    fun submitPaymentTid(booking: BookingEntity, tid: String) {
        launchSafely {
            repository.submitOrderPaymentTid(booking, tid)
            uiControl.value = uiControl.value.copy(
                activePaymentTidBooking = null,
                toastMessage = "TID $tid submitted! Admin verify kar ke payment SETTLED karega."
            )
        }
    }

    fun simulateReferralBonus() {
        launchSafely {
            repository.updateWorkspaceFields(mapOf("customerCredit" to FieldValue.increment(200L)))
            showToast("+Rs. 200 Referral Credit added to your account!")
        }
    }

    fun subscribePlan(plan: CarePlan) {
        launchSafely {
            val active = ActivePlanEntity(
                id = "PL-${System.currentTimeMillis() % 100000}",
                planId = plan.id,
                title = plan.title,
                desc = plan.desc,
                price = plan.price
            )
            repository.insertActivePlan(active)
            showToast("${plan.title} activated! Check Orders tab.")
        }
    }

    fun openBigJobDialog() {
        uiControl.value = uiControl.value.copy(showBigJobDialog = true)
    }

    fun closeBigJobDialog() {
        uiControl.value = uiControl.value.copy(showBigJobDialog = false)
    }

    fun submitBigJob(category: String, area: String, desc: String, budget: Int) {
        launchSafely {
            val job = BigJobEntity(
                id = "BJ-${Random.nextInt(200, 999)}",
                category = category,
                area = area.trim(),
                description = desc.trim(),
                budget = budget,
                status = "OPEN",
                isDemo = false
            )
            repository.insertBigJob(job)
            uiControl.value = uiControl.value.copy(
                showBigJobDialog = false,
                currentTab = AppTab.ORDERS,
                toastMessage = "Big Job posted! Workers can now place bids."
            )
        }
    }

    fun submitWorkerBid(bigJob: BigJobEntity, amount: Int, note: String) {
        val workerName = uiState.value.effectiveWorkerName
        launchSafely {
            repository.insertBid(
                BidEntity(
                    id = "BID-${System.currentTimeMillis() % 100000}",
                    bigJobId = bigJob.id,
                    workerName = workerName,
                    amount = amount,
                    note = note.trim()
                )
            )
            showToast("Bid Rs. $amount submitted by $workerName!")
        }
    }

    fun acceptBid(bigJob: BigJobEntity, bid: BidEntity) {
        launchSafely {
            repository.acceptBid(bigJob.id, bid.id)
            showToast("Bid from ${bid.workerName} accepted!")
        }
    }

    fun openB2bDialog() {
        uiControl.value = uiControl.value.copy(showB2bDialog = true)
    }

    fun closeB2bDialog() {
        uiControl.value = uiControl.value.copy(showB2bDialog = false)
    }

    fun submitB2bLead(bizName: String, phone: String, pkg: String) {
        launchSafely {
            repository.insertB2bLead(
                B2bLeadEntity(
                    id = "B2B-${Random.nextInt(100, 999)}",
                    businessName = bizName.trim(),
                    phone = phone.trim(),
                    packageName = pkg,
                    status = "NEW",
                    isDemo = false
                )
            )
            uiControl.value = uiControl.value.copy(
                showB2bDialog = false,
                toastMessage = "Corporate quote request received! Team will contact shortly."
            )
        }
    }

    fun activateB2bLead(lead: B2bLeadEntity) {
        launchSafely {
            repository.activateB2bLead(lead.id)
            showToast("${lead.businessName} marked ACTIVE")
        }
    }

    fun openChat(bookingId: String, role: String) {
        uiControl.value = uiControl.value.copy(
            activeChatBookingId = bookingId,
            activeChatRole = role
        )
    }

    fun closeChat() {
        uiControl.value = uiControl.value.copy(activeChatBookingId = null)
    }

    fun sendChatMessage(text: String) {
        val bookingId = uiControl.value.activeChatBookingId ?: return
        val role = uiControl.value.activeChatRole
        val state = uiState.value
        val booking = state.bookings.firstOrNull { it.id == bookingId }
        val senderName = if (role == "WORKER") {
            state.effectiveWorkerName
        } else {
            booking?.customerName ?: state.userProfile?.fullName?.ifBlank { null } ?: resolvedUserName.ifBlank { "Customer" }
        }
        val isShared = booking?.customerId?.isNotBlank() == true
        launchSafely {
            repository.insertChatMessage(
                msg = ChatMessageEntity(
                    id = "MSG-${System.currentTimeMillis()}",
                    bookingId = bookingId,
                    customerId = booking?.customerId.orEmpty(),
                    workerId = booking?.workerId.orEmpty(),
                    senderUid = currentUserId,
                    senderRole = role,
                    senderName = senderName,
                    text = text.trim()
                ),
                isSharedOrder = isShared
            )
        }
    }

    fun openPartsDialog(bookingId: String, allowAdd: Boolean) {
        uiControl.value = uiControl.value.copy(
            activePartsBookingId = bookingId,
            partsAllowAdd = allowAdd
        )
    }

    fun closePartsDialog() {
        uiControl.value = uiControl.value.copy(activePartsBookingId = null)
    }

    fun addHardwarePartToBooking(booking: BookingEntity, part: HardwarePart) {
        val updatedSummary = if (booking.addedPartsSummary.isBlank()) {
            "${part.name} (Rs. ${part.price})"
        } else {
            "${booking.addedPartsSummary}, ${part.name} (Rs. ${part.price})"
        }
        launchSafely {
            repository.updateOrderFields(
                booking,
                mapOf(
                    "partsTotal" to (booking.partsTotal + part.price).toLong(),
                    "total" to (booking.total + part.price).toLong(),
                    "addedPartsSummary" to updatedSummary.take(500)
                )
            )
            showToast("${part.name} (+Rs. ${part.price}) added to bill!")
        }
    }

    fun openWarrantyDialog(booking: BookingEntity) {
        uiControl.value = uiControl.value.copy(activeWarrantyBooking = booking)
    }

    fun closeWarrantyDialog() {
        uiControl.value = uiControl.value.copy(activeWarrantyBooking = null)
    }

    fun openRateDialog(booking: BookingEntity) {
        uiControl.value = uiControl.value.copy(activeRateBooking = booking)
    }

    fun closeRateDialog() {
        uiControl.value = uiControl.value.copy(activeRateBooking = null)
    }

    fun submitRating(booking: BookingEntity, rating: Int, reviewNote: String) {
        launchSafely {
            repository.updateOrderFields(
                booking,
                mapOf(
                    "rating" to rating.toLong(),
                    "reviewNote" to reviewNote.trim().take(500)
                )
            )
            uiControl.value = uiControl.value.copy(
                activeRateBooking = null,
                toastMessage = "Shukriya! $rating-star rating saved."
            )
        }
    }

    fun reportStrike(booking: BookingEntity) {
        val currentStrikes = uiState.value.prefs.workerStrikes
        launchSafely {
            repository.reportStrike(booking, currentStrikes)
            uiControl.value = uiControl.value.copy(
                activeRateBooking = null,
                toastMessage = "Quality strike logged for review."
            )
        }
    }

    // Worker actions
    fun selectWorkerSubTab(subTab: WorkerSubTab) {
        uiControl.value = uiControl.value.copy(workerSubTab = subTab)
    }

    fun toggleWorkerOnline() {
        val state = uiState.value
        val profile = state.userProfile
        val next = if (profile?.role == "WORKER") !profile.isOnline else !state.prefs.workerOnline
        launchSafely {
            if (profile?.role == "WORKER") {
                repository.setMarketplaceWorkerOnline(currentUserId, next)
            }
            repository.updateWorkspaceFields(mapOf("workerOnline" to next))
            showToast(if (next) "You are now Online" else "You are now Offline")
        }
    }

    fun changeActiveWorker(name: String) {
        launchSafely {
            repository.updateWorkspaceFields(mapOf("activeWorkerName" to name))
        }
    }

    fun acceptJob(booking: BookingEntity) {
        val state = uiState.value
        val prefs = state.prefs
        val profile = state.userProfile
        if (booking.customerId.isNotBlank() && profile?.role == "WORKER" && profile.verificationStatus != "APPROVED") {
            showToast("Aapka Karegar KYC abhi PENDING hai. Admin approval ke baad orders pick kar sakein ge.")
            return
        }
        val online = if (profile?.role == "WORKER") profile.isOnline else prefs.workerOnline
        if (!online) {
            showToast("Pehle Online switch ON karein!")
            return
        }
        val strikes = if (profile?.role == "WORKER") profile.strikes else prefs.workerStrikes
        if (strikes >= 3) {
            showToast("Account suspended (3/3 strikes). Admin se clear karwayein.")
            return
        }
        val wallet = state.effectiveWalletBalance
        if (wallet < prefs.leadFee) {
            showToast("Wallet mein kam balance hai (Rs. $wallet). Kam az kam Rs. ${prefs.leadFee} recharge karein!")
            return
        }
        val workerName = state.effectiveWorkerName
        val workerPhone = profile?.phone?.ifBlank { null } ?: prefs.whatsapp
        launchSafely {
            repository.acceptJobAsWorker(
                booking = booking,
                workerUid = currentUserId,
                workerName = workerName,
                workerPhone = workerPhone,
                leadFee = prefs.leadFee
            )
            showToast("Job ${booking.id} accepted! Rs. ${prefs.leadFee} lead fee deducted.")
        }
    }

    fun openOtpVerifyDialog(booking: BookingEntity) {
        uiControl.value = uiControl.value.copy(activeOtpVerifyBooking = booking)
    }

    fun closeOtpVerifyDialog() {
        uiControl.value = uiControl.value.copy(activeOtpVerifyBooking = null)
    }

    fun verifyOtpAndStartJob(booking: BookingEntity, enteredOtp: String) {
        if (enteredOtp.trim() == booking.otp) {
            launchSafely {
                repository.updateOrderFields(booking, mapOf("status" to "IN_PROGRESS"))
                uiControl.value = uiControl.value.copy(
                    activeOtpVerifyBooking = null,
                    toastMessage = "OTP verified! Work started."
                )
            }
        } else {
            showToast("Galat OTP! Customer se 4-digit Safety OTP poochein.")
        }
    }

    fun attachProofPhoto(booking: BookingEntity, uriString: String) {
        launchSafely {
            repository.updateOrderFields(booking, mapOf("proofPhotoUri" to uriString))
            showToast("Work proof photo attached!")
        }
    }

    fun completeJob(booking: BookingEntity) {
        val state = uiState.value
        launchSafely {
            repository.completeOrderAndRecordCommission(
                booking = booking,
                proofPhotoUri = booking.proofPhotoUri,
                commissionPercent = state.prefs.commissionPercent,
                actorName = state.effectiveWorkerName
            )
            val msg = if (booking.paymentMethod == "CASH") {
                "Job ${booking.id} completed! Cash payment settled & 7-Day Warranty issued."
            } else {
                "Job ${booking.id} completed! Digital payment TID Admin settlement par verify hoga."
            }
            showToast(msg)
        }
    }

    fun cancelBooking(booking: BookingEntity) {
        if (booking.status != "PENDING" && booking.status != "ACCEPTED") {
            showToast("Is stage par order cancel nahi ho sakta.")
            return
        }
        launchSafely {
            repository.cancelBooking(booking)
            showToast("Order ${booking.id} cancelled.")
        }
    }

    fun rescheduleBooking(booking: BookingEntity, newSlot: String, newAddress: String) {
        if (newAddress.isBlank()) {
            showToast("Address khali nahi ho sakta.")
            return
        }
        launchSafely {
            repository.rescheduleBooking(booking, newSlot, newAddress.trim())
            showToast("Order ${booking.id} rescheduled ($newSlot).")
        }
    }

    fun updateWorkerLocation(booking: BookingEntity, lat: Double, lng: Double) {
        launchSafely {
            repository.updateWorkerLiveLocation(booking, lat, lng)
            showToast("Live GPS shared with customer for ${booking.id}!")
        }
    }

    fun openTopUpDialog() {
        uiControl.value = uiControl.value.copy(showTopUpDialog = true)
    }

    fun closeTopUpDialog() {
        uiControl.value = uiControl.value.copy(showTopUpDialog = false)
    }

    fun submitTopUp(method: String, amount: Int, tid: String) {
        val state = uiState.value
        val workerName = state.effectiveWorkerName
        val useMkt = state.userProfile?.role == "WORKER"
        launchSafely {
            repository.insertTopUpRequest(
                req = TopUpRequestEntity(
                    id = "TOP-${Random.nextInt(100, 999)}",
                    workerUid = if (useMkt) currentUserId else "",
                    workerName = workerName,
                    method = method,
                    amount = amount,
                    tid = tid.trim(),
                    status = "PENDING"
                ),
                useMarketplace = useMkt
            )
            uiControl.value = uiControl.value.copy(
                showTopUpDialog = false,
                toastMessage = "Recharge request (TID: $tid) submitted for Admin approval."
            )
        }
    }

    fun applyToolLoan(tool: ToolCatalogItem) {
        val state = uiState.value
        val workerName = state.effectiveWorkerName
        val useMkt = state.userProfile?.role == "WORKER"
        launchSafely {
            repository.insertToolLoan(
                loan = ToolLoanEntity(
                    id = "TL-${Random.nextInt(100, 999)}",
                    workerUid = if (useMkt) currentUserId else "",
                    workerName = workerName,
                    toolName = tool.name,
                    totalPrice = tool.price,
                    dailyQist = tool.dailyQist,
                    paidAmount = 0,
                    status = "PENDING"
                ),
                useMarketplace = useMkt
            )
            showToast("${tool.name} Asaan Qist request sent to Admin!")
        }
    }

    // Admin actions
    fun approveWorkerProfile(profile: UserProfileEntity, bonusWallet: Long = 500L) {
        launchSafely {
            repository.approveWorkerProfile(profile.userId, bonusWallet)
            showToast("${profile.fullName} APPROVED! (+Rs. $bonusWallet starter wallet)")
        }
    }

    fun rejectWorkerProfile(profile: UserProfileEntity) {
        launchSafely {
            repository.rejectWorkerProfile(profile.userId)
            showToast("${profile.fullName} KYC Rejected.")
        }
    }

    fun verifyDigitalOrderPayment(booking: BookingEntity) {
        launchSafely {
            repository.verifyDigitalOrderPaymentSettled(booking, uiState.value.prefs.commissionPercent)
            showToast("Order ${booking.id} TID ${booking.paymentTid} verified & SETTLED!")
        }
    }

    fun openAdminWithdrawDialog() {
        uiControl.value = uiControl.value.copy(showAdminWithdrawDialog = true)
    }

    fun closeAdminWithdrawDialog() {
        uiControl.value = uiControl.value.copy(showAdminWithdrawDialog = false)
    }

    fun recordAdminWithdrawal(amount: Int, method: String, account: String, note: String) {
        launchSafely {
            repository.recordAdminLedgerEntry(
                AdminLedgerEntity(
                    id = "LED-W-${System.currentTimeMillis() % 100000}",
                    entryType = "WITHDRAWAL",
                    amount = amount,
                    orderId = "",
                    actorName = resolvedUserName.ifBlank { "Admin" },
                    note = note.ifBlank { "Saved earnings via $method" },
                    payoutMethod = method,
                    payoutAccount = account,
                    recordedByUid = currentUserId
                )
            )
            uiControl.value = uiControl.value.copy(
                showAdminWithdrawDialog = false,
                toastMessage = "Saved Rs. $amount withdrawal to Admin Ledger!"
            )
        }
    }

    fun approveTopUp(req: TopUpRequestEntity) {
        launchSafely {
            repository.approveTopUp(req)
            showToast("Approved Rs. ${req.amount} for ${req.workerName}")
        }
    }

    fun rejectTopUp(req: TopUpRequestEntity) {
        launchSafely {
            repository.rejectTopUp(req)
            showToast("Rejected TID ${req.tid}")
        }
    }

    fun approveToolLoan(loan: ToolLoanEntity) {
        launchSafely {
            repository.approveToolLoan(loan)
            showToast("${loan.toolName} approved for ${loan.workerName}")
        }
    }

    fun collectDailyQist(loan: ToolLoanEntity) {
        launchSafely {
            repository.collectDailyQist(loan)
            showToast("Collected Rs. ${loan.dailyQist} daily qist!")
        }
    }

    fun addNewWorker(name: String, skill: String, info: String, femaleSafe: Boolean) {
        launchSafely {
            repository.insertWorker(
                WorkerEntity(
                    id = "w_${System.currentTimeMillis()}",
                    name = name.trim(),
                    skill = skill.trim(),
                    rating = "5.0",
                    jobsCount = "New",
                    femaleSafe = femaleSafe,
                    info = info.trim(),
                    avatarUrl = "",
                    isDemo = false
                )
            )
            showToast("$name verified & added!")
        }
    }

    fun resetStrikes() {
        launchSafely {
            repository.updateWorkspaceFields(mapOf("workerStrikes" to 0L))
            showToast("Strikes cleared & worker unsuspended.")
        }
    }

    fun openSettingsDialog() {
        openProtected(ProtectedTarget.SETTINGS)
    }

    fun closeSettingsDialog() {
        uiControl.value = uiControl.value.copy(showSettingsDialog = false)
    }

    fun saveSettings(
        brand: String,
        city: String,
        whatsapp: String,
        payAccount: String,
        leadFee: Int,
        commissionPercent: Int,
        staffPin: String,
        liveCustomerMode: Boolean
    ) {
        launchSafely {
            val cleanPin = staffPin.trim().ifBlank { "1234" }.let {
                if (it.length < 4) it.padEnd(4, '0') else it.take(8)
            }
            val cleanBrand = brand.trim().ifBlank { "KaamWala" }.take(60)
            val cleanCity = city.trim().ifBlank { "Lahore" }.take(60)
            val cleanWa = whatsapp.trim().ifBlank { "923001234567" }.take(30)
            val cleanPay = payAccount.trim().ifBlank { "JazzCash / Easypaisa: 0300-1234567" }.take(120)
            val cleanLead = leadFee.coerceAtLeast(0)
            val cleanComm = commissionPercent.coerceIn(0, 50)

            repository.updateWorkspaceFields(
                mapOf(
                    "brand" to cleanBrand,
                    "city" to cleanCity,
                    "whatsapp" to cleanWa,
                    "payAccount" to cleanPay,
                    "leadFee" to cleanLead.toLong(),
                    "staffPin" to cleanPin,
                    "liveCustomerMode" to liveCustomerMode
                )
            )
            if (isAdminUser) {
                repository.saveMarketplaceConfig(
                    brand = cleanBrand,
                    city = cleanCity,
                    whatsapp = cleanWa,
                    payAccount = cleanPay,
                    leadFee = cleanLead,
                    commissionPercent = cleanComm
                )
            }
            val nextTab = if (liveCustomerMode && (uiControl.value.currentTab == AppTab.WORKER || uiControl.value.currentTab == AppTab.ADMIN)) {
                AppTab.HOME
            } else uiControl.value.currentTab
            uiControl.value = uiControl.value.copy(
                showSettingsDialog = false,
                currentTab = nextTab,
                toastMessage = "Settings saved!"
            )
        }
    }

    fun clearDemoOrders() {
        launchSafely {
            repository.clearDemoOrders()
            showToast("Sample demo orders cleared for fresh start!")
        }
    }

    fun resetAllDemo() {
        launchSafely {
            repository.resetAllDemo()
            uiControl.value = LocalUiControl(
                isStaffUnlocked = isAdminUser,
                toastMessage = "All demo data restored to defaults!"
            )
        }
    }
}
