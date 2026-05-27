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
fun PatientDashboardScreen(onSignOut: () -> Unit, onNavigateToDetailsForm: () -> Unit) {
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
                        onViewHistory = { showHistory = true },
                        onNavigateToDetailsForm = onNavigateToDetailsForm
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
    onViewHistory: () -> Unit,
    onNavigateToDetailsForm: () -> Unit
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
            QuickActionItem(Icons.Default.Assignment, "Details\nForm", onNavigateToDetailsForm)
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


