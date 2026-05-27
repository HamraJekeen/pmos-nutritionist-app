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
fun HealthHistoryTab(patientInputs: PatientInput?, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(BackgroundColor).verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = PrimaryViolet) }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Health Profile Summary", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryViolet)
        }
        Column(modifier = Modifier.padding(20.dp)) {
        
        if (patientInputs != null) {
            CustomCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(50.dp).background(PrimaryVioletLight, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = PrimaryViolet)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = patientInputs.patient_name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "Age: ${patientInputs.age} yrs | BMI: ${String.format("%.1f", patientInputs.weight_kg / ((patientInputs.height_cm/100)*(patientInputs.height_cm/100)))} | Type: ${if (patientInputs.pcos_type == "Adrenal") "Adrenal Fatigue" else patientInputs.pcos_type}", fontSize = 12.sp, color = SubtitleColor)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Hormonal Levels", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HormoneItem("FSH", patientInputs.fsh.toString(), "mIU/mL", Modifier.weight(1f))
                HormoneItem("LH", patientInputs.lh.toString(), "mIU/mL", Modifier.weight(1f))
                HormoneItem("TSH", patientInputs.tsh.toString(), "mIU/L", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HormoneItem("PRL", patientInputs.prl.toString(), "ng/mL", Modifier.weight(1f))
                HormoneItem("PRG", patientInputs.prg.toString(), "ng/mL", Modifier.weight(1f))
                HormoneItem("RBS", patientInputs.rbs.toString(), "mg/dl", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HormoneItem("AMH", patientInputs.amh.toString(), "ng/mL", Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(2f)) // Padding so AMH doesn't stretch too much
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Conditions", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                if (patientInputs.selected_conditions.isEmpty()) {
                    Text(text = "None", color = SubtitleColor, fontSize = 14.sp)
                } else {
                    patientInputs.selected_conditions.forEach { condition ->
                        ChipItem(condition)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Symptoms", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                val symptoms = mutableListOf<String>()
                if (patientInputs.weight_gain == 1) symptoms.add("Weight Gain")
                if (patientInputs.hair_growth == 1) symptoms.add("Hair Growth")
                if (patientInputs.skin_darkening == 1) symptoms.add("Skin Darkening")
                if (patientInputs.hair_loss == 1) symptoms.add("Hair Loss")
                if (patientInputs.pimples == 1) symptoms.add("Pimples")
                if (patientInputs.irregular_periods == 1) symptoms.add("Irregular Periods")
                
                if (symptoms.isEmpty()) {
                    Text(text = "None", color = SubtitleColor, fontSize = 14.sp)
                } else {
                    symptoms.forEach { ChipItem(it) }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Allergies", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                if (patientInputs.allergies.isEmpty()) {
                    Text(text = "None", color = SubtitleColor, fontSize = 14.sp)
                } else {
                    patientInputs.allergies.forEach { allergy ->
                        ChipItem(allergy)
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No history found.", color = SubtitleColor)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
    }
}


@Composable
fun HormoneItem(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, fontSize = 10.sp, color = SubtitleColor)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryViolet)
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = unit, fontSize = 8.sp, color = SubtitleColor, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}


@Composable
fun ChipItem(text: String) {
    Surface(
        modifier = Modifier.padding(4.dp),
        shape = RoundedCornerShape(20.dp),
        color = PrimaryVioletLight
    ) {
        Text(text = text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        content = content
    )
}


