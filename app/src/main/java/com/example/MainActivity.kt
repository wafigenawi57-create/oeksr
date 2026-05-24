package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainAppScreen()
      }
    }
  }
}

// Global state container
@Composable
fun MainAppScreen() {
  val coroutineScope = rememberCoroutineScope()
  
  // App-level state variables
  var isArabicState by remember { mutableStateOf(false) }
  
  val context = LocalContext.current
  val authManager = remember { AuthManager(context) }
  var currentUser by remember { mutableStateOf<UserProfile?>(authManager.getCurrentUser()) }
  
  var activeRole by remember { mutableStateOf(currentUser?.role ?: "Parent") } // "Parent", "Teacher", "Manager", "Enroll"
  
  // Sync activeRole if currentUser changes
  LaunchedEffect(currentUser) {
    currentUser?.let {
      activeRole = it.role
    }
  }
  
  // Bottom-nav navigation state (maps to subtabs of active role)
  var bottomNavTab by remember { mutableStateOf("Home") } // "Home", "Payments", "AI Help", "Alerts", "Enroll"

  // Parent view states
  var selectedChildId by remember { mutableStateOf("ahmad") }
  val activeChild = remember(selectedChildId) {
    MockRepo.children.firstOrNull { it.id == selectedChildId } ?: MockRepo.children[0]
  }
  var parentSubTab by remember { mutableStateOf("Overview") } // "Overview", "Grades", "Live Class", "Payments", "Teachers", "Board", "Alerts", "AI Help"

  // Sync Bottom Navigation actions to Subtabs
  LaunchedEffect(bottomNavTab) {
    if (activeRole == "Parent") {
      when (bottomNavTab) {
        "Home" -> parentSubTab = "Overview"
        "Payments" -> parentSubTab = "Payments"
        "AI Help" -> parentSubTab = "AI Help"
        "Alerts" -> parentSubTab = "Alerts"
        "Enroll" -> {
          activeRole = "Enroll"
          bottomNavTab = "Enroll"
        }
      }
    } else if (activeRole == "Enroll" && bottomNavTab != "Enroll") {
      activeRole = "Parent" // fallback if clicked some other tab while in enroll
      when (bottomNavTab) {
        "Home" -> parentSubTab = "Overview"
        "Payments" -> parentSubTab = "Payments"
        "AI Help" -> parentSubTab = "AI Help"
        "Alerts" -> parentSubTab = "Alerts"
      }
    }
  }

  // Force sync subtab back to bottom nav if user switches subtab directly
  LaunchedEffect(parentSubTab) {
    if (activeRole == "Parent") {
      when (parentSubTab) {
        "Overview" -> if (bottomNavTab != "Home") bottomNavTab = "Home"
        "Payments" -> if (bottomNavTab != "Payments") bottomNavTab = "Payments"
        "AI Help" -> if (bottomNavTab != "AI Help") bottomNavTab = "AI Help"
        "Alerts" -> if (bottomNavTab != "Alerts") bottomNavTab = "Alerts"
      }
    }
  }

  // Teacher view state
  var teacherSubTab by remember { mutableStateOf("My Students") } // "My Students", "Voice Reports", "Board Upload", "Gradebook"

  // Manager view state
  var managerSubTab by remember { mutableStateOf("Overview") } // "Overview", "Requests", "Timetable", "Broadcast"
  var pendingApplications by remember { mutableStateOf(MockRepo.children.mapIndexed { index, child -> 
    val apps = listOf(
      ApplicationModel("app1", "Mariam Ahmed", "مريم أحمد", "Year 3", "الصف الثالث", "Heba Ahmed", "هبة أحمد", "0100-1234-567", "10 min ago", "منذ ١٠ د"),
      ApplicationModel("app2", "Omar Farouk", "عمر فاروق", "Year 1", "الصف الأول", "Dina Farouk", "دينا فاروق", "0111-9876-543", "2 hrs ago", "منذ ساعتين"),
      ApplicationModel("app3", "Nour El-Din", "نور الدين", "KG2", "الروضة الثانية", "Amira El-Din", "أميرة الدين", "0122-5551-234", "Yesterday", "أمس")
    )
    apps.getOrNull(index) ?: apps[0]
  }.toMutableList()) }

  // Chat conversation
  var chatMessages by remember {
    mutableStateOf(
      listOf(
        ChatMessage(false, "Hello! I'm your OEKS assistant. You can ask me in Arabic or English about Ahmad's schedule, grades, teachers, or payments."),
        ChatMessage(true, "What subject is Ahmad in right now?"),
        ChatMessage(false, "Ahmad is currently in Science class with Mr. Mohamed Hassan. The lesson runs from 11:00 AM to 11:45 AM. Next up: ICT with Ms. Layla Sayed.")
      )
    )
  }
  var chatInputValue by remember { mutableStateOf("") }

  // Voice note recording states (Teacher view)
  var voiceRecordState by remember { mutableStateOf("IDLE") } // "IDLE", "RECORDING", "TRANSCRIBING", "COMPLETE"
  var voiceTimerText by remember { mutableStateOf("0:00") }
  var showVoiceSummaryCard by remember { mutableStateOf(false) }

  // Dynamic direction toggle binding
  Trans.isArabic = isArabicState
  val direction = if (isArabicState) LayoutDirection.Rtl else LayoutDirection.Ltr

  CompositionLocalProvider(LocalLayoutDirection provides direction) {
    if (currentUser == null) {
      LoginScreen(
        authManager = authManager,
        isLanguageArabic = isArabicState,
        onLanguageToggle = { isArabicState = it },
        onAuthSuccess = { user ->
          currentUser = user
          activeRole = user.role
        }
      )
    } else {
      Scaffold(
        modifier = Modifier
          .fillMaxSize()
          .testTag("app_container"),
      topBar = {
        Column(
          modifier = Modifier
            .background(PrimaryNavy)
            .statusBarsPadding()
        ) {
          // School branding top bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Shield Custom Vector Logo with initials badge
            Box(
              modifier = Modifier
                .size(42.dp)
                .background(
                  brush = Brush.radialGradient(listOf(GoldAccent, CrimsonRed)),
                  shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                )
                .border(
                  2.dp, GrayState,
                  shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                ),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = Trans.abbreviation,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = Trans.t("Omdur Schools", "مدارس أمدر"),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = Trans.subtitle,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp
              )
            }

            // Language Toggle Button (EN vs عر)
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .padding(2.dp)
            ) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(18.dp))
                  .background(if (!isArabicState) Color.White else Color.Transparent)
                  .clickable { isArabicState = false }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Text(
                  text = "EN",
                  color = if (!isArabicState) PrimaryNavy else Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(18.dp))
                  .background(if (isArabicState) Color.White else Color.Transparent)
                  .clickable { isArabicState = true }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Text(
                  text = "عر",
                  color = if (isArabicState) PrimaryNavy else Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // User Info & Sign Out
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .clickable {
                  authManager.signOut {
                    currentUser = null
                  }
                }
                .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Text(
                text = when (currentUser?.role) {
                  "Parent" -> "👤"
                  "Teacher" -> "👨‍🏫"
                  "Manager" -> "💼"
                  else -> "👤"
                },
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = Trans.t("Sign out", "خروج"),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Role selection tabs - Restrict based on authenticated user's role!
          val roles = when (currentUser?.role) {
            "Parent" -> listOf("Parent", "Enroll")
            "Teacher" -> listOf("Teacher")
            "Manager" -> listOf("Manager", "Teacher", "Parent", "Enroll")
            else -> emptyList()
          }

          if (roles.size > 1) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(4.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              roles.forEach { role ->
                val isSelected = activeRole == role
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable {
                      activeRole = role
                      if (role == "Enroll") bottomNavTab = "Enroll"
                      else if (role == "Parent") bottomNavTab = "Home"
                    }
                    .padding(vertical = 8.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = when (role) {
                      "Parent" -> Trans.roleParent
                      "Teacher" -> Trans.roleTeacher
                      "Manager" -> Trans.roleManager
                      else -> Trans.enroll
                    },
                    color = if (isSelected) PrimaryNavy else Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      },
      bottomBar = {
        if (activeRole == "Parent" || activeRole == "Enroll") {
          // App bottom navigation bar (sticky)
          Surface(
            tonalElevation = 8.dp,
            color = Color.White,
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.navigationBarsPadding()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceAround,
              verticalAlignment = Alignment.CenterVertically
            ) {
              val bottomNavItems = listOf(
                Triple("Home", Icons.Default.Home, Trans.home),
                Triple("Payments", Icons.Default.Star, Trans.payments),
                Triple("AI Help", Icons.Default.Info, Trans.aiHelp),
                Triple("Alerts", Icons.Default.Notifications, Trans.alerts),
                Triple("Enroll", Icons.Default.AddCircle, Trans.enroll)
              )

              bottomNavItems.forEach { (tabId, icon, label) ->
                val isSelected = bottomNavTab == tabId
                InteractiveBottomNavItem(
                  icon = icon,
                  label = label,
                  isSelected = isSelected,
                  onClick = {
                    bottomNavTab = tabId
                    if (tabId == "Enroll") {
                      activeRole = "Enroll"
                    } else {
                      activeRole = "Parent" // clicking bottom nav home/payments defaults to Parent role views
                    }
                  }
                )
              }
            }
          }
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(PageBg)
          .padding(innerPadding)
      ) {
        when (activeRole) {
          "Parent" -> {
            ParentDashboardScreen(
              activeChild = activeChild,
              selectedChildId = selectedChildId,
              onSelectChild = { selectedChildId = it },
              parentSubTab = parentSubTab,
              onTabChange = { parentSubTab = it },
              chatMessages = chatMessages,
              chatInputValue = chatInputValue,
              onChatValueChange = { chatInputValue = it },
              onSendMessage = { message ->
                if (message.isNotBlank()) {
                  val newMsg = ChatMessage(true, message)
                  chatMessages = chatMessages + newMsg
                  chatInputValue = ""
                  
                  // Auto-simulated answer flow
                  coroutineScope.launch {
                    delay(600)
                    val replyText = when {
                      message.contains("pay", ignoreCase = true) || message.contains("owe", ignoreCase = true) || message.contains("مدفوعات", ignoreCase = true) || message.contains("مبلغ", ignoreCase = true) -> {
                        Trans.t(
                          "Your next payment for Ahmad is 1,200 EGP, due June 1st 2026.",
                          "قسطك القادم لأحمد هو ١,٢٠٠ ج.م يستحق في ١ يونيو ٢٠٢٦."
                        )
                      }
                      message.contains("sick", ignoreCase = true) || message.contains("مريض", ignoreCase = true) || message.contains("غائب", ignoreCase = true) -> {
                        Trans.t(
                          "Got it! I've sent a message to Ahmad's homeroom teacher Mr. Mohamed Hassan and the school administration.",
                          "تم استلام عذرك الطبي! أرسلنا إشعاراً لمربي الفصل أ/ محمد حسن وإدارة المدرسة."
                        )
                      }
                      message.contains("teacher", ignoreCase = true) || message.contains("مدرس", ignoreCase = true) || message.contains("معلم", ignoreCase = true) -> {
                        Trans.t(
                          "Ahmad's homeroom teacher is Mr. Mohamed Hassan. You can contact him on the Teachers tab.",
                          "مربي صف أحمد هو أ/ محمد حسن. يمكنك مراسلته عبر تبويب المعلمين."
                        )
                      }
                      isArabicState -> "أهلاً بك! أنا مساعد مدارس أمدر الذكي. كيف يمكنني خدمتك اليوم؟"
                      else -> "Hello! I am your OEKS assistant. Let me know if you need info about schedule, grades, or payments."
                    }
                    chatMessages = chatMessages + ChatMessage(false, replyText)
                  }
                }
              }
            )
          }
          "Teacher" -> {
            TeacherDashboardScreen(
              subTab = teacherSubTab,
              onTabChange = { teacherSubTab = it },
              voiceRecordState = voiceRecordState,
              voiceTimerText = voiceTimerText,
              showVoiceSummaryCard = showVoiceSummaryCard,
              onTriggerVoiceRecording = {
                coroutineScope.launch {
                  if (voiceRecordState == "IDLE") {
                    voiceRecordState = "RECORDING"
                    showVoiceSummaryCard = false
                    voiceTimerText = "🔴 Recording... 0:01"
                    delay(500)
                    voiceTimerText = "🔴 Recording... 0:02"
                    delay(500)
                    voiceTimerText = "🔴 Recording... 0:03"
                    delay(500)
                    
                    voiceRecordState = "TRANSCRIBING"
                    voiceTimerText = "✅ Transcribing with AI..."
                    delay(1200)
                    
                    voiceRecordState = "COMPLETE"
                    voiceTimerText = "Transcription complete"
                    showVoiceSummaryCard = true
                  } else {
                    // Reset
                    voiceRecordState = "IDLE"
                    showVoiceSummaryCard = false
                  }
                }
              },
              onResetVoiceRecord = {
                voiceRecordState = "IDLE"
                showVoiceSummaryCard = false
              }
            )
          }
          "Manager" -> {
            ManagerDashboardScreen(
              subTab = managerSubTab,
              onTabChange = { managerSubTab = it },
              pendingApps = pendingApplications,
              onApproveApp = { app ->
                val index = pendingApplications.indexOfFirst { it.id == app.id }
                if (index != -1) {
                  // Instant interactive visual cue
                  val updated = pendingApplications.toMutableList()
                  updated[index] = updated[index].copy(approved = true, childEn = "✅ Approved — Drive created")
                  pendingApplications = updated
                  
                  // Dismiss after delayed animation
                  coroutineScope.launch {
                    delay(1500)
                    val filtered = pendingApplications.filter { it.id != app.id }.toMutableList()
                    pendingApplications = filtered
                  }
                }
              }
            )
          }
          "Enroll" -> {
            EnrollmentScreen()
          }
        }
      }
    }
    }
  }
}

// Subcomponent: Animated Bottom Nav Item
@Composable
fun InteractiveBottomNavItem(
  icon: ImageVector,
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val iconScale by animateFloatAsState(targetValue = if (isSelected) 1.15f else 1f)
  Column(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (isSelected) PrimaryNavy else Color.LightGray,
      modifier = Modifier
        .size(24.dp)
        .scale(iconScale)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = label,
      color = if (isSelected) PrimaryNavy else Color.LightGray,
      fontSize = 11.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
    )
  }
}


// SCREEN 1: PARENT VIEW SCREEN
@Composable
fun ParentDashboardScreen(
  activeChild: Child,
  selectedChildId: String,
  onSelectChild: (String) -> Unit,
  parentSubTab: String,
  onTabChange: (String) -> Unit,
  chatMessages: List<ChatMessage>,
  chatInputValue: String,
  onChatValueChange: (String) -> Unit,
  onSendMessage: (String) -> Unit
) {
  Column(modifier = Modifier.fillMaxSize()) {
    // Top Row: Active child tabs (Bento capsules)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(WhiteSurface)
        .padding(top = 10.dp, bottom = 6.dp, start = 16.dp, end = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      MockRepo.children.forEach { baby ->
        val childSelected = baby.id == selectedChildId
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(if (childSelected) RoyalBlue else WhiteSurface)
            .border(
              1.dp,
              if (childSelected) RoyalBlue else BorderColor,
              shape = RoundedCornerShape(24.dp)
            )
            .clickable { onSelectChild(baby.id) }
            .padding(vertical = 10.dp, horizontal = 4.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            if (childSelected) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .background(Color.White, CircleShape)
              )
              Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
              text = if (Trans.isArabic) baby.nameAr.split(" ")[0] else baby.nameEn.split(" ")[0],
              color = if (childSelected) Color.White else TextMuted,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }
        }
      }
    }

    // Sub Tabs Menu Selector Row
    val tabs = listOf("Overview", "Grades", "Live Class", "Payments", "Teachers", "Board", "Alerts", "AI Help")
    val arabTabs = mapOf(
      "Overview" to "نظرة عامة",
      "Grades" to "الدرجات",
      "Live Class" to "الحصة",
      "Payments" to "المدفوعات",
      "Teachers" to "المعلمون",
      "Board" to "السبورة",
      "Alerts" to "التنبيهات",
      "AI Help" to "المساعد الذكي"
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(WhiteSurface)
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      tabs.forEach { tabKey ->
        val tabSelected = parentSubTab == tabKey
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (tabSelected) PrimaryNavy else PageBg)
            .clickable { onTabChange(tabKey) }
            .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
          Text(
            text = if (Trans.isArabic) arabTabs[tabKey] ?: tabKey else tabKey,
            color = if (tabSelected) Color.White else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Divider(color = BorderColor)

    // Dynamic Tab Area Loading with Animated Fade In
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
      AnimatedContent(targetState = parentSubTab, label = "tabSwap") { currentTab ->
        when (currentTab) {
          "Overview" -> ParentOverviewTab(activeChild, onNavigateToTab = onTabChange)
          "Grades" -> ParentGradesTab(activeChild)
          "Live Class" -> ParentLiveClassTab(activeChild)
          "Payments" -> ParentPaymentsTab(activeChild)
          "Teachers" -> ParentTeachersTab(activeChild)
          "Board" -> ParentBoardTab(activeChild)
          "Alerts" -> ParentAlertsTab(activeChild)
          "AI Help" -> ParentAiHelpTab(chatMessages, chatInputValue, onChatValueChange, onSendMessage)
        }
      }
    }
  }
}

// Parent View Subtabs: OVERVIEW (Bento Grid design)
@Composable
fun ParentOverviewTab(child: Child, onNavigateToTab: (String) -> Unit) {
  val arabOweText = Trans.t("Due Soon", "المبلغ المستحق")
  val arabAlertText = Trans.t("New Alerts", "تنبيهات جديدة")
  val arabAvgText = Trans.t("Average Grade", "متوسط الدرجات")

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Metric Bento Grid (3 cards row)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Average Grade
      Box(
        modifier = Modifier
          .weight(1f)
          .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp))
          .clip(RoundedCornerShape(20.dp))
          .background(WhiteSurface)
          .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
          .padding(14.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = Trans.t("Grade", "الدرجة").uppercase(),
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = child.averageGrade,
            color = PrimaryNavy,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
          )
        }
      }

      // Due Soon Balance
      Box(
        modifier = Modifier
          .weight(1f)
          .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp))
          .clip(RoundedCornerShape(20.dp))
          .background(WhiteSurface)
          .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
          .padding(14.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = Trans.t("Due", "المستحق").uppercase(),
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (child.id == "ahmad") "1.2k" else if (child.id == "sara") "1.0k" else "708",
            color = CrimsonRed,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
          )
        }
      }

      // Alerts
      Box(
        modifier = Modifier
          .weight(1f)
          .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp))
          .clip(RoundedCornerShape(20.dp))
          .background(WhiteSurface)
          .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
          .padding(14.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = Trans.t("Alerts", "تنبيهات").uppercase(),
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${child.alertCount}",
            color = GreenState,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
          )
        }
      }
    }

    // 2. Child Profile Bento Card
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = WhiteSurface,
      border = BorderStroke(1.dp, BorderColor),
      tonalElevation = 1.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(
              text = child.name,
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "${child.grade} · Cambridge IGCSE · ${child.section}",
              color = TextMuted,
              fontSize = 11.sp
            )
          }

          // Pulsing LIVE Status Label
          val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
          val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
              animation = tween(1000),
              repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .background(LightGreenBg)
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .background(GreenState.copy(alpha = pulseAlpha), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = Trans.t("LIVE", "مباشر"),
              color = GreenState,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // In-Class nested Science box card
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LightBlueBg)
            .clickable { onNavigateToTab("Live Class") }
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            // Emoji or specialized icon container
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(RoyalBlue, RoundedCornerShape(12.dp)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (child.liveStatus.contains("Science") || child.liveStatus.contains("علوم")) "🧪"
                       else if (child.liveStatus.contains("Play") || child.liveStatus.contains("لعب")) "🎨"
                       else "📚",
                color = Color.White,
                fontSize = 16.sp
              )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
              Text(
                text = child.liveStatus,
                color = RoyalBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = child.liveTime,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          // Round arrow Button
          Box(
            modifier = Modifier
              .size(32.dp)
              .shadow(1.dp, CircleShape)
              .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.ArrowForward,
              contentDescription = "Details",
              tint = TextPrimary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }

    // 3. AI Banner Card (Bento Gradient Cell)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp))
        .clip(RoundedCornerShape(24.dp))
        .background(Brush.horizontalGradient(listOf(PrimaryNavy, RoyalBlue)))
        .clickable { onNavigateToTab("AI Help") }
        .padding(16.dp)
    ) {
      // Background decorative circle shape
      Box(
        modifier = Modifier
          .size(96.dp)
          .align(Alignment.BottomEnd)
          .offset(x = 24.dp, y = 24.dp)
          .border(width = 12.dp, color = Color.White.copy(alpha = 0.08f), shape = CircleShape)
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Emoji icon block
        Box(
          modifier = Modifier
            .size(46.dp)
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(text = "🤖", fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = Trans.t("OEKS AI Assistant", "مساعد أمدر الذكي"),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = Trans.t(
              "Ask me anything about Ahmad's schedule, payments, or teachers.",
              "اسأل عن جدول الحصص، الأقساط أو المعلمين باللغة العربية أو الإنجليزية."
            ),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp
          )
        }
      }
    }

    // 4. Upcoming Events Card (Bento Cell)
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = WhiteSurface,
      border = BorderStroke(1.dp, BorderColor),
      tonalElevation = 1.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = Trans.t("Upcoming Events", "الفعاليات القادمة").uppercase(),
          color = TextMuted,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          modifier = Modifier.padding(bottom = 12.dp)
        )

        listOf(
          Triple(
            Trans.t("Science Project Presentation", "عرض مشاريع العلوم"),
            Trans.t("Monday, 25 May · 9:00 AM", "الإثنين، ٢٥ مايو · ٩:٠٠ ص"),
            "🏆"
          ),
          Triple(
            Trans.t("End of Year Ceremony", "حفل نهاية العام الدراسي"),
            Trans.t("Thursday, 12 June · 10:00 AM", "الخميس، ١٢ يونيو · ١٠:٠٠ ص"),
            "🎓"
          )
        ).forEachIndexed { index, event ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .background(PageBg, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(text = event.third, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                text = event.first,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = event.second,
                color = TextMuted,
                fontSize = 11.sp
              )
            }
          }
          if (index < 1) {
            HorizontalDivider(color = BorderColor.copy(alpha = 0.7f), modifier = Modifier.padding(vertical = 4.dp))
          }
        }
      }
    }
  }
}

