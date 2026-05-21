package com.example.pcos_nutritionist.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pcos_nutritionist.R
import com.example.pcos_nutritionist.api.DietPlanResponse
import com.example.pcos_nutritionist.api.PatientInput
import com.example.pcos_nutritionist.api.RetrofitClient
import com.example.pcos_nutritionist.ui.CustomCard
import com.example.pcos_nutritionist.ui.GradientButton
import com.example.pcos_nutritionist.ui.ModernTextField
import com.example.pcos_nutritionist.ui.theme.*
import com.example.pcos_nutritionist.utils.AuthViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboardScreen(onSignOut: () -> Unit) {
    val authViewModel: AuthViewModel = viewModel()
    val userId = authViewModel.getCurrentUser()?.uid ?: "UNKNOWN"
    
    var currentTab by remember { mutableStateOf(0) } // 0: Home, 1: Plan, 2: Tracking, 3: Q&A, 4: Profile
    var showForm by remember { mutableStateOf(false) }
    var showDailyTracking by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showPmosGuidance by remember { mutableStateOf(false) }

    var approvedPlan by remember { mutableStateOf<Map<String, Map<String, com.example.pcos_nutritionist.api.MealDetail>>?>(null) }
    var planStatus by remember { mutableStateOf<String>("Loading...") }
    var isLoadingPlan by remember { mutableStateOf(true) }
    var patientInputs by remember { mutableStateOf<com.example.pcos_nutritionist.api.PatientInput?>(null) }
    var nutritionistNotes by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        if (userId != "UNKNOWN") {
            try {
                val response = RetrofitClient.apiService.getPatientPlan(userId)
                planStatus = response.status
                if (response.status == "Approved" || response.status == "Pending Review") {
                    approvedPlan = response.plan
                    nutritionistNotes = response.nutritionist_notes
                }
                try {
                    patientInputs = RetrofitClient.apiService.getPatientInputs(userId)
                } catch (e: Exception) {
                    patientInputs = null
                }
            } catch (e: Exception) {
                planStatus = "Error loading plan"
            } finally {
                isLoadingPlan = false
            }
        } else {
            isLoadingPlan = false
            planStatus = "No Plan Found"
        }
    }

    Scaffold(
        bottomBar = {
            if (!showForm && !showDailyTracking && !showHistory) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Home", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryViolet, selectedTextColor = PrimaryViolet, indicatorColor = PrimaryVioletLight)
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.RestaurantMenu, null) },
                        label = { Text("Plan", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryViolet, selectedTextColor = PrimaryViolet, indicatorColor = PrimaryVioletLight)
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.Assessment, null) },
                        label = { Text("Tracking", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryViolet, selectedTextColor = PrimaryViolet, indicatorColor = PrimaryVioletLight)
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Default.QuestionAnswer, null) },
                        label = { Text("Q&A", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryViolet, selectedTextColor = PrimaryViolet, indicatorColor = PrimaryVioletLight)
                    )
                    NavigationBarItem(
                        selected = currentTab == 4,
                        onClick = { currentTab = 4 },
                        icon = { Icon(Icons.Default.Person, null) },
                        label = { Text("Profile", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryViolet, selectedTextColor = PrimaryViolet, indicatorColor = PrimaryVioletLight)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(BackgroundColor)) {
            if (showForm) {
                PatientFormScreen(
                    userId = userId,
                    onBack = { showForm = false },
                    onPlanGenerated = {
                        showForm = false
                        coroutineScope.launch {
                            try {
                                isLoadingPlan = true
                                val response = RetrofitClient.apiService.getPatientPlan(userId)
                                planStatus = response.status
                                if (response.status == "Approved" || response.status == "Pending Review") {
                                    approvedPlan = response.plan
                                    nutritionistNotes = response.nutritionist_notes
                                }
                                patientInputs = RetrofitClient.apiService.getPatientInputs(userId)
                            } catch (e: Exception) {} finally { isLoadingPlan = false }
                        }
                    }
                )
            } else if (showDailyTracking) {
                DailyMealTrackingScreen(
                    userId = userId,
                    onBack = { showDailyTracking = false }
                )
            } else if (showHistory) {
                HealthHistoryTab(patientInputs = patientInputs, onBack = { showHistory = false })
            } else if (showPmosGuidance) {
                PmosGuidanceScreen(onBack = { showPmosGuidance = false })
            } else {
                when (currentTab) {
                    0 -> DashboardHome(
                        patientName = patientInputs?.patient_name ?: "User",
                        planStatus = planStatus,
                        patientInputs = patientInputs,
                        onGenerateNew = { showForm = true },
                        onViewPlan = { currentTab = 1 },
                        onViewHistory = { showHistory = true }
                    )
                    1 -> DietPlanTab(
                        planStatus = planStatus,
                        approvedPlan = approvedPlan,
                        patientInputs = patientInputs,
                        nutritionistNotes = nutritionistNotes
                    )
                    2 -> TrackingOverviewTab(
                        userId = userId,
                        onLogMeal = { showDailyTracking = true }
                    )
                    3 -> PatientQATab(
                        userId = userId,
                        patientName = patientInputs?.patient_name ?: "User"
                    )
                    4 -> ProfileTab(
                        patientName = patientInputs?.patient_name ?: "User",
                        email = authViewModel.getCurrentUser()?.email ?: "",
                        onChangePassword = { showChangePassword = true },
                        onSignOut = {
                            authViewModel.signout()
                            onSignOut()
                        },
                        onPmosGuidanceClick = { showPmosGuidance = true }
                    )
                }
            }
            if (showChangePassword) {
                PatientChangePasswordDialog(onDismiss = { showChangePassword = false })
            }
        }
    }
}

