package com.example.pcos_nutritionist.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// --- Data Models ---

data class PatientInput(
    val patient_id: String,
    val patient_name: String,
    val age: Int,
    val weight_kg: Double,
    val height_cm: Double,
    val fsh: Double,
    val lh: Double,
    val tsh: Double,
    val prl: Double,
    val prg: Double,
    val rbs: Double,
    val amh: Double,
    val weight_gain: Int,
    val hair_growth: Int,
    val skin_darkening: Int,
    val hair_loss: Int,
    val pimples: Int,
    val irregular_periods: Int,
    val selected_conditions: List<String>,
    val pcos_type: String,
    val allergies: List<String>,
    val explanation: List<String>? = null
)

data class DietPlanResponse(
    val message: String?,
    val risk_level: String?,
    val plan: Map<String, Map<String, MealDetail>>?
)

data class MealDetail(
    val food: String,
    val portion: String
)

data class PatientPlanResponse(
    val status: String,
    val plan: Map<String, Map<String, MealDetail>>,
    val nutritionist_notes: String? = null
)

data class NutritionistInput(
    val patient_id: String,
    val physical_activities: String,
    val sleeping_hours: Double,
    val water_intake_goal: Int,
    val notes: String,
    val plan: Map<String, Map<String, MealDetail>>? = null
)

data class GenericResponse(
    val message: String
)

data class PendingPatient(
    val patient_id: String,
    val patient_name: String,
    val status: String
)

data class DailyTrackingInput(
    val patient_id: String,
    val tracking_date: String,
    val water_glasses: Int,
    val workout_minutes: Int,
    val notes: String
)

data class MealLogInput(
    val patient_id: String,
    val tracking_date: String,
    val meal_type: String,
    val food_logged: String,
    val status: String
)

data class QuestionInput(
    val patient_id: String,
    val patient_name: String,
    val question_text: String
)

data class AnswerInput(
    val question_id: Int,
    val answer_text: String
)

data class DailyTrackingData(
    val water_glasses: Int,
    val workout_minutes: Int,
    val notes: String,
    val adherence_percentage: Int
)

data class MealLogData(
    val meal_type: String,
    val food_logged: String,
    val status: String
)

data class WeeklyTrackingData(
    val date: String,
    val adherence: Int
)

data class TrackingResponse(
    val daily: DailyTrackingData,
    val meals: List<MealLogData>,
    val weekly: List<WeeklyTrackingData>
)

data class QuestionData(
    val id: Int,
    val patient_id: String? = null,
    val patient_name: String? = null,
    val question_text: String,
    val answer_text: String?,
    val status: String,
    val asked_at: String?,
    val answered_at: String? = null
)

data class NutritionistTrackingOverview(
    val patient_id: String,
    val patient_name: String,
    val adherence: Int
)

data class HistoricalPlanResponse(
    val id: Int,
    val patient_id: String,
    val patient_name: String,
    val created_at: String,
    val plan_data: Map<String, Map<String, MealDetail>>,
    val notes: String,
    val patient_inputs: PatientInput
)

// --- Api Service ---

interface ApiService {
    @POST("/api/predict_diet")
    suspend fun predictDiet(@Body input: PatientInput): DietPlanResponse

    @GET("/api/plans/{patient_id}")
    suspend fun getPatientPlan(@Path("patient_id") patientId: String): PatientPlanResponse

    @GET("/api/patient/{patient_id}/inputs")
    suspend fun getPatientInputs(@Path("patient_id") patientId: String): PatientInput

    @GET("/api/nutritionist/patients")
    suspend fun getAllPatients(): List<PendingPatient>

    @POST("/api/nutritionist/approve")
    suspend fun approvePlan(@Body input: NutritionistInput): GenericResponse

    @POST("/api/patient/track/daily")
    suspend fun updateDailyTracking(@Body input: DailyTrackingInput): GenericResponse

    @POST("/api/patient/track/meal")
    suspend fun logMeal(@Body input: MealLogInput): GenericResponse

    @GET("/api/patient/track/{patient_id}")
    suspend fun getPatientTracking(
        @Path("patient_id") patientId: String,
        @retrofit2.http.Query("date") date: String
    ): TrackingResponse

    @POST("/api/patient/qa")
    suspend fun askQuestion(@Body input: QuestionInput): GenericResponse

    @GET("/api/patient/qa/{patient_id}")
    suspend fun getPatientQuestions(@Path("patient_id") patientId: String): List<QuestionData>

    @GET("/api/nutritionist/tracking")
    suspend fun getNutritionistTracking(): List<NutritionistTrackingOverview>

    @GET("/api/nutritionist/qa")
    suspend fun getNutritionistQA(): List<QuestionData>

    @POST("/api/nutritionist/qa/answer")
    suspend fun answerQuestion(@Body input: AnswerInput): GenericResponse
    
    @GET("/api/nutritionist/plans/history")
    suspend fun getPlanHistory(): List<HistoricalPlanResponse>
}

object RetrofitClient {
    // Replace with your local machine's IP address (e.g. 192.168.1.x)
    private const val BASE_URL = "http://10.0.2.2:8000" // 10.0.2.2 is the emulator's loopback to localhost

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