// Parent View Subtabs: GRADES
@Composable
fun ParentGradesTab(child: Child) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = WhiteSurface,
      border = BorderStroke(1.dp, BorderColor),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = Trans.t("Term 3 Results — Year 6", "نتائج الفصل الدراسي الثالث — الصف السادس"),
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = TextPrimary,
          modifier = Modifier.padding(bottom = 14.dp)
        )

        child.grades.forEachIndexed { idx, sub ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = sub.subject,
              color = TextPrimary,
              fontWeight = FontWeight.Medium,
              fontSize = 13.sp
            )

            // Color-code grade tag: Green >80, Yellow 60-79, Red <60
            val (bgColor, textColor) = when {
              sub.percentage >= 80 -> Pair(LightGreenBg, GreenState)
              sub.percentage >= 60 -> Pair(LightYellowBg, OrangeText)
              else -> Pair(Color(0xFFFFEBEE), CrimsonRed)
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
              Text(
                text = "${sub.percentage}%",
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }
          }
          if (idx < child.grades.lastIndex) {
            Divider(color = BorderColor)
          }
        }
      }
    }

    Button(
      onClick = {},
      modifier = Modifier.fillMaxWidth(),
      colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
    ) {
      Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.rotate(270f))
      Spacer(modifier = Modifier.width(8.dp))
      Text(text = Trans.t("Download Report PDF", "تحميل التقرير بصيغة PDF"))
    }
  }
}