@Composable
fun DashboardHome(
    patientName: String,
    planStatus: String,
    patientInputs: PatientInput?,
    onGenerateNew: () -> Unit,
    onViewPlan: () -> Unit,
    onViewHistory: () -> Unit
) {
    var showNotifications by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Hello, $patientName", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "\uD83D\uDC4B", fontSize = 18.sp)
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, CircleShape)
                    .clickable { showNotifications = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, null, tint = PrimaryViolet)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Diet Plan Status Card
        CustomCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Your Diet Plan Status", fontSize = 14.sp, color = SubtitleColor)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (planStatus == "Approved") "Approved" else if (planStatus == "Pending Review") "Pending Review" else "No Active Plan",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (planStatus == "Approved") ApprovedColor else if (planStatus == "Pending Review") PendingColor else Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (planStatus == "Approved") "Your diet plan is approved by\nyour nutritionist." else "The nutritionist is currently\nreviewing your plan.",
                            fontSize = 12.sp,
                            color = SubtitleColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onViewPlan,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(text = "View Diet Plan", fontSize = 12.sp)
                        }
                    }
                    // Hourglass icon/illustration placeholder
                    Icon(
                        imageVector = if (planStatus == "Approved") Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = if (planStatus == "Approved") ApprovedColor else PendingColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Health Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // Health Summary Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                label = "PMOS Risk Level",
                value = "Moderate",
                valueColor = RiskModerate,
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = "BMI",
                value = if (patientInputs != null) String.format("%.1f", patientInputs.weight_kg / ((patientInputs.height_cm/100)*(patientInputs.height_cm/100))) else "--",
                valueColor = BMIColor,
                icon = Icons.Default.Height,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                label = "PMOS Type",
                value = patientInputs?.pcos_type ?: "N/A",
                valueColor = PrimaryViolet,
                icon = Icons.Default.Favorite,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = "Last Updated",
                value = "Today",
                valueColor = SubtitleColor,
                icon = Icons.Default.CalendarToday,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Quick Actions", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionItem(Icons.Default.Add, "Generate New\nDiet Plan", onGenerateNew)
            QuickActionItem(Icons.Default.Visibility, "View My\nPlan", onViewPlan)
            QuickActionItem(Icons.Default.History, "Input\nHistory", onViewHistory)
            QuickActionItem(Icons.Default.Person, "My\nProfile", { /* profile */ })
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showNotifications) {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val (greeting, motivationalNote) = when (hour) {
            in 5..10 -> "Good morning!" to "Start your day fresh! Don't forget to track your wholesome breakfast."
            in 11..14 -> "Good afternoon!" to "Keep your energy up with a balanced lunch. Log your meal now!"
            in 15..17 -> "Afternoon slump?" to "A healthy snack is a great idea. Remember to log it!"
            in 18..22 -> "Good evening!" to "Time to wind down with a nourishing dinner. Have you tracked it?"
            else -> "Hello there!" to "Staying hydrated and resting is just as important as eating well!"
        }
        
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = { Text("Notifications", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryViolet) },
            text = {
                Column {
                    Text(text = "Reminder: $greeting", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = motivationalNote, fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotifications = false }) {
                    Text("Close", color = PrimaryViolet, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SummaryCard(label: String, value: String, valueColor: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, fontSize = 11.sp, color = SubtitleColor, maxLines = 1)
                Spacer(modifier = Modifier.weight(1f))
                Icon(icon, null, modifier = Modifier.size(14.dp), tint = PrimaryViolet)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(PrimaryVioletLight, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = PrimaryViolet)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 11.sp, textAlign = TextAlign.Center, color = TextColor, lineHeight = 14.sp)
    }
}

@Composable
fun DietPlanTab(
    planStatus: String,
    approvedPlan: Map<String, Map<String, com.example.pcos_nutritionist.api.MealDetail>>?,
    patientInputs: PatientInput?,
    nutritionistNotes: String?
) {
    var selectedDay by remember { mutableStateOf("Mon") }
    var showExplanation by remember { mutableStateOf(false) }
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    if (showExplanation) {
        DietExplanationDialog(explanation = patientInputs?.explanation) {
            showExplanation = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
            Text(text = if (planStatus == "Approved") "Approved Diet Plan" else "Pending Diet Plan", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        // Day selector
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).background(Color.White).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            days.forEach { day ->
                val isSelected = selectedDay == day
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) PrimaryViolet else Color.Transparent)
                        .clickable { selectedDay = day }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text = day, color = if (isSelected) Color.White else SubtitleColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (planStatus == "Approved" && approvedPlan != null) {
                // Nutritionist Notes
                if (nutritionistNotes != null) {
                    item {
                        var acts = ""
                        var sleep = ""
                        var water = ""
                        var nNotes = ""
                        try {
                            val json = JSONObject(nutritionistNotes)
                            acts = json.optString("activities", "")
                            sleep = json.optString("sleep", "")
                            water = json.optString("water", "")
                            nNotes = json.optString("notes", "")
                        } catch (e: Exception) { nNotes = nutritionistNotes }
                        
                        CustomCard(modifier = Modifier.padding(bottom = 16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(40.dp).background(PrimaryVioletLight, CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.EditNote, null, tint = PrimaryViolet)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = "Nutritionist Recommendations", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                if (acts.isNotBlank()) Text(text = "🏃 Physical Activities: $acts", fontSize = 12.sp, color = TextColor, modifier = Modifier.padding(bottom = 4.dp))
                                if (sleep.isNotBlank()) Text(text = "🌙 Sleep: $sleep hours", fontSize = 12.sp, color = TextColor, modifier = Modifier.padding(bottom = 4.dp))
                                if (water.isNotBlank()) Text(text = "💧 Water Intake: $water glasses", fontSize = 12.sp, color = TextColor, modifier = Modifier.padding(bottom = 4.dp))
                                if (nNotes.isNotBlank() && nNotes != "null") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "💡 Nutritionist Instructions: $nNotes", fontSize = 12.sp, color = SubtitleColor)
                                }
                            }
                        }
                    }
                }

                // Meals for the selected day
                val dayKey = when(selectedDay) {
                    "Mon" -> "Monday"
                    "Tue" -> "Tuesday"
                    "Wed" -> "Wednesday"
                    "Thu" -> "Thursday"
                    "Fri" -> "Friday"
                    "Sat" -> "Saturday"
                    "Sun" -> "Sunday"
                    else -> "Monday"
                }
                
                val meals = approvedPlan[dayKey] ?: emptyMap()
                
                items(meals.toList()) { (mealType, detail) ->
                    MealItem(mealType, detail.food, detail.portion)
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showExplanation = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Info, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Why these foods?")
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No approved plan found.", color = SubtitleColor)
                    }
                }
            }
        }
    }
}

@Composable
fun MealItem(type: String, food: String, portion: String) {
    CustomCard(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                // In a real app, use an image URL. For now, an icon.
                val icon = when(type.lowercase()) {
                    "breakfast" -> Icons.Default.WbSunny
                    "lunch" -> Icons.Default.Restaurant
                    "snack" -> Icons.Default.BakeryDining
                    "dinner" -> Icons.Default.NightsStay
                    else -> Icons.Default.Coffee
                }
                Icon(icon, null, tint = PrimaryViolet, modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = type, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryViolet)
                Text(text = food, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2)
                Text(text = "Portion: $portion", fontSize = 12.sp, color = SubtitleColor)
            }
        }
    }
}

