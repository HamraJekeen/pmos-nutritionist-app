package com.example.pcos_nutritionist.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pcos_nutritionist.screens.LoginScreen
import com.example.pcos_nutritionist.screens.SplashScreen
import com.example.pcos_nutritionist.screens.PatientDashboardScreen
import com.example.pcos_nutritionist.screens.NutritionistDashboardScreen
import com.example.pcos_nutritionist.screens.AdminDashboardScreen
import com.example.pcos_nutritionist.screens.NutritionistRegistrationScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navigateToHome = {
                // To keep it simple, navigate to login initially. 
                // In a real app, you'd check auth state here.
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    when (role) {
                        "Admin" -> navController.navigate("admin_dashboard") { popUpTo("login") { inclusive = true } }
                        "Nutritionist" -> navController.navigate("nutritionist_dashboard") { popUpTo("login") { inclusive = true } }
                        else -> navController.navigate("patient_dashboard") { popUpTo("login") { inclusive = true } }
                    }
                },
                onNavigateToRegistration = {
                    navController.navigate("nutritionist_registration")
                }
            )
        }
        
        composable("patient_dashboard") {
            PatientDashboardScreen(
                onSignOut = {
                    navController.navigate("login") { popUpTo(0) }
                }
            )
        }
        
        composable("nutritionist_dashboard") {
            NutritionistDashboardScreen(
                onSignOut = {
                    navController.navigate("login") { popUpTo(0) }
                }
            )
        }
        
        composable("admin_dashboard") {
            AdminDashboardScreen(
                onSignOut = {
                    navController.navigate("login") { popUpTo(0) }
                }
            )
        }
        
        composable("nutritionist_registration") {
            NutritionistRegistrationScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