// Parent View Subtabs: LIVE CLASS
@Composable
fun ParentLiveClassTab(child: Child) {
  val timetable = remember { MockRepo.listTimetable }
  
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = WhiteSurface,
      border = BorderStroke(1.dp, BorderColor),
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = Trans.t("Live Class — Monday", "الحصص الدراسية — يوم الإثنين"),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TextPrimary
          )

          // Live indicator
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(LightGreenBg)
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .background(GreenState, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = Trans.t("LIVE", "مباشر"), color = GreenState, fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(timetable) { row ->
            val hasPassed = row.color == GrayState && !row.isCurrent
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                  if (row.isCurrent) LightBlueBg
                  else if (row.isBreak) LightYellowBg
                  else if (hasPassed) PageBg.copy(alpha = 0.5f)
                  else WhiteSurface
                )
                .border(
                  width = 1.dp,
                  color = if (row.isCurrent) RoyalBlue else BorderColor,
                  shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = row.time,
                color = if (hasPassed) TextMuted.copy(alpha = 0.5f) else TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier.width(72.dp)
              )

              VerticalDivider(modifier = Modifier.padding(horizontal = 10.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = row.subject,
                  color = if (hasPassed) TextMuted.copy(alpha = 0.6f) else TextPrimary,
                  fontWeight = if (row.isCurrent) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 13.sp
                )
              }

              if (row.isCurrent) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CrimsonRed)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = Trans.t("NOW", "الآن"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

// Vertical Divider helper
@Composable
fun VerticalDivider(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .width(1.dp)
      .fillMaxHeight()
      .background(BorderColor)
  )
}

