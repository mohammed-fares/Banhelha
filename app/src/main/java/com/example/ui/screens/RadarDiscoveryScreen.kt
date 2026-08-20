package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.UserEntity
import com.example.ui.components.RadarView
import com.example.ui.components.TrustLevelBadge
import com.example.ui.components.UserCard
import com.example.ui.theme.*
import com.example.viewmodel.GeoConnectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarDiscoveryScreen(
    viewModel: GeoConnectViewModel,
    onNavigateToChat: (UserEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeUsers by viewModel.activeUsers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userLat by viewModel.userLat.collectAsStateWithLifecycle()
    val userLng by viewModel.userLng.collectAsStateWithLifecycle()
    val distanceFilter by viewModel.distanceFilterKm.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val genderFilter by viewModel.genderFilter.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val selectedUser by viewModel.selectedUser.collectAsStateWithLifecycle()
    val selectedUserTrust by viewModel.selectedUserTrust.collectAsStateWithLifecycle()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle()
    val isFetchingLocation by viewModel.isFetchingLocation.collectAsStateWithLifecycle()

    var userFilterType by remember { mutableStateOf("all") } // "all", "online", "verified", "business"

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val grantedLoc = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(grantedLoc)
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.refreshLocationFromGps()
        }
    }

    // Filter community users with the app installed
    val filteredUsers = remember(activeUsers, currentUser, distanceFilter, genderFilter, userFilterType, searchQuery, userLat, userLng) {
        activeUsers.filter { user ->
            // Exclude self
            if (currentUser != null && user.id == currentUser?.id) return@filter false

            // Distance Calculation
            val distMeters = viewModel.calculateDistanceMeters(
                userLat, userLng, user.latitude, user.longitude
            )
            val withinDistance = (distMeters / 1000.0) <= distanceFilter

            // Gender Filter
            val matchesGender = when (genderFilter) {
                "male" -> user.gender.equals("male", ignoreCase = true)
                "female" -> user.gender.equals("female", ignoreCase = true)
                else -> true
            }

            // Category/Status Filter
            val matchesStatus = when (userFilterType) {
                "online" -> user.isActive
                "verified" -> user.isVerified
                "business" -> user.subscriptionType.equals("business", ignoreCase = true) || user.isStoreOwner
                else -> true
            }

            // Search Query
            val matchesSearch = if (searchQuery.isBlank()) true else {
                user.name.contains(searchQuery, ignoreCase = true) ||
                        user.bio.contains(searchQuery, ignoreCase = true) ||
                        user.interests.contains(searchQuery, ignoreCase = true)
            }

            withinDistance && matchesGender && matchesStatus && matchesSearch
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GeoDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "رادار بنحلها الميداني",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeoDarkOnSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GeoTealPrimary)
                        )
                    }
                    Text(
                        text = "مستخدمو التطبيق المتواجدون بالقرب منك حالياً",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // View Mode Toggle (Radar vs List)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2E8F0))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.viewMode.value = "radar" },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (viewMode == "radar") GeoTealPrimary else Color.Transparent)
                            .testTag("radar_mode_button")
                    ) {
                        Icon(
                            Icons.Default.Radar,
                            contentDescription = "Radar View",
                            tint = if (viewMode == "radar") GeoTealOnPrimary else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.viewMode.value = "list" },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (viewMode == "list") GeoTealPrimary else Color.Transparent)
                            .testTag("list_mode_button")
                    ) {
                        Icon(
                            Icons.Default.FormatListBulleted,
                            contentDescription = "List View",
                            tint = if (viewMode == "list") GeoTealOnPrimary else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Quick Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("بحث عن مستخدم، اهتمامات، فني...", fontSize = 12.sp, color = Color(0xFF74777F)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GeoTealPrimary, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF74777F), modifier = Modifier.size(14.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GeoTealPrimary,
                    unfocusedBorderColor = GeoDarkOutline,
                    focusedContainerColor = GeoDarkSurface,
                    unfocusedContainerColor = GeoDarkSurface,
                    focusedTextColor = GeoDarkOnSurface,
                    unfocusedTextColor = GeoDarkOnSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp)
                    .testTag("radar_search_field")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Tabs
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChipItem(
                        title = "الكل (${filteredUsers.size})",
                        isSelected = userFilterType == "all",
                        onClick = { userFilterType = "all" }
                    )
                }
                item {
                    FilterChipItem(
                        title = "متصلون الآن 🟢",
                        isSelected = userFilterType == "online",
                        onClick = { userFilterType = "online" }
                    )
                }
                item {
                    FilterChipItem(
                        title = "موثقون ⭐",
                        isSelected = userFilterType == "verified",
                        onClick = { userFilterType = "verified" }
                    )
                }
                item {
                    FilterChipItem(
                        title = "فنيين ومتاجر 🏪",
                        isSelected = userFilterType == "business",
                        onClick = { userFilterType = "business" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Distance Range Slider Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SocialDistance, contentDescription = null, tint = GeoTealPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نطاق الرادار: ${distanceFilter.toInt()} كم", fontSize = 11.sp, color = GeoDarkOnSurface, fontWeight = FontWeight.Medium)
                }

                // Quick refresh location
                TextButton(
                    onClick = { viewModel.refreshLocationFromGps() },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    if (isFetchingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = GeoTealPrimary)
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = GeoTealPrimary, modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("تحديث موقعي", fontSize = 11.sp, color = GeoTealPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Slider(
                value = distanceFilter,
                onValueChange = { viewModel.distanceFilterKm.value = it },
                valueRange = 1f..50f,
                steps = 48,
                colors = SliderDefaults.colors(
                    thumbColor = GeoTealPrimary,
                    activeTrackColor = GeoTealPrimary,
                    inactiveTrackColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(26.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Main Content Area (Radar vs List)
            if (viewMode == "radar") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RadarView(
                        centerLat = userLat,
                        centerLng = userLng,
                        maxDistanceKm = distanceFilter,
                        users = filteredUsers,
                        selectedUser = selectedUser,
                        onUserSelected = { user ->
                            viewModel.selectedUser.value = user
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                if (filteredUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Radar, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("لا يوجد مستخدمون في هذا النطاق حالياً", color = GeoDarkOnSurface, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("جرب زيادة نطاق البحث بالكيلومترات لرؤية المزيد", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredUsers, key = { it.id }) { user ->
                            val distMeters = viewModel.calculateDistanceMeters(
                                userLat, userLng, user.latitude, user.longitude
                            )
                            val distanceStr = viewModel.formatDistance(distMeters)
                            UserCard(
                                user = user,
                                distanceStr = distanceStr,
                                trust = null,
                                onCardClick = { viewModel.selectedUser.value = user },
                                onPingClick = { viewModel.sendPing(user.id) },
                                onChatClick = {
                                    viewModel.openChatWith(user)
                                    onNavigateToChat(user)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Sheet for Selected User
        selectedUser?.let { user ->
            val distMeters = viewModel.calculateDistanceMeters(
                userLat, userLng, user.latitude, user.longitude
            )
            val distanceStr = viewModel.formatDistance(distMeters)

            ModalBottomSheet(
                onDismissRequest = { viewModel.selectedUser.value = null },
                containerColor = GeoDarkSurface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0))
                        ) {
                            if (user.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = user.avatarUrl,
                                    contentDescription = user.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = GeoTealPrimary,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GeoDarkOnSurface
                                )
                                if (user.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = GeoTealPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "مستخدم نشط • يبعد عنك $distanceStr",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            if (user.subscriptionType == "business") {
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(top = 3.dp)
                                ) {
                                    Text(
                                        text = "متجر / حساب مهني 🏪",
                                        fontSize = 10.sp,
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (user.bio.isNotBlank()) {
                        Text(
                            text = user.bio,
                            fontSize = 13.sp,
                            color = GeoDarkOnSurface,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (user.interests.isNotBlank()) {
                        Text("الاهتمامات والخدمات:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(user.interests, fontSize = 12.sp, color = GeoDarkOnSurface)
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Trust progression
                    Text("مستوى الأمان والتحقق:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(6.dp))
                    TrustLevelBadge(trust = selectedUserTrust)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.openChatWith(user)
                                viewModel.selectedUser.value = null
                                onNavigateToChat(user)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = GeoTealOnPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بدء محادثة", fontWeight = FontWeight.Bold, color = GeoTealOnPrimary)
                        }

                        OutlinedButton(
                            onClick = { viewModel.sendPing(user.id) },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GeoTealPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = GeoTealPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إرسال Ping", fontWeight = FontWeight.Bold, color = GeoTealPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) GeoTealPrimary else Color(0xFFF1F3F9),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) GeoTealPrimary else GeoDarkOutline
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            color = if (isSelected) GeoTealOnPrimary else Color(0xFF49454F),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
