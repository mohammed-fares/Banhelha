package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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
    val allTrusts by viewModel.allTrusts.collectAsStateWithLifecycle()
    val userLat by viewModel.userLat.collectAsStateWithLifecycle()
    val userLng by viewModel.userLng.collectAsStateWithLifecycle()
    val distanceFilter by viewModel.distanceFilterKm.collectAsStateWithLifecycle()
    val genderFilter by viewModel.genderFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val selectedUser by viewModel.selectedUser.collectAsStateWithLifecycle()
    val selectedUserTrust by viewModel.selectedUserTrust.collectAsStateWithLifecycle()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle()
    val isFetchingLocation by viewModel.isFetchingLocation.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
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

    // Filter users by distance, gender, and search text
    val filteredUsers = remember(activeUsers, distanceFilter, genderFilter, searchQuery, userLat, userLng) {
        activeUsers.filter { user ->
            // Exclude self (id 56 or current)
            val isNotSelf = user.id != 56L
            val distanceMeters = viewModel.calculateDistanceMeters(userLat, userLng, user.latitude, user.longitude)
            val distanceKm = distanceMeters / 1000.0
            val withinDistance = distanceKm <= distanceFilter

            val matchesGender = when (genderFilter) {
                "male" -> user.gender == "male"
                "female" -> user.gender == "female"
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                user.name.contains(searchQuery, ignoreCase = true) ||
                        user.interests.contains(searchQuery, ignoreCase = true) ||
                        user.bio.contains(searchQuery, ignoreCase = true)
            }

            isNotSelf && withinDistance && matchesGender && matchesSearch
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
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "بنحليها",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = GeoDarkOnSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GeoGreenOnline)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isFetchingLocation) "📍 جاري تحديد الموقع..." else "📍 موقعك المباشر: %.4f, %.4f".format(userLat, userLng),
                            fontSize = 11.sp,
                            color = GeoTealPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                if (hasLocationPermission) {
                                    viewModel.refreshLocationFromGps()
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh GPS",
                                tint = GeoTealPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // View Mode Toggle (Radar vs List)
                Surface(
                    color = Color(0xFFF1F3F9),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline)
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        IconButton(
                            onClick = { viewModel.viewMode.value = "radar" },
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (viewMode == "radar") GeoTealPrimary else Color.Transparent, RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = "Radar View",
                                tint = if (viewMode == "radar") GeoTealOnPrimary else Color(0xFF74777F),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.viewMode.value = "list" },
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (viewMode == "list") GeoTealPrimary else Color.Transparent, RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewAgenda,
                                contentDescription = "List View",
                                tint = if (viewMode == "list") GeoTealOnPrimary else Color(0xFF74777F),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Location Permission Rationale Banner (if permission not granted)
            if (!hasLocationPermission) {
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = GeoTealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مطلوب إذن الموقع الجغرافي لحساب المسافات وتفعيل ميزات القرب",
                                fontSize = 11.sp,
                                color = Color(0xFF1E3A8A),
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary)
                        ) {
                            Text("منح الإذن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Compact Scaled Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("بحث بالاسم أو الاهتمام...", fontSize = 12.sp, color = Color(0xFF74777F)) },
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
                    .testTag("radar_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Controllable Distance Slider (Left-Right interactive range control)
            Surface(
                color = GeoDarkSurface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Radar, contentDescription = null, tint = GeoTealPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مؤشر نطاق الرادار:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeoDarkOnSurface
                            )
                        }

                        Surface(
                            color = GeoTealContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (distanceFilter < 1f) "%.1f كم (محيط فوري)".format(distanceFilter)
                                else "%.0f كم".format(distanceFilter),
                                color = GeoTealPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Slider(
                        value = distanceFilter,
                        onValueChange = { viewModel.distanceFilterKm.value = it },
                        valueRange = 0.5f..50f,
                        steps = 98,
                        colors = SliderDefaults.colors(
                            thumbColor = GeoTealPrimary,
                            activeTrackColor = GeoTealPrimary,
                            inactiveTrackColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0.5 كم (أقرب جار)", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text("10 كم", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text("50 كم (المدينة كاملة)", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Content: Radar or List
            AnimatedContent(
                targetState = viewMode,
                label = "view_mode_transition",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { mode ->
                if (mode == "radar") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        RadarView(
                            centerLat = userLat,
                            centerLng = userLng,
                            maxDistanceKm = distanceFilter,
                            users = filteredUsers,
                            selectedUser = selectedUser,
                            onUserSelected = { user -> viewModel.selectedUser.value = user },
                            modifier = Modifier.weight(1f)
                        )

                        // Highlighted bottom card if user tapped on radar
                        if (selectedUser != null) {
                            val targetTrust = allTrusts.firstOrNull { it.targetUserId == selectedUser!!.id }
                            val distMeters = viewModel.calculateDistanceMeters(
                                userLat, userLng, selectedUser!!.latitude, selectedUser!!.longitude
                            )
                            UserCard(
                                user = selectedUser!!,
                                distanceStr = viewModel.formatDistance(distMeters),
                                trust = targetTrust,
                                onCardClick = { /* open details */ },
                                onPingClick = { viewModel.sendPing(selectedUser!!.id) },
                                onChatClick = {
                                    viewModel.openChatWith(selectedUser!!)
                                    onNavigateToChat(selectedUser!!)
                                }
                            )
                        } else {
                            Surface(
                                color = GeoDarkSurface,
                                shape = RoundedCornerShape(14.dp),
                                shadowElevation = 1.dp,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = GeoAzureSecondary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "المس أي نقطة على الرادار لمعاينة الشخص والتواصل معه فوراً",
                                        color = Color(0xFF49454F),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // List View
                    if (filteredUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LocationOff, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("لا يوجد أشخاص مطابقون في هذا النطاق", color = GeoDarkOnSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("جرب زيادة نطاق المسافة إلى 25 كم أو تغيير كلمات البحث", color = Color(0xFF64748B), fontSize = 13.sp, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredUsers, key = { it.id }) { user ->
                                val targetTrust = allTrusts.firstOrNull { it.targetUserId == user.id }
                                val distMeters = viewModel.calculateDistanceMeters(
                                    userLat, userLng, user.latitude, user.longitude
                                )
                                UserCard(
                                    user = user,
                                    distanceStr = viewModel.formatDistance(distMeters),
                                    trust = targetTrust,
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
        }

        // Detailed User Sheet Modal
        if (selectedUser != null) {
            val user = selectedUser!!
            val trust = selectedUserTrust
            val context = LocalContext.current
            val distMeters = viewModel.calculateDistanceMeters(userLat, userLng, user.latitude, user.longitude)

            ModalBottomSheet(
                onDismissRequest = { viewModel.selectedUser.value = null },
                containerColor = GeoDarkSurface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = GeoDarkOutline) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header with Avatar & Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (user.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = user.name,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, GeoTealPrimary, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(if (user.isBot) GeoPurpleAI.copy(alpha = 0.15f) else GeoTealContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (user.isBot) "🤖" else user.name.take(1),
                                    color = if (user.isBot) GeoPurpleAI else GeoTealPrimary,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${user.name}, ${user.age}",
                                    color = GeoDarkOnSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                if (user.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = GeoTealPrimary, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "📍 ${viewModel.formatDistance(distMeters)} away",
                                color = GeoAzureSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            TrustLevelBadge(trust = trust)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "About", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (user.bio.isNotBlank()) user.bio else "No bio available.",
                        color = GeoDarkOnSurface,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "Interests", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        user.interests.split(",").forEach { item ->
                            if (item.isNotBlank()) {
                                Surface(
                                    color = Color(0xFFF1F3F9),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "#${item.trim()}",
                                        color = GeoTealPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Trust Relationship Progression Section
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "نظام الثقة والخصوصية المتقدم (Trust System)",
                        color = GeoAzureSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = Color(0xFFF8F9FE),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Step 1: Ping / Connection
                            val pingAccepted = trust?.pingStatus == "accepted"
                            val pingSent = trust?.pingStatus == "sent"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "1. إشارة التواصل (Ping & Connection)",
                                        color = if (pingAccepted) GeoTealPrimary else GeoDarkOnSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (pingAccepted) "تم قبول الاتصال المتبادل ✅" else if (pingSent) "تم إرسال الإشارة بانتظار القبول..." else "إرسال إشارة للبدء في بناء الثقة",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }

                                if (!pingAccepted) {
                                    Button(
                                        onClick = { viewModel.sendPing(user.id) },
                                        enabled = !pingSent,
                                        colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(if (pingSent) "Sent" else "Send Ping", color = GeoTealOnPrimary, fontSize = 11.sp)
                                    }
                                }
                            }

                            Divider(color = GeoDarkOutline, modifier = Modifier.padding(vertical = 10.dp))

                            // Step 2: Media Access
                            val mediaGranted = trust?.mediaAccessGranted == true
                            val mediaRequested = trust?.mediaAccessRequested == true

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "2. صلاحية مشاركة الصور والوسائط",
                                        color = if (mediaGranted) GeoAzureSecondary else GeoDarkOnSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (mediaGranted) "الصلاحية مفعلة لتبادل الصور 📷" else if (mediaRequested) "تم طلب الإذن بانتظار الموافقة..." else "طلب إذن متبادل لتبادل الصور والوسائط",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }

                                if (!mediaGranted) {
                                    OutlinedButton(
                                        onClick = { viewModel.requestMediaAccess(user.id) },
                                        enabled = !mediaRequested,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoAzureSecondary),
                                        border = ButtonDefaults.outlinedButtonBorder.copy(
                                            brush = Brush.linearGradient(listOf(GeoDarkOutline, GeoDarkOutline))
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(if (mediaRequested) "Requested" else "Request", fontSize = 11.sp)
                                    }
                                }
                            }

                            Divider(color = GeoDarkOutline, modifier = Modifier.padding(vertical = 10.dp))

                            // Step 3: Identity & Social Links Access
                            val identityGranted = trust?.identityAccessGranted == true
                            val identityRequested = trust?.identityAccessRequested == true

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "3. كشف الهوية وروابط التواصل (Identity)",
                                        color = if (identityGranted) GeoGold else GeoDarkOnSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (identityGranted) "تم التحقق وكشف روابط التواصل ✅" else if (identityRequested) "تم إرسال طلب كشف الهوية..." else "مشاركة حسابات واتساب وتلغرام بعد الموافقة",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }

                                if (!identityGranted) {
                                    OutlinedButton(
                                        onClick = { viewModel.requestIdentityAccess(user.id) },
                                        enabled = !identityRequested,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoGold),
                                        border = ButtonDefaults.outlinedButtonBorder.copy(
                                            brush = Brush.linearGradient(listOf(GeoDarkOutline, GeoDarkOutline))
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(if (identityRequested) "Pending" else "Request ID", fontSize = 11.sp)
                                    }
                                }
                            }

                            // If Identity is granted, reveal direct social links
                            if (identityGranted) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("روابط التواصل الموثوقة المفتوحة:", color = GeoGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        if (user.whatsapp.isNotBlank()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${user.whatsapp.replace("+", "")}"))
                                                        context.startActivity(intent)
                                                    }
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Phone, contentDescription = null, tint = GeoGreenOnline, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("WhatsApp: ${user.whatsapp}", color = GeoDarkOnSurface, fontSize = 12.sp)
                                            }
                                        }
                                        if (user.telegram.isNotBlank()) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Send, contentDescription = null, tint = GeoAzureSecondary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Telegram: ${user.telegram}", color = GeoDarkOnSurface, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Main Action Buttons
                    Spacer(modifier = Modifier.height(20.dp))
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
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = GeoTealOnPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("فتح المحادثة 💬", color = GeoTealOnPrimary, fontWeight = FontWeight.Bold)
                        }

                        // Report / Block action
                        OutlinedButton(
                            onClick = {
                                viewModel.showReportDialog.value = user
                            },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoRedError),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(listOf(GeoDarkOutline, GeoDarkOutline))
                            )
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = "Safety", tint = GeoRedError)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
