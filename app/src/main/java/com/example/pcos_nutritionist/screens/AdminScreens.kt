package com.example.pcos_nutritionist.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pcos_nutritionist.api.AdminVerifyInput
import com.example.pcos_nutritionist.api.NutritionistRegistrationResponse
import com.example.pcos_nutritionist.api.RetrofitClient
import com.example.pcos_nutritionist.ui.GradientButton
import com.example.pcos_nutritionist.ui.theme.BackgroundColor
import com.example.pcos_nutritionist.ui.theme.PrimaryViolet
import com.example.pcos_nutritionist.ui.theme.PrimaryVioletLight
import com.example.pcos_nutritionist.ui.theme.SubtitleColor
import com.example.pcos_nutritionist.utils.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onSignOut: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()

    var pendingRegistrations by remember { mutableStateOf<List<NutritionistRegistrationResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadPending() {
        isLoading = true
        coroutineScope.launch {
            try {
                pendingRegistrations = RetrofitClient.apiService.getPendingNutritionists()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load registrations", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadPending()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        authViewModel.signout()
                        onSignOut()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(BackgroundColor)) {
            Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                Text(
                    text = "Pending Nutritionist Registrations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryViolet)
                    }
                } else if (pendingRegistrations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No pending registrations.", color = SubtitleColor)
                    }
                } else {
                    LazyColumn {
                        items(pendingRegistrations) { reg ->
                            NutritionistRegistrationItem(
                                reg = reg,
                                onVerifyComplete = { loadPending() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NutritionistRegistrationItem(
    reg: NutritionistRegistrationResponse,
    onVerifyComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()
    var isVerifying by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(50.dp).background(PrimaryVioletLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = reg.name.take(1).uppercase(),
                        color = PrimaryViolet,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = reg.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = reg.email, fontSize = 12.sp, color = SubtitleColor)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "SLMC Number: ${reg.slmc_number}", fontSize = 14.sp)
            Text(text = "NIC Number: ${reg.nic_number}", fontSize = 14.sp)
            Text(text = "Applied on: ${reg.created_at}", fontSize = 12.sp, color = SubtitleColor)

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://slmc.gov.lk/en/public/registers"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Check SLMC")
                }

                Button(
                    onClick = {
                        if (isVerifying) return@Button
                        isVerifying = true
                        
                        // Generate a simple random password
                        val generatedPassword = "PCOS" + (1000..9999).random().toString() + "!"

                        // Create Firebase Auth account first
                        authViewModel.createNutritionistAccount(
                            context = context,
                            email = reg.email,
                            pass = generatedPassword,
                            onSuccess = {
                                // Once Firebase account is created, tell backend it's verified
                                coroutineScope.launch {
                                    try {
                                        RetrofitClient.apiService.verifyNutritionist(AdminVerifyInput(reg.id))
                                        
                                        // Send email intent
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:")
                                            putExtra(Intent.EXTRA_EMAIL, arrayOf(reg.email))
                                            putExtra(Intent.EXTRA_SUBJECT, "PCOS Nutritionist Application Approved")
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Hello ${reg.name},\n\nYour application has been verified.\n" +
                                                "You can now log in to the PCOS Nutritionist App with the following credentials:\n\n" +
                                                "Email: ${reg.email}\n" +
                                                "Password: $generatedPassword\n\n" +
                                                "Please change your password immediately after logging in from your Profile tab.\n\nBest Regards,\nAdmin Team"
                                            )
                                        }
                                        if (intent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(intent)
                                        } else {
                                            Toast.makeText(context, "No email client found, please email manually.", Toast.LENGTH_LONG).show()
                                        }

                                        Toast.makeText(context, "Nutritionist Verified & Profile Created", Toast.LENGTH_SHORT).show()
                                        onVerifyComplete()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "API Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        isVerifying = false
                                    }
                                }
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, "Auth Error: $errorMsg", Toast.LENGTH_LONG).show()
                                isVerifying = false
                            }
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isVerifying
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Verify & Create")
                    }
                }
            }
        }
    }
}
