package com.example.data.repository

import com.example.data.local.GeoConnectDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlin.math.*

class GeoConnectRepository(private val database: GeoConnectDatabase) {
    private val userDao = database.userDao()
    private val trustDao = database.trustDao()
    private val chatDao = database.chatDao()
    private val serviceDao = database.serviceDao()
    private val reportDao = database.reportDao()
    private val orderDao = database.orderDao()

    val activeUsers: Flow<List<UserEntity>> = userDao.getAllActiveUsers()
    val blockedUsers: Flow<List<UserEntity>> = userDao.getBlockedUsers()
    val botUsers: Flow<List<UserEntity>> = userDao.getBotUsers()
    val allUsersAdmin: Flow<List<UserEntity>> = userDao.getAllUsersAdmin()
    val allServices: Flow<List<LocalServiceEntity>> = serviceDao.getAllServices()
    val allReports: Flow<List<ReportEntity>> = reportDao.getAllReports()
    val allTrustRelationships: Flow<List<TrustRelationshipEntity>> = trustDao.getAllTrustRelationships()
    val allOrders: Flow<List<ServiceOrderEntity>> = orderDao.getAllOrders()

    fun getOrdersForUser(userId: Long): Flow<List<ServiceOrderEntity>> =
        orderDao.getOrdersForUser(userId)

    fun getGiftsReceivedByUser(userId: Long): Flow<List<ServiceOrderEntity>> =
        orderDao.getGiftsReceivedByUser(userId)

    suspend fun insertOrder(order: ServiceOrderEntity): Long =
        orderDao.insertOrder(order)

    suspend fun updateOrderStatus(orderId: Long, status: String) =
        orderDao.updateOrderStatus(orderId, status)

    suspend fun updatePaymentStatus(orderId: Long, paymentStatus: String, ref: String) =
        orderDao.updatePaymentStatus(orderId, paymentStatus, ref)


    fun getConversation(user1: Long, user2: Long): Flow<List<ChatMessageEntity>> =
        chatDao.getConversationMessages(user1, user2)

    fun getRecentConversations(userId: Long): Flow<List<ChatMessageEntity>> =
        chatDao.getRecentConversations(userId)

    fun getUnreadCount(userId: Long): Flow<Int> =
        chatDao.getUnreadCount(userId)

    fun getTrustRelationship(targetId: Long): Flow<TrustRelationshipEntity?> =
        trustDao.getTrustRelationship(targetId)

    suspend fun getTrustRelationshipDirect(targetId: Long): TrustRelationshipEntity? =
        trustDao.getTrustRelationshipDirect(targetId)

    suspend fun getUserById(userId: Long): UserEntity? =
        userDao.getUserById(userId)

    suspend fun getUserByPhone(phone: String): UserEntity? =
        userDao.getUserByPhone(phone)

    suspend fun insertUser(user: UserEntity): Long =
        userDao.insertUser(user)

    suspend fun updateUser(user: UserEntity) =
        userDao.updateUser(user)

    suspend fun updateLocation(userId: Long, lat: Double, lng: Double) =
        userDao.updateLocation(userId, lat, lng, System.currentTimeMillis())

    suspend fun setBlockedStatus(userId: Long, isBlocked: Boolean) =
        userDao.setBlockedStatus(userId, isBlocked)

    suspend fun setVerifiedStatus(userId: Long, isVerified: Boolean) =
        userDao.setVerifiedStatus(userId, isVerified)

    suspend fun setActiveStatus(userId: Long, isActive: Boolean) =
        userDao.setActiveStatus(userId, isActive)

    suspend fun updateBotPersonality(userId: Long, personality: String) =
        userDao.updateBotPersonality(userId, personality)

    suspend fun deleteUser(userId: Long) =
        userDao.deleteUser(userId)

