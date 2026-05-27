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
fun ProfileTab(patientName: String, email: String, onChangePassword: () -> Unit, onSignOut: () -> Unit, onPmosGuidanceClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(40.dp))
        Box(modifier = Modifier.size(100.dp).background(PrimaryVioletLight, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(60.dp), tint = PrimaryViolet)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = patientName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(text = email, fontSize = 14.sp, color = SubtitleColor)
        
        Spacer(modifier = Modifier.height(40.dp))
        
        ProfileMenuItem(Icons.Default.Lock, "Change Password", onChangePassword)
        ProfileMenuItem(Icons.Default.Help, "PMOS Guidance", onPmosGuidanceClick)
        ProfileMenuItem(Icons.Default.ExitToApp, "Sign Out", onSignOut, isError = true)
    }
}


@Composable
fun ProfileMenuItem(icon: ImageVector, label: String, onClick: () -> Unit, isError: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(if (isError) Color(0xFFFFEBEE) else Color(0xFFF1F0F5), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (isError) Color.Red else PrimaryViolet, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, modifier = Modifier.weight(1f), fontSize = 16.sp, color = if (isError) Color.Red else TextColor)
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
    }
}

// --- FORM REDESIGN (Multi-step Stepper) ---


@Composable
fun PatientChangePasswordDialog(onDismiss: () -> Unit) {
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var isChanging by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel()
    val context = LocalContext.current
    var currentPassVisible by remember { mutableStateOf(false) }
    var newPassVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password", fontWeight = FontWeight.Bold, color = PrimaryViolet) },
        text = {
            Column {
                ModernTextField(
                    value = currentPass, 
                    onValueChange = { currentPass = it }, 
                    label = "Current Password", 
                    leadingIcon = Icons.Default.Lock,
                    visualTransformation = if (currentPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { currentPassVisible = !currentPassVisible }) {
                            Icon(if (currentPassVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ModernTextField(
                    value = newPass, 
                    onValueChange = { newPass = it }, 
                    label = "New Password", 
                    leadingIcon = Icons.Default.Lock,
                    visualTransformation = if (newPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newPassVisible = !newPassVisible }) {
                            Icon(if (newPassVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isChanging = true
                    authViewModel.changePassword(currentPass, newPass,
                        onSuccess = {
                            isChanging = false
                            Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        onError = { err ->
                            isChanging = false
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
            ) {
                if (isChanging) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Change")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = PrimaryViolet) }
        },
        containerColor = Color.White
    )
}


