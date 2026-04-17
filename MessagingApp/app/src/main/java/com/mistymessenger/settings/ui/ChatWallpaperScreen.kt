package com.mistymessenger.settings.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.mistymessenger.settings.viewmodel.ChatSettingsViewModel

private val WallpaperColors = listOf(
    Color(0xFFECE5DD), Color(0xFFE3F2FD), Color(0xFFF3E5F5),
    Color(0xFFE8F5E9), Color(0xFFFFF9C4), Color(0xFFFBE9E7),
    Color(0xFFF5F5F5), Color(0xFF1A1A2E), Color(0xFF16213E),
    Color(0xFF0F3460), Color(0xFF1B262C), Color(0xFF000000)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatWallpaperScreen(
    chatId: String,
    navController: NavHostController,
    viewModel: ChatSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.getSettings(chatId).collectAsState(null)
    var selectedColor by remember { mutableStateOf<Color?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(settings) {
        settings?.wallpaperColor?.takeIf { it != 0L }?.let { selectedColor = Color(it) }
        settings?.wallpaperUri?.takeIf { it.isNotBlank() }?.let { selectedUri = Uri.parse(it) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { selectedUri = uri; selectedColor = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = { Text("Chat wallpaper") },
                actions = {
                    TextButton(onClick = {
                        viewModel.saveWallpaper(
                            chatId,
                            wallpaperUri = selectedUri?.toString() ?: "",
                            wallpaperColor = selectedColor?.toArgb()?.toLong() ?: 0L
                        )
                        navController.popBackStack()
                    }) { Text("Apply") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Preview
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp)
                    .background(selectedColor ?: Color(0xFFECE5DD)),
                contentAlignment = Alignment.Center
            ) {
                if (selectedUri != null) {
                    AsyncImage(
                        model = selectedUri, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Preview", color = Color.Black.copy(alpha = 0.4f))
                }
            }
            Spacer(Modifier.height(16.dp))

            // Gallery picker
            OutlinedButton(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Image, null)
                Spacer(Modifier.width(8.dp))
                Text("Choose from gallery")
            }

            Spacer(Modifier.height(16.dp))
            Text("Solid color", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(WallpaperColors.size) { i ->
                    val color = WallpaperColors[i]
                    val isSelected = selectedColor == color && selectedUri == null
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(color)
                            .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                            .clickable { selectedColor = color; selectedUri = null },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) Icon(Icons.Default.Check, null, tint = if (color.luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (settings?.wallpaperUri?.isNotBlank() == true || settings?.wallpaperColor != 0L) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { viewModel.saveWallpaper(chatId, "", 0L); selectedColor = null; selectedUri = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Remove wallpaper") }
            }
        }
    }
}

private fun Color.luminance(): Float {
    val r = red * 0.299f; val g = green * 0.587f; val b = blue * 0.114f
    return r + g + b
}
