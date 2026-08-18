package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ChatMessageEntity
import com.example.data.model.UserEntity
import com.example.ui.components.TrustLevelBadge
import com.example.ui.theme.*
import com.example.viewmodel.GeoConnectViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatListScreen(
    viewModel: GeoConnectViewModel,
    onOpenConversation: (UserEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val recentConversations by viewModel.recentConversations.collectAsStateWithLifecycle()
    val activeUsers by viewModel.activeUsers.collectAsStateWithLifecycle()
    val allTrusts by viewModel.allTrusts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GeoDarkBackground)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "المحادثات المباشرة",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeoDarkOnSurface
                )
                Text(
                    text = "تواصل فوري آمن ومحمي بنظام الثقة",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            Surface(
                color = Color(0xFFF1F3F9),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GeoGreenOnline))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Socket Live", color = GeoTealPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active Users Quick Avatars Bar
        Text(
            text = "الأشخاص القريبون المتصلون الآن",
            color = Color(0xFF64748B),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(activeUsers.filter { it.id != (currentUser?.id ?: 56L) }) { user ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            viewModel.openChatWith(user)
                            onOpenConversation(user)
                        }
                ) {
                    Box {
                        if (user.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = user.name,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, if (user.isBot) GeoPurpleAI else GeoTealPrimary, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(if (user.isBot) GeoPurpleAI.copy(alpha = 0.15f) else GeoTealContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (user.isBot) "🤖" else user.name.take(1), color = if (user.isBot) GeoPurpleAI else GeoTealPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(GeoGreenOnline)
                                .border(2.dp, GeoDarkSurface, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user.name.split(" ").firstOrNull() ?: user.name,
                        color = GeoDarkOnSurface,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = GeoDarkOutline)

        // Conversations List
        if (activeUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد محادثات نشطة بعد", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(activeUsers.filter { it.id != (currentUser?.id ?: 56L) }, key = { it.id }) { user ->
                    val userTrust = allTrusts.firstOrNull { it.targetUserId == user.id }
                    val lastMsg = recentConversations.firstOrNull { it.senderId == user.id || it.receiverId == user.id }

                    Surface(
                        color = GeoDarkSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.openChatWith(user)
                                onOpenConversation(user)
                            }
                            .testTag("conversation_item_${user.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Avatar
                            Box {
                                if (user.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = user.avatarUrl,
                                        contentDescription = user.name,
                                        modifier = Modifier.size(48.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(if (user.isBot) GeoPurpleAI.copy(alpha = 0.15f) else GeoTealContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(if (user.isBot) "🤖" else user.name.take(1), color = if (user.isBot) GeoPurpleAI else GeoTealPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = user.name,
                                        color = GeoDarkOnSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (lastMsg != null) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastMsg.timestamp)) else "Online",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = lastMsg?.message ?: user.bio.ifBlank { "انقر لبدء المحادثة وإرسال Ping" },
                                        color = if (lastMsg != null && !lastMsg.isRead && lastMsg.receiverId == currentUser?.id) GeoDarkOnSurface else Color(0xFF64748B),
                                        fontWeight = if (lastMsg != null && !lastMsg.isRead && lastMsg.receiverId == currentUser?.id) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    TrustLevelBadge(trust = userTrust)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    viewModel: GeoConnectViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeChatUser by viewModel.activeChatUser.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val chatInput by viewModel.chatInput.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allTrusts by viewModel.allTrusts.collectAsStateWithLifecycle()
    val userLat by viewModel.userLat.collectAsStateWithLifecycle()
    val userLng by viewModel.userLng.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val listState = rememberLazyListState()

    val user = activeChatUser ?: return
    val userTrust = allTrusts.firstOrNull { it.targetUserId == user.id }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GeoDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Chat Header
        Surface(
            color = GeoDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GeoDarkOnSurface)
                }

                Box {
                    if (user.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = user.name,
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (user.isBot) GeoPurpleAI.copy(alpha = 0.15f) else GeoTealContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (user.isBot) "🤖" else user.name.take(1), color = if (user.isBot) GeoPurpleAI else GeoTealPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(GeoGreenOnline).border(1.5.dp, GeoDarkSurface, CircleShape).align(Alignment.BottomEnd))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        color = GeoDarkOnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                    Text(
                        text = if (user.isBot) "AI Bot • Always Online" else "📍 ${viewModel.formatDistance(viewModel.calculateDistanceMeters(userLat, userLng, user.latitude, user.longitude))} away",
                        color = GeoAzureSecondary,
                        fontSize = 11.sp
                    )
                }

                // Trust Status & Call
                TrustLevelBadge(trust = userTrust)

                if (userTrust?.identityAccessGranted == true && user.phone.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.phone}"))
                            context.startActivity(dialIntent)
                        }
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = GeoGreenOnline)
                    }
                }
            }
        }

        // Quick Trust Actions Strip
        Surface(
            color = Color(0xFFF1F3F9),
            border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Send Location
                AssistChip(
                    onClick = {
                        viewModel.sendChatMessage("📍 شاركت موقعي الجغرافي المباشر: %.4f, %.4f".format(userLat, userLng), messageType = "location")
                    },
                    label = { Text("إرسال موقعي 📍", fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = GeoDarkSurface, labelColor = GeoDarkOnSurface),
                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = GeoDarkOutline)
                )

                // Ping / Handshake
                val pingAccepted = userTrust?.pingStatus == "accepted"
                AssistChip(
                    onClick = { viewModel.sendPing(user.id) },
                    enabled = !pingAccepted,
                    label = { Text(if (pingAccepted) "متصل 👋" else "إرسال Ping 👋", fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (pingAccepted) GeoTealPrimary.copy(alpha = 0.15f) else GeoDarkSurface,
                        labelColor = if (pingAccepted) GeoTealPrimary else GeoDarkOnSurface
                    ),
                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = GeoDarkOutline)
                )

                // Request Media
                val mediaGranted = userTrust?.mediaAccessGranted == true
                AssistChip(
                    onClick = {
                        if (!mediaGranted) {
                            viewModel.requestMediaAccess(user.id)
                            viewModel.sendChatMessage("📷 طلبت إذن مشاركة الصور والوسائط المتبادلة", messageType = "media_request")
                        }
                    },
                    label = { Text(if (mediaGranted) "الصور مفعلة 📷" else "طلب صور 📷", fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (mediaGranted) GeoAzureSecondary.copy(alpha = 0.15f) else GeoDarkSurface,
                        labelColor = if (mediaGranted) GeoAzureSecondary else GeoDarkOnSurface
                    ),
                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = GeoDarkOutline)
                )
            }
        }

        // Messages Stream
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                val isMe = msg.senderId == (currentUser?.id ?: 56L)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isMe) GeoTealPrimary else GeoDarkSurface,
                        shape = RoundedCornerShape(16.dp).copy(
                            bottomEnd = if (isMe) androidx.compose.foundation.shape.CornerSize(2.dp) else androidx.compose.foundation.shape.CornerSize(16.dp),
                            bottomStart = if (!isMe) androidx.compose.foundation.shape.CornerSize(2.dp) else androidx.compose.foundation.shape.CornerSize(16.dp)
                        ),
                        border = if (!isMe) androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline) else null,
                        shadowElevation = 1.dp,
                        modifier = Modifier.widthIn(max = 290.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = msg.message,
                                color = if (isMe) GeoTealOnPrimary else GeoDarkOnSurface,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                                    color = if (isMe) GeoTealOnPrimary.copy(alpha = 0.75f) else Color(0xFF64748B),
                                    fontSize = 10.sp
                                )
                                if (isMe) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Read",
                                        tint = GeoTealOnPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Input Area
        Surface(
            color = GeoDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { viewModel.chatInput.value = it },
                    placeholder = { Text("اكتب رسالتك...", fontSize = 13.sp, color = Color(0xFF74777F)) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoTealPrimary,
                        unfocusedBorderColor = GeoDarkOutline,
                        focusedTextColor = GeoDarkOnSurface,
                        unfocusedTextColor = GeoDarkOnSurface,
                        focusedContainerColor = Color(0xFFF8F9FE),
                        unfocusedContainerColor = Color(0xFFF8F9FE)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )

                IconButton(
                    onClick = {
                        if (chatInput.isNotBlank()) {
                            viewModel.sendChatMessage(chatInput)
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(GeoTealPrimary)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = GeoTealOnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
