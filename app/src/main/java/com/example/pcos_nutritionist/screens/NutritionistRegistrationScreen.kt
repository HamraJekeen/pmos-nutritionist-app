package com.example.pcos_nutritionist.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Numbers
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
import com.example.pcos_nutritionist.api.NutritionistRegistrationInput
import com.example.pcos_nutritionist.api.RetrofitClient
import com.example.pcos_nutritionist.ui.GradientButton
import com.example.pcos_nutritionist.ui.ModernTextField
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionistRegistrationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var slmcNumber by remember { mutableStateOf("") }
    var nicNumber by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutritionist Registration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "Apply as a Nutritionist",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Enter your details to register. An admin will verify your SLMC number before granting access.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            ModernTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name",
                leadingIcon = Icons.Default.Person
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            ModernTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                leadingIcon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            ModernTextField(
                value = slmcNumber,
                onValueChange = { slmcNumber = it },
                label = "SLMC Registration Number",
                leadingIcon = Icons.Default.Badge
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            ModernTextField(
                value = nicNumber,
                onValueChange = { nicNumber = it },
                label = "NIC Number",
                leadingIcon = Icons.Default.Numbers
            )

            Spacer(modifier = Modifier.height(40.dp))

            GradientButton(
                text = "Submit Registration",
                onClick = {
                    if (name.isEmpty() || email.isEmpty() || slmcNumber.isEmpty() || nicNumber.isEmpty()) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@GradientButton
                    }
                    
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val input = NutritionistRegistrationInput(
                                name = name,
                                email = email,
                                slmc_number = slmcNumber,
                                nic_number = nicNumber
                            )
                            val response = RetrofitClient.apiService.registerNutritionist(input)
                            val toastMsg = response.message ?: "Registration submitted successfully."
                            Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
                            onBack()
                        } catch (e: retrofit2.HttpException) {
                            val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                            Toast.makeText(context, "Registration failed: $errorBody", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                loading = isLoading
            )
        }
    }
}
