package com.mistymessenger.chat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mistymessenger.chat.viewmodel.ChatsViewModel

// All other screens have been moved to their own files.
// Only ArchivedChatsScreen remains here as a stub pending Phase 9 implementation.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedChatsScreen(navController: NavHostController, viewModel: ChatsViewModel = hiltViewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("Archived") }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("No archived chats", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
