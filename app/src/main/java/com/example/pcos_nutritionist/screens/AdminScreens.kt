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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.pcos_nutritionist.ui.ModernTextField
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
    var verifiedRegistrations by remember { mutableStateOf<List<NutritionistRegistrationResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }

    fun loadData() {
        isLoading = true
        coroutineScope.launch {
            try {
                pendingRegistrations = RetrofitClient.apiService.getPendingNutritionists()
                verifiedRegistrations = RetrofitClient.apiService.getVerifiedNutritionists()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load registrations", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
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
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().background(BackgroundColor)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = PrimaryViolet,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryViolet
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Pending") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Verified History") })
            }

            Box(modifier = Modifier.weight(1f).padding(20.dp)) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryViolet)
                    }
                } else if (selectedTab == 0) {
                    if (pendingRegistrations.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No pending registrations.", color = SubtitleColor)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(pendingRegistrations) { reg ->
                                NutritionistRegistrationItem(
                                    reg = reg,
                                    onVerifyComplete = { loadData() }
                                )
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    if (verifiedRegistrations.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No verified nutritionists.", color = SubtitleColor)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(verifiedRegistrations) { reg ->
                                VerifiedNutritionistItem(
                                    reg = reg,
                                    onDeleteComplete = { loadData() }
                                )
                            }
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
    var showVerifyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showVerifyDialog) {
        VerifyNutritionistDialog(
            reg = reg,
            onDismiss = { showVerifyDialog = false },
            onVerifyComplete = {
                showVerifyDialog = false
                onVerifyComplete()
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Application", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = { Text("Are you sure you want to delete ${reg.name}'s application? This action cannot be undone.", color = SubtitleColor) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        coroutineScope.launch {
                            try {
                                val response = RetrofitClient.apiService.deleteNutritionist(reg.id)
                                Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                                onVerifyComplete()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = PrimaryViolet)
                }
            },
            containerColor = Color.White
        )
    }

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
                    onClick = { showVerifyDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Verify & Create")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        val subject = Uri.encode("Update on PMOS Nutritionist Application")
                        val body = Uri.encode(
                            "Hello ${reg.name},\n\n" +
                            "We have carefully reviewed your application for the PMOS Nutritionist program.\n\n" +
                            "Unfortunately, we are unable to accept your application at this time due to issues verifying your SLMC / NIC details. If you believe this is a mistake, please reply to this email with valid documentation.\n\n" +
                            "Best Regards,\nPMOS Admin Team"
                        )
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${reg.email}?subject=$subject&body=$body")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            Toast.makeText(context, "No email app found to send the email.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send Reject Mail")
                }

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun VerifyNutritionistDialog(
    reg: NutritionistRegistrationResponse,
    onDismiss: () -> Unit,
    onVerifyComplete: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verify & Create Profile", fontWeight = FontWeight.Bold, color = PrimaryViolet) },
        text = {
            Column {
                Text("Assign a password for ${reg.name}. They will be asked to change this later.", fontSize = 14.sp, color = SubtitleColor)
                Spacer(modifier = Modifier.height(16.dp))
                ModernTextField(
                    value = password, 
                    onValueChange = { password = it }, 
                    label = "Assign Password", 
                    leadingIcon = Icons.Default.Lock
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isEmpty()) {
                        Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (isVerifying) return@Button
                    isVerifying = true
                    
                    // Create Firebase Auth account first
                    authViewModel.createNutritionistAccount(
                        context = context,
                        email = reg.email,
                        pass = password,
                        onSuccess = {
                            // Once Firebase account is created, tell backend it's verified
                            coroutineScope.launch {
                                try {
                                    RetrofitClient.apiService.verifyNutritionist(AdminVerifyInput(reg.id))
                                    
                                    // Send email intent
                                    val subject = Uri.encode("PCOS Nutritionist Application Approved")
                                    val body = Uri.encode(
                                        "Hello ${reg.name},\n\n" +
                                        "Your application has been verified by the Admin (hamrajekeen@gmail.com).\n\n" +
                                        "You can now log in to the PMOS Nutritionist App using the following credentials:\n\n" +
                                        "Nutritionist Email: ${reg.email}\n" +
                                        "Temporary Password: $password\n\n" +
                                        "Please make sure to change your password immediately after logging in from your Profile tab.\n\n" +
                                        "Best Regards,\nPMOS Admin Team"
                                    )
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:${reg.email}?subject=$subject&body=$body")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: android.content.ActivityNotFoundException) {
                                        Toast.makeText(context, "No email app found to send the email.", Toast.LENGTH_LONG).show()
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Verify & Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = PrimaryViolet) }
        },
        containerColor = Color.White
    )
}

@Composable
fun VerifiedNutritionistItem(
    reg: NutritionistRegistrationResponse,
    onDeleteComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Profile", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = { Text("Are you sure you want to delete ${reg.name}'s profile? They will immediately lose access to the nutritionist dashboard.", color = SubtitleColor) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        coroutineScope.launch {
                            try {
                                val response = RetrofitClient.apiService.deleteNutritionist(reg.id)
                                Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                                onDeleteComplete()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = PrimaryViolet)
                }
            },
            containerColor = Color.White
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(text = reg.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = reg.email, fontSize = 12.sp, color = SubtitleColor)
                Text(text = "SLMC: ${reg.slmc_number}", fontSize = 12.sp, color = SubtitleColor)
            }
            
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.background(Color(0xFFFFEBEE), CircleShape)
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}