// Parent View Subtabs: PAYMENTS
@Composable
fun ParentPaymentsTab(child: Child) {
  val currency = Trans.t("EGP", "ج.م")
  val remainingText = Trans.t("Remaining", "المتبقي")
  val nextInstallmentText = Trans.t("Next Installment", "القسط القادم")
  val dueText = Trans.t("Due in 14 days", "مستحق خلال ١٤ يوم")

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Card 1: Annual Fees
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = WhiteSurface,
      border = BorderStroke(1.dp, BorderColor)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "${Trans.t("Annual Fees", "المصروفات السنوية")} — ${child.name}",
          color = TextPrimary,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(text = Trans.t("Total Annual Fee", "إجمالي المصروفات"), color = TextMuted, fontSize = 11.sp)
            Text(text = "18,000 $currency", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
          Column {
            Text(text = Trans.t("Paid", "المسدد"), color = GreenState, fontSize = 11.sp)
            Text(text = "${child.paymentsPaid} $currency", color = GreenState, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(text = remainingText, color = CrimsonRed, fontSize = 11.sp)
            Text(text = "${child.paymentsRemaining} $currency", color = CrimsonRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // exactly 60% full progress bar
        LinearProgressIndicator(
          progress = child.paymentsPercent,
          color = RoyalBlue,
          trackColor = PageBg,
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = "${(child.paymentsPercent * 100).toInt()}% ${Trans.t("paid", "مسدد")}", color = TextMuted, fontSize = 11.sp)
          Text(text = child.paymentsInstallmentCount, color = TextMuted, fontSize = 11.sp)
        }
      }
    }

    // Card 2: Next installment due info
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = WhiteSurface,
      border = BorderStroke(1.dp, BorderColor)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = nextInstallmentText,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(LightYellowBg)
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(text = dueText, color = OrangeText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "${child.dueSoonVal} $currency",
          color = TextPrimary,
          fontWeight = FontWeight.Black,
          fontSize = 26.sp
        )

        Text(
          text = "${Trans.t("Due date", "تاريخ الاستحقاق")}: ${Trans.t("June 1, 2026", "١ يونيو، ٢٠٢٦")}",
          color = TextMuted,
          fontSize = 12.sp,
          modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {},
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(text = "${Trans.t("Pay Now", "ادفع الآن")} — ${child.dueSoonVal} $currency", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

// Parent View Subtabs: TEACHERS
@Composable
fun ParentTeachersTab(child: Child) {
  val teachers = remember { MockRepo.listTeachers }
  var selectedTeacher by remember { mutableStateOf<TeacherModel?>(null) }
  var composeMessageText by remember { mutableStateOf("") }

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = WhiteSurface,
      border = BorderStroke(1.dp, BorderColor),
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = Trans.t("Ahmad's Teachers", "معلمو أحمد"),
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = TextPrimary,
          modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          items(teachers) { teacher ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .background(teacher.avatarBg, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text(text = teacher.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(text = teacher.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = teacher.subject, color = TextMuted, fontSize = 11.sp)
              }

              OutlinedButton(
                onClick = { selectedTeacher = teacher },
                border = BorderStroke(1.dp, PrimaryNavy),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
              ) {
                Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryNavy)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = Trans.t("Message", "مراسلة"), fontSize = 11.sp, color = PrimaryNavy)
              }
            }
          }
        }
      }
    }

    // Modal Sheet for chat with teacher (interactive popup)
    if (selectedTeacher != null) {
      AlertDialog(
        onDismissRequest = { selectedTeacher = null },
        title = { Text(text = "${Trans.t("Chat with", "محادثة مع")} ${selectedTeacher?.name}") },
        text = {
          Column {
            Text(text = "${Trans.t("Subject", "المادة")}: ${selectedTeacher?.subject}", color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
              value = composeMessageText,
              onValueChange = { composeMessageText = it },
              placeholder = { Text(text = Trans.t("Type a message to teacher...", "اكتب رسالة للمعلم...")) },
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              composeMessageText = ""
              selectedTeacher = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
          ) {
            Text(text = Trans.t("Send", "إرسال"))
          }
        },
        dismissButton = {
          TextButton(onClick = { selectedTeacher = null }) {
            Text(text = Trans.t("Cancel", "إلغاء"))
          }
        }
      )
    }
  }
}

