package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authManager: AuthManager,
    isLanguageArabic: Boolean,
    onLanguageToggle: (Boolean) -> Unit,
    onAuthSuccess: (UserProfile) -> Unit
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Parent") } // "Parent", "Teacher", "Manager"
    var isPasswordVisible by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Translations local helper
    fun t(en: String, ar: String) = if (isLanguageArabic) ar else en

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
    ) {
        // Decorative background top banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(PrimaryNavy, RoyalBlue)
                    )
                )
        ) {
            // Background aesthetics (nested bubbles)
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-30).dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 40.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Language selector top bar
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (!isLanguageArabic) Color.White else Color.Transparent)
                        .clickable { onLanguageToggle(false) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "EN",
                        color = if (!isLanguageArabic) PrimaryNavy else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isLanguageArabic) Color.White else Color.Transparent)
                        .clickable { onLanguageToggle(true) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "عر",
                        color = if (isLanguageArabic) PrimaryNavy else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // School Logo and Title Pairings
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.radialGradient(listOf(GoldAccent, CrimsonRed)),
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .border(
                        2.dp, Color.White,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = t("OEKS", "أمدر"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = t("Omdur English Schools", "مدارس أمدر الإنجليزية"),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = t("Bilingual School Management System", "نظام إدارة المدارس الثنائي اللغة"),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Form container Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = WhiteSurface,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Sign In / Register toggle row
                    TabRow(
                        selectedTabIndex = if (isSignUpMode) 1 else 0,
                        containerColor = PageBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        indicator = { Box(modifier = Modifier.size(0.dp)) },
                        divider = { Box(modifier = Modifier.size(0.dp)) }
                    ) {
                        Tab(
                            selected = !isSignUpMode,
                            onClick = { 
                                isSignUpMode = false
                                errorMessage = null
                                successMessage = null
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isSignUpMode) RoyalBlue else Color.Transparent)
                        ) {
                            Text(
                                text = t("Sign In", "تسجيل دخول"),
                                color = if (!isSignUpMode) Color.White else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Tab(
                            selected = isSignUpMode,
                            onClick = { 
                                isSignUpMode = true 
                                errorMessage = null
                                successMessage = null
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSignUpMode) RoyalBlue else Color.Transparent)
                        ) {
                            Text(
                                text = t("Register", "حساب جديد"),
                                color = if (isSignUpMode) Color.White else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (isSignUpMode) t("Create Academic Account", "إنشاء حساب أكاديمي جديد") else t("Welcome Back", "مرحباً بك مجدداً"),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isLanguageArabic) TextAlign.Right else TextAlign.Left
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            errorMessage = null
                        },
                        label = { Text(t("Academic Email", "البريد الإلكتروني الدراسي")) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = RoyalBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            errorMessage = null
                        },
                        label = { Text(t("Password (min 6 characters)", "كلمة المرور (٦ أحرف على الأقل)")) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = RoyalBlue) },
                        trailingIcon = {
                            TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Text(
                                    text = if (isPasswordVisible) t("Hide", "إخفاء") else t("Show", "إظهار"),
                                    color = RoyalBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true
                    )

                    // SignUp Role Selector
                    if (isSignUpMode) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = t("Select Academic Role / الصفة الأكاديمية", "اختر الصفة التعليمية للأكاديمي:"),
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = if (isLanguageArabic) TextAlign.Right else TextAlign.Left
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val roles = listOf(
                                Triple("Parent", t("Parent", "ولي الأمر"), t("View children grades & live class reports.", "متابعة درجات وجداول الفصول والتقارير لأبنائك.")),
                                Triple("Teacher", t("Teacher", "المعلم"), t("Input gradebooks & capture bilingual reports.", "رصد الدرجات وتسجيل التقارير الصوتية بالفصل.")),
                                Triple("Manager", t("Manager", "المدير"), t("Approve registrations & broadcast announcements.", "الموافقة على طلبات الالتحاق ونشر الإعلانات العامة."))
                            )

                            roles.forEach { (roleId, title, desc) ->
                                val isSelected = selectedRole == roleId
                                Card(
                                    onClick = { selectedRole = roleId },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) LightBlueBg else PageBg
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) RoyalBlue else BorderColor
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedRole = roleId },
                                            colors = RadioButtonDefaults.colors(selectedColor = RoyalBlue)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(
                                                text = title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isSelected) RoyalBlue else TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = desc,
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Progress Loader / Status Box
                    AnimatedVisibility(visible = errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            border = BorderStroke(1.dp, CrimsonRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = CrimsonRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = successMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LightGreenBg),
                            border = BorderStroke(1.dp, GreenState),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = GreenState, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = successMessage ?: "",
                                    color = GreenState,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ACTION BUTTON
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = t("Email and password fields are required.", "يرجى ملء حقلي البريد الإلكتروني وكلمة المرور.")
                                return@Button
                            }
                            if (password.length < 6) {
                                errorMessage = t("Password must be at least 6 characters.", "يجب ألا تقل كلمة المرور عن ٦ رموز.")
                                return@Button
                            }

                            isLoading = true
                            errorMessage = null
                            successMessage = null

                            if (isSignUpMode) {
                                authManager.registerWithFirebase(
                                    email = email.trim(),
                                    password = password,
                                    role = selectedRole,
                                    onSuccess = { user ->
                                        isLoading = false
                                        successMessage = t("Registration successful! Logging in...", "تم إنشاء الحساب بنجاح! جاري الدخول...")
                                        onAuthSuccess(user)
                                    },
                                    onFailure = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    }
                                )
                            } else {
                                authManager.signInWithFirebase(
                                    email = email.trim(),
                                    password = password,
                                    onSuccess = { user ->
                                        isLoading = false
                                        successMessage = t("Sign-in successful!", "تم تسجيل الدخول بنجاح!")
                                        onAuthSuccess(user)
                                    },
                                    onFailure = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                text = if (isSignUpMode) t("Create Account", "إنشاء الحساب") else t("Access System", "دخول النظام"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BYPASS & FIREBASE DIAGNOSTIC SECTION
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = WhiteSurface,
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Firebase Connectivity Diagnostics Card Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isConnected = authManager.isFirebaseConfigured()
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isConnected) GreenState else YellowState, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isConnected) t("Firebase State: Connected (Production)", "قاعدة Firebase: متصلة بنجاح (رسمي)") 
                                   else t("Firebase State: Standalone Sandbox active", "قاعدة Firebase: غير مهيأة (وضع المحاكاة نشط)"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) GreenState else OrangeText
                        )
                    }

                    if (!authManager.isFirebaseConfigured()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = t(
                                "No google-services.json detected. Use the quick sandbox options below to bypass authentication and review full dashboard prototype views instantly.",
                                "لم يتم العثور على ملف الإعدادات المخصص. استخدم خيارات الدخول السريع أدناه لاستعراض كامل لوحات التحكم المخصصة لكل دور فوراً."
                            ),
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Divider(color = BorderColor.copy(alpha = 0.6f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = t("Quick Demo Access / الدخول السريع كزائر", "لوحات العرض السريع المباشر:"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isLanguageArabic) TextAlign.Right else TextAlign.Left
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Demo Parent Bypass button
                        Button(
                            onClick = {
                                authManager.signInSandbox("parent.demo@oeks.edu.eg", "Parent") { user ->
                                    onAuthSuccess(user)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LightBlueBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text(
                                text = t("Parent View: Ahmad & Sara", "عرض ولي الأمر: أحمد وسارة"),
                                color = RoyalBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Demo Teacher Bypass button
                        Button(
                            onClick = {
                                authManager.signInSandbox("teacher.hassan@oeks.edu.eg", "Teacher") { user ->
                                    onAuthSuccess(user)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LightGreenBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text(
                                text = t("Teacher View: Mr. Mohamed Hassan", "عرض المعلم: أ/ محمد حسن"),
                                color = GreenState,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Demo Manager Bypass button
                        Button(
                            onClick = {
                                authManager.signInSandbox("admin.genawi@oeks.edu.eg", "Manager") { user ->
                                    onAuthSuccess(user)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LightOrangeBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text(
                                text = t("Manager View: School Administration", "عرض الإدارة: مدير النظام"),
                                color = OrangeText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
