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

@Composable
fun OverviewStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
        Text(text = value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PatientListItem(p: com.example.pcos_nutritionist.api.PendingPatient, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).background(if (p.status == "Approved") Color(0xFFE8F5E9) else Color(0xFFFFF3E0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = p.patient_name.take(1),
                    color = if (p.status == "Approved") Color(0xFF2E7D32) else Color(0xFFE65100),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = p.patient_name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (p.status == "Approved") Icons.Default.CheckCircle else Icons.Default.Schedule,
                        null,
                        tint = if (p.status == "Approved") Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Status: ${p.status}",
                        fontSize = 12.sp,
                        color = if (p.status == "Approved") Color(0xFF4CAF50) else Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "ID: ${p.patient_id.take(8)}...", fontSize = 11.sp, color = SubtitleColor)
            }
            Box(
                modifier = Modifier
                    .background(PrimaryViolet, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = if (p.status == "Approved") "View" else "Review", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ReviewScreenContent(
    patient: com.example.pcos_nutritionist.api.PendingPatient,
    inputs: com.example.pcos_nutritionist.api.PatientInput?,
    detailsForm: com.example.pcos_nutritionist.api.PatientDetailsFormInput?,
    plan: Map<String, Map<String, com.example.pcos_nutritionist.api.MealDetail>>,
    onPlanUpdate: (Map<String, Map<String, com.example.pcos_nutritionist.api.MealDetail>>) -> Unit,
    activities: String,
    onActivitiesUpdate: (String) -> Unit,
    sleep: String,
    onSleepUpdate: (String) -> Unit,
    water: String,
    onWaterUpdate: (String) -> Unit,
    notes: String,
    onNotesUpdate: (String) -> Unit,
    isApproving: Boolean,
    onApprove: () -> Unit,
    isRegenerating: Boolean,
    onRegenerate: (com.example.pcos_nutritionist.api.PatientInput) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Profile, 1: Diet Plan, 2: Explanation
    var editedInputs by remember(inputs) { mutableStateOf(inputs) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Patient Header
        Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).background(PrimaryVioletLight, CircleShape), contentAlignment = Alignment.Center) {
                    Text(text = patient.patient_name.take(1), color = PrimaryViolet, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = patient.patient_name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Age: ${inputs?.age ?: "--"} | BMI: ${inputs?.let { String.format("%.1f", it.weight_kg / ((it.height_cm/100)*(it.height_cm/100))) } ?: "--"}", fontSize = 12.sp, color = SubtitleColor)
                }
            }
        }
        
        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = PrimaryViolet,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = PrimaryViolet)
            }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Profile") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Diet Plan") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Explanation") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Details Form") })
        }
        
        Box(modifier = Modifier.weight(1f).padding(16.dp)) {
            when (selectedTab) {
                0 -> PatientProfileTab(
                    inputs = editedInputs,
                    onUpdate = { editedInputs = it },
                    isRegenerating = isRegenerating,
                    onRegenerate = { if (editedInputs != null) onRegenerate(editedInputs!!) }
                )
                1 -> EditableDietPlanTab(plan, onPlanUpdate, activities, onActivitiesUpdate, sleep, onSleepUpdate, water, onWaterUpdate, notes, onNotesUpdate)
                2 -> DietExplanationTab(inputs?.explanation)
                3 -> PatientDetailsFormTab(detailsForm)
            }
        }
        
        // Action Bar
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onApprove() }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                Text("Save Changes")
            }
            GradientButton(text = "Approve Plan", onClick = onApprove, modifier = Modifier.weight(1f).height(48.dp), loading = isApproving)
        }
    }
}