@Composable
fun HealthHistoryTab(patientInputs: PatientInput?, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(BackgroundColor).verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = PrimaryViolet) }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Health Profile Summary", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryViolet)
        }
        Column(modifier = Modifier.padding(20.dp)) {
        
        if (patientInputs != null) {
            CustomCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(50.dp).background(PrimaryVioletLight, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = PrimaryViolet)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = patientInputs.patient_name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "Age: ${patientInputs.age} yrs | BMI: ${String.format("%.1f", patientInputs.weight_kg / ((patientInputs.height_cm/100)*(patientInputs.height_cm/100)))} | Type: ${if (patientInputs.pcos_type == "Adrenal") "Adrenal Fatigue" else patientInputs.pcos_type}", fontSize = 12.sp, color = SubtitleColor)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Hormonal Levels", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HormoneItem("FSH", patientInputs.fsh.toString(), "mIU/mL", Modifier.weight(1f))
                HormoneItem("LH", patientInputs.lh.toString(), "mIU/mL", Modifier.weight(1f))
                HormoneItem("TSH", patientInputs.tsh.toString(), "mIU/L", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HormoneItem("PRL", patientInputs.prl.toString(), "ng/mL", Modifier.weight(1f))
                HormoneItem("PRG", patientInputs.prg.toString(), "ng/mL", Modifier.weight(1f))
                HormoneItem("RBS", patientInputs.rbs.toString(), "mg/dl", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HormoneItem("AMH", patientInputs.amh.toString(), "ng/mL", Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(2f)) // Padding so AMH doesn't stretch too much
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Conditions", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                if (patientInputs.selected_conditions.isEmpty()) {
                    Text(text = "None", color = SubtitleColor, fontSize = 14.sp)
                } else {
                    patientInputs.selected_conditions.forEach { condition ->
                        ChipItem(condition)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Symptoms", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                val symptoms = mutableListOf<String>()
                if (patientInputs.weight_gain == 1) symptoms.add("Weight Gain")
                if (patientInputs.hair_growth == 1) symptoms.add("Hair Growth")
                if (patientInputs.skin_darkening == 1) symptoms.add("Skin Darkening")
                if (patientInputs.hair_loss == 1) symptoms.add("Hair Loss")
                if (patientInputs.pimples == 1) symptoms.add("Pimples")
                if (patientInputs.irregular_periods == 1) symptoms.add("Irregular Periods")
                
                if (symptoms.isEmpty()) {
                    Text(text = "None", color = SubtitleColor, fontSize = 14.sp)
                } else {
                    symptoms.forEach { ChipItem(it) }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Allergies", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                if (patientInputs.allergies.isEmpty()) {
                    Text(text = "None", color = SubtitleColor, fontSize = 14.sp)
                } else {
                    patientInputs.allergies.forEach { allergy ->
                        ChipItem(allergy)
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No history found.", color = SubtitleColor)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
    }
}

@Composable
fun HormoneItem(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, fontSize = 10.sp, color = SubtitleColor)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryViolet)
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = unit, fontSize = 8.sp, color = SubtitleColor, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
fun ChipItem(text: String) {
    Surface(
        modifier = Modifier.padding(4.dp),
        shape = RoundedCornerShape(20.dp),
        color = PrimaryVioletLight
    ) {
        Text(text = text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        content = content
    )
}

@Composable
fun DietExplanationDialog(explanation: List<String>?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dietary Explanations", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                if (explanation.isNullOrEmpty()) {
                    item { Text("No explanations available yet.", fontSize = 14.sp) }
                } else {
                    items(explanation) { expl ->
                        Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.CheckCircle, null, tint = ApprovedColor, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = expl, fontSize = 14.sp, color = TextColor)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PrimaryViolet, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ProfileTab(patientName: String, email: String, onChangePassword: () -> Unit, onSignOut: () -> Unit, onPmosGuidanceClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(40.dp))
        Box(modifier = Modifier.size(100.dp).background(PrimaryVioletLight, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(60.dp), tint = PrimaryViolet)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = patientName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(text = email, fontSize = 14.sp, color = SubtitleColor)
        
        Spacer(modifier = Modifier.height(40.dp))
        
        ProfileMenuItem(Icons.Default.Lock, "Change Password", onChangePassword)
        ProfileMenuItem(Icons.Default.Help, "PMOS Guidance", onPmosGuidanceClick)
        ProfileMenuItem(Icons.Default.ExitToApp, "Sign Out", onSignOut, isError = true)
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, label: String, onClick: () -> Unit, isError: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(if (isError) Color(0xFFFFEBEE) else Color(0xFFF1F0F5), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (isError) Color.Red else PrimaryViolet, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, modifier = Modifier.weight(1f), fontSize = 16.sp, color = if (isError) Color.Red else TextColor)
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
    }
}

// --- FORM REDESIGN (Multi-step Stepper) ---

@Composable
fun PatientFormScreen(
    userId: String,
    onBack: () -> Unit,
    onPlanGenerated: (DietPlanResponse) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 7

    // State Variables for Inputs
    var patientName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var fsh by remember { mutableStateOf("") }
    var lh by remember { mutableStateOf("") }
    var tsh by remember { mutableStateOf("") }
    var prl by remember { mutableStateOf("") }
    var prg by remember { mutableStateOf("") }
    var rbs by remember { mutableStateOf("") }
    var amh by remember { mutableStateOf("") }

    // Symptoms
    var weightGain by remember { mutableStateOf(false) }
    var hairGrowth by remember { mutableStateOf(false) }
    var skinDarkening by remember { mutableStateOf(false) }
    var hairLoss by remember { mutableStateOf(false) }
    var pimples by remember { mutableStateOf(false) }
    var irregularPeriods by remember { mutableStateOf(false) }

    // Medical Conditions
    var hasDiabetes by remember { mutableStateOf(false) }
    var hasThyroid by remember { mutableStateOf(false) }
    var hasHypertension by remember { mutableStateOf(false) }

    // PCOS Type
    val pcosTypes = listOf("Insulin Resistance","Hypothyroidism", "Adrenal Fatigue", "Pill Induced", "Inflammation", "General")
    var selectedPcosType by remember { mutableStateOf(pcosTypes.last()) }

    // Allergies
    val allergyOptions = listOf("Almonds",
        "Walnuts",
        "Pumpkin Seeds",
        "Chia Seeds",
        "Flaxseeds",
        "Salmon",
        "Mackerel",
        "Tuna",
        "Sardines",
        "Trout",
        "Chicken",
        "Turkey",
        "Beef",
        "Pork",
        "Tofu",
        "Tempeh",
        "Edamame",
        "Milk",
        "Greek Yogurt",
        "Kefir",
        "Oat Milk",
        "Almond Milk",
        "Wheat",
        "Barley",
        "Oats",
        "Soy",
        "Egg",
        "Coconut",
        "Ghee",
        "Brown Rice",
        "Quinoa",
        "Millet",
        "Buckwheat",
        "Amaranth",
        "Lentils",
        "Chickpeas",
        "Black Beans",
        "Kidney Beans",
        "Spinach",
        "Kale",
        "Swiss Chard",
        "Collard Greens",
        "Leafy Greens",
        "Broccoli",
        "Cauliflower",
        "Brussels Sprouts",
        "Zucchini",
        "Bell Peppers",
        "Asparagus",
        "Carrots",
        "Cucumbers",
        "Artichokes",
        "Tomatoes",
        "Sweet Potatoes",
        "Blueberries",
        "Strawberries",
        "Raspberries",
        "Apples",
        "Pears",
        "Oranges",
        "Lemons",
        "Limes",
        "Cherries",
        "Plums",
        "Avocado",
        "Pumpkin Seeds")
    var selectedAllergies by remember { mutableStateOf(setOf<String>()) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Header with Stepper
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (currentStep > 1) currentStep-- else onBack() }) {
                    Icon(Icons.Default.ArrowBack, null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Health Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            StepIndicator(currentStep, totalSteps)
        }

        Box(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
            when (currentStep) {
                1 -> FormBasicInfo(patientName, dob, weight, height, {patientName=it}, {dob=it}, {weight=it}, {height=it})
                2 -> FormHormones(fsh, lh, tsh, prl, prg, rbs, amh, {fsh=it}, {lh=it}, {tsh=it}, {prl=it}, {prg=it}, {rbs=it}, {amh=it})
                3 -> FormSymptoms(weightGain, hairGrowth, skinDarkening, hairLoss, pimples, irregularPeriods, {weightGain=it}, {hairGrowth=it}, {skinDarkening=it}, {hairLoss=it}, {pimples=it}, {irregularPeriods=it})
                4 -> FormConditions(hasDiabetes, hasThyroid, hasHypertension, {hasDiabetes=it}, {hasThyroid=it}, {hasHypertension=it})
                5 -> FormAllergies(allergyOptions, selectedAllergies, {selectedAllergies=it})
                6 -> FormPcosType(pcosTypes, selectedPcosType, {selectedPcosType=it})
                7 -> FormMedicalReport()
            }
        }

        // Bottom Navigation Buttons
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (currentStep > 1) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Back")
                }
            }
            GradientButton(
                text = if (currentStep == totalSteps) "Generate Plan" else "Next",
                onClick = {
                    if (currentStep < totalSteps) {
                        currentStep++
                    } else {
                        // Submit Logic
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val conditions = mutableListOf<String>()
                                if (hasDiabetes) conditions.add("diabetes")
                                if (hasThyroid) conditions.add("thyroid")
                                if (hasHypertension) conditions.add("hypertension")

                                val calculatedAge = try {
                                    val parts = dob.split("/")
                                    val birthYear = parts[2].toInt()
                                    val birthMonth = parts[1].toInt()
                                    val birthDay = parts[0].toInt()
                                    val today = java.util.Calendar.getInstance()
                                    var a = today.get(java.util.Calendar.YEAR) - birthYear
                                    if (today.get(java.util.Calendar.MONTH) + 1 < birthMonth || 
                                        (today.get(java.util.Calendar.MONTH) + 1 == birthMonth && today.get(java.util.Calendar.DAY_OF_MONTH) < birthDay)) {
                                        a--
                                    }
                                    a
                                } catch (e: Exception) { 0 }

                                val input = PatientInput(
                                    patient_id = userId,
                                    patient_name = patientName,
                                    age = calculatedAge,
                                    weight_kg = weight.toDoubleOrNull() ?: 0.0,
                                    height_cm = height.toDoubleOrNull() ?: 0.0,
                                    fsh = fsh.toDoubleOrNull() ?: 0.0,
                                    lh = lh.toDoubleOrNull() ?: 0.0,
                                    tsh = tsh.toDoubleOrNull() ?: 0.0,
                                    prl = prl.toDoubleOrNull() ?: 0.0,
                                    prg = prg.toDoubleOrNull() ?: 0.0,
                                    rbs = rbs.toDoubleOrNull() ?: 0.0,
                                    amh = amh.toDoubleOrNull() ?: 0.0,
                                    weight_gain = if (weightGain) 1 else 0,
                                    hair_growth = if (hairGrowth) 1 else 0,
                                    skin_darkening = if (skinDarkening) 1 else 0,
                                    hair_loss = if (hairLoss) 1 else 0,
                                    pimples = if (pimples) 1 else 0,
                                    irregular_periods = if (irregularPeriods) 1 else 0,
                                    selected_conditions = conditions,
                                    pcos_type = if (selectedPcosType == "Adrenal Fatigue") "Adrenal" else selectedPcosType,
                                    allergies = selectedAllergies.toList()
                                )
                                val response = RetrofitClient.apiService.predictDiet(input)
                                isLoading = false
                                onPlanGenerated(response)
                            } catch (e: Exception) {
                                isLoading = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                loading = isLoading
            )
        }
    }
}

@Composable
fun StepIndicator(current: Int, total: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        (1..total).forEach { step ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(if (step <= current) PrimaryViolet else Color(0xFFEEEEEE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = step.toString(), color = if (step <= current) Color.White else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (step < total) {
                Box(modifier = Modifier.weight(1f).height(2.dp).background(if (step < current) PrimaryViolet else Color(0xFFEEEEEE)))
            }
        }
    }
}

@Composable
fun FormBasicInfo(name: String, dob: String, weight: String, height: String, onName: (String)->Unit, onDob: (String)->Unit, onWeight: (String)->Unit, onHeight: (String)->Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Step 1 of 6", color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Basic Information", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        ModernTextField(value = name, onValueChange = onName, label = "Full Name", leadingIcon = Icons.Default.Person)
        Spacer(modifier = Modifier.height(16.dp))
        
        val context = LocalContext.current
        val calendar = java.util.Calendar.getInstance()
        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDob("$dayOfMonth/${month + 1}/$year")
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
        
        Box(modifier = Modifier.fillMaxWidth()) {
            ModernTextField(
                value = dob, 
                onValueChange = {}, 
                label = "Date of Birth (DD/MM/YYYY)", 
                leadingIcon = Icons.Default.CalendarToday, 
                modifier = Modifier.fillMaxWidth()
            )
            Box(modifier = Modifier.matchParentSize().background(Color.Transparent).clickable { datePickerDialog.show() })
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        ModernTextField(value = weight, onValueChange = onWeight, label = "Weight (kg)", leadingIcon = Icons.Default.MonitorWeight, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(16.dp))
        ModernTextField(value = height, onValueChange = onHeight, label = "Height (cm)", leadingIcon = Icons.Default.Height, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
}

@Composable
fun FormHormones(fsh: String, lh: String, tsh: String, prl: String, prg: String, rbs: String, amh: String, onFsh: (String)->Unit, onLh: (String)->Unit, onTsh: (String)->Unit, onPrl: (String)->Unit, onPrg: (String)->Unit, onRbs: (String)->Unit, onAmh: (String)->Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Step 2 of 6", color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Hormonal Levels", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        ModernTextField(value = fsh, onValueChange = onFsh, label = "FSH (mIU/mL)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(12.dp))
        ModernTextField(value = lh, onValueChange = onLh, label = "LH (mIU/mL)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(12.dp))
        ModernTextField(value = tsh, onValueChange = onTsh, label = "TSH (mIU/L)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(12.dp))
        ModernTextField(value = prl, onValueChange = onPrl, label = "Prolactin (PRL) (ng/mL)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(12.dp))
        ModernTextField(value = prg, onValueChange = onPrg, label = "Progesterone (PRG) (ng/mL)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(12.dp))
        ModernTextField(value = rbs, onValueChange = onRbs, label = "RBS (Blood Sugar) (mg/dL)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(12.dp))
        ModernTextField(value = amh, onValueChange = onAmh, label = "AMH (ng/mL)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
}

@Composable
fun FormSymptoms(wg: Boolean, hg: Boolean, sd: Boolean, hl: Boolean, p: Boolean, ip: Boolean, onWg: (Boolean)->Unit, onHg: (Boolean)->Unit, onSd: (Boolean)->Unit, onHl: (Boolean)->Unit, onP: (Boolean)->Unit, onIp: (Boolean)->Unit) {
    Column {
        Text("Step 3 of 6", color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("PMOS Symptoms", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("(Select all that apply)", color = SubtitleColor, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(24.dp))
        
        val symptoms = listOf(
            SymptomData("Weight Gain", Icons.Default.AddBox, wg, onWg),
            SymptomData("Excess Hair Growth", Icons.Default.Face, hg, onHg),
            SymptomData("Skin Darkening", Icons.Default.Warning, sd, onSd),
            SymptomData("Hair Loss", Icons.Default.FaceRetouchingNatural, hl, onHl),
            SymptomData("Acne", Icons.Default.Face, p, onP),
            SymptomData("Irregular Periods", Icons.Default.CalendarViewMonth, ip, onIp)
        )
        
        LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(symptoms) { symptom ->
                SymptomCard(symptom)
            }
        }
    }
}

data class SymptomData(val label: String, val icon: ImageVector, val isChecked: Boolean, val onToggle: (Boolean) -> Unit)

@Composable
fun SymptomCard(data: SymptomData) {
    Card(
        modifier = Modifier.height(100.dp).clickable { data.onToggle(!data.isChecked) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (data.isChecked) PrimaryViolet.copy(alpha = 0.1f) else Color(0xFFF8F8F8)),
        border = if (data.isChecked) BorderStroke(1.dp, PrimaryViolet) else null
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(data.icon, null, tint = if (data.isChecked) PrimaryViolet else Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = data.label, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 12.sp, color = if (data.isChecked) PrimaryViolet else Color.Gray)
            if (data.isChecked) {
                Icon(Icons.Default.CheckCircle, null, tint = PrimaryViolet, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun FormConditions(d: Boolean, t: Boolean, h: Boolean, onD: (Boolean)->Unit, onT: (Boolean)->Unit, onH: (Boolean)->Unit) {
    Column {
        Text("Step 4 of 6", color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Medical Conditions", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        ConditionCheckRow("Diabetes", d, onD)
        ConditionCheckRow("Thyroid", t, onT)
        ConditionCheckRow("Hypertension", h, onH)
    }
}

@Composable
fun ConditionCheckRow(label: String, checked: Boolean, onToggle: (Boolean)->Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onToggle(!checked) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (checked) PrimaryVioletLight else Color(0xFFF8F8F8))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Checkbox(checked = checked, onCheckedChange = onToggle, colors = CheckboxDefaults.colors(checkedColor = PrimaryViolet))
        }
    }
}

@Composable
fun FormAllergies(options: List<String>, selected: Set<String>, onUpdate: (Set<String>)->Unit) {
    Column {
        Text("Step 5 of 6", color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Food Allergies", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("(Select all that apply)", color = SubtitleColor, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        FlowRow(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            options.forEach { allergy ->
                val isSelected = selected.contains(allergy)
                Surface(
                    modifier = Modifier.padding(4.dp).clickable { 
                        val newSet = selected.toMutableSet()
                        if (isSelected) newSet.remove(allergy) else newSet.add(allergy)
                        onUpdate(newSet)
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) PrimaryViolet else Color(0xFFF1F0F5),
                    border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text(
                        text = allergy,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (isSelected) Color.White else SubtitleColor,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FormPcosType(types: List<String>, selected: String, onUpdate: (String)->Unit) {
    Column {
        Text("Step 6 of 6", color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("PMOS Subtype", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        types.forEach { type ->
            val isSelected = selected == type
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onUpdate(type) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) PrimaryVioletLight else Color(0xFFF8F8F8)),
                border = if (isSelected) BorderStroke(1.dp, PrimaryViolet) else null
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = type, modifier = Modifier.weight(1f), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = PrimaryViolet)
                }
            }
        }
    }
}

@Composable
fun FormMedicalReport() {
    var dummyUploaded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Column {
        Text("Step 7 of 7", color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Upload Medical Report", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("(Optional)", color = SubtitleColor, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().height(150.dp).clickable { 
                dummyUploaded = true
                Toast.makeText(context, "Medical Report Uploaded (Dummy)", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = if (dummyUploaded) PrimaryVioletLight else Color(0xFFF8F8F8)),
            border = BorderStroke(1.dp, PrimaryViolet)
        ) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(if (dummyUploaded) Icons.Default.CheckCircle else Icons.Default.CloudUpload, null, tint = PrimaryViolet, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = if (dummyUploaded) "Report Uploaded Successfully!" else "Tap to upload image", color = PrimaryViolet, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- Tracking Components ---

@Composable
fun TrackingOverviewTab(userId: String, onLogMeal: () -> Unit) {
    var trackingData by remember { mutableStateOf<com.example.pcos_nutritionist.api.TrackingResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showActivityDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

    fun loadData() {
        coroutineScope.launch {
            try {
                isLoading = true
                trackingData = RetrofitClient.apiService.getPatientTracking(userId, todayDate)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(userId) { loadData() }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryViolet)
        }
        return
    }

    val daily = trackingData?.daily
    val meals = trackingData?.meals ?: emptyList()
    val weekly = trackingData?.weekly ?: emptyList()
    val mealsLoggedCount = meals.count { it.status == "Logged" || it.status == "Completed" }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9)).padding(20.dp).verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Meal Tracking", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.NotificationsNone, null, tint = PrimaryViolet)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Track your meals, stay on track, feel your best!", color = SubtitleColor, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))

        // Purple Gradient Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF9C27B0), Color(0xFF673AB7))
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = "Today's Progress", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(text = "Great job! \uD83C\uDF89", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "You're doing amazing!", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                        }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                            CircularProgressIndicator(
                                progress = { (daily?.adherence_percentage ?: 0) / 100f },
                                modifier = Modifier.fillMaxSize(),
                                color = Color(0xFF00E676), // Bright Green
                                trackColor = Color.White.copy(alpha = 0.2f),
                                strokeWidth = 6.dp
                            )
                            Text(text = "${daily?.adherence_percentage ?: 0}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onLogMeal,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PrimaryViolet),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RestaurantMenu, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Log a Meal", fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showActivityDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PrimaryViolet),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsRun, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Log Activity", fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)) // Light blue for hydration
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Opacity, contentDescription = "Reminder", tint = Color(0xFF1E88E5), modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Hydration Reminder", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 14.sp)
                    Text(text = "Don't forget to drink water!\nStay hydrated and feel your best.", color = Color(0xFF1E88E5), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Today Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "View all >", color = SubtitleColor, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OverviewCard("Meals Logged", "$mealsLoggedCount / 5", Icons.Default.Restaurant, Modifier.weight(1f), Color(0xFFF3E5F5), PrimaryViolet)
            OverviewCard("Water", "${daily?.water_glasses ?: 0} / 8 glasses", Icons.Default.LocalDrink, Modifier.weight(1f), Color(0xFFE3F2FD), Color(0xFF1E88E5))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OverviewCard("Activity", "${daily?.workout_minutes ?: 0} min", Icons.Default.DirectionsRun, Modifier.weight(1f), Color(0xFFFFEBEE), Color(0xFFE53935))
            OverviewCard("Notes", if (daily?.notes.isNullOrEmpty()) "0" else "1", Icons.Default.NoteAlt, Modifier.weight(1f), Color(0xFFFFF8E1), Color(0xFFFFA000))
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Weekly Adherence", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "Last 7 Days \u2304", color = SubtitleColor, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PrimaryVioletLight)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Great consistency! \uD83C\uDF89", fontWeight = FontWeight.Bold, color = PrimaryViolet, fontSize = 14.sp)
                    Text("You met your goals ${weekly.count { it.adherence >= 75 }} out of 7 days.", color = PrimaryViolet.copy(alpha = 0.8f), fontSize = 12.sp)
                }
                Box(modifier = Modifier.size(50.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                    val avgAdherence = if (weekly.isNotEmpty()) weekly.map { it.adherence }.average().toInt() else 0
                    Text("$avgAdherence%", fontWeight = FontWeight.Bold, color = PrimaryViolet)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Adherence Over Time", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Beautiful custom bar chart representation for weekly adherence
        if (weekly.isEmpty()) {
            Text(text = "No weekly data available yet.", color = SubtitleColor, fontSize = 14.sp)
        } else {
            Row(modifier = Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                weekly.takeLast(7).forEach { dayData ->
                    val color = if (dayData.adherence >= 75) Color(0xFF4CAF50) else if (dayData.adherence >= 50) Color(0xFFFFB300) else Color(0xFFE53935)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
                        Text(text = "${dayData.adherence}%", fontSize = 10.sp, color = SubtitleColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier
                            .width(24.dp)
                            .height(((dayData.adherence * 1.0).coerceAtLeast(10.0)).dp)
                            .background(color, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
                        Spacer(modifier = Modifier.height(8.dp))
                        // Extracting day abbreviation like 'Mon', 'Tue'
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        val outSdf = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
                        val dayStr = try { outSdf.format(sdf.parse(dayData.date)!!) } catch (e: Exception) { "Day" }
                        Text(text = dayStr, fontSize = 10.sp, color = SubtitleColor, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showActivityDialog) {
        DailyActivityDialog(
            initialWater = trackingData?.daily?.water_glasses ?: 0,
            initialWorkout = trackingData?.daily?.workout_minutes ?: 0,
            initialNotes = trackingData?.daily?.notes ?: "",
            onDismiss = { showActivityDialog = false },
            onSubmit = { w, wo, n ->
                coroutineScope.launch {
                    try {
                        RetrofitClient.apiService.updateDailyTracking(
                            com.example.pcos_nutritionist.api.DailyTrackingInput(
                                patient_id = userId,
                                tracking_date = todayDate,
                                water_glasses = w,
                                workout_minutes = wo,
                                notes = n
                            )
                        )
                        showActivityDialog = false
                        loadData()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }
}

@Composable
fun DailyActivityDialog(
    initialWater: Int,
    initialWorkout: Int,
    initialNotes: String,
    onDismiss: () -> Unit,
    onSubmit: (Int, Int, String) -> Unit
) {
    var water by remember { mutableStateOf(initialWater) }
    var workout by remember { mutableStateOf(initialWorkout) }
    var notes by remember { mutableStateOf(initialNotes) }
    var activityType by remember { mutableStateOf("Workout") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Log Activity", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }
        },
        text = {
            Column {
                Text("Activity Type", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val types = listOf("Walking" to Icons.Default.DirectionsWalk, "Workout" to Icons.Default.FitnessCenter, "Yoga" to Icons.Default.SelfImprovement, "Other" to Icons.Default.MoreHoriz)
                    types.forEach { (name, icon) ->
                        val isSelected = activityType == name
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { activityType = name }) {
                            Box(modifier = Modifier.size(48.dp).background(if (isSelected) PrimaryVioletLight else Color(0xFFF5F5F5), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = if (isSelected) PrimaryViolet else Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(name, fontSize = 10.sp, color = if (isSelected) PrimaryViolet else Color.Gray)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Duration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$workout min", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Row {
                        IconButton(onClick = { if (workout > 0) workout -= 5 }) { Icon(Icons.Default.Remove, null) }
                        IconButton(onClick = { workout += 5 }) { Icon(Icons.Default.Add, null) }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Water Intake", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$water Glasses", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Row {
                        IconButton(onClick = { if (water > 0) water -= 1 }) { Icon(Icons.Default.Remove, null) }
                        IconButton(onClick = { water += 1 }) { Icon(Icons.Default.Add, null) }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Notes (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("How did your activity go?", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryViolet,
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(water, workout, notes) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
            ) {
                Text("Save Activity", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun OverviewCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, bgColor: Color, iconColor: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(bgColor, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 12.sp, color = SubtitleColor)
                Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyMealTrackingScreen(userId: String, onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var trackingData by remember { mutableStateOf<com.example.pcos_nutritionist.api.TrackingResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    
    // Add logic to fetch approved plan to know what foods to log
    var approvedPlan by remember { mutableStateOf<Map<String, Map<String, com.example.pcos_nutritionist.api.MealDetail>>?>(null) }
    
    fun loadData() {
        coroutineScope.launch {
            try {
                isLoading = true
                trackingData = RetrofitClient.apiService.getPatientTracking(userId, todayDate)
                val planResponse = RetrofitClient.apiService.getPatientPlan(userId)
                if (planResponse.status == "Approved") {
                    approvedPlan = planResponse.plan
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(userId) { loadData() }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9))) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Today's Progress", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "Overall Progress", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    val adherence = trackingData?.daily?.adherence_percentage ?: 0
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "${trackingData?.meals?.count { it.status == "Completed" || it.status == "Logged" } ?: 0} of 5 meals logged", fontSize = 14.sp, color = SubtitleColor)
                        Text(text = "$adherence%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (adherence >= 75) Color(0xFF4CAF50) else PrimaryViolet)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { adherence / 100f },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = Color(0xFF4CAF50), // Green
                        trackColor = Color(0xFFE0E0E0)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = "Meals", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val dayOfWeek = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(java.util.Date())
                val todayPlan = approvedPlan?.get(dayOfWeek) ?: emptyMap()

                val mealTypes = listOf("Breakfast", "Lunch", "Snack", "Dinner", "Beverage")
                items(mealTypes) { mealType ->
                    val mealLog = trackingData?.meals?.find { it.meal_type.equals(mealType, ignoreCase = true) }
                    val plannedFood = todayPlan[mealType]?.food ?: "Not planned"
                    val isLogged = mealLog?.status == "Logged" || mealLog?.status == "Completed"
                    
                    val (icon, bgColor, iconColor) = when (mealType) {
                        "Breakfast" -> Triple(Icons.Default.WbSunny, Color(0xFFFFF8E1), Color(0xFFFFA000))
                        "Lunch" -> Triple(Icons.Default.Restaurant, Color(0xFFFBE9E7), Color(0xFFFF7043))
                        "Snack" -> Triple(Icons.Default.LocalFlorist, Color(0xFFE8F5E9), Color(0xFF4CAF50))
                        "Dinner" -> Triple(Icons.Default.NightsStay, Color(0xFFEDE7F6), Color(0xFF7E57C2))
                        else -> Triple(Icons.Default.LocalDrink, Color(0xFFE3F2FD), Color(0xFF42A5F5))
                    }
                    
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).background(bgColor, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = mealType, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(text = plannedFood, fontSize = 12.sp, color = SubtitleColor, maxLines = 1)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isLogged) {
                                Row(
                                    modifier = Modifier.background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Logged", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp).clickable {
                                        coroutineScope.launch {
                                            RetrofitClient.apiService.logMeal(com.example.pcos_nutritionist.api.MealLogInput(userId, todayDate, mealType, plannedFood, "Logged"))
                                            loadData()
                                        }
                                    },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Schedule, null, tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Pending", color = Color(0xFFFF9800), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

// --- Patient Q&A Tab ---

@Composable
fun PatientQATab(userId: String, patientName: String) {
    val coroutineScope = rememberCoroutineScope()
    var questions by remember { mutableStateOf<List<com.example.pcos_nutritionist.api.QuestionData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var questionText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, Answered, Pending

    fun fetchQuestions() {
        coroutineScope.launch {
            try {
                isLoading = true
                questions = RetrofitClient.apiService.getPatientQuestions(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(userId) { fetchQuestions() }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Text(text = "Ask a Question", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PrimaryVioletLight)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Have a question?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryViolet)
                    Text(text = "Our nutritionist is here to help you", fontSize = 12.sp, color = PrimaryViolet)
                }
                Icon(Icons.Default.SupportAgent, null, tint = PrimaryViolet, modifier = Modifier.size(48.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = questionText,
            onValueChange = { if (it.length <= 500) questionText = it },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            placeholder = { Text("Type your question here...") },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
        Text(text = "${questionText.length}/500", fontSize = 10.sp, color = SubtitleColor, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.End)
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (questionText.isNotBlank()) {
                    coroutineScope.launch {
                        RetrofitClient.apiService.askQuestion(com.example.pcos_nutritionist.api.QuestionInput(userId, patientName, questionText))
                        questionText = ""
                        fetchQuestions()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Send Question", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "My Questions", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Filters
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("All", "Answered", "Pending").forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) PrimaryViolet else Color(0xFFF0F0F0))
                    .clickable { selectedFilter = filter }
                    .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = filter, fontSize = 12.sp, color = if (isSelected) Color.White else SubtitleColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = PrimaryViolet)
        } else {
            val filteredQuestions = questions.filter { 
                when (selectedFilter) {
                    "Answered" -> it.status == "Answered"
                    "Pending" -> it.status == "Pending"
                    else -> true
                }
            }
            
            if (filteredQuestions.isEmpty()) {
                Text(text = "No questions found.", color = SubtitleColor, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp))
            } else {
                filteredQuestions.forEach { q ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Q: ${q.question_text}", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text(text = q.status, color = if (q.status == "Answered") ApprovedColor else PendingColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "Asked on ${q.asked_at?.take(10)}", fontSize = 10.sp, color = SubtitleColor)
                            
                            if (q.status == "Answered" && q.answer_text != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = Color(0xFFE0E0E0))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = "Answer:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryViolet)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = q.answer_text, fontSize = 12.sp, color = TextColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatientChangePasswordDialog(onDismiss: () -> Unit) {
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var isChanging by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel()
    val context = LocalContext.current
    var currentPassVisible by remember { mutableStateOf(false) }
    var newPassVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password", fontWeight = FontWeight.Bold, color = PrimaryViolet) },
        text = {
            Column {
                ModernTextField(
                    value = currentPass, 
                    onValueChange = { currentPass = it }, 
                    label = "Current Password", 
                    leadingIcon = Icons.Default.Lock,
                    visualTransformation = if (currentPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { currentPassVisible = !currentPassVisible }) {
                            Icon(if (currentPassVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ModernTextField(
                    value = newPass, 
                    onValueChange = { newPass = it }, 
                    label = "New Password", 
                    leadingIcon = Icons.Default.Lock,
                    visualTransformation = if (newPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newPassVisible = !newPassVisible }) {
                            Icon(if (newPassVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isChanging = true
                    authViewModel.changePassword(currentPass, newPass,
                        onSuccess = {
                            isChanging = false
                            Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        onError = { err ->
                            isChanging = false
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
            ) {
                if (isChanging) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Change")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = PrimaryViolet) }
        },
        containerColor = Color.White
    )
}

@Composable
fun PmosGuidanceScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = PrimaryViolet) }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "PMOS Guidance", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryViolet)
        }
        
        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            item {
                Text("🌸 What is PMOS?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryViolet)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Polyendocrine Metabolic Ovarian Syndrome (PMOS) is a complex endocrine and metabolic disorder that affects women of reproductive age. It occurs due to hormonal imbalance involving insulin, reproductive hormones, and ovarian function, which can lead to irregular menstrual cycles, weight gain, acne, excess hair growth, and fertility-related complications.",
                    fontSize = 14.sp, color = TextColor, lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // 1. Insulin Resistance PMOS
            item {
                PmosTypeCard(
                    title = "1. Insulin Resistance PMOS",
                    description = "This is the most common type of PMOS.\nIn this type, the body’s cells do not respond properly to insulin.\n\nInsulin is the hormone that controls blood sugar. When the body becomes resistant to insulin, the pancreas produces more insulin. High insulin levels can increase androgen (male hormone) production, which affects ovulation.",
                    causes = listOf("High sugar diet", "Processed foods", "Lack of exercise", "Obesity or high BMI", "Genetics"),
                    symptoms = listOf("Weight gain (especially belly fat)", "Difficulty losing weight", "Cravings for sugar", "Fatigue after meals", "Dark patches on skin (acanthosis nigricans)", "Irregular periods", "Acne and hair growth"),
                    indicators = listOf("High insulin levels", "High blood sugar or high RBS", "BMI often > 25", "Increased LH/FSH ratio in some patients")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Inflammation PMOS
            item {
                PmosTypeCard(
                    title = "2. Inflammation PMOS",
                    description = "This type is linked with chronic low-grade inflammation in the body.\n\nInflammation can stimulate the ovaries to produce more androgens, causing PMOS symptoms.",
                    causes = listOf("Poor diet", "Stress", "Smoking", "Gut health issues", "Environmental toxins"),
                    symptoms = listOf("Fatigue", "Headaches", "Skin problems/acne", "Joint pain", "Irregular periods", "Mood issues", "Unexplained weight gain"),
                    indicators = listOf("Elevated inflammatory markers", "Acne and skin irritation", "High androgen symptoms")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 3. Hypothyroidism-related PMOS
            item {
                PmosTypeCard(
                    title = "3. Hypothyroidism-related PMOS",
                    description = "This type is associated with an underactive thyroid gland.\n\nThe thyroid controls metabolism. When thyroid hormone levels are low, metabolism slows down and hormonal imbalance can worsen PMOS symptoms.",
                    causes = listOf("Thyroid hormone deficiency", "Autoimmune thyroid disease", "Genetics"),
                    symptoms = listOf("Weight gain", "Slow metabolism", "Fatigue", "Depression", "Hair thinning", "Dry skin", "Irregular or heavy periods"),
                    indicators = listOf("High TSH levels", "Low thyroid hormone levels", "Persistent tiredness")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. Adrenal Fatigue / Stress-related PMOS
            item {
                PmosTypeCard(
                    title = "4. Adrenal Fatigue / Stress-related PMOS",
                    description = "This type is linked with chronic stress and high cortisol levels.\n\nThe adrenal glands produce stress hormones. Long-term stress may disturb reproductive hormones and worsen PMOS symptoms.",
                    causes = listOf("Chronic stress", "Poor sleep", "Anxiety", "Overworking", "Excessive exercise"),
                    symptoms = listOf("Anxiety", "Sleep problems", "Fatigue", "Irregular periods", "Hair loss", "Cravings", "Mood swings"),
                    indicators = listOf("Elevated stress hormones", "Normal insulin levels in some cases", "Symptoms worsen during stress")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 5. Pill-Induced PCOS
            item {
                PmosTypeCard(
                    title = "5. Pill-Induced PCOS",
                    description = "This type may appear after stopping hormonal birth control pills.\n\nBirth control pills suppress ovulation. After stopping them, some women temporarily experience PCOS-like symptoms.",
                    causes = listOf("Discontinuation of oral contraceptive pills", "Hormonal rebound effects"),
                    symptoms = listOf("Missing periods", "Acne", "Hair loss", "Mood changes", "Temporary irregular cycles"),
                    indicators = listOf("Symptoms begin after stopping pills", "Previously regular cycles before pill use", "Hormonal imbalance after discontinuation")
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Table part
            item {
                Text("🩺 Required Medical Reports & Hormonal Tests", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryViolet)
                Spacer(modifier = Modifier.height(16.dp))
                
                MedicalReportRow("Medical Report / Test", "Purpose", isHeader = true)
                MedicalReportRow("LH (Luteinizing Hormone) Blood Test", "Detect hormonal imbalance and ovulation irregularities")
                MedicalReportRow("FSH (Follicle Stimulating Hormone) Blood Test", "Used to calculate LH/FSH ratio for PCOS assessment")
                MedicalReportRow("TSH (Thyroid Stimulating Hormone) Blood Test", "Detect thyroid-related hormonal disorders")
                MedicalReportRow("Random Blood Sugar (RBS) Test / Blood Glucose Test / HbA1c Test", "Check blood sugar levels and insulin resistance")
                MedicalReportRow("PRL (Prolactin) Blood Test", "Assess prolactin hormone imbalance")
                MedicalReportRow("Progesterone (PRG) Blood Test", "Evaluate ovulation and menstrual cycle health")
                MedicalReportRow("AMH (Anti-Müllerian Hormone) Test (Optional)", "Supportive hormonal marker for PCOS detection")
                MedicalReportRow("BMI (Body Mass Index)", "Evaluate obesity and weight-related risk factors")
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun PmosTypeCard(title: String, description: String, causes: List<String>, symptoms: List<String>, indicators: List<String>) {
    CustomCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryViolet)
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, fontSize = 14.sp, color = TextColor, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Main Causes:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            causes.forEach { Text("• $it", fontSize = 13.sp, color = SubtitleColor) }
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("Common Symptoms:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            symptoms.forEach { Text("• $it", fontSize = 13.sp, color = SubtitleColor) }
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("Indicators:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            indicators.forEach { Text("• $it", fontSize = 13.sp, color = SubtitleColor) }
        }
    }
}

@Composable
fun MedicalReportRow(test: String, purpose: String, isHeader: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHeader) PrimaryViolet else Color.White)
            .border(1.dp, Color(0xFFEEEEEE))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = test,
            modifier = Modifier.weight(1f),
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Medium,
            color = if (isHeader) Color.White else TextColor,
            fontSize = if (isHeader) 14.sp else 13.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = purpose,
            modifier = Modifier.weight(1.2f),
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            color = if (isHeader) Color.White else SubtitleColor,
            fontSize = if (isHeader) 14.sp else 13.sp
        )
    }
}
