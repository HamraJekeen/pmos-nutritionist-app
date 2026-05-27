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