@Composable
fun PatientProfileTab(
    inputs: com.example.pcos_nutritionist.api.PatientInput?,
    onUpdate: (com.example.pcos_nutritionist.api.PatientInput) -> Unit,
    isRegenerating: Boolean,
    onRegenerate: () -> Unit
) {
    if (inputs == null) {
        Text("No profile data available.")
        return
    }

    var fsh by remember(inputs) { mutableStateOf(inputs.fsh.toString()) }
    var lh by remember(inputs) { mutableStateOf(inputs.lh.toString()) }
    var tsh by remember(inputs) { mutableStateOf(inputs.tsh.toString()) }
    var prl by remember(inputs) { mutableStateOf(inputs.prl.toString()) }
    var prg by remember(inputs) { mutableStateOf(inputs.prg.toString()) }
    var rbs by remember(inputs) { mutableStateOf(inputs.rbs.toString()) }
    var amh by remember(inputs) { mutableStateOf(inputs.amh.toString()) }

    LaunchedEffect(fsh, lh, tsh, prl, prg, rbs, amh) {
        val updated = inputs.copy(
            fsh = fsh.toDoubleOrNull() ?: inputs.fsh,
            lh = lh.toDoubleOrNull() ?: inputs.lh,
            tsh = tsh.toDoubleOrNull() ?: inputs.tsh,
            prl = prl.toDoubleOrNull() ?: inputs.prl,
            prg = prg.toDoubleOrNull() ?: inputs.prg,
            rbs = rbs.toDoubleOrNull() ?: inputs.rbs,
            amh = amh.toDoubleOrNull() ?: inputs.amh
        )
        if (updated != inputs) {
            onUpdate(updated)
        }
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Hormonal Levels (Editable)", fontWeight = FontWeight.Bold, color = PrimaryViolet)
        Spacer(modifier = Modifier.height(12.dp))
        
        ModernTextField(value = fsh, onValueChange = { fsh = it }, label = "FSH (mIU/mL)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(8.dp))
        ModernTextField(value = lh, onValueChange = { lh = it }, label = "LH (mIU/mL)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(8.dp))
        ModernTextField(value = tsh, onValueChange = { tsh = it }, label = "TSH (mIU/L)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(8.dp))
        ModernTextField(value = prl, onValueChange = { prl = it }, label = "Prolactin (PRL) (ng/mL)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(8.dp))
        ModernTextField(value = prg, onValueChange = { prg = it }, label = "Progesterone (PRG) (ng/mL)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(8.dp))
        ModernTextField(value = rbs, onValueChange = { rbs = it }, label = "RBS (mg/dL)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(8.dp))
        ModernTextField(value = amh, onValueChange = { amh = it }, label = "AMH (ng/mL)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Medical Conditions", fontWeight = FontWeight.Bold, color = PrimaryViolet)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow {
            inputs.selected_conditions.forEach { ChipItem(it) }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("PMOS Type", fontWeight = FontWeight.Bold, color = PrimaryViolet)
        Spacer(modifier = Modifier.height(8.dp))
        val displayPcosType = if (inputs.pcos_type == "Adrenal") "Adrenal Fatigue" else inputs.pcos_type
        Text(text = displayPcosType, fontSize = 14.sp, color = TextColor)

        Spacer(modifier = Modifier.height(24.dp))
        Text("Symptoms", fontWeight = FontWeight.Bold, color = PrimaryViolet)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow {
            if (inputs.weight_gain == 1) ChipItem("Weight Gain")
            if (inputs.hair_growth == 1) ChipItem("Excess Hair")
            if (inputs.skin_darkening == 1) ChipItem("Skin Darkening")
            if (inputs.hair_loss == 1) ChipItem("Hair Loss")
            if (inputs.pimples == 1) ChipItem("Acne")
            if (inputs.irregular_periods == 1) ChipItem("Irregular Periods")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Allergies", fontWeight = FontWeight.Bold, color = PrimaryViolet)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow {
            inputs.allergies.forEach { ChipItem(it) }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Medical Report", fontWeight = FontWeight.Bold, color = PrimaryViolet)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Image, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Dummy Medical Report View", color = Color.Gray, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        GradientButton(
            text = "Update & Regenerate Plan",
            onClick = onRegenerate,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            loading = isRegenerating
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun HistoricalPatientProfileTab(inputs: com.example.pcos_nutritionist.api.PatientInput?) {
    if (inputs == null) {
        Text("No profile data available.")
        return
    }
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Hormonal Levels", fontWeight = FontWeight.Bold, color = PrimaryViolet)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HormoneItem("FSH", inputs.fsh.toString(), "mIU/mL", Modifier.weight(1f))
            HormoneItem("LH", inputs.lh.toString(), "mIU/mL", Modifier.weight(1f))
            HormoneItem("TSH", inputs.tsh.toString(), "mIU/L", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HormoneItem("PRL", inputs.prl.toString(), "ng/mL", Modifier.weight(1f))
            HormoneItem("PRG", inputs.prg.toString(), "ng/mL", Modifier.weight(1f))
            HormoneItem("RBS", inputs.rbs.toString(), "mg/dL", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HormoneItem("AMH", inputs.amh.toString(), "ng/mL", Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(2f)) // Padding so AMH doesn't stretch too much
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Medical Conditions", fontWeight = FontWeight.Bold, color = PrimaryViolet)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow {
            inputs.selected_conditions.forEach { ChipItem(it) }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("PMOS Type", fontWeight = FontWeight.Bold, color = PrimaryViolet)
        Spacer(modifier = Modifier.height(8.dp))
        val displayPcosType = if (inputs.pcos_type == "Adrenal") "Adrenal Fatigue" else inputs.pcos_type
        Text(text = displayPcosType, fontSize = 14.sp, color = TextColor)

        Spacer(modifier = Modifier.height(24.dp))
        Text("Symptoms", fontWeight = FontWeight.Bold, color = PrimaryViolet)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow {
            if (inputs.weight_gain == 1) ChipItem("Weight Gain")
            if (inputs.hair_growth == 1) ChipItem("Excess Hair")
            if (inputs.skin_darkening == 1) ChipItem("Skin Darkening")
            if (inputs.hair_loss == 1) ChipItem("Hair Loss")
            if (inputs.pimples == 1) ChipItem("Acne")
            if (inputs.irregular_periods == 1) ChipItem("Irregular Periods")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Allergies", fontWeight = FontWeight.Bold, color = PrimaryViolet)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow {
            inputs.allergies.forEach { ChipItem(it) }
        }
    }
}

@Composable
fun EditableDietPlanTab(
    plan: Map<String, Map<String, com.example.pcos_nutritionist.api.MealDetail>>,
    onPlanUpdate: (Map<String, Map<String, com.example.pcos_nutritionist.api.MealDetail>>) -> Unit,
    activities: String,
    onActivitiesUpdate: (String) -> Unit,
    sleep: String,
    onSleepUpdate: (String) -> Unit,
    water: String,
    onWaterUpdate: (String) -> Unit,
    notes: String,
    onNotesUpdate: (String) -> Unit
) {
    var selectedDay by remember { mutableStateOf("Monday") }
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    Column {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            days.forEach { day ->
                val isSelected = selectedDay == day
                Surface(
                    modifier = Modifier.padding(end = 8.dp).clickable { selectedDay = day },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) PrimaryViolet else Color.White,
                    border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text(text = day.take(3), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = if (isSelected) Color.White else SubtitleColor)
                }
            }
        }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            val meals = plan[selectedDay] ?: emptyMap()
            items(meals.toList()) { (mealType, detail) ->
                CustomCard(modifier = Modifier.padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(text = mealType, fontWeight = FontWeight.Bold, color = PrimaryViolet, modifier = Modifier.weight(1f))
                            Text(text = "Edit", fontSize = 12.sp, color = PrimaryViolet, modifier = Modifier.clickable { /* edit */ })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ModernTextField(
                            value = detail.food,
                            onValueChange = { nv ->
                                val up = plan.toMutableMap()
                                val um = up[selectedDay]?.toMutableMap() ?: mutableMapOf()
                                um[mealType] = detail.copy(food = nv)
                                up[selectedDay] = um
                                onPlanUpdate(up)
                            },
                            label = "Food Item"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ModernTextField(
                            value = detail.portion,
                            onValueChange = { nv ->
                                val up = plan.toMutableMap()
                                val um = up[selectedDay]?.toMutableMap() ?: mutableMapOf()
                                um[mealType] = detail.copy(portion = nv)
                                up[selectedDay] = um
                                onPlanUpdate(up)
                            },
                            label = "Portion"
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Manual Additions", fontWeight = FontWeight.Bold, color = PrimaryViolet)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Physical Activities", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PrimaryViolet)
                Spacer(modifier = Modifier.height(8.dp))
                val exercisePreferences = remember { mutableStateMapOf(
                    "Walking" to activities.contains("Walking"), "Running/Jogging" to activities.contains("Running/Jogging"), "Yoga" to activities.contains("Yoga"),
                    "Strength Training" to activities.contains("Strength Training"), "Swimming" to activities.contains("Swimming"), "Cycling" to activities.contains("Cycling"),
                    "Pilates" to activities.contains("Pilates"), "Home Workouts" to activities.contains("Home Workouts")
                ) }
                LaunchedEffect(exercisePreferences.toMap()) {
                    val selected = exercisePreferences.filter { it.value }.keys.joinToString(", ")
                    onActivitiesUpdate(selected)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    exercisePreferences.keys.forEach { activity ->
                        val isSelected = exercisePreferences[activity] == true
                        Surface(
                            modifier = Modifier.padding(bottom = 8.dp).clickable { exercisePreferences[activity] = !isSelected },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) PrimaryViolet else Color(0xFFF0F0F0)
                        ) {
                            Text(text = activity, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, color = if (isSelected) Color.White else SubtitleColor)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                ModernTextField(value = sleep, onValueChange = onSleepUpdate, label = "Recommended Sleep (Hours)", leadingIcon = Icons.Default.Nightlight, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(modifier = Modifier.height(12.dp))
                ModernTextField(value = water, onValueChange = onWaterUpdate, label = "Water Intake Goal (Glasses)", leadingIcon = Icons.Default.LocalDrink, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(modifier = Modifier.height(12.dp))
                ModernTextField(value = notes, onValueChange = onNotesUpdate, label = "Nutritionist Instructions", leadingIcon = Icons.Default.Note)
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun DietExplanationTab(explanation: List<String>?) {
    if (explanation == null) {
        Text("No explanation generated.")
        return
    }
    LazyColumn {
        items(explanation) { expl ->
            Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.CheckCircle, null, tint = ApprovedColor, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = expl, fontSize = 14.sp, color = TextColor)
            }
        }
    }
}

@Composable
fun PatientDetailsFormTab(details: com.example.pcos_nutritionist.api.PatientDetailsFormInput?) {
    if (details == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Dietary Habits", fontWeight = FontWeight.Bold, color = PrimaryViolet, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        DetailItem("Breakfast", details.breakfast)
        DetailItem("Lunch", details.lunch)
        DetailItem("Dinner", details.dinner)
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Lifestyle & Health", fontWeight = FontWeight.Bold, color = PrimaryViolet, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        DetailItem("Irregular Periods", details.irregular_periods)
        DetailItem("Period Cycle Length", "${details.period_cycle_length} days")
        DetailItem("Fast Food Frequency", details.fast_food_freq)
        DetailItem("Fruit/Veg Frequency", details.veg_fruit_freq)
        DetailItem("Work Hours", "${details.work_hours} hours/day")
        DetailItem("Stress Level (1-10)", details.stress_level.toString())
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(text = label, fontSize = 12.sp, color = SubtitleColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value.ifBlank { "Not provided" }, fontSize = 16.sp, color = TextColor)
        Divider(modifier = Modifier.padding(top = 8.dp), color = Color(0xFFF0F0F0))
    }
}

// --- Nutritionist Dashboard Tracking Overview ---

@Composable
fun NutritionistDashboardTab(
    allPatients: List<com.example.pcos_nutritionist.api.PendingPatient>,
    trackingOverview: List<com.example.pcos_nutritionist.api.NutritionistTrackingOverview>,
    isLoading: Boolean,
    onViewPatientTracking: (String) -> Unit
) {
    val totalPatients = trackingOverview.size
    val goodAdherenceCount = trackingOverview.count { it.adherence >= 75 }
    val needAttentionCount = trackingOverview.count { it.adherence < 75 }
    val avgAdherence = if (totalPatients > 0) trackingOverview.sumOf { it.adherence } / totalPatients else 0

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9)).padding(20.dp).verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Here's what's happening today", color = SubtitleColor, fontSize = 14.sp)
            }
            Icon(Icons.Default.NotificationsNone, null, tint = PrimaryViolet)
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        // Modern Grid for Metrics
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.weight(1f).aspectRatio(1f), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = PrimaryViolet)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Icon(Icons.Default.People, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
                    Column {
                        Text("$totalPatients", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("Active Patients", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
            Column(modifier = Modifier.weight(1f).aspectRatio(1f), verticalArrangement = Arrangement.SpaceBetween) {
                Card(modifier = Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("$avgAdherence%", color = Color(0xFF2E7D32), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Avg Adherence", color = Color(0xFF4CAF50), fontSize = 10.sp)
                        }
                        Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF4CAF50))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("$needAttentionCount", color = Color(0xFFE65100), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Needs Attention", color = Color(0xFFFF9800), fontSize = 10.sp)
                        }
                        Icon(Icons.Default.WarningAmber, null, tint = Color(0xFFFF9800))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Patients Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = PrimaryViolet)
        } else if (trackingOverview.isEmpty()) {
            Text("No tracking data available.", color = SubtitleColor, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp))
        } else {
            trackingOverview.forEach { t ->
                val statusColor = if (t.adherence >= 75) Color(0xFF4CAF50) else Color(0xFFFF9800)
                val statusText = if (t.adherence >= 75) "Good" else "Needs Attention"
                val bgColor = if (t.adherence >= 75) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onViewPatientTracking(t.patient_id) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(50.dp).background(PrimaryVioletLight, CircleShape), contentAlignment = Alignment.Center) {
                            Text(text = t.patient_name.take(1), color = PrimaryViolet, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = t.patient_name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LinearProgressIndicator(
                                    progress = { t.adherence / 100f },
                                    modifier = Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = statusColor,
                                    trackColor = Color(0xFFEEEEEE)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "${t.adherence}%", fontSize = 12.sp, color = SubtitleColor, fontWeight = FontWeight.Medium)
                            }
                        }
                        Box(modifier = Modifier.background(bgColor, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(text = statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun NutritionistDetailedTrackingTab(patientId: String) {
    var trackingData by remember { mutableStateOf<com.example.pcos_nutritionist.api.TrackingResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    var approvedPlan by remember { mutableStateOf<Map<String, Map<String, com.example.pcos_nutritionist.api.MealDetail>>?>(null) }

    LaunchedEffect(patientId) {
        try {
            isLoading = true
            trackingData = RetrofitClient.apiService.getPatientTracking(patientId, todayDate)
            val planResponse = RetrofitClient.apiService.getPatientPlan(patientId)
            if (planResponse.status == "Approved") {
                approvedPlan = planResponse.plan
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryViolet) }
        return
    }

    val adherence = trackingData?.daily?.adherence_percentage ?: 0
    val weekly = trackingData?.weekly ?: emptyList()
    
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F9)).padding(20.dp).verticalScroll(rememberScrollState())) {
        Text("Daily Adherence", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                    CircularProgressIndicator(
                        progress = { adherence / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = if (adherence >= 75) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        trackColor = Color(0xFFE0E0E0),
                        strokeWidth = 6.dp
                    )
                    Text(text = "$adherence%", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = if (adherence >= 75) "Good Job!" else "Needs Improvement", color = if (adherence >= 75) Color(0xFF4CAF50) else Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                    Text(text = "Today's Adherence Rate", fontSize = 12.sp, color = SubtitleColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Adherence Trend", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "Last 7 Days", color = SubtitleColor, fontSize = 12.sp)
        }
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
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        val outSdf = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
                        val dayStr = try { outSdf.format(sdf.parse(dayData.date)!!) } catch (e: Exception) { "Day" }
                        Text(text = dayStr, fontSize = 10.sp, color = SubtitleColor, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Meal Logs (Today)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        val dayOfWeek = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(java.util.Date())
        val todayPlan = approvedPlan?.get(dayOfWeek) ?: emptyMap()

        val mealTypes = listOf("Breakfast", "Lunch", "Snack", "Dinner", "Beverage")
        mealTypes.forEach { mealType ->
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

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
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
                            modifier = Modifier.background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
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
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// --- Nutritionist Q&A ---

@Composable
fun NutritionistQATab() {
    var questions by remember { mutableStateOf<List<com.example.pcos_nutritionist.api.QuestionData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All Questions") }
    var answeringQuestion by remember { mutableStateOf<com.example.pcos_nutritionist.api.QuestionData?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun fetchQuestions() {
        coroutineScope.launch {
            try {
                isLoading = true
                questions = RetrofitClient.apiService.getNutritionistQA()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchQuestions() }

    if (answeringQuestion != null) {
        AnswerQuestionScreen(
            question = answeringQuestion!!,
            onBack = { answeringQuestion = null },
            onSubmitAnswer = { answerText ->
                coroutineScope.launch {
                    try {
                        RetrofitClient.apiService.answerQuestion(com.example.pcos_nutritionist.api.AnswerInput(answeringQuestion!!.id, answerText))
                        answeringQuestion = null
                        fetchQuestions()
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Text("Q&A Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("All Questions", "Pending", "Answered").forEach { filter ->
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
        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = PrimaryViolet)
        } else {
            val filteredQuestions = questions.filter {
                when (selectedFilter) {
                    "Pending" -> it.status == "Pending"
                    "Answered" -> it.status == "Answered"
                    else -> true
                }
            }

            if (filteredQuestions.isEmpty()) {
                Text("No questions found.", color = SubtitleColor, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp))
            } else {
                filteredQuestions.forEach { q ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { if (q.status == "Pending") answeringQuestion = q }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(PrimaryVioletLight, CircleShape), contentAlignment = Alignment.Center) {
                                Text(text = (q.patient_name ?: "U").take(1), color = PrimaryViolet, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = q.patient_name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = q.question_text, fontSize = 12.sp, color = TextColor, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text(text = "Asked on ${q.asked_at?.take(10)}", fontSize = 10.sp, color = SubtitleColor)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = q.status, color = if (q.status == "Answered") ApprovedColor else PendingColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerQuestionScreen(question: com.example.pcos_nutritionist.api.QuestionData, onBack: () -> Unit, onSubmitAnswer: (String) -> Unit) {
    var answerText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundColor)) {
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Answer Question", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = PrimaryVioletLight)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                            Text(text = (question.patient_name ?: "U").take(1), color = PrimaryViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = question.patient_name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryViolet)
                            Text(text = "Asked on ${question.asked_at?.take(10)}", fontSize = 10.sp, color = PrimaryViolet)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = question.question_text, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = PrimaryViolet)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Your Answer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = answerText,
                onValueChange = { answerText = it },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                placeholder = { Text("Type your answer here...") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryViolet,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { if (answerText.isNotBlank()) onSubmitAnswer(answerText) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Send Answer", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NutritionistPlansTab(plans: List<com.example.pcos_nutritionist.api.HistoricalPlanResponse>) {
    var selectedPlan by remember { mutableStateOf<com.example.pcos_nutritionist.api.HistoricalPlanResponse?>(null) }
    var selectedPatientName by remember { mutableStateOf<String?>(null) }

    val groupedPlans = remember(plans) { plans.groupBy { it.patient_name ?: "Unknown" } }

    if (selectedPlan != null) {
        HistoricalDetailedView(plan = selectedPlan!!) { selectedPlan = null }
    } else if (selectedPatientName != null) {
        val patientPlans = groupedPlans[selectedPatientName] ?: emptyList()
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedPatientName = null }) { Icon(Icons.Default.ArrowBack, null, tint = PrimaryViolet) }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "Diet Plan History", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = "Patient: $selectedPatientName", fontSize = 12.sp, color = SubtitleColor)
                }
            }
            LazyColumn(modifier = Modifier.padding(20.dp)) {
                val sortedPlans = patientPlans.sortedByDescending { it.created_at }
                itemsIndexed(sortedPlans) { index, plan ->
                    val planNumber = sortedPlans.size - index
                    HistoricalPlanCard(plan, planNumber) { selectedPlan = plan }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text("Diet Plan History Library", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryViolet)
            Spacer(modifier = Modifier.height(16.dp))
            if (plans.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No historical plans found.", color = SubtitleColor)
                }
            } else {
                LazyColumn {
                    items(groupedPlans.keys.toList()) { patientName ->
                        val latestPlan = groupedPlans[patientName]?.maxByOrNull { it.created_at }
                        CustomCard(modifier = Modifier.padding(bottom = 12.dp).clickable { selectedPatientName = patientName }) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(PrimaryVioletLight, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.History, null, tint = PrimaryViolet)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Patient: $patientName", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (latestPlan != null) {
                                        Text(text = "Last Approved: ${latestPlan.created_at.take(10)}", fontSize = 12.sp, color = SubtitleColor)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = SubtitleColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoricalPlanCard(plan: com.example.pcos_nutritionist.api.HistoricalPlanResponse, planNumber: Int, onClick: () -> Unit) {
    CustomCard(modifier = Modifier.padding(bottom = 12.dp).clickable { onClick() }) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(PrimaryVioletLight, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.History, null, tint = PrimaryViolet)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Diet plan no $planNumber", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = plan.created_at, fontSize = 12.sp, color = SubtitleColor)
            }
            Icon(Icons.Default.ChevronRight, null, tint = SubtitleColor)
        }
    }
}

@Composable
fun HistoricalDetailedView(plan: com.example.pcos_nutritionist.api.HistoricalPlanResponse, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = PrimaryViolet) }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = "Historical Plan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "${plan.patient_name} • ${plan.created_at.take(10)}", fontSize = 12.sp, color = SubtitleColor)
            }
        }
        
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = PrimaryViolet,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = PrimaryViolet)
            }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Health Profile Snapshot") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Diet Plan Snapshot") })
        }
        
        Box(modifier = Modifier.weight(1f).padding(16.dp)) {
            when (selectedTab) {
                0 -> HistoricalPatientProfileTab(plan.patient_inputs)
                1 -> HistoricalDietPlanTab(plan.plan_data, plan.notes)
            }
        }
    }
}

@Composable
fun HistoricalDietPlanTab(plan: Map<String, Map<String, com.example.pcos_nutritionist.api.MealDetail>>, notes: String) {
    var selectedDay by remember { mutableStateOf("Monday") }
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    Column {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            days.forEach { day ->
                val isSelected = selectedDay == day
                Surface(
                    modifier = Modifier.padding(end = 8.dp).clickable { selectedDay = day },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) PrimaryViolet else Color.White,
                    border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text(text = day.take(3), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = if (isSelected) Color.White else SubtitleColor)
                }
            }
        }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (notes.isNotBlank()) {
                item {
                    var acts = ""
                    var sleep = ""
                    var water = ""
                    var nNotes = ""
                    try {
                        val json = JSONObject(notes)
                        acts = json.optString("activities", "")
                        sleep = json.optString("sleep", "")
                        water = json.optString("water", "")
                        nNotes = json.optString("notes", "")
                    } catch (e: Exception) { nNotes = notes }
                    
                    CustomCard(modifier = Modifier.padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Nutritionist Recommendations", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (acts.isNotBlank()) Text(text = "🏃 Activities: $acts", fontSize = 12.sp, color = TextColor, modifier = Modifier.padding(bottom = 4.dp))
                            if (sleep.isNotBlank()) Text(text = "🌙 Sleep: $sleep hrs", fontSize = 12.sp, color = TextColor, modifier = Modifier.padding(bottom = 4.dp))
                            if (water.isNotBlank()) Text(text = "💧 Water: $water glasses", fontSize = 12.sp, color = TextColor, modifier = Modifier.padding(bottom = 4.dp))
                            if (nNotes.isNotBlank() && nNotes != "null") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "💡 Nutritionist Instructions: $nNotes", fontSize = 12.sp, color = SubtitleColor)
                            }
                        }
                    }
                }
            }

            val meals = plan[selectedDay] ?: emptyMap()
            items(meals.toList()) { (mealType, detail) ->
                CustomCard(modifier = Modifier.padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = mealType, fontWeight = FontWeight.Bold, color = PrimaryViolet)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = detail.food, fontSize = 14.sp, color = TextColor)
                        Text(text = "Portion: ${detail.portion}", fontSize = 12.sp, color = SubtitleColor)
                    }
                }
            }
        }
    }
}

@Composable
fun NutritionistProfileTab(email: String, onChangePassword: () -> Unit, onSignOut: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(40.dp))
        Box(modifier = Modifier.size(100.dp).background(PrimaryVioletLight, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Face, null, modifier = Modifier.size(60.dp), tint = PrimaryViolet)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Nutritionist", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(text = email, fontSize = 14.sp, color = SubtitleColor)
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onChangePassword() }.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(40.dp).background(Color(0xFFF1F0F5), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, null, tint = PrimaryViolet, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "Change Password", modifier = Modifier.weight(1f), fontSize = 16.sp, color = TextColor)
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }

        Row(
            modifier = Modifier.fillMaxWidth().clickable { onSignOut() }.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(40.dp).background(Color(0xFFFFEBEE), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ExitToApp, null, tint = Color.Red, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "Sign Out", modifier = Modifier.weight(1f), fontSize = 16.sp, color = Color.Red)
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@Composable
fun CreatePatientDialog(onDismiss: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Patient Account", fontWeight = FontWeight.Bold, color = PrimaryViolet) },
        text = {
            Column {
                Text("Enter the email and password for the new patient.", fontSize = 14.sp, color = SubtitleColor)
                Spacer(modifier = Modifier.height(16.dp))
                ModernTextField(value = email, onValueChange = { email = it }, label = "Email", leadingIcon = Icons.Default.Email)
                Spacer(modifier = Modifier.height(8.dp))
                ModernTextField(value = password, onValueChange = { password = it }, label = "Password", leadingIcon = Icons.Default.Lock)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isCreating = true
                    authViewModel.createPatientAccount(context, email, password,
                        onSuccess = {
                            isCreating = false
                            Toast.makeText(context, "Account created successfully", Toast.LENGTH_SHORT).show()
                            
                            val subject = Uri.encode("Your PMOS Nutritionist App Account")
                            val body = Uri.encode(
                                "Hello,\n\n" +
                                "Your account has been created by your Nutritionist.\n" +
                                "Your temporary password is: $password\n\n" +
                                "Please log in to the PMOS Nutritionist App and change your password in the Profile section.\n\n" +
                                "Thank you."
                            )
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$email?subject=$subject&body=$body")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: android.content.ActivityNotFoundException) {
                                Toast.makeText(context, "No email app found to send the email.", Toast.LENGTH_LONG).show()
                            }
                            
                            onDismiss()
                        },
                        onError = { err ->
                            isCreating = false
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Create & Send Mail")
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
fun NutritionistChangePasswordDialog(onDismiss: () -> Unit) {
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
