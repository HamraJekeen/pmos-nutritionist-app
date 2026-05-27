package com.example.pcos_nutritionist.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pcos_nutritionist.api.PatientDetailsFormInput
import com.example.pcos_nutritionist.api.RetrofitClient
import com.example.pcos_nutritionist.ui.theme.*
import com.example.pcos_nutritionist.utils.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailsFormScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val userId = authViewModel.getCurrentUser()?.uid ?: "UNKNOWN"
    val coroutineScope = rememberCoroutineScope()

    var breakfast by remember { mutableStateOf("") }
    var lunch by remember { mutableStateOf("") }
    var dinner by remember { mutableStateOf("") }
    var irregularPeriods by remember { mutableStateOf("Rarely") }
    var periodCycleLength by remember { mutableStateOf("") }
    var fastFoodFreq by remember { mutableStateOf("Occasionally") }
    var vegFruitFreq by remember { mutableStateOf("Few times a week") }
    var workHours by remember { mutableStateOf("") }
    var stressLevel by remember { mutableStateOf(5f) }

    var isLoading by remember { mutableStateOf(false) }

    val irregularPeriodsOptions = listOf("Rarely", "Occasionally", "Often", "Always")
    val fastFoodOptions = listOf("Daily", "Weekly", "Occasionally", "Rarely")
    val vegFruitOptions = listOf("Every meal", "Once a day", "Few times a week", "Rarely")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details Form", fontWeight = FontWeight.Bold, color = PrimaryViolet) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryViolet)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(BackgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Please answer the following questions so your nutritionist can better understand your lifestyle.", color = SubtitleColor, fontSize = 14.sp)

            OutlinedTextField(
                value = breakfast,
                onValueChange = { breakfast = it },
                label = { Text("What do you normally take for breakfast?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet)
            )

            OutlinedTextField(
                value = lunch,
                onValueChange = { lunch = it },
                label = { Text("What do you normally take for lunch?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet)
            )

            OutlinedTextField(
                value = dinner,
                onValueChange = { dinner = it },
                label = { Text("What do you normally take for dinner?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet)
            )

            DropdownSelector(
                label = "How frequent is your periods irregular?",
                options = irregularPeriodsOptions,
                selectedOption = irregularPeriods,
                onSelectionChange = { irregularPeriods = it }
            )

            OutlinedTextField(
                value = periodCycleLength,
                onValueChange = { periodCycleLength = it },
                label = { Text("How long is your period cycle? (days)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet)
            )

            DropdownSelector(
                label = "How often do you have fast foods?",
                options = fastFoodOptions,
                selectedOption = fastFoodFreq,
                onSelectionChange = { fastFoodFreq = it }
            )

            DropdownSelector(
                label = "How often do you intake fruit and vegetable?",
                options = vegFruitOptions,
                selectedOption = vegFruitFreq,
                onSelectionChange = { vegFruitFreq = it }
            )

            OutlinedTextField(
                value = workHours,
                onValueChange = { workHours = it },
                label = { Text("How many hours do you work daily?") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet)
            )

            Column {
                Text(text = "Rate your stress level (1 - 10): ${stressLevel.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Slider(
                    value = stressLevel,
                    onValueChange = { stressLevel = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryViolet,
                        activeTrackColor = PrimaryViolet,
                        inactiveTrackColor = PrimaryVioletLight
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            val input = PatientDetailsFormInput(
                                patient_id = userId,
                                breakfast = breakfast,
                                lunch = lunch,
                                dinner = dinner,
                                irregular_periods = irregularPeriods,
                                period_cycle_length = periodCycleLength,
                                fast_food_freq = fastFoodFreq,
                                veg_fruit_freq = vegFruitFreq,
                                work_hours = workHours,
                                stress_level = stressLevel.toInt()
                            )
                            RetrofitClient.apiService.submitPatientDetailsForm(input)
                            Toast.makeText(context, "Details saved successfully", Toast.LENGTH_SHORT).show()
                            onBack()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error saving details", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Saving..." else "Submit Details", fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelectionChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(focusedBorderColor = PrimaryViolet),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelectionChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
