package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.LocalServiceEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey = BuildConfig.GEMINI_API_KEY

    suspend fun getAiRecommendation(
        userQuery: String,
        nearbyUsers: List<UserEntity>,
        nearbyServices: List<LocalServiceEntity>
    ): Pair<String, List<Long>> = withContext(Dispatchers.IO) {
        // First check if user query can be matched locally or via Gemini
        val promptContext = buildString {
            appendLine("You are GeoConnect's intelligent Geo-Social AI Concierge bot. The user is asking: \"$userQuery\"")
            appendLine("Here is the list of currently available nearby people and services in the database:")
            appendLine("PEOPLE:")
            nearbyUsers.forEach { user ->
                appendLine("- ID: ${user.id}, Name: ${user.name}, Age: ${user.age}, Interests: ${user.interests}, Bio: ${user.bio}")
            }
            appendLine("SERVICES:")
            nearbyServices.forEach { service ->
                appendLine("- ID: ${service.id}, Title: ${service.title}, Category: ${service.category}, Provider: ${service.providerName}, Rating: ${service.rating}, Tags: ${service.tags}")
            }
            appendLine("Provide a friendly, helpful, concise answer in the user's language (Arabic or English) recommending the most suitable people or services. Mention their names and why they are a great match.")
        }

        if (apiKey.isNotBlank() && !apiKey.contains("MY_GEMINI_API_KEY")) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
                val jsonBody = JSONObject().apply {
                    val contents = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val parts = JSONArray().apply {
                                put(JSONObject().put("text", promptContext))
                            }
                            put("parts", parts)
                        }
                        put(contentObj)
                    }
                    put("contents", contents)
                }

                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseStr = response.body?.string() ?: ""
                    val rootJson = JSONObject(responseStr)
                    val candidates = rootJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val text = parts?.optJSONObject(0)?.optString("text")
                        if (!text.isNullOrBlank()) {
                            // Extract matched user IDs
                            val matchedUserIds = nearbyUsers.filter { user ->
                                text.contains(user.name, ignoreCase = true) ||
                                        user.interests.split(",").any { interest ->
                                            interest.isNotBlank() && text.contains(interest.trim(), ignoreCase = true)
                                        }
                            }.map { it.id }
                            return@withContext Pair(text, matchedUserIds)
                        }
                    }
                } else {
                    Log.w("GeminiAiService", "API call returned ${response.code}: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("GeminiAiService", "Gemini call failed, falling back to smart local matching", e)
            }
        }

        // Smart Local Matcher Engine (NLP intent parsing)
        val queryLower = userQuery.lowercase()
        val isArabic = userQuery.any { it in '\u0600'..'\u06FF' }

        // Find people matching interests or name
        val matchedPeople = nearbyUsers.filter { user ->
            val interestsList = user.interests.lowercase().split(",").map { it.trim() }
            interestsList.any { queryLower.contains(it) } ||
                    queryLower.contains(user.name.lowercase()) ||
                    (queryLower.contains("رياض") || queryLower.contains("قدم") || queryLower.contains("sport") || queryLower.contains("football")) && user.interests.contains("Football", ignoreCase = true) ||
                    (queryLower.contains("برمج") || queryLower.contains("تقني") || queryLower.contains("كود") || queryLower.contains("code") || queryLower.contains("tech")) && user.interests.contains("Technology", ignoreCase = true) ||
                    (queryLower.contains("تصميم") || queryLower.contains("صورة") || queryLower.contains("design") || queryLower.contains("photo")) && user.interests.contains("Design", ignoreCase = true) ||
                    (queryLower.contains("قهوة") || queryLower.contains("coffee") || queryLower.contains("كافيه")) && user.interests.contains("Coffee", ignoreCase = true) ||
                    (queryLower.contains("كتاب") || queryLower.contains("قراءة") || queryLower.contains("book")) && user.interests.contains("Books", ignoreCase = true)
        }

        // Find services matching query
        val matchedServices = nearbyServices.filter { service ->
            queryLower.contains(service.title.lowercase()) ||
                    queryLower.contains(service.category.lowercase()) ||
                    queryLower.contains(service.tags.lowercase()) ||
                    (queryLower.contains("كهرب") || queryLower.contains("electric")) && service.category == "tech" ||
                    (queryLower.contains("أكل") || queryLower.contains("مطعم") || queryLower.contains("برجر") || queryLower.contains("food") || queryLower.contains("burger")) && service.category == "food" ||
                    (queryLower.contains("تاكسي") || queryLower.contains("توصيل") || queryLower.contains("نقل") || queryLower.contains("taxi") || queryLower.contains("ride")) && service.category == "transport" ||
                    (queryLower.contains("طبيب") || queryLower.contains("صحة") || queryLower.contains("عيادة") || queryLower.contains("doctor") || queryLower.contains("clinic")) && service.category == "health" ||
                    (queryLower.contains("تدريس") || queryLower.contains("معلم") || queryLower.contains("رياضيات") || queryLower.contains("tutor") || queryLower.contains("math")) && service.category == "education"
        }

        val responseText = if (isArabic) {
            buildString {
                if (matchedPeople.isNotEmpty() || matchedServices.isNotEmpty()) {
                    append("بناءً على طلبك وموقعك الجغرافي الحالي 📍، إليك أفضل النتائج المقترحة حولك:\n\n")
                    if (matchedPeople.isNotEmpty()) {
                        append("👤 الأشخاص القريبون:\n")
                        matchedPeople.forEach { u ->
                            append("• ${u.name} (${u.age} سنة) - اهتماماته: ${u.interests}\n")
                        }
                        append("\n")
                    }
                    if (matchedServices.isNotEmpty()) {
                        append("⚡ الخدمات المحلية القريبة:\n")
                        matchedServices.forEach { s ->
                            append("• ${s.title} (تقييم ${s.rating} ⭐) - مزود الخدمة: ${s.providerName}\n")
                        }
                    }
                    append("\nيمكنك الضغط على أي بطاقة للتواصل الفوري أو إرسال Ping وثقة.")
                } else {
                    append("بحثت حول موقعك الحالي عن \"$userQuery\"! يمكنك استعراض الرادار لاكتشاف الأشخاص الأقرب، أو تجربة البحث عن كلمات مثل 'رياضة'، 'برمجة'، 'قهوة'، 'كهربائي'، أو 'مطعم'.")
                }
            }
        } else {
            buildString {
                if (matchedPeople.isNotEmpty() || matchedServices.isNotEmpty()) {
                    append("Based on your request and live location 📍, here are the best matches around you:\n\n")
                    if (matchedPeople.isNotEmpty()) {
                        append("👤 Nearby People:\n")
                        matchedPeople.forEach { u ->
                            append("• ${u.name} (${u.age} yrs) - Interests: ${u.interests}\n")
                        }
                        append("\n")
                    }
                    if (matchedServices.isNotEmpty()) {
                        append("⚡ Local Services:\n")
                        matchedServices.forEach { s ->
                            append("• ${s.title} (Rating: ${s.rating} ⭐) - By ${s.providerName}\n")
                        }
                    }
                    append("\nYou can tap on their cards to chat or send a Trust Ping!")
                } else {
                    append("I searched around your area for \"$userQuery\"! Try searching for categories like 'Football', 'Coding', 'Electrician', 'Coffee', or 'Taxi'!")
                }
            }
        }

        val allMatchedIds = matchedPeople.map { it.id }
        Pair(responseText, allMatchedIds)
    }
}
