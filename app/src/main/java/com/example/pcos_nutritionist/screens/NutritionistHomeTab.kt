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
fun OverviewStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
        Text(text = value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}


