package com.mistymessenger.chat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.mistymessenger.chat.viewmodel.MediaGalleryViewModel
import com.mistymessenger.core.db.entity.MessageEntity
import com.mistymessenger.navigation.Screen
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGalleryScreen(
    chatId: String,
    navController: NavHostController,
    viewModel: MediaGalleryViewModel = hiltViewModel()
) {
    val mediaMessages by viewModel.mediaMessages.collectAsState()

    LaunchedEffect(chatId) { viewModel.load(chatId) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = { Text("Media") }
            )
        }
    ) { padding ->
        if (mediaMessages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No media yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(mediaMessages, key = { it.id }) { msg ->
                    MediaGridItem(
                        message = msg,
                        onClick = {
                            val encodedUrl = URLEncoder.encode(msg.mediaUrl ?: "", "UTF-8")
                            val mime = if (msg.type == "video") "video/mp4" else "image/jpeg"
                            val encodedMime = URLEncoder.encode(mime, "UTF-8")
                            navController.navigate(Screen.MediaViewer.createRoute(encodedUrl, encodedMime))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaGridItem(message: MessageEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = if (message.type == "video") message.thumbnailUrl ?: message.mediaUrl
                    else message.mediaUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (message.type == "video") {
            Icon(
                Icons.Default.PlayCircle,
                null,
                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(32.dp).align(Alignment.Center)
            )
        }
    }
}
