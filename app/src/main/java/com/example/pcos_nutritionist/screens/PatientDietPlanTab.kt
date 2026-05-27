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


