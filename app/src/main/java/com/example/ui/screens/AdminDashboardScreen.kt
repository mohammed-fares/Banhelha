package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ReportEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.*
import com.example.viewmodel.GeoConnectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: GeoConnectViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allUsers by viewModel.allAdminUsers.collectAsStateWithLifecycle()
    val allReports by viewModel.allReports.collectAsStateWithLifecycle()
    val allServices by viewModel.allServices.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0: Overview & Users, 1: Reports Inbox, 2: AI Bots Config

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GeoDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Surface(
            color = GeoDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GeoDarkOnSurface)
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GeoPurpleAI.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = GeoPurpleAI, modifier = Modifier.size(20.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "لوحة الإدارة والتحكم (Admin Portal)",
                        color = GeoDarkOnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "إدارة المستخدمين، البلاغات، والروبوتات الجغرافية",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Navigation Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = GeoDarkSurface,
            contentColor = GeoTealPrimary,
            divider = { Divider(color = GeoDarkOutline) }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("المستخدمين (${allUsers.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                selectedContentColor = GeoTealPrimary,
                unselectedContentColor = Color(0xFF64748B)
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("البلاغات (${allReports.count { it.status == "pending" }})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                selectedContentColor = GeoTealPrimary,
                unselectedContentColor = Color(0xFF64748B)
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("روبوتات AI", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                selectedContentColor = GeoTealPrimary,
                unselectedContentColor = Color(0xFF64748B)
            )
        }

        when (selectedTab) {
            0 -> {
                // Overview stats + Users List
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // KPI cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminKpiCard(
                                title = "إجمالي المستخدمين",
                                value = "${allUsers.size}",
                                icon = Icons.Default.People,
                                color = GeoTealPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            AdminKpiCard(
                                title = "الخدمات المحلية",
                                value = "${allServices.size}",
                                icon = Icons.Default.Storefront,
                                color = GeoAzureSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            AdminKpiCard(
                                title = "بلاغات معلقة",
                                value = "${allReports.count { it.status == "pending" }}",
                                icon = Icons.Default.Report,
                                color = GeoRedError,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("قائمة الأعضاء والإشراف:", color = GeoDarkOnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    items(allUsers, key = { it.id }) { user ->
                        AdminUserRow(
                            user = user,
                            onToggleBan = { viewModel.toggleAdminUserBan(user) },
                            onToggleVerify = { viewModel.toggleAdminUserVerification(user) }
                        )
                    }
                }
            }

            1 -> {
                // Reports inbox
                if (allReports.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد بلاغات مسجلة حالياً! المنصة آمنة تماماً ✅", color = GeoGreenOnline, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allReports, key = { it.id }) { report ->
                            val targetUser = allUsers.firstOrNull { it.id == report.reportedUserId }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (report.status == "pending") GeoRedError.copy(alpha = 0.4f) else GeoDarkOutline),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "بلاغ ضد: ${targetUser?.name ?: "User #${report.reportedUserId}"}",
                                            color = GeoDarkOnSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Surface(
                                            color = if (report.status == "pending") GeoRedError.copy(alpha = 0.15f) else Color(0xFFDCFCE7),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = report.status.uppercase(),
                                                color = if (report.status == "pending") GeoRedError else GeoGreenOnline,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "السبب: ${report.reason}", color = GeoDarkOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    if (report.details.isNotBlank()) {
                                        Text(text = "التفاصيل: ${report.details}", color = Color(0xFF64748B), fontSize = 12.sp)
                                    }

                                    if (report.status == "pending") {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.resolveAdminReport(report.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("حل البلاغ والاعتماد", color = GeoTealOnPrimary, fontSize = 12.sp)
                                            }

                                            if (targetUser != null && !targetUser.isBlocked) {
                                                OutlinedButton(
                                                    onClick = { viewModel.toggleAdminUserBan(targetUser) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoRedError),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoRedError.copy(alpha = 0.5f))
                                                ) {
                                                    Text("حظر المستخدم", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // AI Virtual Bots Management
                val bots = allUsers.filter { it.isBot }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Surface(
                            color = Color(0xFFF5F3FF),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GeoPurpleAI.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("إدارة شخصيات الذكاء الاصطناعي التفاعلية", color = GeoPurpleAI, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("الروبوتات الافتراضية تتفاعل جغرافياً مع المستخدمين وترد عبر نموذج Gemini بذكاء وسرعة.", color = Color(0xFF49454F), fontSize = 12.sp)
                            }
                        }
                    }

                    items(bots, key = { it.id }) { bot ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🤖", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(bot.name, color = GeoDarkOnSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("نموذج: ${bot.botPersonality}", color = GeoPurpleAI, fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(bot.bio, color = Color(0xFF49454F), fontSize = 13.sp)

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("تغيير نمط الرد:", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val styles = listOf("Concierge & Local Guide", "Friendly Assistant", "Tech Geek")
                                    styles.forEach { style ->
                                        val isSelected = bot.botPersonality == style
                                        Surface(
                                            color = if (isSelected) GeoPurpleAI else Color(0xFFF1F3F9),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GeoPurpleAI else GeoDarkOutline),
                                            modifier = Modifier.clickable {
                                                viewModel.updateBotPersonality(bot.id, style)
                                            }
                                        ) {
                                            Text(
                                                text = style,
                                                color = if (isSelected) Color.White else Color(0xFF49454F),
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = GeoDarkSurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = GeoDarkOnSurface, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text(title, color = Color(0xFF64748B), fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun AdminUserRow(
    user: UserEntity,
    onToggleBan: () -> Unit,
    onToggleVerify: () -> Unit
) {
    Surface(
        color = GeoDarkSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        color = if (user.isBlocked) Color.Gray else GeoDarkOnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (user.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = GeoTealPrimary, modifier = Modifier.size(14.dp))
                    }
                    if (user.isBot) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🤖", fontSize = 12.sp)
                    }
                }
                Text(
                    text = "${user.phone} • ${user.subscriptionType}",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Verify button
                IconButton(
                    onClick = onToggleVerify,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (user.isVerified) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                        contentDescription = "Verify",
                        tint = if (user.isVerified) GeoTealPrimary else Color(0xFF74777F),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Ban button
                IconButton(
                    onClick = onToggleBan,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (user.isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                        contentDescription = "Ban",
                        tint = if (user.isBlocked) GeoGreenOnline else GeoRedError,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
