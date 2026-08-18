package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LocalServiceEntity
import com.example.data.model.MapItem
import com.example.data.model.UserEntity
import com.example.ui.theme.*
import com.example.viewmodel.GeoConnectViewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: GeoConnectViewModel,
    onOpenChat: (UserEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userLat by viewModel.userLat.collectAsStateWithLifecycle()
    val userLng by viewModel.userLng.collectAsStateWithLifecycle()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle()
    val isFetchingLocation by viewModel.isFetchingLocation.collectAsStateWithLifecycle()
    val locationError by viewModel.locationErrorMessage.collectAsStateWithLifecycle()

    val activeUsers by viewModel.activeUsers.collectAsStateWithLifecycle()
    val allServices by viewModel.allServices.collectAsStateWithLifecycle()

    var mapFilter by remember { mutableStateOf("all") } // "all", "users", "services", "clusters"
    var selectedItem by remember { mutableStateOf<MapItem?>(null) }
    var zoomLevel by remember { mutableFloatStateOf(15f) } // scale factor
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
    }

    // Build map items based on filter and coordinates
    val mapItems = remember(activeUsers, allServices, mapFilter, userLat, userLng) {
        val list = mutableListOf<MapItem>()

        if (mapFilter == "all" || mapFilter == "users") {
            activeUsers.forEach { u ->
                list.add(MapItem.UserPin(u))
            }
        }
        if (mapFilter == "all" || mapFilter == "services") {
            allServices.forEach { s ->
                list.add(MapItem.ServicePin(s))
            }
        }
        list
    }

    // Compute Clusters when zoomed out (e.g. zoom < 14f)
    val displayItems = remember(mapItems, zoomLevel, mapFilter) {
        if (zoomLevel < 12f || mapFilter == "clusters") {
            // Simple spatial grid clustering
            val clusters = mutableListOf<MapItem>()
            val visited = mutableSetOf<String>()
            val threshold = 0.008 / (zoomLevel / 10f)

            for (i in mapItems.indices) {
                val item = mapItems[i]
                if (item.id in visited) continue

                val group = mutableListOf<MapItem>()
                group.add(item)
                visited.add(item.id)

                for (j in i + 1 until mapItems.size) {
                    val other = mapItems[j]
                    if (other.id in visited) continue
                    val dist = sqrt((item.latitude - other.latitude).pow(2) + (item.longitude - other.longitude).pow(2))
                    if (dist < threshold) {
                        group.add(other)
                        visited.add(other.id)
                    }
                }

                if (group.size > 1) {
                    val avgLat = group.map { it.latitude }.average()
                    val avgLng = group.map { it.longitude }.average()
                    clusters.add(
                        MapItem.ClusterPin(
                            id = "cluster_${item.id}",
                            latitude = avgLat,
                            longitude = avgLng,
                            itemsCount = group.size,
                            items = group
                        )
                    )
                } else {
                    clusters.add(item)
                }
            }
            clusters
        } else {
            mapItems
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GeoDarkBackground)
    ) {
        // --- 1. Interactive Custom Canvas Map Surface ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomLevel = (zoomLevel * zoom).coerceIn(6f, 35f)
                        panOffsetX += pan.x
                        panOffsetY += pan.y
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f + panOffsetX, size.height / 2f + panOffsetY)
                val scale = zoomLevel * 85f // pixels per degree approx

                // Draw map background tile grids
                drawRect(color = Color(0xFFF1F5F9))

                // Draw grid lines representing city blocks / roads
                val gridSpacing = 60.dp.toPx()
                val startX = (panOffsetX % gridSpacing)
                val startY = (panOffsetY % gridSpacing)

                var curX = startX
                while (curX < size.width) {
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(curX, 0f),
                        end = Offset(curX, size.height),
                        strokeWidth = 1.5f
                    )
                    curX += gridSpacing
                }

                var curY = startY
                while (curY < size.height) {
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, curY),
                        end = Offset(size.width, curY),
                        strokeWidth = 1.5f
                    )
                    curY += gridSpacing
                }

                // Draw arterial avenue lines
                val avenueSpacing = gridSpacing * 3
                val avX = (panOffsetX % avenueSpacing)
                val avY = (panOffsetY % avenueSpacing)
                drawLine(
                    color = Color(0xFFCBD5E1),
                    start = Offset(avX, 0f),
                    end = Offset(avX, size.height),
                    strokeWidth = 4f
                )
                drawLine(
                    color = Color(0xFFCBD5E1),
                    start = Offset(0f, avY),
                    end = Offset(size.width, avY),
                    strokeWidth = 4f
                )

                // Draw User Radar concentric range circles
                val radius1km = 0.009f * scale
                val radius5km = 0.045f * scale
                drawCircle(
                    color = GeoTealPrimary.copy(alpha = 0.08f),
                    radius = radius1km,
                    center = center
                )
                drawCircle(
                    color = GeoTealPrimary.copy(alpha = 0.25f),
                    radius = radius1km,
                    center = center,
                    style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                )
                drawCircle(
                    color = GeoTealPrimary.copy(alpha = 0.15f),
                    radius = radius5km,
                    center = center,
                    style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f))
                )

                // Draw User's Current Location (Blue glowing dot)
                drawCircle(
                    color = GeoTealPrimary.copy(alpha = 0.2f),
                    radius = 24.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = GeoTealPrimary,
                    radius = 10.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = center
                )
            }

            // --- 2. Interactive Overlay Markers for Map Items ---
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenW = maxWidth.value
                val screenH = maxHeight.value
                val centerPxX = (screenW / 2f) * 2.75f + panOffsetX
                val centerPxY = (screenH / 2f) * 2.75f + panOffsetY
                val scale = zoomLevel * 85f

                displayItems.forEach { item ->
                    // Calculate pixel offset relative to user location
                    val dLat = item.latitude - userLat
                    val dLng = item.longitude - userLng
                    // in screen coords: x is lng (east), y is -lat (north)
                    val posX = centerPxX + (dLng.toFloat() * scale)
                    val posY = centerPxY - (dLat.toFloat() * scale)

                    // Convert px to dp approx
                    val dpX = (posX / 2.75f).dp
                    val dpY = (posY / 2.75f).dp

                    Box(
                        modifier = Modifier
                            .offset(x = dpX - 22.dp, y = dpY - 22.dp)
                            .clickable {
                                selectedItem = item
                            }
                    ) {
                        when (item) {
                            is MapItem.UserPin -> {
                                UserMapMarker(
                                    user = item.user,
                                    isSelected = selectedItem?.id == item.id
                                )
                            }
                            is MapItem.ServicePin -> {
                                ServiceMapMarker(
                                    service = item.service,
                                    isSelected = selectedItem?.id == item.id
                                )
                            }
                            is MapItem.ClusterPin -> {
                                ClusterMapMarker(
                                    count = item.itemsCount,
                                    isSelected = selectedItem?.id == item.id
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. Top Controls Bar: Header & Category Filters ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(
                color = GeoDarkSurface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GeoTealContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                tint = GeoTealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "خريطة الاستكشاف الحي",
                                color = GeoDarkOnSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (isFetchingLocation) "جاري تحديث الموقع..." else "${displayItems.size} عنصر في النطاق الجغرافي",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // GPS Refresh button
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
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F3F9))
                    ) {
                        Icon(
                            imageVector = if (isFetchingLocation) Icons.Default.Refresh else Icons.Default.MyLocation,
                            contentDescription = "GPS",
                            tint = GeoTealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips (All, Users, Services, Clusters)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf(
                    "all" to "الكل (${mapItems.size})",
                    "users" to "الأشخاص (${activeUsers.size})",
                    "services" to "الخدمات (${allServices.size})",
                    "clusters" to "المجموعات (Clusters)"
                )
                items(filters) { (key, label) ->
                    val isSelected = mapFilter == key
                    Surface(
                        color = if (isSelected) GeoTealPrimary else GeoDarkSurface,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GeoTealPrimary else GeoDarkOutline),
                        shadowElevation = if (isSelected) 2.dp else 1.dp,
                        modifier = Modifier.clickable { mapFilter = key }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else GeoDarkOnSurface,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // --- 4. Floating Zoom & Center Controls ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = GeoDarkSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                shadowElevation = 3.dp
            ) {
                Column {
                    IconButton(
                        onClick = { zoomLevel = (zoomLevel * 1.3f).coerceAtMost(35f) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = GeoDarkOnSurface)
                    }
                    Divider(color = GeoDarkOutline, modifier = Modifier.width(40.dp))
                    IconButton(
                        onClick = { zoomLevel = (zoomLevel / 1.3f).coerceAtLeast(6f) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = GeoDarkOnSurface)
                    }
                }
            }

            // Recenter Button
            Surface(
                color = GeoDarkSurface,
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                shadowElevation = 3.dp,
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(
                    onClick = {
                        panOffsetX = 0f
                        panOffsetY = 0f
                        zoomLevel = 15f
                    }
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Recenter", tint = GeoTealPrimary)
                }
            }
        }

        // --- 5. Selected Item Bottom Details Preview Card ---
        AnimatedVisibility(
            visible = selectedItem != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            selectedItem?.let { item ->
                Surface(
                    color = GeoDarkSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GeoTealPrimary),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (item) {
                                                is MapItem.UserPin -> if (item.user.gender == "female") Color(0xFFFCE7F3) else GeoTealContainer
                                                is MapItem.ServicePin -> Color(0xFFFEF3C7)
                                                is MapItem.ClusterPin -> GeoPurpleAI.copy(alpha = 0.15f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (item) {
                                            is MapItem.UserPin -> Icons.Default.Person
                                            is MapItem.ServicePin -> Icons.Default.Storefront
                                            is MapItem.ClusterPin -> Icons.Default.Hub
                                        },
                                        contentDescription = null,
                                        tint = when (item) {
                                            is MapItem.UserPin -> if (item.user.gender == "female") Color(0xFFEC4899) else GeoTealPrimary
                                            is MapItem.ServicePin -> Color(0xFFD97706)
                                            is MapItem.ClusterPin -> GeoPurpleAI
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = item.title,
                                        color = GeoDarkOnSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.subtitle,
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = { selectedItem = null },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons based on Type
                        when (item) {
                            is MapItem.UserPin -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onOpenChat(item.user)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("محادثة فورية", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.sendPing(item.user.id)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GeoTealPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, tint = GeoTealPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("إرسال Ping", color = GeoTealPrimary, fontSize = 13.sp)
                                    }
                                }
                            }
                            is MapItem.ServicePin -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.selectedService.value = item.service
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تفاصيل الخدمة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.service.phone}"))
                                            context.startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = GeoDarkOnSurface, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("اتصال", color = GeoDarkOnSurface, fontSize = 13.sp)
                                    }
                                }
                            }
                            is MapItem.ClusterPin -> {
                                Button(
                                    onClick = {
                                        // Zoom in to expand cluster
                                        zoomLevel = (zoomLevel * 1.6f).coerceAtMost(35f)
                                        panOffsetX += 10f
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GeoPurpleAI),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("تكبير وفك تجميع العناصر (${item.itemsCount})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
fun UserMapMarker(user: UserEntity, isSelected: Boolean) {
    val isFemale = user.gender == "female"
    val markerColor = if (isFemale) Color(0xFFEC4899) else GeoTealPrimary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Surface(
            color = if (isSelected) markerColor else Color.White,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, markerColor),
            shadowElevation = if (isSelected) 6.dp else 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else markerColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (user.isBot) Icons.Default.SmartToy else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isSelected) markerColor else markerColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = user.name.split(" ").firstOrNull() ?: user.name,
                    color = if (isSelected) Color.White else GeoDarkOnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        // Pointer triangle
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(markerColor, shape = CircleShape)
        )
    }
}

@Composable
fun ServiceMapMarker(service: LocalServiceEntity, isSelected: Boolean) {
    val markerColor = Color(0xFFD97706)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Surface(
            color = if (isSelected) markerColor else Color.White,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, markerColor),
            shadowElevation = if (isSelected) 6.dp else 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else markerColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = service.title.take(10),
                    color = if (isSelected) Color.White else GeoDarkOnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .size(8.dp)
                .background(markerColor, shape = CircleShape)
        )
    }
}

@Composable
fun ClusterMapMarker(count: Int, isSelected: Boolean) {
    Surface(
        color = if (isSelected) GeoPurpleAI else GeoPurpleAI.copy(alpha = 0.9f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(2.5.dp, Color.White),
        shadowElevation = 6.dp,
        modifier = Modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "+$count",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
    }
}
