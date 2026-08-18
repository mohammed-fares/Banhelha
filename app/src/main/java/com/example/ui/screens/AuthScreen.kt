package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.theme.*
import com.example.viewmodel.AuthStep
import com.example.viewmodel.GeoConnectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: GeoConnectViewModel,
    modifier: Modifier = Modifier
) {
    val authStep by viewModel.authStep.collectAsStateWithLifecycle()
    val phoneInput by viewModel.phoneInput.collectAsStateWithLifecycle()
    val otpInput by viewModel.otpInput.collectAsStateWithLifecycle()
    val generatedOtp by viewModel.generatedOtp.collectAsStateWithLifecycle()
    val emailInput by viewModel.emailInput.collectAsStateWithLifecycle()
    val passwordInput by viewModel.passwordInput.collectAsStateWithLifecycle()
    val regName by viewModel.regName.collectAsStateWithLifecycle()
    val regAge by viewModel.regAge.collectAsStateWithLifecycle()
    val regGender by viewModel.regGender.collectAsStateWithLifecycle()
    val regBio by viewModel.regBio.collectAsStateWithLifecycle()
    val regInterests by viewModel.regInterests.collectAsStateWithLifecycle()
    val errorMessage by viewModel.authErrorMessage.collectAsStateWithLifecycle()
    val isGlobalLoading by viewModel.isGlobalLoading.collectAsStateWithLifecycle()
    val globalLoadingMessage by viewModel.globalLoadingMessage.collectAsStateWithLifecycle()

    var authModeTab by remember { mutableIntStateOf(0) } // 0: Google & Phone, 1: Email/Password

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GeoDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo & Brand Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(GeoTealContainer)
                    .border(1.5.dp, GeoTealPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationSearching,
                    contentDescription = "Logo",
                    tint = GeoTealPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GeoDarkOnSurface,
                letterSpacing = 1.sp
            )
            Text(
                text = stringResource(R.string.tagline),
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Error notice
            if (errorMessage != null) {
                Surface(
                    color = GeoRedError.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeoRedError.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = GeoRedError, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = GeoRedError,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Auth Step Content Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = GeoDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    AnimatedContent(
                        targetState = authStep,
                        label = "auth_step_transition"
                    ) { step ->
                        when (step) {
                            AuthStep.PHONE_INPUT -> {
                                Column {
                                    // Mode Switcher Tabs
                                    TabRow(
                                        selectedTabIndex = authModeTab,
                                        containerColor = Color(0xFFF1F5F9),
                                        contentColor = GeoTealPrimary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        Tab(
                                            selected = authModeTab == 0,
                                            onClick = { authModeTab = 0 },
                                            text = { Text("الهاتف & Google", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                        )
                                        Tab(
                                            selected = authModeTab == 1,
                                            onClick = { authModeTab = 1 },
                                            text = { Text("البريد الإلكتروني", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    if (authModeTab == 0) {
                                        // Google Sign-In Button (Credential Manager + Firebase Auth)
                                        Button(
                                            onClick = { viewModel.signInWithGoogle() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("google_signin_button"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "تسجيل الدخول باستخدام Google",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Divider(modifier = Modifier.weight(1f), color = GeoDarkOutline)
                                            Text(" أو برقم الهاتف ", color = Color(0xFF94A3B8), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                            Divider(modifier = Modifier.weight(1f), color = GeoDarkOutline)
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))

                                        OutlinedTextField(
                                            value = phoneInput,
                                            onValueChange = { viewModel.phoneInput.value = it },
                                            label = { Text("رقم الهاتف") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Phone, contentDescription = null, tint = GeoTealPrimary)
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = GeoTealPrimary,
                                                unfocusedBorderColor = GeoDarkOutline,
                                                focusedTextColor = GeoDarkOnSurface,
                                                unfocusedTextColor = GeoDarkOnSurface,
                                                focusedContainerColor = Color(0xFFF8F9FE),
                                                unfocusedContainerColor = Color(0xFFF8F9FE)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("phone_input")
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { viewModel.sendOtp() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .testTag("send_otp_button"),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary)
                                        ) {
                                            Text(
                                                text = "إرسال رمز التحقق (OTP) ›",
                                                color = GeoTealOnPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else {
                                        // Email / Password Form
                                        OutlinedTextField(
                                            value = emailInput,
                                            onValueChange = { viewModel.emailInput.value = it },
                                            label = { Text("البريد الإلكتروني") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Email, contentDescription = null, tint = GeoTealPrimary)
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = GeoTealPrimary,
                                                unfocusedBorderColor = GeoDarkOutline,
                                                focusedTextColor = GeoDarkOnSurface,
                                                unfocusedTextColor = GeoDarkOnSurface,
                                                focusedContainerColor = Color(0xFFF8F9FE),
                                                unfocusedContainerColor = Color(0xFFF8F9FE)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        OutlinedTextField(
                                            value = passwordInput,
                                            onValueChange = { viewModel.passwordInput.value = it },
                                            label = { Text("كلمة المرور") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Lock, contentDescription = null, tint = GeoTealPrimary)
                                            },
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = GeoTealPrimary,
                                                unfocusedBorderColor = GeoDarkOutline,
                                                focusedTextColor = GeoDarkOnSurface,
                                                unfocusedTextColor = GeoDarkOnSurface,
                                                focusedContainerColor = Color(0xFFF8F9FE),
                                                unfocusedContainerColor = Color(0xFFF8F9FE)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.signInWithEmail(emailInput, passwordInput) },
                                                colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f).height(48.dp)
                                            ) {
                                                Text("تسجيل الدخول", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }

                                            OutlinedButton(
                                                onClick = { viewModel.signUpWithEmail(emailInput, passwordInput) },
                                                shape = RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, GeoTealPrimary),
                                                modifier = Modifier.weight(1f).height(48.dp)
                                            ) {
                                                Text("حساب جديد", color = GeoTealPrimary, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            AuthStep.OTP_VERIFICATION -> {
                                Column {
                                    Text(
                                        text = "التحقق من رقم الهاتف",
                                        color = GeoDarkOnSurface,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "تم إرسال رمز التحقق إلى $phoneInput",
                                        color = Color(0xFF64748B),
                                        fontSize = 13.sp
                                    )

                                    // Dev helper banner
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        color = Color(0xFFF1F3F9),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = GeoTealPrimary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "الرمز في وضع التطوير: $generatedOtp",
                                                color = GeoDarkOnSurface,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = otpInput,
                                        onValueChange = { viewModel.otpInput.value = it },
                                        label = { Text("رمز التحقق (6 أرقام)") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = GeoTealPrimary)
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = GeoTealPrimary,
                                            unfocusedBorderColor = GeoDarkOutline,
                                            focusedTextColor = GeoDarkOnSurface,
                                            unfocusedTextColor = GeoDarkOnSurface,
                                            focusedContainerColor = Color(0xFFF8F9FE),
                                            unfocusedContainerColor = Color(0xFFF8F9FE)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("otp_input")
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { viewModel.verifyOtp() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("verify_otp_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary)
                                    ) {
                                        Text(
                                            text = "تأكيد والدخول ›",
                                            color = GeoTealOnPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    TextButton(
                                        onClick = { viewModel.sendOtp() },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text("إعادة إرسال الرمز", color = GeoTealPrimary, fontSize = 13.sp)
                                    }
                                }
                            }

                            AuthStep.REGISTRATION -> {
                                Column {
                                    Text(
                                        text = "إنشاء حساب جديد",
                                        color = GeoDarkOnSurface,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "أكمل بيانات ملفك الشخصي لتظهر للأشخاص القريبين منك",
                                        color = Color(0xFF64748B),
                                        fontSize = 13.sp
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = regName,
                                        onValueChange = { viewModel.regName.value = it },
                                        label = { Text("الاسم الكامل") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = GeoTealPrimary)
                                        },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = GeoTealPrimary,
                                            unfocusedBorderColor = GeoDarkOutline,
                                            focusedTextColor = GeoDarkOnSurface,
                                            unfocusedTextColor = GeoDarkOnSurface,
                                            focusedContainerColor = Color(0xFFF8F9FE),
                                            unfocusedContainerColor = Color(0xFFF8F9FE)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reg_name_input")
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = regAge,
                                            onValueChange = { viewModel.regAge.value = it },
                                            label = { Text("العمر") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = GeoTealPrimary,
                                                unfocusedBorderColor = GeoDarkOutline,
                                                focusedTextColor = GeoDarkOnSurface,
                                                unfocusedTextColor = GeoDarkOnSurface,
                                                focusedContainerColor = Color(0xFFF8F9FE),
                                                unfocusedContainerColor = Color(0xFFF8F9FE)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        Column(modifier = Modifier.weight(1.5f)) {
                                            Text("الجنس", fontSize = 11.sp, color = Color(0xFF64748B))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                listOf("male" to "ذكر", "female" to "أنثى").forEach { (g, label) ->
                                                    val isSel = regGender == g
                                                    Surface(
                                                        color = if (isSel) GeoTealPrimary else Color(0xFFF1F3F9),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) GeoTealPrimary else GeoDarkOutline),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable { viewModel.regGender.value = g }
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            color = if (isSel) Color.White else GeoDarkOnSurface,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.padding(vertical = 8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = regBio,
                                        onValueChange = { viewModel.regBio.value = it },
                                        label = { Text("نبذة عنك (Bio)") },
                                        maxLines = 2,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = GeoTealPrimary,
                                            unfocusedBorderColor = GeoDarkOutline,
                                            focusedTextColor = GeoDarkOnSurface,
                                            unfocusedTextColor = GeoDarkOnSurface,
                                            focusedContainerColor = Color(0xFFF8F9FE),
                                            unfocusedContainerColor = Color(0xFFF8F9FE)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = regInterests,
                                        onValueChange = { viewModel.regInterests.value = it },
                                        label = { Text("الاهتمامات (مثال: برمجيات، رياضة، قهوة)") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = GeoTealPrimary,
                                            unfocusedBorderColor = GeoDarkOutline,
                                            focusedTextColor = GeoDarkOnSurface,
                                            unfocusedTextColor = GeoDarkOnSurface,
                                            focusedContainerColor = Color(0xFFF8F9FE),
                                            unfocusedContainerColor = Color(0xFFF8F9FE)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { viewModel.registerNewUser() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("finish_register_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GeoTealPrimary)
                                    ) {
                                        Text(
                                            text = "إكمال التسجيل والدخول 📍",
                                            color = GeoTealOnPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }

                            AuthStep.AUTHENTICATED -> {
                                Box(modifier = Modifier.size(1.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // One-tap Quick Demo Login button
            OutlinedButton(
                onClick = { viewModel.quickLoginAsTestUser() },
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoDarkOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("demo_login_button")
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = GeoTealPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "دخول سريع بالمستخدم التجريبي (Test User #56)",
                    color = GeoTealPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Global Loading Progress Overlay
        if (isGlobalLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 10.dp,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = GeoTealPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = globalLoadingMessage ?: "جاري المعالجة...",
                            color = GeoDarkOnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
