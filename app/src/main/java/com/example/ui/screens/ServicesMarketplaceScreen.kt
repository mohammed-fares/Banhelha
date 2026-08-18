package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserEntity
import com.example.ui.components.ServiceCard
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
    val userLat by viewModel.userLat.collectAsStateWithLifecycle()
    val userLng by viewModel.userLng.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery = remember { mutableStateOf("") }
    val showAddServiceDialog = remember { mutableStateOf(false) }
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
        ) {
            // Header with Google Maps Import Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "دليل المتاجر والخدمات المحلية",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeoDarkOnSurface
                    )
                    Text(
                        text = "محلات، مخابز، فنيين، وعيادات موثقة في مصر",
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
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF1A73E8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استيراد من Google", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Compact Search Bar
            OutlinedTextField(
                value = searchQuery.value,
                onValueChange = { searchQuery.value = it },
                placeholder = { Text("بحث عن متجر أو خدمة (حلواني، صيانة، صيدلية)...", fontSize = 12.sp, color = Color(0xFF74777F)) },
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

            // Category Chips Carousel
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

            Spacer(modifier = Modifier.height(10.dp))

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
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Service Dialog
    if (showAddServiceDialog.value) {
        val title = remember { mutableStateOf("") }
        val category = remember { mutableStateOf("tech") }
        val description = remember { mutableStateOf("") }
        val provider = remember { mutableStateOf(viewModel.currentUser.value?.name ?: "Provider") }
        val phone = remember { mutableStateOf(viewModel.currentUser.value?.phone ?: "+971500000000") }
        val price = remember { mutableStateOf("$$") }
        val tags = remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddServiceDialog.value = false },
            containerColor = GeoDarkSurface,
            title = {
                Text("إضافة خدمة محلية جديدة", color = GeoDarkOnSurface, fontWeight = FontWeight.Bold)
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
                        label = { Text("عنوان الخدمة (مثال: فني تكييف وكهرباء)") },
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
                        label = { Text("تفاصيل الخدمة ومميزاتها") },
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
                        label = { Text("الكلمات المفتاحية (صيانة، تكييف، طوارئ)") },
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
                        label = { Text("رقم التواصل") },
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
                    Text("نشر الخدمة", color = GeoTealOnPrimary, fontWeight = FontWeight.Bold)
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
