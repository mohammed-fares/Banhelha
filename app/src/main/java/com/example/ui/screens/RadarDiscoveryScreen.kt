package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.UserEntity
import com.example.data.nearby.DiscoveredPeer
import com.example.data.nearby.PeerSource
import com.example.ui.components.DiscoveredPeerCard
import com.example.ui.components.RadarPeerView
import com.example.ui.components.TrustLevelBadge
import com.example.ui.theme.*
import com.example.viewmodel.GeoConnectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarDiscoveryScreen(
    viewModel: GeoConnectViewModel,
    onNavigateToChat: (UserEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val discoveredPeers by viewModel.discoveredPeers.collectAsStateWithLifecycle()
    val isNearbyScanning by viewModel.isNearbyScanning.collectAsStateWithLifecycle()
    val bluetoothEnabled by viewModel.bluetoothEnabled.collectAsStateWithLifecycle()
    val wifiEnabled by viewModel.wifiEnabled.collectAsStateWithLifecycle()
    val selectedPeer by viewModel.selectedPeer.collectAsStateWithLifecycle()
    val peerSourceFilter by viewModel.peerSourceFilter.collectAsStateWithLifecycle()
    val allTrusts by viewModel.allTrusts.collectAsStateWithLifecycle()

    val userLat by viewModel.userLat.collectAsStateWithLifecycle()
    val userLng by viewModel.userLng.collectAsStateWithLifecycle()
    val distanceFilter by viewModel.distanceFilterKm.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle()
    val isFetchingLocation by viewModel.isFetchingLocation.collectAsStateWithLifecycle()

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val grantedLoc = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(grantedLoc)
        viewModel.startNearbyDiscovery()
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(permissionsToRequest)
        } else {
            viewModel.refreshLocationFromGps()
            viewModel.startNearbyDiscovery()
        }
    }

    // Filter peers by source, distance, and search query
    val filteredPeers = remember(discoveredPeers, peerSourceFilter, distanceFilter, searchQuery) {
        discoveredPeers.filter { peer ->
            // Source Filter
            val matchesSource = when (peerSourceFilter) {
                "bluetooth" -> peer.source == PeerSource.BLUETOOTH
                "wifi" -> peer.source == PeerSource.WIFI
                "gps" -> peer.source == PeerSource.GPS_COMMUNITY
                "uninstalled" -> !peer.hasAppInstalled
                else -> true
            }

            // Distance filter
            val withinDistance = (peer.distanceMeters / 1000.0) <= distanceFilter

            // Search Filter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                peer.name.contains(searchQuery, ignoreCase = true) ||
                        peer.bioOrInfo.contains(searchQuery, ignoreCase = true) ||
                        peer.macOrIdentifier.contains(searchQuery, ignoreCase = true)
            }

            matchesSource && withinDistance && matchesSearch
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
            // Top App Bar with Hardware Status Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
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
                            text = if (isFetchingLocation) "📍 جاري تحديد GPS..." else "📍 GPS: %.4f, %.4f".format(userLat, userLng),
                            fontSize = 11.sp,
                            color = GeoTealPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                if (hasLocationPermission) {
                                    viewModel.refreshLocationFromGps()
                                    viewModel.refreshNearbyDiscovery()
                                } else {
                                    permissionLauncher.launch(permissionsToRequest)
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

            // Hardware Radio Status Strip (Bluetooth + Wi-Fi + GPS Scan Action)
            Surface(
                color = GeoDarkSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Bluetooth Chip
                        Surface(
                            color = Color(0xFF2563EB).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(text = "بلوتوث نشط", fontSize = 10.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                            }
                        }

                        // Wi-Fi Chip
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(text = "واي فاي نشط", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Scan Nearby Button
                    Button(
                        onClick = {
                            viewModel.refreshNearbyDiscovery()
                            viewModel.startNearbyDiscovery()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "مسح المحيط",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مسح الآن", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Multi-Source Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = peerSourceFilter == "all",
                        onClick = { viewModel.peerSourceFilter.value = "all" },
                        label = { Text("الكل (${discoveredPeers.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GeoTealPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = peerSourceFilter == "bluetooth",
                        onClick = { viewModel.peerSourceFilter.value = "bluetooth" },
                        leadingIcon = { Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("بلوتوث (${discoveredPeers.count { it.source == PeerSource.BLUETOOTH }})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2563EB),
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = peerSourceFilter == "wifi",
                        onClick = { viewModel.peerSourceFilter.value = "wifi" },
                        leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("واي فاي (${discoveredPeers.count { it.source == PeerSource.WIFI }})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981),
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = peerSourceFilter == "gps",
                        onClick = { viewModel.peerSourceFilter.value = "gps" },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("أعضاء بنحلها (${discoveredPeers.count { it.source == PeerSource.GPS_COMMUNITY }})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GeoAzureSecondary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = peerSourceFilter == "uninstalled",
                        onClick = { viewModel.peerSourceFilter.value = "uninstalled" },
                        leadingIcon = { Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("أجهزة للدعوة ✉️ (${discoveredPeers.count { !it.hasAppInstalled }})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE11D48),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("بحث باسم الشخص، الجهاز، أو البث...", fontSize = 12.sp, color = Color(0xFF74777F)) },
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

            Spacer(modifier = Modifier.height(6.dp))

            // Controllable Distance Slider
            Surface(
                color = GeoDarkSurface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                            .height(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

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
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadarPeerView(
                            centerLat = userLat,
                            centerLng = userLng,
                            maxDistanceKm = distanceFilter,
                            peers = filteredPeers,
                            selectedPeer = selectedPeer,
                            onPeerSelected = { peer -> viewModel.selectedPeer.value = peer },
                            modifier = Modifier.weight(1f)
                        )

                        // Highlighted bottom card if peer tapped on radar
                        if (selectedPeer != null) {
                            DiscoveredPeerCard(
                                peer = selectedPeer!!,
                                distanceStr = if (selectedPeer!!.distanceMeters < 1000) "${selectedPeer!!.distanceMeters.toInt()} متر" else "%.1f كم".format(selectedPeer!!.distanceMeters / 1000.0),
                                onCardClick = { /* open details */ },
                                onInviteClick = { viewModel.invitePeerToDownload(selectedPeer!!, "share") },
                                onChatClick = {
                                    val u = selectedPeer!!.userEntity
                                    if (u != null) {
                                        viewModel.openChatWith(u)
                                        onNavigateToChat(u)
                                    }
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
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = GeoAzureSecondary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "المس أي نقطة على الرادار (🔵 بلوتوث، 📶 واي فاي، 📍 GPS) لمعاينته ودعوته أو محادثته",
                                        color = Color(0xFF49454F),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // List View
                    if (filteredPeers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SensorsOff, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("لا توجد أجهزة أو أشخاص ضمن الفلتر المختار", color = GeoDarkOnSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("تأكد من تفعيل البلوتوث والواي فاي أو قم بتوسيع نطاق المسافة", color = Color(0xFF64748B), fontSize = 13.sp, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredPeers, key = { it.id }) { peer ->
                                DiscoveredPeerCard(
                                    peer = peer,
                                    distanceStr = if (peer.distanceMeters < 1000) "${peer.distanceMeters.toInt()} متر" else "%.1f كم".format(peer.distanceMeters / 1000.0),
                                    onCardClick = { viewModel.selectedPeer.value = peer },
                                    onInviteClick = { viewModel.invitePeerToDownload(peer, "share") },
                                    onChatClick = {
                                        val u = peer.userEntity
                                        if (u != null) {
                                            viewModel.openChatWith(u)
                                            onNavigateToChat(u)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modal Sheet for Discovered Peer Details & Invitation Sheet
        if (selectedPeer != null) {
            val peer = selectedPeer!!
            val user = peer.userEntity
            val targetTrust = allTrusts.firstOrNull { it.targetUserId == (user?.id ?: -1L) }

            ModalBottomSheet(
                onDismissRequest = { viewModel.selectedPeer.value = null },
                containerColor = GeoDarkSurface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = GeoDarkOutline) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val sourceColor = when (peer.source) {
                            PeerSource.BLUETOOTH -> Color(0xFF2563EB)
                            PeerSource.WIFI -> Color(0xFF10B981)
                            PeerSource.GPS_COMMUNITY -> GeoTealPrimary
                        }

                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(sourceColor.copy(alpha = 0.15f))
                                .border(2.dp, sourceColor, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (peer.source) {
                                    PeerSource.BLUETOOTH -> Icons.Default.Bluetooth
                                    PeerSource.WIFI -> Icons.Default.Wifi
                                    PeerSource.GPS_COMMUNITY -> Icons.Default.Person
                                },
                                contentDescription = peer.name,
                                tint = sourceColor,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = peer.name,
                                color = GeoDarkOnSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "📍 على بعد ${if (peer.distanceMeters < 1000) "${peer.distanceMeters.toInt()} متر" else "%.1f كم".format(peer.distanceMeters / 1000.0)} • إشارة: ${peer.signalDbm} dBm",
                                color = GeoAzureSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = if (peer.hasAppInstalled) GeoTealContainer else Color(0xFFFFE4E6),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (peer.hasAppInstalled) "عضو مسجل في بنحلها ✔" else "جهاز مكتشف مجاور (غير مثبت للتطبيق)",
                                    color = if (peer.hasAppInstalled) GeoTealPrimary else Color(0xFFE11D48),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hardware Details Box
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "بيانات الاكتشاف والاتصال:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GeoDarkOnSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "• المصدر: ${if (peer.source == PeerSource.BLUETOOTH) "بث البلوتوث المفتوح (BLE Broadcast)" else if (peer.source == PeerSource.WIFI) "الشبكة المحلية / نقطة واي فاي" else "مشاركة الموقع الجغرافي الحي GPS"}", fontSize = 11.sp, color = Color(0xFF475569))
                            if (peer.macOrIdentifier.isNotBlank()) {
                                Text(text = "• العنوان / المعرف: ${peer.macOrIdentifier}", fontSize = 11.sp, color = Color(0xFF475569))
                            }
                            if (peer.bioOrInfo.isNotBlank()) {
                                Text(text = "• ملاحظات: ${peer.bioOrInfo}", fontSize = 11.sp, color = Color(0xFF475569))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Actions
                    if (!peer.hasAppInstalled) {
                        Text(
                            text = "إرسال دعوة للانضمام إلى بنحلها:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GeoDarkOnSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.invitePeerToDownload(peer, "whatsapp") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("دعوة عبر واتساب (WhatsApp)", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.invitePeerToDownload(peer, "sms") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("دعوة عبر رسالة نصية SMS", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { viewModel.invitePeerToDownload(peer, "share") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = GeoTealPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مشاركة رابط التحميل عبر التطبيقات الأخرى", fontWeight = FontWeight.Bold, color = GeoTealPrimary)
                        }
                    } else if (user != null) {
                        // Community user actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.sendPing(user.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = GeoTealPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إرسال Ping", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.openChatWith(user)
                                    viewModel.selectedPeer.value = null
                                    onNavigateToChat(user)
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("بدء محادثة فورية", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
