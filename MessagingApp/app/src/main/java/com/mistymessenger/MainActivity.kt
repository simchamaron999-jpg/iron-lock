package com.mistymessenger

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.mistymessenger.core.ui.theme.AppTheme
import com.mistymessenger.core.viewmodel.AppViewModel
import com.mistymessenger.navigation.AppNavHost
import com.mistymessenger.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val appViewModel: AppViewModel by viewModels()
    private var sessionAuthenticated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeState by appViewModel.themeState.collectAsState()
            val authState by appViewModel.authState.collectAsState()

            AppTheme(
                darkTheme = themeState.isDark,
                seedColor = themeState.seedColor,
                fontFamily = themeState.fontFamily,
                useDynamicColor = themeState.useDynamicColor
            ) {
                val navController = rememberNavController()
                val startDestination = if (authState.isLoggedIn) Screen.Main.route
                                       else Screen.PhoneEntry.route
                AppNavHost(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appViewModel.onAppForeground()
        if (appViewModel.isAppLockEnabled && !sessionAuthenticated) {
            showBiometricPrompt()
        }
    }

    override fun onPause() {
        super.onPause()
        appViewModel.onAppBackground()
        if (appViewModel.isAppLockEnabled) sessionAuthenticated = false
    }

    private fun showBiometricPrompt() {
        val canAuth = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return

        BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    sessionAuthenticated = true
                }
                override fun onAuthenticationFailed() { finish() }
                override fun onAuthenticationError(code: Int, msg: CharSequence) {
                    if (code != BiometricPrompt.ERROR_USER_CANCELED &&
                        code != BiometricPrompt.ERROR_NEGATIVE_BUTTON) finish()
                }
            }
        ).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("MistyMessenger locked")
                .setSubtitle("Authenticate to continue")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        )
    }
}
