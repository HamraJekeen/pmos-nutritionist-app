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