// Parent View Subtabs: BOARD
@Composable
fun ParentBoardTab(child: Child) {
  val boards = remember { MockRepo.listBoards }
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = Trans.t("Today's Lesson Summaries", "ملخصات دروس اليوم"),
      fontWeight = FontWeight.Bold,
      fontSize = 15.sp,
      color = TextPrimary
    )

    // 2x2 grid of photo placeholder boards
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      for (i in 0 until boards.size step 2) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          for (j in i..i + 1) {
            val board = boards.getOrNull(j)
            if (board != null) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (board.imageAvailable) PrimaryNavy else PageBg,
                border = BorderStroke(
                  width = 1.dp,
                  color = if (board.imageAvailable) RoyalBlue else BorderColor
                ),
                modifier = Modifier
                  .weight(1f)
                  .height(110.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  if (board.imageAvailable) {
                    // Simulating school chalkboard board
                    Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      modifier = Modifier.padding(8.dp)
                    ) {
                      Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(24.dp))
                      Spacer(modifier = Modifier.height(6.dp))
                      Text(text = board.subject, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                      Text(text = board.time, color = Color.LightGray, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                  } else {
                    // Dotted non uploaded board
                    Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      modifier = Modifier.padding(8.dp)
                    ) {
                      Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                      Spacer(modifier = Modifier.height(6.dp))
                      Text(text = board.subject, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                      Text(text = Trans.t("Uploading soon...", "قريباً..."), color = TextMuted, fontSize = 10.sp)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // Info card
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = LightBlueBg,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = Trans.t("Photos uploaded by teachers after each lesson", "يقوم المعلمون برفع ملخصات السبورة صوراً مباشرة بعد كل حصة"),
          color = RoyalBlue,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

// Parent View Subtabs: ALERTS
@Composable
fun ParentAlertsTab(child: Child) {
  val alerts = remember { MockRepo.listAlerts }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = WhiteSurface,
    border = BorderStroke(1.dp, BorderColor)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = Trans.t("Notifications & Alerts", "التنبيهات والإشعارات"),
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 12.dp)
      )

      LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(alerts) { alert ->
          val iconColor = when (alert.type) {
            "PAYMENT" -> CrimsonRed
            "REPORT" -> RoyalBlue
            "GRADE" -> GoldAccent
            else -> GrayState
          }

          val iconVector = when (alert.type) {
            "PAYMENT" -> Icons.Default.Warning
            "REPORT" -> Icons.Default.Check
            "GRADE" -> Icons.Default.Star
            else -> Icons.Default.Notifications
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(iconColor.copy(alpha = 0.15f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(imageVector = iconVector, contentDescription = null, tint = if (iconColor == GoldAccent) OrangeText else iconColor, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = alert.text,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
              )
              Text(
                text = alert.time,
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
              )
            }
          }
        }
      }
    }
  }
}

// Parent View Subtabs: AI CHAT ASSISTANT
@Composable
fun ParentAiHelpTab(
  messages: List<ChatMessage>,
  inputValue: String,
  onInputValueChange: (String) -> Unit,
  onSendMessage: (String) -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    // Top chatbot description banner
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PrimaryNavy,
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp)
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = GoldAccent)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(text = Trans.t("OEKS Help Bot Center", "مركز مساعدة أمدر الذكي"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Text(text = Trans.t("Dynamic responsive virtual billing & scheduling assistant.", "مساعد ذكي للرد الفوري على تفاصيل المواعيد والدفع."), color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
        }
      }
    }

    // Scrollable Chat Message List
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = WhiteSurface,
      border = BorderStroke(1.dp, BorderColor),
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Box(modifier = Modifier.weight(1f)) {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(messages) { msg ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
              ) {
                Box(
                  modifier = Modifier
                    .clip(
                      RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (msg.isUser) 12.dp else 0.dp,
                        bottomEnd = if (msg.isUser) 0.dp else 12.dp
                      )
                    )
                    .background(if (msg.isUser) LightBlueBg else PageBg)
                    .padding(12.dp)
                    .widthIn(max = 240.dp)
                ) {
                  Text(
                    text = msg.text,
                    color = TextPrimary,
                    fontSize = 12.sp
                  )
                }
              }
            }
          }
        }

        Divider(color = BorderColor, modifier = Modifier.padding(vertical = 10.dp))

        // Quick tapping buttons
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(
            "How much do I owe?",
            "Ahmad is sick today",
            "كم المبلغ المتبقي؟"
          ).forEach { txt ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(LightBlueBg)
                .clickable { onSendMessage(txt) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text(text = txt, color = RoyalBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        // Input Field
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = inputValue,
            onValueChange = onInputValueChange,
            placeholder = { Text(text = Trans.t("Ask anything… (Arabic or English)", "اسأل عن أي شيء...")) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            maxLines = 1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSendMessage(inputValue) })
          )

          Spacer(modifier = Modifier.width(8.dp))

          IconButton(
            onClick = { onSendMessage(inputValue) },
            modifier = Modifier
              .background(PrimaryNavy, CircleShape)
              .size(40.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Send,
              contentDescription = "Send",
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}


// SCREEN 2: TEACHER VIEW SCREEN
@Composable
fun TeacherDashboardScreen(
  subTab: String,
  onTabChange: (String) -> Unit,
  voiceRecordState: String,
  voiceTimerText: String,
  showVoiceSummaryCard: Boolean,
  onTriggerVoiceRecording: () -> Unit,
  onResetVoiceRecord: () -> Unit
) {
  val tabs = listOf("My Students", "Voice Reports", "Board Upload", "Gradebook")
  val arabTabs = mapOf(
    "My Students" to "طلابي",
    "Voice Reports" to "تقرير صوتي",
    "Board Upload" to "رفع ملخص السبورة",
    "Gradebook" to "الدرجات"
  )

  Column(modifier = Modifier.fillMaxSize()) {
    // Subtabs scrollable line
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(WhiteSurface)
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      tabs.forEach { key ->
        val selected = subTab == key
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) PrimaryNavy else PageBg)
            .clickable { onTabChange(key) }
            .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
          Text(
            text = if (Trans.isArabic) arabTabs[key] ?: key else key,
            color = if (selected) Color.White else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Divider(color = BorderColor)

    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
      AnimatedContent(targetState = subTab, label = "teachTab") { currentTab ->
        when (currentTab) {
          "My Students" -> TeacherStudentsTab()
          "Voice Reports" -> TeacherVoiceReportsTab(
            voiceRecordState,
            voiceTimerText,
            showVoiceSummaryCard,
            onTriggerVoiceRecording,
            onResetVoiceRecord
          )
          "Board Upload" -> TeacherBoardUploadTab()
          "Gradebook" -> TeacherGradebookTab()
        }
      }
    }
  }
}

// Teacher Subtabs: MY STUDENTS
@Composable
fun TeacherStudentsTab() {
  val list = remember { MockRepo.students }
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = WhiteSurface,
    border = BorderStroke(1.dp, BorderColor)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = Trans.t("Mr. Mohamed Hassan · Science · Year 5, 6, 7 · 47 students", "أ/ محمد حسن · مادة العلوم · صف ٥، ٦، ٧ · ٤٧ طالباً"),
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 12.dp)
      )

      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(list) { student ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // green online or offline dot indicator
            Box(
              modifier = Modifier
                .size(10.dp)
                .background(if (student.isOnline) GreenState else CrimsonRed, CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
              modifier = Modifier
                .size(36.dp)
                .background(student.avatarBg, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(text = student.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(text = student.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
              Text(text = student.grade, color = TextMuted, fontSize = 11.sp)
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (student.percentage >= 60) LightGreenBg else Color(0xFFFFEBEE))
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                text = "${student.percentage}%",
                color = if (student.percentage >= 60) GreenState else CrimsonRed,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              )
            }
          }
        }
      }
    }
  }
}

