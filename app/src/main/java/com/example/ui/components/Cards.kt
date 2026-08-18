package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.LocalServiceEntity
import com.example.data.model.TrustRelationshipEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.*

@Composable
fun UserCard(
    user: UserEntity,
    distanceStr: String,
    trust: TrustRelationshipEntity?,
    onCardClick: () -> Unit,
    onPingClick: () -> Unit,
    onChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("user_card_${user.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(
                    if (user.isBot) GeoPurpleAI.copy(alpha = 0.5f)
                    else if (user.subscriptionType == "business") GeoGold.copy(alpha = 0.5f)
                    else GeoTealPrimary.copy(alpha = 0.25f),
                    GeoDarkOutline
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar with online / verified ring
                Box {
                    if (user.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = user.name,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (user.isVerified) GeoTealPrimary else Color.Transparent, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    if (user.isBot) GeoPurpleAI.copy(alpha = 0.15f)
                                    else if (user.gender == "female") GeoMagentaTertiary.copy(alpha = 0.15f)
                                    else GeoTealContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (user.isBot) "🤖" else user.name.take(1),
                                color = if (user.isBot) GeoPurpleAI else if (user.gender == "female") GeoMagentaTertiary else GeoTealPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        }
                    }

                    // Online indicator
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(GeoGreenOnline)
                            .border(2.dp, GeoDarkSurface, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${user.name}, ${user.age}",
                            color = GeoDarkOnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (user.isVerified) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = GeoTealPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (user.subscriptionType == "business") {
                            Surface(
                                color = GeoGold.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    color = GeoGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFFF1F3F9),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = GeoAzureSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = distanceStr,
                                    color = GeoAzureSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (user.isBot) {
                            Text(
                                text = "🤖 AI Virtual Bot",
                                color = GeoPurpleAI,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (user.bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = user.bio,
                    color = Color(0xFF49454F),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            // Interest Chips
            if (user.interests.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    user.interests.split(",").take(3).forEach { interest ->
                        val tag = interest.trim()
                        if (tag.isNotEmpty()) {
                            Surface(
                                color = Color(0xFFF1F3F9),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    color = Color(0xFF5A6175),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = GeoDarkOutline)
            Spacer(modifier = Modifier.height(12.dp))

            // Action row & Trust status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrustLevelBadge(trust = trust)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Ping / Handshake button
                    val isPingAccepted = trust?.pingStatus == "accepted"
                    val isPingSent = trust?.pingStatus == "sent"

                    OutlinedButton(
                        onClick = { if (!isPingAccepted && !isPingSent) onPingClick() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isPingAccepted) GeoTealPrimary else if (isPingSent) Color.Gray else GeoDarkOnSurface
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(GeoDarkOutline, GeoDarkOutline))
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isPingAccepted) Icons.Default.Check else Icons.Default.FavoriteBorder,
                            contentDescription = "Ping",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPingAccepted) "Connected" else if (isPingSent) "Pinged" else "Ping",
                            fontSize = 12.sp
                        )
                    }

                    // Chat Button
                    Button(
                        onClick = onChatClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Chat",
                            tint = GeoTealOnPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Chat",
                            color = GeoTealOnPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceCard(
    service: LocalServiceEntity,
    distanceStr: String,
    onCallClick: () -> Unit,
    onChatClick: () -> Unit,
    onGoogleMapsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("service_card_${service.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                if (service.isGoogleImported) listOf(Color(0xFF4285F4).copy(alpha = 0.35f), GeoDarkOutline)
                else listOf(GeoTealPrimary.copy(alpha = 0.25f), GeoDarkOutline)
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Icon Badge
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when (service.category) {
                                "sweets" -> Color(0xFFFDE8E8)
                                "food" -> Color(0xFFFEF3C7)
                                "tech" -> Color(0xFFEADDFF)
                                "transport" -> Color(0xFFDBEAFE)
                                "health" -> Color(0xFFD1FAE5)
                                "groceries" -> Color(0xFFDCFCE7)
                                "shops" -> Color(0xFFFCE7F3)
                                else -> Color(0xFFF1F5F9)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (service.category) {
                            "sweets" -> "🍰"
                            "food" -> "🍔"
                            "tech" -> "⚡"
                            "transport" -> "🚕"
                            "health" -> "🏥"
                            "groceries" -> "🛒"
                            "shops" -> "🛍️"
                            "crafts" -> "🔨"
                            else -> "📍"
                        },
                        fontSize = 26.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = service.title,
                            color = GeoDarkOnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (service.isGoogleImported) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFE8F0FE),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Place,
                                        contentDescription = "Google Maps",
                                        tint = Color(0xFF1A73E8),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Google",
                                        color = Color(0xFF1A73E8),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "المسؤول: ${service.providerName}",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Rating
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = GeoGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${service.rating} (${service.reviewsCount})",
                                color = GeoGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Distance
                        Text("•", color = Color(0xFFCBD5E1))
                        Text(
                            text = distanceStr,
                            color = GeoTealPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Price
                        Text("•", color = Color(0xFFCBD5E1))
                        Text(
                            text = service.priceRange,
                            color = GeoGreenOnline,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = service.description,
                color = Color(0xFF49454F),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            // Address & Open Hours
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = service.address,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = GeoTealPrimary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = service.openingHours,
                            color = GeoDarkOnSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (service.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    service.tags.split(",").take(3).forEach { tag ->
                        Surface(
                            color = Color(0xFFF1F3F9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "#${tag.trim()}",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCallClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoGreenOnline),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(GeoDarkOutline, GeoDarkOutline))
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "اتصال",
                        tint = GeoGreenOnline,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "اتصال", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onChatClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "محادثة",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "محادثة",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onGoogleMapsClick,
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1A73E8)),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(Color(0xFFBFDBFE), Color(0xFFBFDBFE)))
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "خرائط Google",
                        tint = Color(0xFF1A73E8),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "خريطة Google", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

