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
fun PmosGuidanceScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = PrimaryViolet) }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "PMOS Guidance", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryViolet)
        }
        
        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            item {
                Text("🌸 What is PMOS?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryViolet)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Polyendocrine Metabolic Ovarian Syndrome (PMOS) is a complex endocrine and metabolic disorder that affects women of reproductive age. It occurs due to hormonal imbalance involving insulin, reproductive hormones, and ovarian function, which can lead to irregular menstrual cycles, weight gain, acne, excess hair growth, and fertility-related complications.",
                    fontSize = 14.sp, color = TextColor, lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // 1. Insulin Resistance PMOS
            item {
                PmosTypeCard(
                    title = "1. Insulin Resistance PMOS",
                    description = "This is the most common type of PMOS.\nIn this type, the body’s cells do not respond properly to insulin.\n\nInsulin is the hormone that controls blood sugar. When the body becomes resistant to insulin, the pancreas produces more insulin. High insulin levels can increase androgen (male hormone) production, which affects ovulation.",
                    causes = listOf("High sugar diet", "Processed foods", "Lack of exercise", "Obesity or high BMI", "Genetics"),
                    symptoms = listOf("Weight gain (especially belly fat)", "Difficulty losing weight", "Cravings for sugar", "Fatigue after meals", "Dark patches on skin (acanthosis nigricans)", "Irregular periods", "Acne and hair growth"),
                    indicators = listOf("High insulin levels", "High blood sugar or high RBS", "BMI often > 25", "Increased LH/FSH ratio in some patients")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Inflammation PMOS
            item {
                PmosTypeCard(
                    title = "2. Inflammation PMOS",
                    description = "This type is linked with chronic low-grade inflammation in the body.\n\nInflammation can stimulate the ovaries to produce more androgens, causing PMOS symptoms.",
                    causes = listOf("Poor diet", "Stress", "Smoking", "Gut health issues", "Environmental toxins"),
                    symptoms = listOf("Fatigue", "Headaches", "Skin problems/acne", "Joint pain", "Irregular periods", "Mood issues", "Unexplained weight gain"),
                    indicators = listOf("Elevated inflammatory markers", "Acne and skin irritation", "High androgen symptoms")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 3. Hypothyroidism-related PMOS
            item {
                PmosTypeCard(
                    title = "3. Hypothyroidism-related PMOS",
                    description = "This type is associated with an underactive thyroid gland.\n\nThe thyroid controls metabolism. When thyroid hormone levels are low, metabolism slows down and hormonal imbalance can worsen PMOS symptoms.",
                    causes = listOf("Thyroid hormone deficiency", "Autoimmune thyroid disease", "Genetics"),
                    symptoms = listOf("Weight gain", "Slow metabolism", "Fatigue", "Depression", "Hair thinning", "Dry skin", "Irregular or heavy periods"),
                    indicators = listOf("High TSH levels", "Low thyroid hormone levels", "Persistent tiredness")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. Adrenal Fatigue / Stress-related PMOS
            item {
                PmosTypeCard(
                    title = "4. Adrenal Fatigue / Stress-related PMOS",
                    description = "This type is linked with chronic stress and high cortisol levels.\n\nThe adrenal glands produce stress hormones. Long-term stress may disturb reproductive hormones and worsen PMOS symptoms.",
                    causes = listOf("Chronic stress", "Poor sleep", "Anxiety", "Overworking", "Excessive exercise"),
                    symptoms = listOf("Anxiety", "Sleep problems", "Fatigue", "Irregular periods", "Hair loss", "Cravings", "Mood swings"),
                    indicators = listOf("Elevated stress hormones", "Normal insulin levels in some cases", "Symptoms worsen during stress")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 5. Pill-Induced PCOS
            item {
                PmosTypeCard(
                    title = "5. Pill-Induced PCOS",
                    description = "This type may appear after stopping hormonal birth control pills.\n\nBirth control pills suppress ovulation. After stopping them, some women temporarily experience PCOS-like symptoms.",
                    causes = listOf("Discontinuation of oral contraceptive pills", "Hormonal rebound effects"),
                    symptoms = listOf("Missing periods", "Acne", "Hair loss", "Mood changes", "Temporary irregular cycles"),
                    indicators = listOf("Symptoms begin after stopping pills", "Previously regular cycles before pill use", "Hormonal imbalance after discontinuation")
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Table part
            item {
                Text("🩺 Required Medical Reports & Hormonal Tests", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryViolet)
                Spacer(modifier = Modifier.height(16.dp))
                
                MedicalReportRow("Medical Report / Test", "Purpose", isHeader = true)
                MedicalReportRow("LH (Luteinizing Hormone) Blood Test", "Detect hormonal imbalance and ovulation irregularities")
                MedicalReportRow("FSH (Follicle Stimulating Hormone) Blood Test", "Used to calculate LH/FSH ratio for PCOS assessment")
                MedicalReportRow("TSH (Thyroid Stimulating Hormone) Blood Test", "Detect thyroid-related hormonal disorders")
                MedicalReportRow("Random Blood Sugar (RBS) Test / Blood Glucose Test / HbA1c Test", "Check blood sugar levels and insulin resistance")
                MedicalReportRow("PRL (Prolactin) Blood Test", "Assess prolactin hormone imbalance")
                MedicalReportRow("Progesterone (PRG) Blood Test", "Evaluate ovulation and menstrual cycle health")
                MedicalReportRow("AMH (Anti-Müllerian Hormone) Test (Optional)", "Supportive hormonal marker for PCOS detection")
                MedicalReportRow("BMI (Body Mass Index)", "Evaluate obesity and weight-related risk factors")
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}


@Composable
fun PmosTypeCard(title: String, description: String, causes: List<String>, symptoms: List<String>, indicators: List<String>) {
    CustomCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryViolet)
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, fontSize = 14.sp, color = TextColor, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Main Causes:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            causes.forEach { Text("• $it", fontSize = 13.sp, color = SubtitleColor) }
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("Common Symptoms:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            symptoms.forEach { Text("• $it", fontSize = 13.sp, color = SubtitleColor) }
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("Indicators:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            indicators.forEach { Text("• $it", fontSize = 13.sp, color = SubtitleColor) }
        }
    }
}


@Composable
fun MedicalReportRow(test: String, purpose: String, isHeader: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHeader) PrimaryViolet else Color.White)
            .border(1.dp, Color(0xFFEEEEEE))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = test,
            modifier = Modifier.weight(1f),
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Medium,
            color = if (isHeader) Color.White else TextColor,
            fontSize = if (isHeader) 14.sp else 13.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = purpose,
            modifier = Modifier.weight(1.2f),
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            color = if (isHeader) Color.White else SubtitleColor,
            fontSize = if (isHeader) 14.sp else 13.sp
        )
    }
}

