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


