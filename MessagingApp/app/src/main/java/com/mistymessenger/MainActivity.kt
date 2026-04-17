package com.mistymessenger

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    }

    override fun onPause() {
        super.onPause()
        appViewModel.onAppBackground()
    }
}
