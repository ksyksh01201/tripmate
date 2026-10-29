package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.navigation.Screen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SignUpScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    TripMateApp()
                }
            }
        }
    }
}

@Composable
fun TripMateApp() {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.Splash) }

    when (currentScreen) {
        Screen.Splash -> {
            SplashScreen(
                onNavigateToLogin = { currentScreen = Screen.Login }
            )
        }
        Screen.Login -> {
            BackHandler {
                currentScreen = Screen.Splash
            }
            LoginScreen(
                onNavigateToSignUp = { currentScreen = Screen.SignUp },
                onLoginSuccess = { _ ->
                    currentScreen = Screen.Home
                }
            )
        }
        Screen.SignUp -> {
            BackHandler {
                currentScreen = Screen.Login
            }
            SignUpScreen(
                onNavigateToLogin = { currentScreen = Screen.Login },
                onSignUpSuccess = { currentScreen = Screen.Login }
            )
        }
        Screen.Home -> {
            BackHandler {
                currentScreen = Screen.Login
            }
            HomeScreen()
        }
    }
}