// Teacher Subtabs: VOICE REPORTS WITH TRANSCRIPTION ANIMATIONS
@Composable
fun TeacherVoiceReportsTab(
  recordState: String,
  timerText: String,
  showSummaryCard: Boolean,
  onTriggerRecord: () -> Unit,
  onReset: () -> Unit
) {
  var selectedStudentName by remember { mutableStateOf("Ahmad Al-Rashidi — Year 6B") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = WhiteSurface,
      border = BorderStroke(1.dp, BorderColor),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = Trans.t("Voice Note Report", "تسجيل التقرير الصوتي المباشر"),
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          color = TextPrimary
        )
        Text(
          text = Trans.t("Select a student, record a voice note. AI will transcribe and summarize it.", "اختر الطالب وسجل صوتك، سيقوم الذكاء الاصطناعي بنسخه وتلخيصه تلقائياً."),
          color = TextMuted,
          fontSize = 12.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(vertical = 8.dp)
        )

        // Dropdown student selection
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PageBg)
            .clickable {} // simulated
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = selectedStudentName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
          Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Circular microphone button
        val micColor = if (recordState == "RECORDING") CrimsonRed else if (recordState == "TRANSCRIBING") RoyalBlue else PrimaryNavy
        IconButton(
          onClick = onTriggerRecord,
          modifier = Modifier
            .size(72.dp)
            .background(micColor, CircleShape)
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
          ) {
            Box(
              modifier = Modifier
                .width(16.dp)
                .height(24.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .width(24.dp)
                .height(4.dp)
                .background(Color.White, RoundedCornerShape(2.dp))
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Timer/transcription text label
        Text(
          text = if (recordState == "IDLE") Trans.t("Tap to record", "اضغط للتسجيل الصوتي") else timerText,
          color = if (recordState == "RECORDING") CrimsonRed else TextPrimary,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
      }
    }

    // AI Transcription result card
    AnimatedVisibility(visible = showSummaryCard) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = LightBlueBg,
        border = BorderStroke(1.dp, RoyalBlue),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = RoyalBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = Trans.t("AI Transcription Report Result", "نتيجة النص الملخص بالذكاء الاصطناعي"),
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = RoyalBlue
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = Trans.t(
              "Ahmad shows excellent understanding of ecosystems. He participates actively and his experiment work is outstanding. Recommend advancing him to the enrichment program.",
              "يظهر أحمد فهماً ممتازاً للنظم البيئية. يشارك بنشاط وعمله الاختباري في المختبر رائع جداً. نوصي بترقيته إلى برنامج الإثراء المتقدم."
            ),
            color = TextPrimary,
            fontSize = 12.sp,
            lineHeight = 18.sp
          )

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onReset,
              colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
              modifier = Modifier.weight(1f)
            ) {
              Text(text = Trans.t("Confirm & Send", "تأكيد وإرسال"), fontSize = 11.sp)
            }
            OutlinedButton(
              onClick = onReset,
              border = BorderStroke(1.dp, CrimsonRed),
              modifier = Modifier.weight(1f)
            ) {
              Text(text = Trans.t("Edit", "تعديل"), color = CrimsonRed, fontSize = 11.sp)
            }
          }
        }
      }
    }
  }
}

// Teacher Subtabs: BOARD UPLOAD
@Composable
fun TeacherBoardUploadTab() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = WhiteSurface,
      border = BorderStroke(1.dp, BorderColor),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = Trans.t("Upload Board Summary", "رفع صورة ملخص السبورة"),
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = TextPrimary
        )
        Text(
          text = Trans.t("Take a photo of the board and students will see it instantly in their parent app.", "التقط صورة لسبورة الدرس ليراها أولياء الأمور والطلاب في التطبيق فوراً."),
          color = TextMuted,
          fontSize = 12.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Year Dropdown Selection Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PageBg)
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = "Year 6B — Science", fontSize = 12.sp, fontWeight = FontWeight.Medium)
          Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PageBg)
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = "Monday — Lesson 1", fontSize = 12.sp, fontWeight = FontWeight.Medium)
          Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = {},
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(text = Trans.t("Take Photo", "التقاط صورة"))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = {},
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(text = Trans.t("Upload from Gallery", "رفع من ألبوم الصور"), color = PrimaryNavy)
        }
      }
    }
  }
}

// Teacher Subtabs: GRADEBOOK
@Composable
fun TeacherGradebookTab() {
  val rows = remember { MockRepo.gradebook }
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = WhiteSurface,
      border = BorderStroke(1.dp, BorderColor),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = Trans.t("Gradebook — Year 6B Science · May 2026", "سجل الدرجات — صف ٦ب علوم · مايو ٢٠٢٦"),
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
          color = TextPrimary,
          modifier = Modifier.padding(bottom = 12.dp)
        )

        // Table headers Custom Grid
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(LightBlueBg)
            .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = Trans.t("Student", "الطالب"), modifier = Modifier.weight(1.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
          Text(text = "CW", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy, textAlign = TextAlign.Center)
          Text(text = "HW", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy, textAlign = TextAlign.Center)
          Text(text = "Quiz", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy, textAlign = TextAlign.Center)
          Text(text = "Test", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy, textAlign = TextAlign.Center)
          Text(text = "Avg", modifier = Modifier.weight(1.2f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy, textAlign = TextAlign.Center)
        }

        Divider(color = BorderColor)

        // Alternating Table Rows
        rows.forEachIndexed { sIdx, row ->
          val isEven = sIdx % 2 == 0
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(if (isEven) WhiteSurface else PageBg)
              .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = row.name, modifier = Modifier.weight(1.8f), fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(text = "${row.cw}", modifier = Modifier.weight(1f), fontSize = 11.sp, color = TextPrimary, textAlign = TextAlign.Center)
            Text(text = "${row.hw}", modifier = Modifier.weight(1f), fontSize = 11.sp, color = TextPrimary, textAlign = TextAlign.Center)
            Text(text = "${row.quiz}", modifier = Modifier.weight(1f), fontSize = 11.sp, color = TextPrimary, textAlign = TextAlign.Center)
            Text(text = "${row.test}", modifier = Modifier.weight(1f), fontSize = 11.sp, color = TextPrimary, textAlign = TextAlign.Center)
            
            val isPass = row.avg >= 60
            Text(
              text = "${row.avg}%",
              modifier = Modifier.weight(1.2f),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (isPass) GreenState else CrimsonRed,
              textAlign = TextAlign.Center
            )
          }
          Divider(color = BorderColor)
        }
      }
    }

    // Gold export button
    Button(
      onClick = {},
      modifier = Modifier.fillMaxWidth(),
      colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
    ) {
      Text(text = Trans.t("Export to Excel", "تصدير إلى إكسل"), color = PrimaryNavy, fontWeight = FontWeight.Bold)
    }
  }
}


