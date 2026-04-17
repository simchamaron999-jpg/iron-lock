package com.mistymessenger.settings.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mistymessenger.settings.viewmodel.ChatSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatLockScreen(
    chatId: String,
    navController: NavHostController,
    viewModel: ChatSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.getSettings(chatId).collectAsState(null)
    val isLocked = settings?.isChatLocked == true
    val context = LocalContext.current

    fun promptBiometric(onSuccess: () -> Unit) {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            }
        )
        biometricPrompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate to change chat lock")
                .setSubtitle("Confirm your identity")
                .setNegativeButtonText("Cancel")
                .build()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = { Text("Chat lock") }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Icon(
                if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                null,
                modifier = Modifier.size(72.dp),
                tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (isLocked) "This chat is locked" else "This chat is not locked",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "When locked, you must authenticate with biometrics before opening this chat.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))

            val canUseBiometric = BiometricManager.from(context).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            ) == BiometricManager.BIOMETRIC_SUCCESS

            if (canUseBiometric) {
                Button(
                    onClick = {
                        promptBiometric { viewModel.setChatLocked(chatId, !isLocked) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isLocked) "Unlock this chat" else "Lock this chat")
                }
            } else {
                Text("Biometric authentication is not available on this device.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
