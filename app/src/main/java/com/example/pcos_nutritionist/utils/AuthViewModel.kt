package com.example.pcos_nutritionist.utils

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pcos_nutritionist.api.RetrofitClient
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Using StateFlow which is highly recommended for Jetpack Compose
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        if (auth.currentUser == null) {
            _authState.value = AuthState.Unauthenticated
        } else {
            _authState.value = AuthState.Authenticated
        }
    }
    
    fun getCurrentUser() = auth.currentUser

    fun login(email: String, password: String, onRoleDetermined: (String) -> Unit) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email or password can't be empty")
            return
        }
        
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Authenticated
                    // Determine role
                    viewModelScope.launch {
                        try {
                            val roleResponse = RetrofitClient.apiService.getUserRole(email)
                            onRoleDetermined(roleResponse.role)
                        } catch (e: Exception) {
                            // Fallback
                            val role = if (email.trim().lowercase() == "hamrajekeen@gmail.com") "Admin" else if (email.trim().lowercase() == "nutritionisthamra@gmail.com") "Nutritionist" else "Patient"
                            onRoleDetermined(role)
                        }
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Something Went Wrong")
                }
            }
    }

    fun createPatientAccount(
        context: Context,
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isEmpty() || pass.isEmpty()) {
            onError("Email or password can't be empty")
            return
        }
        
        try {
            val primaryApp = FirebaseApp.getInstance()
            val options = FirebaseOptions.Builder()
                .setApiKey(primaryApp.options.apiKey)
                .setApplicationId(primaryApp.options.applicationId)
                .setDatabaseUrl(primaryApp.options.databaseUrl)
                .setStorageBucket(primaryApp.options.storageBucket)
                .setProjectId(primaryApp.options.projectId)
                .build()

            var secondaryApp: FirebaseApp? = null
            try {
                secondaryApp = FirebaseApp.getInstance("SecondaryApp")
            } catch (e: IllegalStateException) {
                secondaryApp = FirebaseApp.initializeApp(context, options, "SecondaryApp")
            }

            if (secondaryApp != null) {
                val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
                secondaryAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            secondaryAuth.signOut()
                            onSuccess()
                        } else {
                            onError(task.exception?.message ?: "Failed to create account")
                        }
                    }
            } else {
                onError("Failed to initialize secondary Firebase App")
            }
        } catch (e: Exception) {
            onError("Error: ${e.message}")
        }
    }

    fun createNutritionistAccount(
        context: Context,
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Reuse patient account creation logic since it simply creates a secondary Auth user
        createPatientAccount(context, email, pass, onSuccess, onError)
    }


    fun changePassword(
        currentPass: String,
        newPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser
        val userEmail = user?.email
        if (user == null || userEmail == null) {
            onError("User not authenticated")
            return
        }
        
        if (currentPass.isEmpty() || newPass.isEmpty()) {
            onError("Passwords cannot be empty")
            return
        }

        val credential = EmailAuthProvider.getCredential(userEmail, currentPass)
        user.reauthenticate(credential)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    user.updatePassword(newPass)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                onSuccess()
                            } else {
                                onError(updateTask.exception?.message ?: "Failed to update password")
                            }
                        }
                } else {
                    onError(authTask.exception?.message ?: "Re-authentication failed. Check current password.")
                }
            }
    }

    fun signout() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
}
