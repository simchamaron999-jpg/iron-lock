package com.mistymessenger.auth.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mistymessenger.auth.viewmodel.AuthViewModel
import com.mistymessenger.core.ui.components.AvatarImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        avatarUri = uri
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Set up your profile", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Add a name and optional photo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))

            // Avatar picker
            Box(
                modifier = Modifier.size(96.dp).clip(CircleShape).clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.BottomEnd
            ) {
                if (avatarUri != null) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    AvatarImage(url = "", name = name.ifBlank { "?" }, size = 96.dp)
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CameraAlt, "Change photo", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("${name.length}/25") }
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 139) bio = it },
                label = { Text("About (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                supportingText = { Text("${bio.length}/139") }
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.saveProfile(name.trim(), bio.trim(), avatarUri, onComplete)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else Text("Done")
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