// SCREEN 3: MANAGER VIEW SCREEN
@Composable
fun ManagerDashboardScreen(
  subTab: String,
  onTabChange: (String) -> Unit,
  pendingApps: List<ApplicationModel>,
  onApproveApp: (ApplicationModel) -> Unit
) {
  val tabs = listOf("Overview", "Requests", "Timetable", "Broadcast")
  val arabTabs = mapOf(
    "Overview" to "نظرة عامة",
    "Requests" to "الطلبات",
    "Timetable" to "الجدول اليومي",
    "Broadcast" to "إعلان للكل"
  )

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(WhiteSurface)
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      tabs.forEach { key ->
        val selected = subTab == key
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) PrimaryNavy else PageBg)
            .clickable { onTabChange(key) }
            .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
          Text(
            text = if (Trans.isArabic) arabTabs[key] ?: key else key,
            color = if (selected) Color.White else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Divider(color = BorderColor)

    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
      AnimatedContent(targetState = subTab, label = "mgrTab") { currentTab ->
        when (currentTab) {
          "Overview" -> ManagerOverviewTab()
          "Requests" -> ManagerRequestsTab(pendingApps, onApproveApp)
          "Timetable" -> ManagerTimetableTab()
          "Broadcast" -> ManagerBroadcastTab()
        }
      }
    }
  }
}

// Manager Subtabs: OVERVIEW STATS & LOGS
@Composable
fun ManagerOverviewTab() {
  val logs = remember { MockRepo.activityLogs }
  val currency = Trans.t("EGP", "ج.م")

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 2x2 Stats Grid
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = WhiteSurface,
          border = BorderStroke(1.dp, BorderColor),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "347", color = PrimaryNavy, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(text = Trans.t("Total Students", "إجمالي الطلاب"), color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
          }
        }
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = WhiteSurface,
          border = BorderStroke(1.dp, BorderColor),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "28,400 $currency", color = GreenState, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = Trans.t("Collected This Month", "المحصل هذا الشهر"), color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
          }
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = WhiteSurface,
          border = BorderStroke(1.dp, BorderColor),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "12", color = CrimsonRed, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(text = Trans.t("Overdue Payments", "متأخرات مالية"), color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
          }
        }
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = WhiteSurface,
          border = BorderStroke(1.dp, BorderColor),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "5", color = OrangeText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(text = Trans.t("New Applications", "طلبات تسجيل جديدة"), color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
          }
        }
      }
    }

    // AI Automation active banner
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PrimaryNavy,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = GoldAccent)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(text = Trans.t("AI Automation Active", "أتمتة النظام بالذكاء الاصطناعي"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Text(text = Trans.t("New enrollments → auto-added to Google Sheets + Drive folder + Gmail alert", "التسجيلات الجديدة ← تضاف للمستندات والملفات وإرسال إشعار فوري وتنبيه للمدير."), color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp)
        }
      }
    }

    // Latest activity log card
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = WhiteSurface,
      border = BorderStroke(1.dp, BorderColor),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = Trans.t("Latest Activity", "آخر الأنشطة والعمليات"),
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = TextPrimary,
          modifier = Modifier.padding(bottom = 12.dp)
        )

        logs.forEachIndexed { num, log ->
          val dotColor = when (log.type) {
            "GREEN" -> GreenState
            "BLUE" -> RoyalBlue
            "RED" -> CrimsonRed
            else -> YellowState
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(text = log.text, color = TextPrimary, fontSize = 12.sp)
              Text(text = log.time, color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
            }
          }
          if (num < logs.lastIndex) {
            Divider(color = BorderColor)
          }
        }
      }
    }
  }
}

// Manager Subtabs: REQUESTS APPLICATIONS WITH FADE TRANSITION
@Composable
fun ManagerRequestsTab(
  pendingApps: List<ApplicationModel>,
  onApprove: (ApplicationModel) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "${Trans.t("Pending Applications", "طلبات القبول المعلقة")} (${pendingApps.size})",
      fontWeight = FontWeight.Bold,
      fontSize = 14.sp,
      color = TextPrimary
    )

    if (pendingApps.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(text = Trans.t("No pending requests left", "لا توجد طلبات معلقة حالياً"), color = TextMuted, fontSize = 13.sp)
      }
    } else {
      pendingApps.forEach { app ->
        // Animated Visibility or raw simulation
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = WhiteSurface,
          border = BorderStroke(1.dp, if (app.approved) GreenState else BorderColor),
          tonalElevation = 1.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = app.child, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(LightBlueBg)
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(text = app.grade, color = RoyalBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "${Trans.t("Parent", "ولي الأمر")}: ${app.parent} · ${app.phone}", color = TextMuted, fontSize = 12.sp)
            Text(text = "✓ " + Trans.t("Birth certificate + Parent ID uploaded", "شهادة الميلاد + الهوية الشخصية لولي الأمر مرفوعة"), color = GreenState, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))

            Spacer(modifier = Modifier.height(12.dp))

            if (app.approved) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(LightGreenBg)
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = GreenState)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = Trans.t("Approved — Drive folder created", "تم القبول — تم إنشاء مجلد جوجل درايف للطلب"), color = GreenState, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            } else {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = { onApprove(app) },
                  colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                  modifier = Modifier.weight(1f)
                ) {
                  Text(text = Trans.t("Approve", "قبول والاعتماد"), fontSize = 12.sp)
                }

                OutlinedButton(
                  onClick = { onApprove(app) }, // dismiss/reject behaves similarly
                  border = BorderStroke(1.dp, CrimsonRed),
                  modifier = Modifier.weight(1f)
                ) {
                  Text(text = Trans.t("Reject", "رفض الطلب"), color = CrimsonRed, fontSize = 12.sp)
                }
              }
            }
          }
        }
      }
    }
  }
}

