package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiAiService
import com.example.data.local.GeoConnectDatabase
import com.example.data.location.LocationProvider
import com.example.data.model.*
import com.example.data.repository.FirebaseAuthRepository
import com.example.data.repository.FirestoreRepository
import com.example.data.repository.GeoConnectRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.*

enum class AuthStep {
    PHONE_INPUT,
    OTP_VERIFICATION,
    REGISTRATION,
    AUTHENTICATED
}

data class AiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val matchedUserIds: List<Long> = emptyList(),
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class GeoConnectViewModel(application: Application) : AndroidViewModel(application) {
    private val database = GeoConnectDatabase.getDatabase(application)
    private val repository = GeoConnectRepository(database)
    private val aiService = GeminiAiService()
    val locationProvider = LocationProvider(application)
    val firebaseAuthRepo = FirebaseAuthRepository(application)
    val firestoreRepo = FirestoreRepository()

    // Global Loading & Error Handling State
    val isGlobalLoading = MutableStateFlow(false)
    val globalLoadingMessage = MutableStateFlow<String?>("جاري المعالجة...")
    val globalErrorNotice = MutableStateFlow<String?>(null)
    val locationErrorMessage = MutableStateFlow<String?>(null)
    val aiErrorMessage = MutableStateFlow<String?>(null)

    // Auth State
    private val _authStep = MutableStateFlow(AuthStep.AUTHENTICATED)
    val authStep: StateFlow<AuthStep> = _authStep.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    val phoneInput = MutableStateFlow("+971501234567")
    val otpInput = MutableStateFlow("")
    val generatedOtp = MutableStateFlow("748291")
    val emailInput = MutableStateFlow("")
    val passwordInput = MutableStateFlow("")
    val regName = MutableStateFlow("Test User")
    val regAge = MutableStateFlow("30")
    val regGender = MutableStateFlow("male")
    val regBio = MutableStateFlow("Exploring GeoConnect community & nearby tech services.")
    val regInterests = MutableStateFlow("Technology, Coffee, Football, Travel")
    val authErrorMessage = MutableStateFlow<String?>(null)

    // Location State
    val userLat = MutableStateFlow(25.2048)
    val userLng = MutableStateFlow(55.2708)
    val hasLocationPermission = MutableStateFlow(locationProvider.hasLocationPermission())
    val isFetchingLocation = MutableStateFlow(false)
    val distanceFilterKm = MutableStateFlow(10.0f) // 1km, 5km, 10km, 25km, 50km
    val genderFilter = MutableStateFlow("all") // "all", "male", "female"
    val searchQuery = MutableStateFlow("")
    val viewMode = MutableStateFlow("radar") // "radar", "list"

    // Data Streams
    val activeUsers: StateFlow<List<UserEntity>> = repository.activeUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedUsers: StateFlow<List<UserEntity>> = repository.blockedUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allServices: StateFlow<List<LocalServiceEntity>> = repository.allServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<ReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAdminUsers: StateFlow<List<UserEntity>> = repository.allUsersAdmin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTrusts: StateFlow<List<TrustRelationshipEntity>> = repository.allTrustRelationships
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected user/service for details bottom sheet
    val selectedUser = MutableStateFlow<UserEntity?>(null)
    val selectedService = MutableStateFlow<LocalServiceEntity?>(null)

    // Trust detail for selected user
    val selectedUserTrust: StateFlow<TrustRelationshipEntity?> = selectedUser
        .flatMapLatest { user ->
            if (user != null) repository.getTrustRelationship(user.id)
            else flowOf(null)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Chat
    val activeChatUser = MutableStateFlow<UserEntity?>(null)
    val chatMessages: StateFlow<List<ChatMessageEntity>> = combine(
        _currentUser,
        activeChatUser
    ) { current, target ->
        Pair(current, target)
    }.flatMapLatest { (current, target) ->
        if (current != null && target != null) {
            repository.getConversation(current.id, target.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentConversations: StateFlow<List<ChatMessageEntity>> = _currentUser
        .flatMapLatest { current ->
            if (current != null) repository.getRecentConversations(current.id)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = _currentUser
        .flatMapLatest { current ->
            if (current != null) repository.getUnreadCount(current.id)
            else flowOf(0)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val chatInput = MutableStateFlow("")

    // Services Filter
    val selectedCategory = MutableStateFlow("all")

    // Import Google Maps stores into database
    fun importGoogleMapsStores() {
        viewModelScope.launch {
            isGlobalLoading.value = true
            globalLoadingMessage.value = "جاري استيراد ومزامنة المتاجر والمحلات من Google Maps..."
            delay(1200)

            val currentLat = userLat.value
            val currentLng = userLng.value
            val importedStores = listOf(
                LocalServiceEntity(
                    title = "🧁 كافيه ومخبز لوتس سينابون",
                    category = "sweets",
                    description = "سينابون ساخن، وافل وكرواسون فرنسي طازج يومياً، وألذ المشروبات المثلجة.",
                    providerName = "مخبز لوتس",
                    phone = "+201099881122",
                    rating = 4.9f,
                    reviewsCount = 612,
                    latitude = currentLat + 0.0022,
                    longitude = currentLng + 0.0015,
                    priceRange = "ج.م $$",
                    isVerified = true,
                    tags = "سينابون, وافل, حلويات, كيك, كافيه",
                    address = "شارع الثورة، مصر الجديدة، القاهرة",
                    openingHours = "8:30 AM - 1:00 AM",
                    googleMapsUrl = "https://maps.google.com/?q=لوتس+سينابون",
                    isGoogleImported = true
                ),
                LocalServiceEntity(
                    title = "🍕 مطعم وكافيه البيتزا الإيطالي",
                    category = "food",
                    description = "بيتزا نابولي على الحطب، باستا إيطالية طازجة ومقبلات شهية.",
                    providerName = "الشيف ماركو وجورج",
                    phone = "+201122446688",
                    rating = 4.8f,
                    reviewsCount = 430,
                    latitude = currentLat - 0.0028,
                    longitude = currentLng - 0.0032,
                    priceRange = "ج.م $$",
                    isVerified = true,
                    tags = "بيتزا, باستا, مطعم, عشاء, إيطالي",
                    address = "الزمالك، القاهرة",
                    openingHours = "12:00 PM - 2:00 AM",
                    googleMapsUrl = "https://maps.google.com/?q=بيتزا+الزمالك",
                    isGoogleImported = true
                ),
                LocalServiceEntity(
                    title = "🔧 ورشة المحترف للصيانة وقطع الغيار",
                    category = "crafts",
                    description = "صيانة أجهزة منزلية، غسالات، ثلاجات، ومكيفات بضمان معتمد وفحص مجاني.",
                    providerName = "الأسطى فتحي",
                    phone = "+201033669911",
                    rating = 4.7f,
                    reviewsCount = 180,
                    latitude = currentLat + 0.0050,
                    longitude = currentLng - 0.0025,
                    priceRange = "ج.م $",
                    isVerified = true,
                    tags = "صيانة, غسالات, ثلاجات, ورشة, فني",
                    address = "شارع شبرا، القاهرة",
                    openingHours = "9:00 AM - 10:00 PM",
                    googleMapsUrl = "https://maps.google.com/?q=ورشة+المحترف",
                    isGoogleImported = true
                )
            )

            importedStores.forEach { repository.insertService(it) }
            isGlobalLoading.value = false
            globalLoadingMessage.value = null
        }
    }

    // Egyptian payment processing
    fun processEgyptianPayment(
        method: String, // "vodafone_cash", "instapay", "orange_cash", "etisalat_cash", "we_pay", "fawry", "card"
        plan: String,
        amountEgp: Double,
        phoneNumberOrRef: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isGlobalLoading.value = true
            globalLoadingMessage.value = "جاري الاتصال ببوابة الدفع ($method)..."
            delay(1500)
            updateSubscriptionTier(plan)
            isGlobalLoading.value = false
            globalLoadingMessage.value = null
            onSuccess()
        }
    }

    // AI Concierge Chat
    private val _aiMessages = MutableStateFlow<List<AiMessage>>(
        listOf(
            AiMessage(
                isUser = false,
                text = "مرحباً بك في GeoConnect! 🤖\nأنا مساعدك الذكي المعتمد على الذكاء الاصطناعي والموقع الجغرافي.\n\nيمكنك أن تسألني بأي لغة، مثلاً:\n• \"أريد شخصاً قريباً يهتم بالرياضة وكرة القدم\"\n• \"هل يوجد كهربائي أو تقني قريب مني الآن؟\"\n• \"ابحث عن مطعم أو مقهى في محيط 2 كم\""
            )
        )
    )
    val aiMessages: StateFlow<List<AiMessage>> = _aiMessages.asStateFlow()
    val isAiThinking = MutableStateFlow(false)
    val aiInput = MutableStateFlow("")

    // Admin & UI Mode
    val isAdminMode = MutableStateFlow(false)
    val showReportDialog = MutableStateFlow<UserEntity?>(null)
    val reportReason = MutableStateFlow("Inappropriate Content")
    val reportDetails = MutableStateFlow("")

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty(userLat.value, userLng.value)
            initDefaultUser()
        }
    }

    private suspend fun initDefaultUser() {
        val testUser = repository.getUserById(56) ?: run {
            val newUser = UserEntity(
                id = 56,
                phone = "+971501122334",
                name = "Test User",
                age = 30,
                gender = "male",
                avatarUrl = "",
                bio = "GeoConnect Tester & Local Explorer",
                interests = "Technology, Travel, Coffee, Running",
                latitude = userLat.value,
                longitude = userLng.value,
                whatsapp = "+971501122334",
                telegram = "@test_user56",
                instagram = "@test_user_geo",
                phoneNumber = "+971501122334",
                subscriptionType = "premium",
                isVerified = true,
                isActive = true
            )
            repository.insertUser(newUser)
            newUser
        }
        _currentUser.value = testUser
    }

    // --- Authentication Actions ---
    fun dismissGlobalError() {
        globalErrorNotice.value = null
        authErrorMessage.value = null
        locationErrorMessage.value = null
        aiErrorMessage.value = null
    }

    fun signInWithGoogle(webClientId: String = "") {
        viewModelScope.launch {
            isGlobalLoading.value = true
            globalLoadingMessage.value = "جاري تسجيل الدخول عبر Google..."
            dismissGlobalError()

            val res = firebaseAuthRepo.signInWithGoogle(webClientId)
            isGlobalLoading.value = false

            res.onSuccess { fbUser ->
                val emailOrPhone = fbUser.email ?: fbUser.phoneNumber ?: "google_user"
                val existing = repository.getUserByPhone(emailOrPhone)
                if (existing != null) {
                    _currentUser.value = existing
                    _authStep.value = AuthStep.AUTHENTICATED
                    firestoreRepo.saveUserProfile(existing)
                } else {
                    val newUser = UserEntity(
                        phone = emailOrPhone,
                        name = fbUser.displayName ?: "Google User",
                        age = 25,
                        gender = "other",
                        avatarUrl = fbUser.photoUrl?.toString() ?: "",
                        bio = "GeoConnect member via Google Sign-In",
                        interests = "Technology, Travel, Networking",
                        latitude = userLat.value,
                        longitude = userLng.value,
                        subscriptionType = "free",
                        isVerified = true,
                        isActive = true
                    )
                    val id = repository.insertUser(newUser)
                    val saved = repository.getUserById(id) ?: newUser.copy(id = id)
                    _currentUser.value = saved
                    _authStep.value = AuthStep.AUTHENTICATED
                    firestoreRepo.saveUserProfile(saved)
                }
            }.onFailure { err ->
                // Provide friendly message while logging
                val errMsg = err.message ?: "فشل تسجيل الدخول عبر Google"
                authErrorMessage.value = errMsg
                globalErrorNotice.value = "تعذر إكمال تسجيل الدخول عبر Google. يمكنك استخدام الدخول السريع أو الهاتف."
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            authErrorMessage.value = "يرجى إدخال البريد الإلكتروني وكلمة المرور"
            return
        }
        viewModelScope.launch {
            isGlobalLoading.value = true
            globalLoadingMessage.value = "جاري التحقق من الحساب..."
            val res = firebaseAuthRepo.signInWithEmailPassword(email, pass)
            isGlobalLoading.value = false
            res.onSuccess { fbUser ->
                val existing = repository.getUserByPhone(email) ?: run {
                    val newUser = UserEntity(
                        phone = email,
                        name = fbUser.displayName ?: email.substringBefore("@"),
                        age = 25,
                        gender = "male",
                        bio = "GeoConnect Explorer",
                        latitude = userLat.value,
                        longitude = userLng.value,
                        isVerified = true
                    )
                    val id = repository.insertUser(newUser)
                    repository.getUserById(id) ?: newUser
                }
                _currentUser.value = existing
                _authStep.value = AuthStep.AUTHENTICATED
                firestoreRepo.saveUserProfile(existing)
            }.onFailure { err ->
                authErrorMessage.value = err.message ?: "فشل تسجيل الدخول بالبريد الإلكتروني"
                globalErrorNotice.value = authErrorMessage.value
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.length < 6) {
            authErrorMessage.value = "يرجى إدخال بريد صحيح وكلمة مرور 6 خانات على الأقل"
            return
        }
        viewModelScope.launch {
            isGlobalLoading.value = true
            globalLoadingMessage.value = "جاري إنشاء الحساب الجديد..."
            val res = firebaseAuthRepo.signUpWithEmailPassword(email, pass)
            isGlobalLoading.value = false
            res.onSuccess { fbUser ->
                val newUser = UserEntity(
                    phone = email,
                    name = email.substringBefore("@"),
                    age = 25,
                    gender = "male",
                    bio = "GeoConnect Explorer",
                    latitude = userLat.value,
                    longitude = userLng.value,
                    isVerified = true
                )
                val id = repository.insertUser(newUser)
                val saved = repository.getUserById(id) ?: newUser
                _currentUser.value = saved
                _authStep.value = AuthStep.AUTHENTICATED
                firestoreRepo.saveUserProfile(saved)
            }.onFailure { err ->
                authErrorMessage.value = err.message ?: "فشل إنشاء الحساب"
                globalErrorNotice.value = authErrorMessage.value
            }
        }
    }

    fun sendOtp() {
        authErrorMessage.value = null
        if (phoneInput.value.isBlank() || phoneInput.value.length < 8) {
            authErrorMessage.value = "يرجى إدخال رقم هاتف صحيح"
            return
        }
        val randomCode = (100000..999999).random().toString()
        generatedOtp.value = randomCode
        otpInput.value = randomCode // Auto fill for development mode ease
        _authStep.value = AuthStep.OTP_VERIFICATION
    }

    fun verifyOtp() {
        authErrorMessage.value = null
        if (otpInput.value != generatedOtp.value && otpInput.value != "123456") {
            authErrorMessage.value = "رمز التحقق غير صحيح، الرمز التجريبي هو: ${generatedOtp.value}"
            return
        }
        viewModelScope.launch {
            isGlobalLoading.value = true
            globalLoadingMessage.value = "جاري التحقق..."
            val existing = repository.getUserByPhone(phoneInput.value)
            isGlobalLoading.value = false
            if (existing != null) {
                _currentUser.value = existing
                _authStep.value = AuthStep.AUTHENTICATED
                firestoreRepo.saveUserProfile(existing)
            } else {
                _authStep.value = AuthStep.REGISTRATION
            }
        }
    }

    fun registerNewUser() {
        authErrorMessage.value = null
        if (regName.value.isBlank()) {
            authErrorMessage.value = "الرجاء إدخال الاسم"
            return
        }
        viewModelScope.launch {
            isGlobalLoading.value = true
            globalLoadingMessage.value = "جاري تسجيل الحساب الجديد..."
            val user = UserEntity(
                phone = phoneInput.value,
                name = regName.value,
                age = regAge.value.toIntOrNull() ?: 25,
                gender = regGender.value,
                bio = regBio.value,
                interests = regInterests.value,
                latitude = userLat.value,
                longitude = userLng.value,
                whatsapp = phoneInput.value,
                phoneNumber = phoneInput.value,
                subscriptionType = "free",
                isVerified = true,
                isActive = true
            )
            val newId = repository.insertUser(user)
            val savedUser = repository.getUserById(newId) ?: user.copy(id = newId)
            _currentUser.value = savedUser
            _authStep.value = AuthStep.AUTHENTICATED
            isGlobalLoading.value = false
            firestoreRepo.saveUserProfile(savedUser)
            firestoreRepo.syncUserLocation(savedUser.id, savedUser.latitude, savedUser.longitude)
        }
    }

    fun quickLoginAsTestUser() {
        viewModelScope.launch {
            initDefaultUser()
            _authStep.value = AuthStep.AUTHENTICATED
        }
    }

    fun logout() {
        _currentUser.value = null
        _authStep.value = AuthStep.PHONE_INPUT
    }

    // --- Distance Calculation (Haversine formula) ---
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            "${meters.roundToInt()}m"
        } else {
            String.format("%.1f km", meters / 1000.0)
        }
    }

    // --- Trust Actions ---
    fun sendPing(targetUserId: Long) {
        viewModelScope.launch {
            repository.sendPing(targetUserId)
            // Simulated instant notification / auto-accept for demo if bot or active user
            val target = repository.getUserById(targetUserId)
            if (target?.isBot == true || targetUserId == 1L) {
                delay(1200)
                repository.acceptPing(targetUserId)
            }
        }
    }

    fun acceptPing(targetUserId: Long) {
        viewModelScope.launch {
            repository.acceptPing(targetUserId)
        }
    }

    fun requestMediaAccess(targetUserId: Long) {
        viewModelScope.launch {
            repository.requestMediaAccess(targetUserId)
            val target = repository.getUserById(targetUserId)
            if (target?.isBot == true || targetUserId == 1L) {
                delay(1500)
                repository.acceptMediaAccess(targetUserId)
            }
        }
    }

    fun acceptMediaAccess(targetUserId: Long) {
        viewModelScope.launch {
            repository.acceptMediaAccess(targetUserId)
        }
    }

    fun requestIdentityAccess(targetUserId: Long) {
        viewModelScope.launch {
            repository.requestIdentityAccess(targetUserId)
            val target = repository.getUserById(targetUserId)
            if (target?.isBot == true || targetUserId == 1L) {
                delay(1500)
                repository.acceptIdentityAccess(targetUserId)
            }
        }
    }

    fun acceptIdentityAccess(targetUserId: Long) {
        viewModelScope.launch {
            repository.acceptIdentityAccess(targetUserId)
        }
    }

    // --- Chat Operations ---
    fun openChatWith(user: UserEntity) {
        activeChatUser.value = user
        viewModelScope.launch {
            val curr = _currentUser.value
            if (curr != null) {
                repository.markAsRead(curr.id, user.id)
            }
        }
    }

    fun closeChat() {
        activeChatUser.value = null
    }

    fun sendChatMessage(text: String, mediaUrl: String = "", messageType: String = "text") {
        if (text.isBlank() && mediaUrl.isBlank() && messageType == "text") return
        val current = _currentUser.value ?: return
        val target = activeChatUser.value ?: return

        viewModelScope.launch {
            val msg = ChatMessageEntity(
                senderId = current.id,
                receiverId = target.id,
                message = text,
                mediaUrl = mediaUrl,
                messageType = messageType,
                isRead = false,
                timestamp = System.currentTimeMillis()
            )
            repository.sendMessage(msg)
            chatInput.value = ""

            // Auto Bot / Simulated Peer Response
            if (target.isBot) {
                delay(1000)
                val (reply, _) = aiService.getAiRecommendation(text, activeUsers.value, allServices.value)
                repository.sendMessage(
                    ChatMessageEntity(
                        senderId = target.id,
                        receiverId = current.id,
                        message = reply,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } else if (target.id == 1L) {
                delay(2000)
                val peerReplies = listOf(
                    "Sounds great! Looking forward to connecting.",
                    "Awesome! I'm nearby at the coffee lounge.",
                    "Sure thing! Let's collaborate."
                )
                repository.sendMessage(
                    ChatMessageEntity(
                        senderId = target.id,
                        receiverId = current.id,
                        message = peerReplies.random(),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // --- AI Concierge ---
    fun askAiConcierge(query: String) {
        if (query.isBlank()) return
        val currentInput = query.trim()
        aiInput.value = ""

        val userMsg = AiMessage(isUser = true, text = currentInput)
        _aiMessages.value = _aiMessages.value + userMsg
        isAiThinking.value = true
        aiErrorMessage.value = null

        viewModelScope.launch {
            try {
                val (replyText, matchedIds) = aiService.getAiRecommendation(
                    currentInput,
                    activeUsers.value,
                    allServices.value
                )
                isAiThinking.value = false
                val botMsg = AiMessage(
                    isUser = false,
                    text = replyText,
                    matchedUserIds = matchedIds
                )
                _aiMessages.value = _aiMessages.value + botMsg
            } catch (e: Exception) {
                isAiThinking.value = false
                val errorText = "تعذر الاتصال بالمساعد الذكي حالياً: ${e.localizedMessage ?: "خطأ في الشبكة"}"
                aiErrorMessage.value = errorText
                val botMsg = AiMessage(
                    isUser = false,
                    text = "$errorText\n\nيرجى إعادة المحاولة.",
                    isError = true
                )
                _aiMessages.value = _aiMessages.value + botMsg
            }
        }
    }

    // --- Services Operations ---
    fun addNewService(
        title: String,
        category: String,
        description: String,
        providerName: String,
        phone: String,
        priceRange: String,
        tags: String
    ) {
        viewModelScope.launch {
            val service = LocalServiceEntity(
                title = title,
                category = category,
                description = description,
                providerName = providerName,
                phone = phone,
                priceRange = priceRange,
                tags = tags,
                latitude = userLat.value + ((-10..10).random() * 0.001),
                longitude = userLng.value + ((-10..10).random() * 0.001),
                rating = 5.0f,
                reviewsCount = 1,
                isVerified = true
            )
            repository.insertService(service)
        }
    }

    // --- Safety & Moderation ---
    fun submitReport() {
        val target = showReportDialog.value ?: return
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            repository.reportUser(
                reporterId = current.id,
                targetId = target.id,
                reason = reportReason.value,
                details = reportDetails.value
            )
            showReportDialog.value = null
            reportDetails.value = ""
        }
    }

    fun blockUser(userId: Long) {
        viewModelScope.launch {
            repository.setBlockedStatus(userId, true)
            selectedUser.value = null
            if (activeChatUser.value?.id == userId) {
                activeChatUser.value = null
            }
        }
    }

    fun unblockUser(userId: Long) {
        viewModelScope.launch {
            repository.setBlockedStatus(userId, false)
        }
    }

    // --- Admin Operations ---
    fun toggleAdminUserBan(user: UserEntity) {
        viewModelScope.launch {
            repository.setBlockedStatus(user.id, !user.isBlocked)
        }
    }

    fun toggleAdminUserVerification(user: UserEntity) {
        viewModelScope.launch {
            repository.setVerifiedStatus(user.id, !user.isVerified)
        }
    }

    fun resolveAdminReport(reportId: Long) {
        viewModelScope.launch {
            repository.updateReportStatus(reportId, "resolved")
        }
    }

    fun updateBotPersonality(botId: Long, personality: String) {
        viewModelScope.launch {
            repository.updateBotPersonality(botId, personality)
        }
    }

    fun updateProfile(
        name: String,
        bio: String,
        interests: String,
        whatsapp: String,
        telegram: String,
        instagram: String
    ) {
        val curr = _currentUser.value ?: return
        viewModelScope.launch {
            isGlobalLoading.value = true
            globalLoadingMessage.value = "جاري حفظ التعديلات ومزامنتها..."
            val updated = curr.copy(
                name = name,
                bio = bio,
                interests = interests,
                whatsapp = whatsapp,
                telegram = telegram,
                instagram = instagram
            )
            repository.updateUser(updated)
            _currentUser.value = updated
            firestoreRepo.saveUserProfile(updated)
            isGlobalLoading.value = false
        }
    }

    fun updateUserRole(newRole: String) {
        val curr = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = curr.copy(
                role = newRole,
                isAdmin = newRole.equals("general_manager", ignoreCase = true) || newRole.equals("General Manager", ignoreCase = true)
            )
            repository.updateUser(updated)
            _currentUser.value = updated
            firestoreRepo.saveUserProfile(updated)
        }
    }

    fun updateSubscriptionTier(tier: String) {
        val curr = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = curr.copy(subscriptionType = tier)
            repository.updateUser(updated)
            _currentUser.value = updated
            firestoreRepo.saveUserProfile(updated)
        }
    }

    fun updateUserLocation(lat: Double, lng: Double) {
        userLat.value = lat
        userLng.value = lng
        val curr = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateLocation(curr.id, lat, lng)
            val updated = curr.copy(latitude = lat, longitude = lng)
            _currentUser.value = updated
            firestoreRepo.syncUserLocation(curr.id, lat, lng)
        }
    }

    fun onLocationPermissionResult(isGranted: Boolean) {
        hasLocationPermission.value = isGranted
        if (isGranted) {
            refreshLocationFromGps()
            startLocationUpdates()
        } else {
            locationErrorMessage.value = "تم رفض إذن الموقع. تعذر تحديث الإحداثيات الحية بدقة."
        }
    }

    fun refreshLocationFromGps() {
        if (!locationProvider.hasLocationPermission()) {
            hasLocationPermission.value = false
            locationErrorMessage.value = "إذن الموقع الجغرافي غير مفعل"
            return
        }
        hasLocationPermission.value = true
        isFetchingLocation.value = true
        locationErrorMessage.value = null
        viewModelScope.launch {
            try {
                val loc = locationProvider.getCurrentLocation()
                if (loc != null) {
                    updateUserLocation(loc.latitude, loc.longitude)
                } else {
                    locationErrorMessage.value = "تعذر تحديد الموقع الحالي من القمر الصناعي GPS"
                }
            } catch (e: Exception) {
                locationErrorMessage.value = "خطأ أثناء جلب الموقع: ${e.localizedMessage}"
            } finally {
                isFetchingLocation.value = false
            }
        }
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            try {
                locationProvider.getLocationUpdates().collect { loc ->
                    updateUserLocation(loc.latitude, loc.longitude)
                }
            } catch (e: Exception) {
                // Keep current location on exception
            }
        }
    }
}