    // Trust System Operations
    suspend fun sendPing(targetUserId: Long) {
        val existing = trustDao.getTrustRelationshipDirect(targetUserId)
        if (existing == null) {
            trustDao.insertTrustRelationship(
                TrustRelationshipEntity(
                    targetUserId = targetUserId,
                    pingStatus = "sent",
                    trustScore = 25
                )
            )
        } else {
            trustDao.updateTrustRelationship(
                existing.copy(pingStatus = "sent", trustScore = max(existing.trustScore, 25), lastUpdated = System.currentTimeMillis())
            )
        }
    }

    suspend fun acceptPing(targetUserId: Long) {
        val existing = trustDao.getTrustRelationshipDirect(targetUserId)
        if (existing != null) {
            trustDao.updateTrustRelationship(
                existing.copy(pingStatus = "accepted", trustScore = max(existing.trustScore, 50), lastUpdated = System.currentTimeMillis())
            )
        } else {
            trustDao.insertTrustRelationship(
                TrustRelationshipEntity(
                    targetUserId = targetUserId,
                    pingStatus = "accepted",
                    trustScore = 50
                )
            )
        }
    }

    suspend fun requestMediaAccess(targetUserId: Long) {
        val existing = trustDao.getTrustRelationshipDirect(targetUserId) ?: TrustRelationshipEntity(targetUserId = targetUserId)
        trustDao.insertTrustRelationship(
            existing.copy(mediaAccessRequested = true, lastUpdated = System.currentTimeMillis())
        )
    }