// Manager Subtabs: TIMETABLE EDITOR GRID CODES
@Composable
fun ManagerTimetableTab() {
  val timetableGrid = listOf(
    // Row 1
    listOf("Time", "Sun", "Mon", "Tue", "Wed", "Thu"),
    listOf("8:30", "Math", "Eng", "Sci", "Ar", "ICT"),
    listOf("9:15", "Ar", "Math", "Eng", "PE", "Sci"),
    listOf("10:00", "Break", "Break", "Break", "Break", "Break"),
    listOf("10:20", "Eng", "Ar", "Math", "Sci", "PE"),
    listOf("11:00", "Sci", "Sci", "PE", "Eng", "Ar")
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = WhiteSurface,
      border = BorderStroke(1.dp, BorderColor),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = Trans.t("Timetable Editor — Year 6B", "تعديل جدول الحصص — الصف السادس ب"),
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
        Text(
          text = Trans.t("Click a slot to edit · Changes go live immediately", "اضغط على أي خانة للتعديل المباشر · الحفظ تلقائي لحظي"),
          color = TextMuted,
          fontSize = 11.sp,
          modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom grid
        timetableGrid.forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
          ) {
            row.forEach { cell ->
              val isHeader = cell in listOf("Time", "Sun", "Mon", "Tue", "Wed", "Thu")
              val cellColor = when (cell) {
                "Math" -> LightBlueBg
                "Eng" -> LightPurpleBg
                "Sci" -> LightGreenBg
                "Ar" -> LightOrangeBg
                "PE", "ICT" -> LightYellowBg
                "Break" -> LightGrayState
                else -> if (isHeader) PrimaryNavy else WhiteSurface
              }

              val cellTextColor = when (cell) {
                "Math" -> RoyalBlue
                "Eng" -> PurpleState
                "Sci" -> GreenState
                "Ar" -> OrangeText
                "PE", "ICT" -> OrangeText
                "Break" -> GrayState
                else -> if (isHeader) Color.White else TextPrimary
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(34.dp)
                  .background(cellColor)
                  .border(0.5.dp, BorderColor)
                  .clickable {} // click slot
                  .padding(4.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = cell,
                  color = cellTextColor,
                  fontSize = 9.sp,
                  fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Medium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    }

    Button(
      onClick = {},
      colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(text = Trans.t("Save Timetable — Goes Live Now", "احفظ الجدول — النشر الآن فورياً"))
    }
  }
}

// Manager Subtabs: BROADCASTS
@Composable
fun ManagerBroadcastTab() {
  var audienceTarget by remember { mutableStateOf("All Parents") }
  var urgencyLevel by remember { mutableStateOf("Info") }
  var bodyValue by remember { mutableStateOf("") }
  var confirmationMessageText by remember { mutableStateOf("") }

  val targetList = listOf("All Parents", "Year 6", "Year 5", "KG", "Teachers")
  val targetArab = mapOf(
    "All Parents" to "جميع الآباء",
    "Year 6" to "الصف السادس",
    "Year 5" to "الصف الخامس",
    "KG" to "الروضة",
    "Teachers" to "المعلمون"
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = WhiteSurface,
      border = BorderStroke(1.dp, BorderColor),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = Trans.t("Send Announcement", "إرسال تعميم وإعلان للكل"),
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = TextPrimary,
          modifier = Modifier.padding(bottom = 12.dp)
        )

        // Audience target toggles
        Text(text = Trans.t("Audience Selection", "الفئة المستهدفة"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          targetList.forEach { trg ->
            val isSel = audienceTarget == trg
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSel) PrimaryNavy else PageBg)
                .clickable { audienceTarget = trg }
                .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text(
                text = if (Trans.isArabic) targetArab[trg] ?: trg else trg,
                color = if (isSel) Color.White else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Urgency level toggles
        Text(text = Trans.t("Notification Type", "نوع الإشعار والخطورة"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("Info", "Urgent", "Event").forEach { urg ->
            val selected = urgencyLevel == urg
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) RoyalBlue else PageBg)
                .clickable { urgencyLevel = urg }
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = when (urg) {
                  "Info" -> Trans.t("📢 Info", "📢 إرشادي")
                  "Urgent" -> Trans.t("🔴 Urgent", "🔴 عاجل هام")
                  else -> Trans.t("📅 Event", "📅 فعالية")
                },
                color = if (selected) Color.White else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = bodyValue,
          onValueChange = { bodyValue = it },
          placeholder = { Text(text = Trans.t("Type your message in Arabic or English…", "اكتب محتوى الإعلان بالدراسة واللغات...")) },
          modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            if (bodyValue.isNotBlank()) {
              confirmationMessageText = "✅ Announcement published directly!"
              bodyValue = ""
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(text = Trans.t("Send Push Notification Now", "إرسال الإشعار الفوري الآن للجميع"))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = {},
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(text = Trans.t("Schedule for Later", "جدولة الإرسال لاحقاً"), color = PrimaryNavy)
        }

        if (confirmationMessageText.isNotEmpty()) {
          Text(
            text = confirmationMessageText,
            color = GreenState,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 10.dp)
          )
        }
      }
    }
  }
}


// SCREEN 4: ENROLLMENT FORM SCREEN
@Composable
fun EnrollmentScreen() {
  val list = remember { MockRepo.availableGrades }
  var showConfirmDialog by remember { mutableStateOf(false) }
  val currency = Trans.t("EGP", "ج.م")

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Banner gradient
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = PrimaryNavy
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(text = Trans.t("Reserve a Seat", "حجز مقعد جديد"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text(text = Trans.t("After reserving, AI auto-creates your folder in Google Drive and notifies the manager.", "بمجرد الحجز، سينشئ الذكاء الاصطناعي مجلداً تلقائياً في جوجل درايف وإرسال تنبيه للإدارة."), color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
        }
      }
    }

    Text(
      text = Trans.t("Available Grades — 2025/2026", "الصفوف الدراسية الشاغرة — ٢٠٢٥/٢٠٢٦"),
      fontWeight = FontWeight.Bold,
      fontSize = 14.sp,
      color = TextPrimary
    )

    // Grade Cards list
    list.forEach { grade ->
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = WhiteSurface,
        border = BorderStroke(1.dp, BorderColor)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Left initials avatar box representation
          Box(
            modifier = Modifier
              .size(50.dp)
              .background(PrimaryNavy, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
          ) {
            Text(text = grade.code, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1.0f)) {
            Text(text = grade.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            Text(text = "${grade.fee} $currency/yr · ${grade.installment} $currency/mo", fontSize = 11.sp, color = TextMuted)
            Text(text = grade.teacher, fontSize = 11.sp, color = RoyalBlue)
          }

          // Seats badge
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(if (grade.isLow) LightYellowBg else LightGreenBg)
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = "${grade.seats} " + Trans.t("seats", "مقاعد"),
              color = if (grade.isLow) OrangeText else GreenState,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // Reservation action
    Button(
      onClick = { showConfirmDialog = true },
      colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(text = Trans.t("Reserve a Seat Now", "احجز مقعداً دراسياً الآن"), fontWeight = FontWeight.Bold)
    }

    OutlinedButton(
      onClick = { showConfirmDialog = true },
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(text = Trans.t("Continue with Google", "الاستمرار والتسجيل بحساب جوجل"), color = PrimaryNavy)
    }

    if (showConfirmDialog) {
      AlertDialog(
        onDismissRequest = { showConfirmDialog = false },
        title = { Text(text = Trans.t("Enrollment Registration", "تأكيد طلب التقديم")) },
        text = { Text(text = Trans.t("Your application request has been received! Our registrar team is reviewing matching documents dynamically in Drive.", "تم استلام رغبة التسجيل بنجاح! يقوم مسؤول التسجيل حالياً بمراجعة الهوية وشهادة الميلاد بالتنسيق التلقائي.")) },
        confirmButton = {
          Button(
            onClick = { showConfirmDialog = false },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
          ) {
            Text(text = Trans.t("Acknowledge", "موافق"))
          }
        }
      )
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

