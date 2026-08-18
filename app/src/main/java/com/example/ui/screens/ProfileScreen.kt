package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.theme.*
import com.example.viewmodel.GeoConnectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: GeoConnectViewModel,
    onNavigateToAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()
    val userLat by viewModel.userLat.collectAsStateWithLifecycle()
    val userLng by viewModel.userLng.collectAsStateWithLifecycle()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle()
    val isFetchingLocation by viewModel.isFetchingLocation.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
    }

    val showEditProfileDialog = remember { mutableStateOf(false) }
    val showPlansDialog = remember { mutableStateOf(false) }

    val user = currentUser

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GeoDarkBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        val isGeneralManager = user?.isGeneralManager == true
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.profile_settings_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GeoDarkOnSurface
            )

            // Dashboard / Admin Portal Access Button (ONLY for General Manager role)
            if (isGeneralManager) {
                Button(
                    onClick = onNavigateToAdmin,
                    colors = ButtonDefaults.buttonColors(containerColor = GeoPurpleAI),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("dashboard_nav_icon")
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = stringResource(R.string.tab_dashboard),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.dashboard_button),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(GeoTealContainer)
                            .border(2.dp, GeoTealPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.name?.take(1) ?: "U",
                            color = GeoTealPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user?.name ?: "User",
                                color = GeoDarkOnSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            if (user?.isVerified == true) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = GeoTealPrimary, modifier = Modifier.size(18.dp))
                            }
                        }

                        Text(
                            text = user?.phone ?: "+971500000000",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = if (user?.subscriptionType == "business") Color(0xFFFEF3C7) else Color(0xFFF1F3F9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "خطة: ${(user?.subscriptionType ?: "free").uppercase()}",
                                color = if (user?.subscriptionType == "business") GeoGold else GeoTealPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(onClick = { showEditProfileDialog.value = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = GeoTealPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = user?.bio ?: "No bio yet.",
                    color = Color(0xFF49454F),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                if (!user?.interests.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        user?.interests?.split(",")?.forEach { tag ->
                            Surface(
                                color = Color(0xFFF1F3F9),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "#${tag.trim()}",
                                    color = GeoTealPrimary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Role Switcher Card (UI Role Check testing & management)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = GeoPurpleAI, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.role_current),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GeoDarkOnSurface
                        )
                    }
                    Text(
                        text = if (isGeneralManager) stringResource(R.string.role_general_manager)
                        else if (user?.role == "store_owner") stringResource(R.string.role_store_owner)
                        else stringResource(R.string.role_member),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGeneralManager) GeoPurpleAI else GeoTealPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.switch_role),
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val roles = listOf(
                        "general_manager" to "مدير عام ⭐",
                        "store_owner" to "صاحب متجر 🏪",
                        "member" to "عضو عادي 👤"
                    )
                    roles.forEach { (rKey, rLabel) ->
                        val isSelected = (user?.role ?: "general_manager") == rKey || (rKey == "general_manager" && isGeneralManager)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateUserRole(rKey) },
                            label = { Text(rLabel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (rKey == "general_manager") Color(0xFFF3E8FF) else GeoTealContainer,
                                selectedLabelColor = if (rKey == "general_manager") GeoPurpleAI else GeoTealPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Live GPS Coordinates Card & Location Tweaker
        Surface(
            color = Color(0xFFF8F9FE),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = GeoTealPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إحداثيات الموقع الجغرافي الحي", color = GeoDarkOnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text("%.4f, %.4f".format(userLat, userLng), color = GeoTealPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("الموقع الجغرافي الحالي يُستخدم لمسح الرادار وحساب المسافة بدقة عالية عبر Google Play Location Services", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
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
                    colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isFetchingLocation) Icons.Default.Refresh else Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFetchingLocation) "جاري جلب إحداثيات GPS الحية..." else if (hasLocationPermission) "تحديث الموقع عبر GPS" else "طلب إذن الموقع وتحديث GPS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Subscription Plans Upgrades Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPlansDialog.value = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = GeoGold, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("ترقية الحساب والاشتراكات", color = GeoDarkOnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("الوصول غير المحدود، أولوية الظهور في الرادار، وتوثيق الأعمال", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF74777F))
            }
        }

        // Blocked & Blacklisted Users Section
        Surface(
            color = GeoDarkSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = GeoRedError, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("قائمة المحظورين (${blockedUsers.size})", color = GeoDarkOnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (blockedUsers.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا يوجد مستخدمون محظورون حالياً.", color = Color(0xFF64748B), fontSize = 12.sp)
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    blockedUsers.forEach { bUser ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(bUser.name, color = GeoDarkOnSurface, fontSize = 13.sp)
                            TextButton(onClick = { viewModel.unblockUser(bUser.id) }) {
                                Text("إلغاء الحظر", color = GeoTealPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Logout
        OutlinedButton(
            onClick = { viewModel.logout() },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoRedError),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.linearGradient(listOf(GeoDarkOutline, GeoDarkOutline))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_button")
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = GeoRedError)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تسجيل الخروج", color = GeoRedError, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Edit Profile Modal Dialog
    if (showEditProfileDialog.value && user != null) {
        val name = remember { mutableStateOf(user.name) }
        val bio = remember { mutableStateOf(user.bio) }
        val interests = remember { mutableStateOf(user.interests) }
        val whatsapp = remember { mutableStateOf(user.whatsapp) }
        val telegram = remember { mutableStateOf(user.telegram) }
        val instagram = remember { mutableStateOf(user.instagram) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog.value = false },
            containerColor = GeoDarkSurface,
            title = { Text("تعديل الملف الشخصي", color = GeoDarkOnSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name.value,
                        onValueChange = { name.value = it },
                        label = { Text("الاسم") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GeoDarkOnSurface,
                            unfocusedTextColor = GeoDarkOnSurface,
                            focusedBorderColor = GeoTealPrimary,
                            unfocusedBorderColor = GeoDarkOutline
                        )
                    )
                    OutlinedTextField(
                        value = bio.value,
                        onValueChange = { bio.value = it },
                        label = { Text("نبذة عنك") },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GeoDarkOnSurface,
                            unfocusedTextColor = GeoDarkOnSurface,
                            focusedBorderColor = GeoTealPrimary,
                            unfocusedBorderColor = GeoDarkOutline
                        )
                    )
                    OutlinedTextField(
                        value = interests.value,
                        onValueChange = { interests.value = it },
                        label = { Text("الاهتمامات") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GeoDarkOnSurface,
                            unfocusedTextColor = GeoDarkOnSurface,
                            focusedBorderColor = GeoTealPrimary,
                            unfocusedBorderColor = GeoDarkOutline
                        )
                    )
                    OutlinedTextField(
                        value = whatsapp.value,
                        onValueChange = { whatsapp.value = it },
                        label = { Text("رقم واتساب الموثوق") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GeoDarkOnSurface,
                            unfocusedTextColor = GeoDarkOnSurface,
                            focusedBorderColor = GeoTealPrimary,
                            unfocusedBorderColor = GeoDarkOutline
                        )
                    )
                    OutlinedTextField(
                        value = telegram.value,
                        onValueChange = { telegram.value = it },
                        label = { Text("معرف تلغرام (@username)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GeoDarkOnSurface,
                            unfocusedTextColor = GeoDarkOnSurface,
                            focusedBorderColor = GeoTealPrimary,
                            unfocusedBorderColor = GeoDarkOutline
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(
                            name = name.value,
                            bio = bio.value,
                            interests = interests.value,
                            whatsapp = whatsapp.value,
                            telegram = telegram.value,
                            instagram = instagram.value
                        )
                        showEditProfileDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("حفظ التغييرات", color = GeoTealOnPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog.value = false }) {
                    Text("إلغاء", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Plans upgrade dialog with Egyptian payment options
    if (showPlansDialog.value) {
        var selectedPlan by remember { mutableStateOf("premium") } // "free", "premium", "business"
        var selectedPaymentMethod by remember { mutableStateOf("fawry") }
        var paymentAccountInput by remember { mutableStateOf(user?.phone ?: "") }
        var isPayingStep by remember { mutableStateOf(false) }
        val fawryRefCode = remember { "7889" + (10000000..99999999).random().toString() }

        val plans = listOf(
            Triple("free", stringResource(R.string.plan_free_name), 0.0),
            Triple("premium", stringResource(R.string.plan_premium_name), 99.0),
            Triple("business", stringResource(R.string.plan_business_name), 249.0)
        )

        val egyptianPaymentMethods = listOf(
            "fawry" to stringResource(R.string.payment_fawry),
            "vodafone_cash" to stringResource(R.string.payment_vodafone_cash),
            "instapay" to stringResource(R.string.payment_instapay),
            "orange_cash" to stringResource(R.string.payment_orange_cash),
            "etisalat_cash" to stringResource(R.string.payment_etisalat_cash),
            "we_pay" to stringResource(R.string.payment_we_pay),
            "card" to stringResource(R.string.payment_meeza_card)
        )

        AlertDialog(
            onDismissRequest = { showPlansDialog.value = false },
            containerColor = GeoDarkSurface,
            title = {
                Text(
                    text = if (isPayingStep) stringResource(R.string.choose_egypt_payment_title) else stringResource(R.string.subscription_plans_title),
                    color = GeoDarkOnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isPayingStep) {
                        // Step 1: Select Plan
                        // Free Plan
                        Surface(
                            color = if (selectedPlan == "free") GeoTealContainer else Color(0xFFF8F9FE),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (selectedPlan == "free") 2.dp else 1.dp,
                                if (selectedPlan == "free") GeoTealPrimary else GeoDarkOutline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlan = "free" }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.plan_free_name), color = GeoDarkOnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(stringResource(R.string.plan_free_price), color = GeoGreenOnline, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(stringResource(R.string.plan_free_desc), color = Color(0xFF64748B), fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }

                        // Premium Plan
                        Surface(
                            color = if (selectedPlan == "premium") GeoTealContainer else Color(0xFFF8F9FE),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (selectedPlan == "premium") 2.dp else 1.dp,
                                if (selectedPlan == "premium") GeoTealPrimary else GeoDarkOutline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlan = "premium" }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.plan_premium_name), color = GeoTealPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(stringResource(R.string.plan_premium_price), color = GeoTealPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(stringResource(R.string.plan_premium_desc), color = Color(0xFF64748B), fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }

                        // Business Pro Plan
                        Surface(
                            color = if (selectedPlan == "business") Color(0xFFFEF3C7) else Color(0xFFF8F9FE),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (selectedPlan == "business") 2.dp else 1.dp,
                                if (selectedPlan == "business") GeoGold else GeoDarkOutline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlan = "business" }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.plan_business_name), color = GeoGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(stringResource(R.string.plan_business_price), color = GeoGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(stringResource(R.string.plan_business_desc), color = Color(0xFF64748B), fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    } else {
                        // Step 2: Select Egyptian Payment Gateway
                        val currentPlanObj = plans.firstOrNull { it.first == selectedPlan }
                        Text(
                            text = stringResource(R.string.total_required_amount, currentPlanObj?.third?.toInt() ?: 0),
                            fontWeight = FontWeight.Bold,
                            color = GeoDarkOnSurface,
                            fontSize = 14.sp
                        )

                        Text(
                            text = stringResource(R.string.available_egypt_gateways),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )

                        egyptianPaymentMethods.forEach { (key, label) ->
                            val isSelected = selectedPaymentMethod == key
                            Surface(
                                color = if (isSelected) (if (key == "fawry") Color(0xFFFFFBEB) else GeoTealContainer) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) (if (key == "fawry") Color(0xFFF59E0B) else GeoTealPrimary) else GeoDarkOutline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPaymentMethod = key }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPaymentMethod = key },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = if (key == "fawry") Color(0xFFD97706) else GeoTealPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        label,
                                        fontSize = 12.sp,
                                        color = GeoDarkOnSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // If Fawry is selected, show Fawry reference voucher & code
                        if (selectedPaymentMethod == "fawry") {
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCD34D)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.fawry_ref_code_label),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                        Text(
                                            text = fawryRefCode,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.fawry_instructions),
                                        fontSize = 11.sp,
                                        color = Color(0xFF78350F),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = paymentAccountInput,
                            onValueChange = { paymentAccountInput = it },
                            label = { Text(stringResource(R.string.wallet_or_phone_input_label)) },
                            placeholder = { Text(stringResource(R.string.wallet_phone_placeholder)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = GeoDarkOnSurface,
                                unfocusedTextColor = GeoDarkOnSurface,
                                focusedBorderColor = GeoTealPrimary,
                                unfocusedBorderColor = GeoDarkOutline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (!isPayingStep) {
                    Button(
                        onClick = {
                            if (selectedPlan == "free") {
                                viewModel.updateSubscriptionTier("free")
                                showPlansDialog.value = false
                            } else {
                                isPayingStep = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (selectedPlan == "free") stringResource(R.string.confirm_free_plan) else stringResource(R.string.proceed_egypt_payment),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            val price = if (selectedPlan == "premium") 99.0 else 249.0
                            viewModel.processEgyptianPayment(
                                method = selectedPaymentMethod,
                                plan = selectedPlan,
                                amountEgp = price,
                                phoneNumberOrRef = paymentAccountInput,
                                onSuccess = {
                                    showPlansDialog.value = false
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedPaymentMethod == "fawry") Color(0xFFD97706) else GeoGreenOnline),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.confirm_and_pay),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (isPayingStep) isPayingStep = false
                        else showPlansDialog.value = false
                    }
                ) {
                    Text(
                        text = if (isPayingStep) stringResource(R.string.back_to_plans) else stringResource(R.string.cancel_button),
                        color = Color(0xFF64748B)
                    )
                }
            }
        )
    }
}
