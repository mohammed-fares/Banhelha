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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.LocalServiceEntity
import com.example.data.model.UserEntity
import com.example.ui.components.ServiceCard
import com.example.ui.components.ServiceOrderCard
import com.example.ui.theme.*
import com.example.viewmodel.GeoConnectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesMarketplaceScreen(
    viewModel: GeoConnectViewModel,
    onNavigateToChat: (UserEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val allServices by viewModel.allServices.collectAsStateWithLifecycle()
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val activeUsers by viewModel.activeUsers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userLat by viewModel.userLat.collectAsStateWithLifecycle()
    val userLng by viewModel.userLng.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("marketplace") } // "marketplace" or "orders"
    val searchQuery = remember { mutableStateOf("") }
    val showAddServiceDialog = remember { mutableStateOf(false) }
    var orderingService by remember { mutableStateOf<LocalServiceEntity?>(null) }
    val context = LocalContext.current

    val categories = listOf(
        "all" to "الكل 🌐",
        "sweets" to "حلويات ومخابز 🍰",
        "food" to "مطاعم وكافيهات ☕",
        "groceries" to "سوبرماركت وبقالة 🛒",
        "tech" to "فنيين وصيانة ⚡",
        "transport" to "توصيل ومواصلات 🚕",
        "health" to "صيدليات وعيادات 🏥",
        "shops" to "متاجر وأزياء 🛍️",
        "crafts" to "ورش وحرف 🔨"
    )

    val filteredServices = remember(allServices, selectedCategory, searchQuery.value) {
        allServices.filter { service ->
            val matchesCat = selectedCategory == "all" || service.category == selectedCategory
            val matchesQuery = searchQuery.value.isBlank() ||
                    service.title.contains(searchQuery.value, ignoreCase = true) ||
                    service.description.contains(searchQuery.value, ignoreCase = true) ||
                    service.tags.contains(searchQuery.value, ignoreCase = true) ||
                    service.address.contains(searchQuery.value, ignoreCase = true) ||
                    service.providerName.contains(searchQuery.value, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GeoDarkBackground,
        floatingActionButton = {
            if (activeTab == "marketplace") {
                FloatingActionButton(
                    onClick = { showAddServiceDialog.value = true },
                    containerColor = GeoTealPrimary,
                    contentColor = GeoTealOnPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_service_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Store")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة متجر / خدمة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
        ) {
            // Screen Header & Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "خدمات ومتاجر بنحلها",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeoDarkOnSurface
                    )
                    Text(
                        text = "اطلب خدمة أو أهدِ صديقك طلب مدفوع مقدماً 🎁",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Import from Google Maps Button
                OutlinedButton(
                    onClick = { viewModel.importGoogleMapsStores() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFE8F0FE),
                        contentColor = Color(0xFF1A73E8)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF1A73E8), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استيراد من Google", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Top Tab Switcher: Marketplace vs Orders
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE2E8F0))
                    .padding(3.dp)
            ) {
                TabButton(
                    title = "🏪 دليل المتاجر والخدمات",
                    isSelected = activeTab == "marketplace",
                    onClick = { activeTab = "marketplace" },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    title = "🎁 طلباتي والهدايا (${allOrders.size})",
                    isSelected = activeTab == "orders",
                    onClick = { activeTab = "orders" },
                    modifier = Modifier.weight(1f)
                )
            }

            if (activeTab == "marketplace") {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery.value,
                    onValueChange = { searchQuery.value = it },
                    placeholder = { Text("بحث عن متجر، حلواني، سوبرماركت، صيانة...", fontSize = 12.sp, color = Color(0xFF74777F)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GeoTealPrimary, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.value.isNotEmpty()) {
                            IconButton(onClick = { searchQuery.value = "" }, modifier = Modifier.size(20.dp)) {
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
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Carousel
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { (catKey, catTitle) ->
                        val isSelected = selectedCategory == catKey
                        Surface(
                            color = if (isSelected) GeoTealPrimary else Color(0xFFF1F3F9),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) GeoTealPrimary else GeoDarkOutline
                            ),
                            modifier = Modifier
                                .clickable { viewModel.selectedCategory.value = catKey }
                        ) {
                            Text(
                                text = catTitle,
                                color = if (isSelected) GeoTealOnPrimary else Color(0xFF49454F),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Services List
                if (filteredServices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("لا توجد متاجر أو خدمات متاحة في هذا التصنيف", color = GeoDarkOnSurface, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("يمكنك استيراد متاجر ومحلات مباشرة من Google Maps!", color = Color(0xFF64748B), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.importGoogleMapsStores() },
                                colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("استيراد بيانات Google Maps الآن", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredServices, key = { it.id }) { service ->
                            val distMeters = viewModel.calculateDistanceMeters(
                                userLat, userLng, service.latitude, service.longitude
                            )
                            ServiceCard(
                                service = service,
                                distanceStr = viewModel.formatDistance(distMeters),
                                onCallClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${service.phone}"))
                                    context.startActivity(dialIntent)
                                },
                                onChatClick = {
                                    val dummyProviderUser = UserEntity(
                                        id = 100 + service.id,
                                        phone = service.phone,
                                        name = service.providerName,
                                        age = 35,
                                        gender = "male",
                                        bio = service.title,
                                        interests = service.tags,
                                        latitude = service.latitude,
                                        longitude = service.longitude,
                                        isVerified = service.isVerified,
                                        subscriptionType = "business"
                                    )
                                    viewModel.openChatWith(dummyProviderUser)
                                    onNavigateToChat(dummyProviderUser)
                                },
                                onGoogleMapsClick = {
                                    val mapsUrl = if (service.googleMapsUrl.isNotBlank()) service.googleMapsUrl
                                    else "geo:${service.latitude},${service.longitude}?q=${Uri.encode(service.title)}"
                                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
                                    context.startActivity(mapIntent)
                                },
                                onOrderOrGiftClick = {
                                    orderingService = service
                                }
                            )
                        }
                    }
                }
            } else {
                // Orders and Gifts Tracking View
                if (allOrders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("لم تقم بطلب أو إهداء خدمات حتى الآن", color = GeoDarkOnSurface, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("اختر أي متجر أو خدمة من الدليل وأرسلها كهدية مدفوعة لأصدقائك!", color = Color(0xFF64748B), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { activeTab = "marketplace" },
                                colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("تصفح دليل الخدمات والمتاجر", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allOrders, key = { it.id }) { order ->
                            ServiceOrderCard(
                                order = order,
                                onChatWithProvider = {
                                    val providerUser = UserEntity(
                                        id = 100 + order.serviceId,
                                        phone = order.recipientPhone,
                                        name = order.providerName,
                                        age = 35,
                                        gender = "male",
                                        bio = order.serviceTitle,
                                        interests = "خدمات",
                                        latitude = userLat,
                                        longitude = userLng,
                                        isVerified = true
                                    )
                                    viewModel.openChatWith(providerUser)
                                    onNavigateToChat(providerUser)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Order / Gift Bottom Sheet Modal ---
    orderingService?.let { service ->
        var isGift by remember { mutableStateOf(true) }
        var selectedRecipientUser by remember { mutableStateOf<UserEntity?>(null) }
        var recipientName by remember { mutableStateOf("") }
        var recipientPhone by remember { mutableStateOf("") }
        var orderItems by remember { mutableStateOf("طلب خاص من ${service.title}") }
        var orderPrice by remember { mutableStateOf("250") }
        var deliveryAddress by remember { mutableStateOf(service.address) }
        var giftGreeting by remember { mutableStateOf("ألف مبروك وبالهنا والشفاء يا غالي! 🎉") }
        var paymentGateway by remember { mutableStateOf("fawry") } // "fawry", "vodafone_cash", "instapay", "card"

        // Candidate users for gifting (exclude self)
        val candidateRecipients = remember(activeUsers, currentUser) {
            activeUsers.filter { it.id != currentUser?.id }
        }

        ModalBottomSheet(
            onDismissRequest = { orderingService = null },
            containerColor = GeoDarkSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEDE9FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎁", fontSize = 22.sp)
                    }
                    Column {
                        Text(
                            text = "طلب خدمة / إهداء لمستخدم",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeoDarkOnSurface
                        )
                        Text(
                            text = "المتجر: ${service.title} • الدفع المسبق من المرسل ✔",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Selector: Gift vs Personal Order
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(3.dp)
                ) {
                    TabButton(
                        title = "🎁 إهداء لصديق / مستخدم",
                        isSelected = isGift,
                        onClick = { isGift = true },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        title = "🛍️ طلب شخصي لي",
                        isSelected = !isGift,
                        onClick = { isGift = false },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isGift) {
                    Text("اختر الصديق أو المستخدم المستلم للهديّة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GeoDarkOnSurface)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(candidateRecipients) { user ->
                            val isSelected = selectedRecipientUser?.id == user.id
                            Surface(
                                color = if (isSelected) Color(0xFF8B5CF6) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isSelected) Color(0xFF7C3AED) else GeoDarkOutline
                                ),
                                modifier = Modifier.clickable {
                                    selectedRecipientUser = user
                                    recipientName = user.name
                                    recipientPhone = user.phone
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else GeoTealContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.name.take(1),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF7C3AED) else GeoTealPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = user.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else GeoDarkOnSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = { recipientName = it },
                        label = { Text("اسم المستلم (الصديق)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = GeoDarkOutline
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = giftGreeting,
                        onValueChange = { giftGreeting = it },
                        label = { Text("رسالة الإهداء وكارت التهنئة 💌") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = GeoDarkOutline
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Order Details
                OutlinedTextField(
                    value = orderItems,
                    onValueChange = { orderItems = it },
                    label = { Text("تفاصيل الطلب / الهدية من المتجر") },
                    placeholder = { Text("مثال: علبة مشكل حلويات 1 كجم، باقة ورد، صيانة تكييف...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoTealPrimary,
                        unfocusedBorderColor = GeoDarkOutline
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = orderPrice,
                        onValueChange = { orderPrice = it },
                        label = { Text("المبلغ (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoTealPrimary,
                            unfocusedBorderColor = GeoDarkOutline
                        )
                    )

                    OutlinedTextField(
                        value = deliveryAddress,
                        onValueChange = { deliveryAddress = it },
                        label = { Text("عنوان التوصيل") },
                        modifier = Modifier.weight(1.5f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeoTealPrimary,
                            unfocusedBorderColor = GeoDarkOutline
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Gateway Selector (Fawry, Vodafone Cash, InstaPay)
                Text("اختر وسيلة الدفع المسبق (بوابات الدفع المصرية):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GeoDarkOnSurface)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentChip(
                        title = "فوري 🟡",
                        subtitle = "كود سداد",
                        isSelected = paymentGateway == "fawry",
                        onClick = { paymentGateway = "fawry" },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentChip(
                        title = "فودافون كاش 🔴",
                        subtitle = "محفظة",
                        isSelected = paymentGateway == "vodafone_cash",
                        onClick = { paymentGateway = "vodafone_cash" },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentChip(
                        title = "إنستاباي 🟣",
                        subtitle = "تحويل لحظي",
                        isSelected = paymentGateway == "instapay",
                        onClick = { paymentGateway = "instapay" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pricing Summary Box
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("قيمة الطلب:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("${orderPrice.ifBlank { "0" }} ج.م", fontSize = 12.sp, color = GeoDarkOnSurface, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("رسوم التوصيل والخدمة:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("مجاناً 🎁", fontSize = 12.sp, color = GeoGreenOnline, fontWeight = FontWeight.Bold)
                        }
                        Divider(modifier = Modifier.padding(vertical = 6.dp), color = GeoDarkOutline.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الإجمالي المدفوع مقدماً:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GeoDarkOnSurface)
                            Text("${orderPrice.ifBlank { "0" }} ج.م", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GeoTealPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Payment and Order Button
                Button(
                    onClick = {
                        val priceVal = orderPrice.toDoubleOrNull() ?: 200.0
                        viewModel.placeServiceOrder(
                            service = service,
                            orderItems = orderItems,
                            price = priceVal,
                            paymentGateway = paymentGateway,
                            isGift = isGift,
                            recipientUser = selectedRecipientUser,
                            recipientName = recipientName.ifBlank { "صديق عزيز" },
                            recipientPhone = recipientPhone.ifBlank { "+201000000000" },
                            deliveryAddress = deliveryAddress,
                            giftGreeting = giftGreeting
                        )
                        orderingService = null
                        activeTab = "orders"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGift) Color(0xFF7C3AED) else GeoTealPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_order_button")
                ) {
                    Icon(
                        if (isGift) Icons.Default.CardGiftcard else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isGift) "تأكيد الإهداء والدفع المسبق 🎁" else "تأكيد طلب الخدمة والدفع ✔",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Add Service Dialog
    if (showAddServiceDialog.value) {
        val title = remember { mutableStateOf("") }
        val category = remember { mutableStateOf("tech") }
        val description = remember { mutableStateOf("") }
        val provider = remember { mutableStateOf(viewModel.currentUser.value?.name ?: "Provider") }
        val phone = remember { mutableStateOf(viewModel.currentUser.value?.phone ?: "+201000000000") }
        val price = remember { mutableStateOf("150 ج.م") }
        val tags = remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddServiceDialog.value = false },
            containerColor = GeoDarkSurface,
            title = {
                Text("إضافة متجر أو خدمة محلية جديدة", color = GeoDarkOnSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title.value,
                        onValueChange = { title.value = it },
                        label = { Text("اسم المتجر أو الخدمة (مثال: حلواني العبد)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GeoDarkOnSurface,
                            unfocusedTextColor = GeoDarkOnSurface,
                            focusedBorderColor = GeoTealPrimary,
                            unfocusedBorderColor = GeoDarkOutline
                        )
                    )

                    OutlinedTextField(
                        value = description.value,
                        onValueChange = { description.value = it },
                        label = { Text("وصف المنتجات والخدمات المتاحة") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GeoDarkOnSurface,
                            unfocusedTextColor = GeoDarkOnSurface,
                            focusedBorderColor = GeoTealPrimary,
                            unfocusedBorderColor = GeoDarkOutline
                        )
                    )

                    OutlinedTextField(
                        value = tags.value,
                        onValueChange = { tags.value = it },
                        label = { Text("الكلمات المفتاحية (حلويات، هدايا، تورتة، صيانة)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GeoDarkOnSurface,
                            unfocusedTextColor = GeoDarkOnSurface,
                            focusedBorderColor = GeoTealPrimary,
                            unfocusedBorderColor = GeoDarkOutline
                        )
                    )

                    OutlinedTextField(
                        value = phone.value,
                        onValueChange = { phone.value = it },
                        label = { Text("رقم هاتف المتجر") },
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
                        if (title.value.isNotBlank()) {
                            viewModel.addNewService(
                                title = title.value,
                                category = category.value,
                                description = description.value,
                                providerName = provider.value,
                                phone = phone.value,
                                priceRange = price.value,
                                tags = tags.value
                            )
                            showAddServiceDialog.value = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نشر في الدليل", color = GeoTealOnPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddServiceDialog.value = false }) {
                    Text("إلغاء", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) GeoDarkSurface else Color.Transparent,
        shape = RoundedCornerShape(9.dp),
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) GeoTealPrimary else Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
        )
    }
}

@Composable
private fun PaymentChip(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) Color(0xFFF3E8FF) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) Color(0xFF7C3AED) else GeoDarkOutline
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF7C3AED) else GeoDarkOnSurface,
                maxLines = 1
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}
