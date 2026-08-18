package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TrustRelationshipEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun RadarView(
    centerLat: Double,
    centerLng: Double,
    maxDistanceKm: Float,
    users: List<UserEntity>,
    selectedUser: UserEntity?,
    onUserSelected: (UserEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Infinite sweep rotation
    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    // Concentric wave pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFF0F4FF), Color(0xFFF8F9FE)),
                    center = Offset.Unspecified,
                    radius = 800f
                )
            )
            .border(1.dp, GeoDarkOutline, RoundedCornerShape(24.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(users, maxDistanceKm) {
                    detectTapGestures { tapOffset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val maxRadius = min(centerX, centerY) * 0.85f

                        // Find closest user to tap
                        var closestUser: UserEntity? = null
                        var minTouchDistance = Float.MAX_VALUE

                        for (user in users) {
                            val dLat = user.latitude - centerLat
                            val dLng = user.longitude - centerLng
                            // Scale coordinates to radar range
                            val maxDelta = (maxDistanceKm / 111.0f).toDouble()
                            val normX = (dLng / maxDelta).toFloat().coerceIn(-1f, 1f)
                            val normY = (-dLat / maxDelta).toFloat().coerceIn(-1f, 1f)

                            val userPx = centerX + normX * maxRadius
                            val userPy = centerY + normY * maxRadius

                            val touchDist = sqrt((tapOffset.x - userPx).pow(2) + (tapOffset.y - userPy).pow(2))
                            if (touchDist < 50.dp.toPx() && touchDist < minTouchDistance) {
                                minTouchDistance = touchDist
                                closestUser = user
                            }
                        }

                        if (closestUser != null) {
                            onUserSelected(closestUser)
                        }
                    }
                }
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxRadius = min(centerX, centerY) * 0.85f

            // 1. Draw Concentric Circles & Range grid
            val rings = 4
            for (i in 1..rings) {
                val r = maxRadius * (i.toFloat() / rings)
                drawCircle(
                    color = GeoTealPrimary.copy(alpha = 0.18f),
                    radius = r,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                )
            }

            // 2. Crosshair Axis Lines
            drawLine(
                color = GeoTealPrimary.copy(alpha = 0.15f),
                start = Offset(centerX - maxRadius, centerY),
                end = Offset(centerX + maxRadius, centerY),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = GeoTealPrimary.copy(alpha = 0.15f),
                start = Offset(centerX, centerY - maxRadius),
                end = Offset(centerX, centerY + maxRadius),
                strokeWidth = 1.dp.toPx()
            )

            // 3. Expanding Pulse Wave
            drawCircle(
                color = GeoTealPrimary.copy(alpha = pulseAlpha * 0.35f),
                radius = maxRadius * pulseScale,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.dp.toPx())
            )

            // 4. Radar Sweep Cone (Gradient Arc)
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.7f to Color.Transparent,
                    0.95f to GeoTealPrimary.copy(alpha = 0.08f),
                    1.0f to GeoTealPrimary.copy(alpha = 0.30f),
                    center = Offset(centerX, centerY)
                ),
                startAngle = sweepAngle - 90f,
                sweepAngle = 90f,
                useCenter = true,
                topLeft = Offset(centerX - maxRadius, centerY - maxRadius),
                size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
            )

            // 5. Radar Sweep Leading Line
            val rad = Math.toRadians(sweepAngle.toDouble())
            val lineEndX = centerX + (maxRadius * cos(rad)).toFloat()
            val lineEndY = centerY + (maxRadius * sin(rad)).toFloat()
            drawLine(
                color = GeoTealPrimary.copy(alpha = 0.6f),
                start = Offset(centerX, centerY),
                end = Offset(lineEndX, lineEndY),
                strokeWidth = 2.dp.toPx()
            )

            // 6. Center User Pinpoint (Self)
            drawCircle(
                color = GeoTealPrimary.copy(alpha = 0.2f),
                radius = 16.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = GeoTealPrimary,
                radius = 7.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(centerX, centerY)
            )

            // 7. Render Nearby User Blips
            val maxDelta = (maxDistanceKm / 111.0f).toDouble()
            for (user in users) {
                val dLat = user.latitude - centerLat
                val dLng = user.longitude - centerLng
                val normX = (dLng / maxDelta).toFloat().coerceIn(-1f, 1f)
                val normY = (-dLat / maxDelta).toFloat().coerceIn(-1f, 1f)

                val userPx = centerX + normX * maxRadius
                val userPy = centerY + normY * maxRadius
                val isSelected = selectedUser?.id == user.id

                val blipColor = when {
                    user.isBot -> GeoPurpleAI
                    user.subscriptionType == "business" -> GeoGold
                    user.gender == "female" -> GeoMagentaTertiary
                    else -> GeoAzureSecondary
                }

                // Blip halo if selected
                if (isSelected) {
                    drawCircle(
                        color = GeoTealPrimary.copy(alpha = 0.25f),
                        radius = 20.dp.toPx(),
                        center = Offset(userPx, userPy)
                    )
                    drawCircle(
                        color = blipColor,
                        radius = 14.dp.toPx(),
                        center = Offset(userPx, userPy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                } else {
                    drawCircle(
                        color = blipColor.copy(alpha = 0.25f),
                        radius = 12.dp.toPx(),
                        center = Offset(userPx, userPy)
                    )
                }

                // Inner core
                drawCircle(
                    color = blipColor,
                    radius = 6.dp.toPx(),
                    center = Offset(userPx, userPy)
                )
            }
        }

        // Overlay Distance Ring Labels
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
        ) {
            Text(
                text = "📍 Live Radar Scan",
                color = GeoTealPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = "${users.size} people in ${maxDistanceKm.toInt()}km radius",
                color = Color(0xFF64748B),
                fontSize = 10.sp
            )
        }

        // Radar Range Indicator Badge
        Surface(
            color = Color.White.copy(alpha = 0.92f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GeoGreenOnline)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Radius: ${maxDistanceKm.toInt()} km",
                    color = GeoDarkOnSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TrustLevelBadge(
    trust: TrustRelationshipEntity?,
    modifier: Modifier = Modifier
) {
    val pingAccepted = trust?.pingStatus == "accepted"
    val mediaGranted = trust?.mediaAccessGranted == true
    val identityGranted = trust?.identityAccessGranted == true

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF1F3F9))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TrustStepIcon(
            icon = Icons.Default.Favorite,
            label = "Ping",
            isActive = pingAccepted,
            activeColor = GeoTealPrimary
        )
        Text("›", color = Color(0xFF94A3B8), fontSize = 12.sp)
        TrustStepIcon(
            icon = Icons.Default.PhotoLibrary,
            label = "Media",
            isActive = mediaGranted,
            activeColor = GeoAzureSecondary
        )
        Text("›", color = Color(0xFF94A3B8), fontSize = 12.sp)
        TrustStepIcon(
            icon = Icons.Default.VerifiedUser,
            label = "Identity",
            isActive = identityGranted,
            activeColor = GeoGold
        )
    }
}

@Composable
private fun TrustStepIcon(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) activeColor else Color(0xFF94A3B8).copy(alpha = 0.5f),
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) GeoDarkOnSurface else Color(0xFF94A3B8)
        )
    }
}
