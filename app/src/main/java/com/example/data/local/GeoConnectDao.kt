package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isBlocked = 0 AND isActive = 1 ORDER BY id DESC")
    fun getAllActiveUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE isBlocked = 1")
    fun getBlockedUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isBot = 1")
    fun getBotUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY id DESC")
    fun getAllUsersAdmin(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isBlocked = :isBlocked WHERE id = :userId")
    suspend fun setBlockedStatus(userId: Long, isBlocked: Boolean)

    @Query("UPDATE users SET isVerified = :isVerified WHERE id = :userId")
    suspend fun setVerifiedStatus(userId: Long, isVerified: Boolean)

    @Query("UPDATE users SET isActive = :isActive WHERE id = :userId")
    suspend fun setActiveStatus(userId: Long, isActive: Boolean)

    @Query("UPDATE users SET botPersonality = :personality WHERE id = :userId")
    suspend fun updateBotPersonality(userId: Long, personality: String)

    @Query("UPDATE users SET latitude = :lat, longitude = :lng, locationUpdatedAt = :time WHERE id = :userId")
    suspend fun updateLocation(userId: Long, lat: Double, lng: Double, time: Long)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: Long)
}

@Dao
interface TrustDao {
    @Query("SELECT * FROM trust_relationships WHERE targetUserId = :targetId LIMIT 1")
    fun getTrustRelationship(targetId: Long): Flow<TrustRelationshipEntity?>

    @Query("SELECT * FROM trust_relationships WHERE targetUserId = :targetId LIMIT 1")
    suspend fun getTrustRelationshipDirect(targetId: Long): TrustRelationshipEntity?

    @Query("SELECT * FROM trust_relationships")
    fun getAllTrustRelationships(): Flow<List<TrustRelationshipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrustRelationship(rel: TrustRelationshipEntity): Long

    @Update
    suspend fun updateTrustRelationship(rel: TrustRelationshipEntity)
}

@Dao
interface ChatDao {
    @Query("""
        SELECT * FROM chat_messages 
        WHERE (senderId = :userId1 AND receiverId = :userId2) 
           OR (senderId = :userId2 AND receiverId = :userId1)
        ORDER BY timestamp ASC
    """)
    fun getConversationMessages(userId1: Long, userId2: Long): Flow<List<ChatMessageEntity>>

    @Query("""
        SELECT * FROM chat_messages 
        WHERE id IN (
            SELECT MAX(id) FROM chat_messages 
            WHERE senderId = :currentUserId OR receiverId = :currentUserId
            GROUP BY CASE WHEN senderId = :currentUserId THEN receiverId ELSE senderId END
        )
        ORDER BY timestamp DESC
    """)
    fun getRecentConversations(currentUserId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE receiverId = :currentUserId AND isRead = 0")
    fun getUnreadCount(currentUserId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET isRead = 1 WHERE receiverId = :currentUserId AND senderId = :otherUserId")
    suspend fun markConversationAsRead(currentUserId: Long, otherUserId: Long)
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM local_services ORDER BY rating DESC")
    fun getAllServices(): Flow<List<LocalServiceEntity>>

    @Query("SELECT * FROM local_services WHERE category = :category ORDER BY rating DESC")
    fun getServicesByCategory(category: String): Flow<List<LocalServiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: LocalServiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<LocalServiceEntity>)

    @Delete
    suspend fun deleteService(service: LocalServiceEntity)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity): Long

    @Query("UPDATE reports SET status = :status WHERE id = :reportId")
    suspend fun updateReportStatus(reportId: Long, status: String)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM service_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<ServiceOrderEntity>>

    @Query("SELECT * FROM service_orders WHERE senderUserId = :userId OR recipientUserId = :userId ORDER BY createdAt DESC")
    fun getOrdersForUser(userId: Long): Flow<List<ServiceOrderEntity>>

    @Query("SELECT * FROM service_orders WHERE recipientUserId = :userId AND isGift = 1 ORDER BY createdAt DESC")
    fun getGiftsReceivedByUser(userId: Long): Flow<List<ServiceOrderEntity>>

    @Query("SELECT * FROM service_orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: Long): ServiceOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: ServiceOrderEntity): Long

    @Update
    suspend fun updateOrder(order: ServiceOrderEntity)

    @Query("UPDATE service_orders SET orderStatus = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: String)

    @Query("UPDATE service_orders SET paymentStatus = :paymentStatus, paymentReference = :ref WHERE id = :orderId")
    suspend fun updatePaymentStatus(orderId: Long, paymentStatus: String, ref: String)
}

