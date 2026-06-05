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
        Spacer(modifier = Modifier.height(32.dp))
        Text("Daily Activity", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        val water = trackingData?.daily?.water_glasses ?: 0
        val workout = trackingData?.daily?.workout_minutes ?: 0
        val notes = trackingData?.daily?.notes ?: "None"

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsRun, null, tint = Color(0xFFE53935), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Workout: $workout min", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalDrink, null, tint = Color(0xFF42A5F5), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Water: $water glasses", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.NoteAlt, null, tint = Color(0xFFFFA000), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "Notes & Activity Type:", fontSize = 12.sp, color = SubtitleColor)
                        Text(text = if (notes.isBlank()) "None" else notes, fontSize = 14.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// --- Nutritionist Q&A ---


