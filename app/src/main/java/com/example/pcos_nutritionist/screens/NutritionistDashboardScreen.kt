package com.example.pcos_nutritionist.screens

import androidx.compose.ui.res.painterResource
import com.example.pcos_nutritionist.R
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.pcos_nutritionist.api.NutritionistInput
import com.example.pcos_nutritionist.api.RetrofitClient
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pcos_nutritionist.ui.CustomCard
import com.example.pcos_nutritionist.ui.GradientButton
import com.example.pcos_nutritionist.ui.ModernTextField
import com.example.pcos_nutritionist.ui.theme.*
import com.example.pcos_nutritionist.utils.AuthViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.content.Intent
import android.net.Uri


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionistDashboardScreen(onSignOut: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()

    var allPatients by remember { mutableStateOf<List<com.example.pcos_nutritionist.api.PendingPatient>>(emptyList()) }
    var trackingOverview by remember { mutableStateOf<List<com.example.pcos_nutritionist.api.NutritionistTrackingOverview>>(emptyList()) }
    var historicalPlans by remember { mutableStateOf<List<com.example.pcos_nutritionist.api.HistoricalPlanResponse>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<com.example.pcos_nutritionist.api.PendingPatient?>(null) }
    var selectedPatientTrackingId by remember { mutableStateOf<String?>(null) }
    var currentTab by remember { mutableStateOf(0) }
    
    // Patient Data State
    var patientInputs by remember { mutableStateOf<com.example.pcos_nutritionist.api.PatientInput?>(null) }
    var patientDetailsForm by remember { mutableStateOf<com.example.pcos_nutritionist.api.PatientDetailsFormInput?>(null) }
    var editablePlan by remember { mutableStateOf<Map<String, Map<String, com.example.pcos_nutritionist.api.MealDetail>>>(emptyMap()) }
    
    var activities by remember { mutableStateOf("") }
    var sleepHours by remember { mutableStateOf("8.0") }
    var waterIntake by remember { mutableStateOf("8") }
    var notes by remember { mutableStateOf("") }
    
    var isLoadingList by remember { mutableStateOf(true) }
    var isLoadingDetails by remember { mutableStateOf(false) }
    var isApproving by remember { mutableStateOf(false) }
    var isRegenerating by remember { mutableStateOf(false) }
    
    var showCreatePatient by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }

    // Fetch patients on load
    LaunchedEffect(Unit) {
        try {
            allPatients = RetrofitClient.apiService.getAllPatients()
            trackingOverview = RetrofitClient.apiService.getNutritionistTracking()
            historicalPlans = RetrofitClient.apiService.getPlanHistory()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load dashboard data", Toast.LENGTH_SHORT).show()
        } finally {
            isLoadingList = false
        }
    }

    // Fetch details when patient selected
    LaunchedEffect(selectedPatient) {
        if (selectedPatient != null) {
            isLoadingDetails = true
            try {
                val pId = selectedPatient!!.patient_id
                val planResponse = RetrofitClient.apiService.getPatientPlan(pId)
                editablePlan = planResponse.plan ?: emptyMap()
                
                if (planResponse.nutritionist_notes != null) {
                    try {
                        val json = JSONObject(planResponse.nutritionist_notes!!)
                        activities = json.optString("activities", "")
                        sleepHours = json.optString("sleep", "8.0")
                        waterIntake = json.optString("water", "8")
                        notes = json.optString("notes", "")
                    } catch (e: Exception) {
                        notes = planResponse.nutritionist_notes!!
                    }
                } else {
                    activities = ""
                    sleepHours = "8.0"
                    waterIntake = "8"
                    notes = ""
                }
                
                patientInputs = RetrofitClient.apiService.getPatientInputs(pId)
                try {
                    patientDetailsForm = RetrofitClient.apiService.getPatientDetailsForm(pId)
                } catch (e: Exception) {
                    patientDetailsForm = null
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load patient details", Toast.LENGTH_SHORT).show()
                editablePlan = emptyMap()
                patientInputs = null
                patientDetailsForm = null
            } finally {
                isLoadingDetails = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedPatient != null) "Review Plan" else if (selectedPatientTrackingId != null) "Detailed Tracking" else "Nutritionist Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (selectedPatient != null) {
                        IconButton(onClick = { selectedPatient = null }) { Icon(Icons.Default.ArrowBack, null) }
                    } else if (selectedPatientTrackingId != null) {
                        IconButton(onClick = { selectedPatientTrackingId = null }) { Icon(Icons.Default.ArrowBack, null) }
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.login_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .padding(start = 16.dp, end = 8.dp)
                                .size(32.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        authViewModel.signout()
                        onSignOut()
                    }) {
                        Icon(Icons.Default.Notifications, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (selectedPatient == null && selectedPatientTrackingId == null) {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = currentTab == 0, onClick = { currentTab = 0 }, icon = { Icon(Icons.Default.Dashboard, null) }, label = { Text("Dashboard", fontSize = 10.sp) })
                    NavigationBarItem(selected = currentTab == 1, onClick = { currentTab = 1 }, icon = { Icon(Icons.Default.People, null) }, label = { Text("Patients", fontSize = 10.sp) })
                    NavigationBarItem(selected = currentTab == 2, onClick = { currentTab = 2 }, icon = { Icon(Icons.Default.Star, null) }, label = { Text("Plans", fontSize = 10.sp) })
                    NavigationBarItem(selected = currentTab == 3, onClick = { currentTab = 3 }, icon = { Icon(Icons.Default.QuestionAnswer, null) }, label = { Text("Q&A", fontSize = 10.sp) })
                    NavigationBarItem(selected = currentTab == 4, onClick = { currentTab = 4 }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile", fontSize = 10.sp) })
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(BackgroundColor)) {
            if (selectedPatient != null) {
                // Review View
                if (isLoadingDetails) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryViolet) }
                } else {
                    ReviewScreenContent(
                        patient = selectedPatient!!,
                        inputs = patientInputs,
                        detailsForm = patientDetailsForm,
                        plan = editablePlan,
                        onPlanUpdate = { editablePlan = it },
                        activities = activities,
                        onActivitiesUpdate = { activities = it },
                        sleep = sleepHours,
                        onSleepUpdate = { sleepHours = it },
                        water = waterIntake,
                        onWaterUpdate = { waterIntake = it },
                        notes = notes,
                        onNotesUpdate = { notes = it },
                        isApproving = isApproving,
                        onApprove = {
                            isApproving = true
                            coroutineScope.launch {
                                try {
                                    val input = NutritionistInput(
                                        patient_id = selectedPatient!!.patient_id,
                                        physical_activities = activities,
                                        sleeping_hours = sleepHours.toDoubleOrNull() ?: 8.0,
                                        water_intake_goal = waterIntake.toIntOrNull() ?: 8,
                                        notes = notes,
                                        plan = editablePlan
                                    )
                                    RetrofitClient.apiService.approvePlan(input)
                                    isApproving = false
                                    Toast.makeText(context, "Plan Approved!", Toast.LENGTH_SHORT).show()
                                    allPatients = RetrofitClient.apiService.getAllPatients()
                                    historicalPlans = RetrofitClient.apiService.getPlanHistory()
                                    selectedPatient = null
                                } catch (e: Exception) {
                                    isApproving = false
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        isRegenerating = isRegenerating,
                        onRegenerate = { updatedInput ->
                            isRegenerating = true
                            coroutineScope.launch {
                                try {
                                    val response = RetrofitClient.apiService.predictDiet(updatedInput)
                                    patientInputs = updatedInput
                                    editablePlan = response.plan ?: emptyMap()
                                    Toast.makeText(context, "Plan Regenerated Successfully!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isRegenerating = false
                                }
                            }
                        }
                    )
                }
            } else if (selectedPatientTrackingId != null) {
                NutritionistDetailedTrackingTab(patientId = selectedPatientTrackingId!!)
            } else {
                when (currentTab) {
                    0 -> NutritionistDashboardTab(allPatients, trackingOverview, isLoadingList) { selectedPatientTrackingId = it }
                    1 -> Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("All Patients", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Button(onClick = { showCreatePatient = true }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)) {
                                Text("Create Patient")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (isLoadingList) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        else allPatients.forEach { p -> PatientListItem(p) { selectedPatient = p } }
                    }
                    2 -> NutritionistPlansTab(historicalPlans)
                    3 -> NutritionistQATab()
                    4 -> NutritionistProfileTab(
                        email = authViewModel.getCurrentUser()?.email ?: "nutritionisthamra@gmail.com",
                        onChangePassword = { showChangePassword = true },
                        onSignOut = { authViewModel.signout(); onSignOut() }
                    )
                }
            }
            
            if (showCreatePatient) {
                CreatePatientDialog(onDismiss = { showCreatePatient = false })
            }
            
            if (showChangePassword) {
                NutritionistChangePasswordDialog(onDismiss = { showChangePassword = false })
            }
        }
    }
}


