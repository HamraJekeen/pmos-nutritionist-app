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
        Text("Step 1 of 7", color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        Text("Step 2 of 7", color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        Text("Step 3 of 7", color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        Text("Step 4 of 7", color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        Text("Step 5 of 7", color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        Text("Step 6 of 7", color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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


