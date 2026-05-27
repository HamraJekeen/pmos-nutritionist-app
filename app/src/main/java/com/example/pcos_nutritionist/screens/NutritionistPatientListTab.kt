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
fun PatientListItem(p: com.example.pcos_nutritionist.api.PendingPatient, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).background(if (p.status == "Approved") Color(0xFFE8F5E9) else Color(0xFFFFF3E0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = p.patient_name.take(1),
                    color = if (p.status == "Approved") Color(0xFF2E7D32) else Color(0xFFE65100),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = p.patient_name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (p.status == "Approved") Icons.Default.CheckCircle else Icons.Default.Schedule,
                        null,
                        tint = if (p.status == "Approved") Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Status: ${p.status}",
                        fontSize = 12.sp,
                        color = if (p.status == "Approved") Color(0xFF4CAF50) else Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "ID: ${p.patient_id.take(8)}...", fontSize = 11.sp, color = SubtitleColor)
            }
            Box(
                modifier = Modifier
                    .background(PrimaryViolet, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = if (p.status == "Approved") "View" else "Review", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
fun CreatePatientDialog(onDismiss: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Patient Account", fontWeight = FontWeight.Bold, color = PrimaryViolet) },
        text = {
            Column {
                Text("Enter the email and password for the new patient.", fontSize = 14.sp, color = SubtitleColor)
                Spacer(modifier = Modifier.height(16.dp))
                ModernTextField(value = email, onValueChange = { email = it }, label = "Email", leadingIcon = Icons.Default.Email)
                Spacer(modifier = Modifier.height(8.dp))
                ModernTextField(value = password, onValueChange = { password = it }, label = "Password", leadingIcon = Icons.Default.Lock)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isCreating = true
                    authViewModel.createPatientAccount(context, email, password,
                        onSuccess = {
                            isCreating = false
                            Toast.makeText(context, "Account created successfully", Toast.LENGTH_SHORT).show()
                            
                            val subject = Uri.encode("Your PMOS Nutritionist App Account")
                            val body = Uri.encode(
                                "Hello,\n\n" +
                                "Your account has been created by your Nutritionist.\n" +
                                "Your temporary password is: $password\n\n" +
                                "Please log in to the PMOS Nutritionist App and change your password in the Profile section.\n\n" +
                                "Thank you."
                            )
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$email?subject=$subject&body=$body")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: android.content.ActivityNotFoundException) {
                                Toast.makeText(context, "No email app found to send the email.", Toast.LENGTH_LONG).show()
                            }
                            
                            onDismiss()
                        },
                        onError = { err ->
                            isCreating = false
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Create & Send Mail")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = PrimaryViolet) }
        },
        containerColor = Color.White
    )
}


