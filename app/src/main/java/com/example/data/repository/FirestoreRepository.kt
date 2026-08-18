package com.example.data.repository

import com.example.data.model.UserEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val usersCollection get() = firestore.collection("users")
    private val locationsCollection get() = firestore.collection("locations")

    suspend fun saveUserProfile(user: UserEntity): Result<Unit> {
        return try {
            val userMap = hashMapOf(
                "id" to user.id,
                "name" to user.name,
                "phone" to user.phone,
                "age" to user.age,
                "gender" to user.gender,
                "bio" to user.bio,
                "interests" to user.interests,
                "avatarUrl" to user.avatarUrl,
                "subscriptionType" to user.subscriptionType,
                "isVerified" to user.isVerified,
                "latitude" to user.latitude,
                "longitude" to user.longitude,
                "updatedAt" to System.currentTimeMillis()
            )
            usersCollection.document(user.id.toString())
                .set(userMap, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncUserLocation(userId: Long, lat: Double, lng: Double): Result<Unit> {
        return try {
            val locMap = hashMapOf(
                "userId" to userId,
                "latitude" to lat,
                "longitude" to lng,
                "timestamp" to System.currentTimeMillis()
            )
            locationsCollection.document(userId.toString())
                .set(locMap, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeNearbyLocations(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = locationsCollection.limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }
}
