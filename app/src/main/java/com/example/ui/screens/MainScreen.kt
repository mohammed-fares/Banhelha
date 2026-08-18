package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.UserEntity
import com.example.ui.theme.*
import com.example.viewmodel.AuthStep
import com.example.viewmodel.GeoConnectViewModel

enum class MainTab(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    RADAR(R.string.tab_radar, Icons.Default.Radar, Icons.Outlined.Radar, "tab_radar"),
    MAP(R.string.tab_map, Icons.Default.Map, Icons.Outlined.Map, "tab_map"),
    SERVICES(R.string.tab_services, Icons.Default.Storefront, Icons.Outlined.Storefront, "tab_services"),
    CHATS(R.string.tab_chats, Icons.Default.Chat, Icons.Outlined.Chat, "tab_chats"),
    PROFILE(R.string.tab_profile, Icons.Default.Person, Icons.Outlined.Person, "tab_profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: GeoConnectViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val authStep by viewModel.authStep.collectAsStateWithLifecycle()
    val activeChatUser by viewModel.activeChatUser.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
    val showReportUser by viewModel.showReportDialog.collectAsStateWithLifecycle()
    val reportReason by viewModel.reportReason.collectAsStateWithLifecycle()
    val reportDetails by viewModel.reportDetails.collectAsStateWithLifecycle()

    val isGlobalLoading by viewModel.isGlobalLoading.collectAsStateWithLifecycle()
    val globalLoadingMessage by viewModel.globalLoadingMessage.collectAsStateWithLifecycle()
    val globalErrorNotice by viewModel.globalErrorNotice.collectAsStateWithLifecycle()
    val locationErrorMessage by viewModel.locationErrorMessage.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainTab.RADAR) }
    var inAdminScreen by remember { mutableStateOf(false) }

    // User Role Check: Only 'General Manager' role can access the dashboard navigation
    val isGeneralManager = currentUser?.isGeneralManager == true

    // If not authenticated, render AuthScreen
    if (authStep != AuthStep.AUTHENTICATED) {
        AuthScreen(viewModel = viewModel, modifier = modifier)
        return
    }

    // If Admin/Manager Dashboard screen is active
    if (inAdminScreen) {
        AdminDashboardScreen(
            viewModel = viewModel,
            onBack = { inAdminScreen = false },
            modifier = modifier
        )
        return
    }

    // If active chat user is selected, render full-screen conversation
    if (activeChatUser != null) {
        ChatConversationScreen(
            viewModel = viewModel,
            onBack = { viewModel.closeChat() },
            modifier = modifier
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GeoDarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = GeoDarkSurface,
                tonalElevation = 3.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                MainTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (tab == MainTab.CHATS && unreadCount > 0) {
                                        Badge(containerColor = GeoTealPrimary, contentColor = GeoTealOnPrimary) {
                                            Text("$unreadCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.titleRes),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(tab.titleRes),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GeoTealPrimary,
                            selectedTextColor = GeoTealPrimary,
                            indicatorColor = GeoTealContainer,
                            unselectedIconColor = Color(0xFF74777F),
                            unselectedTextColor = Color(0xFF74777F)
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }

                // Conditionally render Dashboard navigation icon ONLY for General Manager role
                if (isGeneralManager) {
                    NavigationBarItem(
                        selected = false,
                        onClick = { inAdminScreen = true },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = stringResource(R.string.tab_dashboard),
                                modifier = Modifier.size(22.dp),
                                tint = GeoPurpleAI
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.tab_dashboard),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeoPurpleAI
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GeoPurpleAI,
                            selectedTextColor = GeoPurpleAI,
                            indicatorColor = Color(0xFFF3E8FF),
                            unselectedIconColor = GeoPurpleAI,
                            unselectedTextColor = GeoPurpleAI
                        ),
                        modifier = Modifier.testTag("dashboard_nav_icon")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Global Error Notification Banner
                if (globalErrorNotice != null || locationErrorMessage != null) {
                    val notice = globalErrorNotice ?: locationErrorMessage
                    Surface(
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = GeoRedError,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = notice ?: "",
                                    color = Color(0xFF991B1B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissGlobalError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF991B1B), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = currentTab,
                        label = "tab_content_transition"
                    ) { tab ->
                        when (tab) {
                            MainTab.RADAR -> RadarDiscoveryScreen(
                                viewModel = viewModel,
                                onNavigateToChat = { user -> viewModel.openChatWith(user) }
                            )
                            MainTab.MAP -> MapScreen(
                                viewModel = viewModel,
                                onOpenChat = { user -> viewModel.openChatWith(user) }
                            )
                            MainTab.SERVICES -> ServicesMarketplaceScreen(
                                viewModel = viewModel,
                                onNavigateToChat = { user -> viewModel.openChatWith(user) }
                            )
                            MainTab.CHATS -> ChatListScreen(
                                viewModel = viewModel,
                                onOpenConversation = { user -> viewModel.openChatWith(user) }
                            )
                            MainTab.PROFILE -> ProfileScreen(
                                viewModel = viewModel,
                                onNavigateToAdmin = { inAdminScreen = true }
                            )
                        }
                    }
                }
            }

            // Global Loading Indicator Modal
            if (isGlobalLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = GeoTealPrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = globalLoadingMessage ?: "جاري المعالجة والمزامنة...",
                                color = GeoDarkOnSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Safety Report Dialog
    if (showReportUser != null) {
        val userToReport = showReportUser!!
        val reasons = listOf("محتوى غير لائق", "حساب وهمي أو احتيالي", "مضايقة أو إزعاج", "أخرى")

        AlertDialog(
            onDismissRequest = { viewModel.showReportDialog.value = null },
            containerColor = GeoDarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = GeoRedError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إبلاغ وحماية ضد: ${userToReport.name}", color = GeoDarkOnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اختر سبب الإبلاغ ليقوم فريق الإشراف بالتحقق فوراً:", color = Color(0xFF49454F), fontSize = 13.sp)

                    reasons.forEach { r ->
                        Surface(
                            color = if (reportReason == r) GeoRedError.copy(alpha = 0.12f) else Color(0xFFF1F3F9),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.reportReason.value = r }
                        ) {
                            Text(
                                text = r,
                                color = if (reportReason == r) GeoRedError else GeoDarkOnSurface,
                                fontWeight = if (reportReason == r) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { viewModel.reportDetails.value = it },
                        placeholder = { Text("تفاصيل إضافية إن وجدت...", color = Color.Gray, fontSize = 12.sp) },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GeoDarkOnSurface,
                            unfocusedTextColor = GeoDarkOnSurface,
                            focusedBorderColor = GeoTealPrimary,
                            unfocusedBorderColor = GeoDarkOutline
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitReport() },
                    colors = ButtonDefaults.buttonColors(containerColor = GeoRedError),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إرسال البلاغ والحظر", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showReportDialog.value = null }) {
                    Text("إلغاء", color = Color(0xFF64748B))
                }
            }
        )
    }
}
