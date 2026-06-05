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
    
    val parsedType = if (initialNotes.contains(" - ")) initialNotes.substringBefore(" - ") else "Workout"
    val parsedNotes = if (initialNotes.contains(" - ")) initialNotes.substringAfter(" - ") else initialNotes

    var notes by remember { mutableStateOf(parsedNotes) }
    var activityType by remember { mutableStateOf(parsedType) }

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
                onClick = { 
                    val combinedNotes = if (notes.isNotBlank()) "$activityType - $notes" else activityType
                    onSubmit(water, workout, combinedNotes) 
                },
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