    suspend fun acceptMediaAccess(targetUserId: Long) {
        val existing = trustDao.getTrustRelationshipDirect(targetUserId) ?: TrustRelationshipEntity(targetUserId = targetUserId)
        trustDao.insertTrustRelationship(
            existing.copy(
                mediaAccessGranted = true,
                mediaAccessRequested = false,
                trustScore = max(existing.trustScore, 75),
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    suspend fun requestIdentityAccess(targetUserId: Long) {
        val existing = trustDao.getTrustRelationshipDirect(targetUserId) ?: TrustRelationshipEntity(targetUserId = targetUserId)
        trustDao.insertTrustRelationship(
            existing.copy(identityAccessRequested = true, lastUpdated = System.currentTimeMillis())
        )
    }

    suspend fun acceptIdentityAccess(targetUserId: Long) {
        val existing = trustDao.getTrustRelationshipDirect(targetUserId) ?: TrustRelationshipEntity(targetUserId = targetUserId)
        trustDao.insertTrustRelationship(
            existing.copy(
                identityAccessGranted = true,
                identityAccessRequested = false,
                trustScore = 100,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    // Chat Operations
    suspend fun sendMessage(message: ChatMessageEntity): Long =
        chatDao.insertMessage(message)

    suspend fun markAsRead(currentUserId: Long, otherUserId: Long) =
        chatDao.markConversationAsRead(currentUserId, otherUserId)

    // Services Operations
    suspend fun insertService(service: LocalServiceEntity): Long =
        serviceDao.insertService(service)

    suspend fun deleteService(service: LocalServiceEntity) =
        serviceDao.deleteService(service)

    // Reports
    suspend fun reportUser(reporterId: Long, targetId: Long, reason: String, details: String): Long {
        return reportDao.insertReport(
            ReportEntity(
                reporterUserId = reporterId,
                reportedUserId = targetId,
                reason = reason,
                details = details
            )
        )
    }

    suspend fun updateReportStatus(reportId: Long, status: String) =
        reportDao.updateReportStatus(reportId, status)

    // Database Seeder
    suspend fun seedInitialDataIfEmpty(currentLat: Double = 25.2048, currentLng: Double = 55.2708) {
        val users = userDao.getUserById(1)
        if (users == null) {
            val seededUsers = listOf(
                UserEntity(
                    id = 1,
                    phone = "+201012345678",
                    name = "طارق المنصور",
                    age = 28,
                    gender = "male",
                    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
                    bio = "مطور تطبيقات ومحب لاستكشاف المقاهي والأماكن التراثية في القاهرة ☕",
                    interests = "تقنية, كافيهات, تصوير, قراءة",
                    latitude = currentLat + 0.0018,
                    longitude = currentLng + 0.0012,
                    whatsapp = "+201012345678",
                    telegram = "@tariq_eg",
                    instagram = "@tariq.cairo",
                    subscriptionType = "premium",
                    isVerified = true,
                    isActive = true
                ),
                UserEntity(
                    id = 2,
                    phone = "+201098765432",
                    name = "سارة إبراهيم",
                    age = 26,
                    gender = "female",
                    avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=400&q=80",
                    bio = "مصممة واجهات ومصورة فوتوغرافية، أبحث عن صناع محتوى ومبدعين للتعاون.",
                    interests = "تصميم, فنون, تصوير, حلويات",
                    latitude = currentLat - 0.0035,
                    longitude = currentLng + 0.0028,
                    whatsapp = "+201098765432",
                    telegram = "@sarah_design",
                    instagram = "@sarah.creatives",
                    subscriptionType = "free",
                    isVerified = true,
                    isActive = true
                ),
                UserEntity(
                    id = 3,
                    phone = "+201144332211",
                    name = "كابتن زياد الحسيني",
                    age = 31,
                    gender = "male",
                    avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80",
                    bio = "مدرب لياقة بدنية ومنظم مباريات كرة قدم أسبوعية في المعادي والمهندسين!",
                    interests = "كرة قدم, جيم, رياضة, جري",
                    latitude = currentLat + 0.0095,
                    longitude = currentLng - 0.0065,
                    whatsapp = "+201144332211",
                    telegram = "@coach_zaid",
                    instagram = "@zaid_fit",
                    subscriptionType = "business",
                    isVerified = true,
                    isActive = true
                ),
                UserEntity(
                    id = 4,
                    phone = "+201255667788",
                    name = "نور الصباح",
                    age = 24,
                    gender = "female",
                    avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=400&q=80",
                    bio = "عاشقة للروايات والقهوة وصناعة الحلويات الشرقية المنزلية 🍰",
                    interests = "كتب, حلويات, طبخ, موسيقى",
                    latitude = currentLat - 0.0080,
                    longitude = currentLng - 0.0040,
                    whatsapp = "+201255667788",
                    telegram = "@nour_reads",
                    instagram = "@nour_books",
                    subscriptionType = "free",
                    isVerified = true,
                    isActive = true
                )
            )
            userDao.insertUsers(seededUsers)

            // Seed initial trust relationships
            trustDao.insertTrustRelationship(
                TrustRelationshipEntity(
                    id = 1,
                    targetUserId = 1,
                    pingStatus = "accepted",
                    mediaAccessGranted = true,
                    identityAccessGranted = false,
                    trustScore = 70
                )
            )
            trustDao.insertTrustRelationship(
                TrustRelationshipEntity(
                    id = 2,
                    targetUserId = 6, // Bot
                    pingStatus = "accepted",
                    mediaAccessGranted = true,
                    identityAccessGranted = true,
                    trustScore = 100
                )
            )

            // Seed initial messages
            val now = System.currentTimeMillis()
            val initialMessages = listOf(
                ChatMessageEntity(
                    senderId = 6,
                    receiverId = 56, // Current user id placeholder
                    message = "مرحباً بك في GeoConnect! 📍 أنا مساعدك الذكي الافتراضي. يمكنك أن تسألني عن أي شخص قريب باهتمامات معينة أو خدمات محلية مثل الكهربائيين والمطاعم وسيارات الأجرة.",
                    timestamp = now - 3600000,
                    isRead = true
                ),
                ChatMessageEntity(
                    senderId = 1,
                    receiverId = 56,
                    message = "Hey! Saw we're both in the area and interested in Tech & Coffee. Have you checked out the cafe nearby?",
                    timestamp = now - 1800000,
                    isRead = false
                )
            )
            initialMessages.forEach { chatDao.insertMessage(it) }

            // Seed initial services and local stores with Google Maps details
            val services = listOf(
                LocalServiceEntity(
                    id = 1,
                    title = "🍰 حلواني ومخبز بنحليها الملكي",
                    category = "sweets",
                    description = "أشهى الحلويات الشرقية والغربية، كنافة نابلسية، بسبوسة بالسمن البلدي، وتورتات للمناسبات.",
                    providerName = "الشيف أحمد الحلواني",
                    phone = "+201012345678",
                    rating = 4.9f,
                    reviewsCount = 480,
                    latitude = currentLat + 0.0015,
                    longitude = currentLng + 0.0010,
                    priceRange = "ج.م $$",
                    isVerified = true,
                    tags = "حلويات, كنافة, بسبوسة, مخبز, تورتات, بنحليها",
                    address = "شارع الجمهورية، وسط البلد، القاهرة",
                    openingHours = "8:00 AM - 12:00 AM",
                    googleMapsUrl = "https://maps.google.com/?q=حلواني+بنحليها",
                    isGoogleImported = true
                ),
                LocalServiceEntity(
                    id = 2,
                    title = "☕ مقهى ومحمصة الأهرام التراثي",
                    category = "food",
                    description = "قهوة مختصة، شاي بالنعناع، فطور شرقي وفطير مشلتت طازج يومياً.",
                    providerName = "عم صبحي",
                    phone = "+201098765432",
                    rating = 4.8f,
                    reviewsCount = 320,
                    latitude = currentLat + 0.0028,
                    longitude = currentLng - 0.0018,
                    priceRange = "ج.م $",
                    isVerified = true,
                    tags = "مقهى, قهوة مختصة, فطور, عصائر, كافيه",
                    address = "ميدان التحرير، القاهرة",
                    openingHours = "7:00 AM - 1:00 AM",
                    googleMapsUrl = "https://maps.google.com/?q=مقهى+الأهرام",
                    isGoogleImported = true
                ),
                LocalServiceEntity(
                    id = 3,
                    title = "⚡ مركز النيل للصيانة والكهرباء والتكييف",
                    category = "tech",
                    description = "صيانة منزلية سريعة 24/7، تأسيس كهرباء، شحن وصيانة تكييفات وسباكة حديثة مع ضمان معتمد.",
                    providerName = "م. كريم المصري",
                    phone = "+201122334455",
                    rating = 4.9f,
                    reviewsCount = 195,
                    latitude = currentLat - 0.0035,
                    longitude = currentLng + 0.0022,
                    priceRange = "ج.م $$",
                    isVerified = true,
                    tags = "كهربائي, تكييف, صيانة, طوارئ, سباكة",
                    address = "شارع النصر، المعادي، القاهرة",
                    openingHours = "متاح 24 ساعة يومياً",
                    googleMapsUrl = "https://maps.google.com/?q=مركز+النيل+للصيانة",
                    isGoogleImported = true
                ),
                LocalServiceEntity(
                    id = 4,
                    title = "🛒 سوبرماركت وخضار الواحة الطازج",
                    category = "groceries",
                    description = "كافة السلع التموينية، أجبان ريفية، فواكه وخضروات طازجة مع خدمة التوصيل السريع للمنازل.",
                    providerName = "الحاج محمود",
                    phone = "+201233445566",
                    rating = 4.7f,
                    reviewsCount = 210,
                    latitude = currentLat + 0.0045,
                    longitude = currentLng + 0.0035,
                    priceRange = "ج.م $$",
                    isVerified = true,
                    tags = "سوبرماركت, خضروات, بقالة, توصيل منزلي",
                    address = "شارع جامعة الدول العربية، المهندسين، الجيزة",
                    openingHours = "24 ساعة",
                    googleMapsUrl = "https://maps.google.com/?q=سوبرماركت+الواحة",
                    isGoogleImported = true
                ),
                LocalServiceEntity(
                    id = 5,
                    title = "🏥 صيدلية الشفاء والعناية الطبية",
                    category = "health",
                    description = "أدوية ومستحضرات تجميل، قياس سكر وضغط مجاناً، وخدمة توصيل الروشتات على مدار الساعة.",
                    providerName = "د. سارة خليل",
                    phone = "+201055667788",
                    rating = 4.9f,
                    reviewsCount = 540,
                    latitude = currentLat - 0.0040,
                    longitude = currentLng - 0.0030,
                    priceRange = "ج.م $$",
                    isVerified = true,
                    tags = "صيدلية, أدوية, عناية, استشارات طبية, طوارئ",
                    address = "شارع عباس العقاد، مدينة نصر، القاهرة",
                    openingHours = "خدمة 24 ساعة",
                    googleMapsUrl = "https://maps.google.com/?q=صيدلية+الشفاء",
                    isGoogleImported = true
                ),
                LocalServiceEntity(
                    id = 6,
                    title = "🚕 كابتن مصر للنقل والتوصيل السريع",
                    category = "transport",
                    description = "توصيل طلبات ومشاوير سريعة داخل القاهرة والجيزة، وسيارات مكيفة بأسعار مميزة.",
                    providerName = "كابتن طارق",
                    phone = "+201088990011",
                    rating = 4.8f,
                    reviewsCount = 164,
                    latitude = currentLat + 0.0060,
                    longitude = currentLng - 0.0040,
                    priceRange = "ج.م $",
                    isVerified = true,
                    tags = "توصيل, مشاوير, تاكسي, نقل عفش, دليفري",
                    address = "القاهرة والجيزة",
                    openingHours = "6:00 AM - 2:00 AM",
                    googleMapsUrl = "https://maps.google.com/?q=خدمات+نقل+القاهرة",
                    isGoogleImported = false
                ),
                LocalServiceEntity(
                    id = 7,
                    title = "📱 متجر المستقبل للإلكترونيات والموبايل",
                    category = "shops",
                    description = "أحدث الهواتف الذكية، إكسسوارات أصلية، وصيانة فورية للشاشات والبطاريات بقطع أصلية.",
                    providerName = "المهندس هاني",
                    phone = "+201144556677",
                    rating = 4.8f,
                    reviewsCount = 280,
                    latitude = currentLat - 0.0020,
                    longitude = currentLng + 0.0040,
                    priceRange = "ج.م $$$",
                    isVerified = true,
                    tags = "موبايلات, صيانة إلكترونيات, شواحن, هواتف, أجهزة",
                    address = "شارع مصدق، الدقي، الجيزة",
                    openingHours = "10:00 AM - 11:30 PM",
                    googleMapsUrl = "https://maps.google.com/?q=متجر+المستقبل+للموبايلات",
                    isGoogleImported = true
                ),
                LocalServiceEntity(
                    id = 8,
                    title = "👗 جاليري الأصالة للأزياء والعبايات",
                    category = "shops",
                    description = "أحدث صيحات الملابس الراقية، عبايات خليجية ومصرية، وتفصيل وتطريز حسب الطلب.",
                    providerName = "مدام نورهان",
                    phone = "+201077889922",
                    rating = 4.9f,
                    reviewsCount = 175,
                    latitude = currentLat + 0.0035,
                    longitude = currentLng - 0.0050,
                    priceRange = "ج.م $$",
                    isVerified = true,
                    tags = "ملابس, أزياء, فساتين, عبايات, خياطة",
                    address = "شارع الهرم، الجيزة",
                    openingHours = "11:00 AM - 11:00 PM",
                    googleMapsUrl = "https://maps.google.com/?q=جاليري+الأصالة",
                    isGoogleImported = true
                )
            )
            serviceDao.insertServices(services)
        }
    }
}
