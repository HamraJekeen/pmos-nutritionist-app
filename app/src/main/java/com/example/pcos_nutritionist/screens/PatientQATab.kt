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
fun PatientQATab(userId: String, patientName: String) {
    val coroutineScope = rememberCoroutineScope()
    var questions by remember { mutableStateOf<List<com.example.pcos_nutritionist.api.QuestionData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var questionText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, Answered, Pending

    fun fetchQuestions() {
        coroutineScope.launch {
            try {
                isLoading = true
                questions = RetrofitClient.apiService.getPatientQuestions(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(userId) { fetchQuestions() }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Text(text = "Ask a Question", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PrimaryVioletLight)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Have a question?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryViolet)
                    Text(text = "Our nutritionist is here to help you", fontSize = 12.sp, color = PrimaryViolet)
                }
                Icon(Icons.Default.SupportAgent, null, tint = PrimaryViolet, modifier = Modifier.size(48.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = questionText,
            onValueChange = { if (it.length <= 500) questionText = it },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            placeholder = { Text("Type your question here...") },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
        Text(text = "${questionText.length}/500", fontSize = 10.sp, color = SubtitleColor, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.End)
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (questionText.isNotBlank()) {
                    coroutineScope.launch {
                        RetrofitClient.apiService.askQuestion(com.example.pcos_nutritionist.api.QuestionInput(userId, patientName, questionText))
                        questionText = ""
                        fetchQuestions()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Send Question", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "My Questions", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Filters
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("All", "Answered", "Pending").forEach { filter ->
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
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = PrimaryViolet)
        } else {
            val filteredQuestions = questions.filter { 
                when (selectedFilter) {
                    "Answered" -> it.status == "Answered"
                    "Pending" -> it.status == "Pending"
                    else -> true
                }
            }
            
            if (filteredQuestions.isEmpty()) {
                Text(text = "No questions found.", color = SubtitleColor, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp))
            } else {
                filteredQuestions.forEach { q ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Q: ${q.question_text}", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text(text = q.status, color = if (q.status == "Answered") ApprovedColor else PendingColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "Asked on ${q.asked_at?.take(10)}", fontSize = 10.sp, color = SubtitleColor)
                            
                            if (q.status == "Answered" && q.answer_text != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = Color(0xFFE0E0E0))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = "Answer:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryViolet)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = q.answer_text, fontSize = 12.sp, color = TextColor)
                            }
                        }
                    }
                }
            }
        }
    }
}


