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
        DetailItem("Regular Exercise", details.regular_exercise)
        DetailItem("Exercise Frequency", details.exercise_frequency)
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


