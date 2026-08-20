package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phone: String,
    val name: String,
    val age: Int,
    val gender: String, // "male", "female", "other"
    val avatarUrl: String = "",
    val bio: String = "",
    val interests: String = "", // Comma-separated tags (e.g. "Football, Technology, Coffee, Photography")
    val latitude: Double,
    val longitude: Double,
    val locationUpdatedAt: Long = System.currentTimeMillis(),
    val whatsapp: String = "",
    val telegram: String = "",
    val instagram: String = "",
    val phoneNumber: String = "",
    val subscriptionType: String = "free", // "free", "premium", "business"
    val subscriptionExpiresAt: Long = 0L,
    val isBot: Boolean = false,
    val botPersonality: String = "", // "Friendly Local", "Tech Guide", "Sports Buddy", "Local Foodie"
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
    val isBlocked: Boolean = false,
    val isAdmin: Boolean = false,
    val isStoreOwner: Boolean = false,
    val role: String = "general_manager", // "general_manager", "store_owner", "member"
    val createdAt: Long = System.currentTimeMillis()
) {
    val isGeneralManager: Boolean
        get() = role.equals("general_manager", ignoreCase = true) ||
                role.equals("General Manager", ignoreCase = true) ||
                isAdmin
}

@Entity(tableName = "trust_relationships")
data class TrustRelationshipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetUserId: Long,
    val pingStatus: String = "none", // "none", "sent", "received", "accepted", "rejected"
    val mediaAccessGranted: Boolean = false,
    val mediaAccessRequested: Boolean = false,
    val identityAccessGranted: Boolean = false,
    val identityAccessRequested: Boolean = false,
    val trustScore: Int = 10, // 0 - 100
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderId: Long,
    val receiverId: Long,
    val message: String,
    val mediaUrl: String = "",
    val messageType: String = "text", // "text", "image", "location", "ping", "media_request", "identity_request"
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_services")
data class LocalServiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String, // "sweets", "food", "shops", "tech", "transport", "health", "crafts", "groceries", "services"
    val description: String,
    val providerName: String,
    val phone: String,
    val rating: Float = 4.8f,
    val reviewsCount: Int = 12,
    val latitude: Double,
    val longitude: Double,
    val priceRange: String = "$$",
    val isVerified: Boolean = true,
    val tags: String = "",
    val address: String = "القاهرة، مصر",
    val openingHours: String = "9:00 AM - 11:00 PM",
    val googleMapsUrl: String = "",
    val isGoogleImported: Boolean = false
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reporterUserId: Long,
    val reportedUserId: Long,
    val reason: String,
    val details: String = "",
    val status: String = "pending", // "pending", "reviewed", "resolved", "dismissed"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "service_orders")
data class ServiceOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serviceId: Long,
    val serviceTitle: String,
    val providerName: String,
    val senderUserId: Long,
    val senderName: String,
    val recipientUserId: Long? = null, // null if self-ordered, non-null if gifted to another user
    val recipientName: String = "",
    val recipientPhone: String = "",
    val deliveryAddress: String = "",
    val giftGreeting: String = "", // e.g. "ألف مبروك النجاح يا غالي! 🎁"
    val orderItems: String, // item details or service requested
    val orderPrice: Double, // in EGP
    val paymentGateway: String = "fawry", // "fawry", "vodafone_cash", "instapay", "card", "cash"
    val paymentStatus: String = "paid", // "paid", "pending", "refunded"
    val paymentReference: String = "",
    val orderStatus: String = "preparing", // "preparing", "out_for_delivery", "delivered", "cancelled"
    val isGift: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

