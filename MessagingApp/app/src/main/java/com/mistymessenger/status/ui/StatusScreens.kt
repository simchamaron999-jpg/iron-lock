package com.mistymessenger.status.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mistymessenger.core.db.entity.StatusEntity
import com.mistymessenger.core.ui.components.AvatarImage
import com.mistymessenger.status.viewmodel.StatusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    onStatusClick: (String) -> Unit,
    onCreateStatus: () -> Unit,
    viewModel: StatusViewModel = hiltViewModel()
) {
    val myStatuses by viewModel.myStatuses.collectAsState()
    val contactStatuses by viewModel.contactStatuses.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            // My status row
            ListItem(
                modifier = Modifier.clickable { onCreateStatus() },
                headlineContent = { Text(if (myStatuses.isEmpty()) "Add status" else "My status") },
                supportingContent = {
                    Text(
                        if (myStatuses.isEmpty()) "Tap to add status update" else "${myStatuses.size} update${if (myStatuses.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Box {
                        AvatarImage(url = viewModel.myAvatarUrl, name = viewModel.myName, size = 48.dp)
                        if (myStatuses.isEmpty()) {
                            Box(
                                modifier = Modifier.size(16.dp).align(Alignment.BottomEnd).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(10.dp), tint = Color.White) }
                        }
                    }
                }
            )
            HorizontalDivider()
            if (contactStatuses.isNotEmpty()) {
                Text(
                    "Recent updates",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                )
            }
        }
        items(contactStatuses.size) { i ->
            val (userId, statuses) = contactStatuses[i]
            StatusContactItem(
                userId = userId,
                statuses = statuses,
                onClick = { onStatusClick(userId) }
            )
        }
    }
}

@Composable
private fun StatusContactItem(userId: String, statuses: List<StatusEntity>, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(statuses.firstOrNull()?.userId ?: userId) },
        supportingContent = {
            val time = statuses.maxByOrNull { it.createdAt }?.createdAt ?: 0L
            Text(formatStatusTime(time), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(userId.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
    )
}

private fun formatStatusTime(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "Yesterday"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusViewerScreen(userId: String, onBack: () -> Unit, viewModel: StatusViewModel = hiltViewModel()) {
    val statuses by viewModel.getStatusesForUser(userId).collectAsState(emptyList())
    var currentIndex by remember { mutableIntStateOf(0) }

    if (statuses.isEmpty()) { onBack(); return }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Progress bars
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp).align(Alignment.TopStart),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            statuses.forEachIndexed { i, _ ->
                LinearProgressIndicator(
                    progress = { when { i < currentIndex -> 1f; i == currentIndex -> 0.5f; else -> 0f } },
                    modifier = Modifier.weight(1f).height(2.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
        // Top bar
        TopAppBar(
            modifier = Modifier.align(Alignment.TopStart),
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) } },
            title = { Text(userId, color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        // Download button (WhatsApp Plus feature)
        IconButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            onClick = { viewModel.downloadStatus(statuses[currentIndex]) }
        ) {
            Icon(Icons.Default.Download, "Save status", tint = Color.White)
        }
        // Navigation tap zones
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                if (currentIndex > 0) currentIndex-- else onBack()
            })
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                if (currentIndex < statuses.size - 1) currentIndex++ else onBack()
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusCreatorScreen(onPosted: () -> Unit, onBack: () -> Unit, viewModel: StatusViewModel = hiltViewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("New Status") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Create status update", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = { /* open camera */ }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(4.dp)); Text("Camera")
                }
                OutlinedButton(onClick = { /* open gallery */ }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Photo, null); Spacer(Modifier.width(4.dp)); Text("Gallery")
                }
                OutlinedButton(onClick = { /* text status */ }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.TextFields, null); Spacer(Modifier.width(4.dp)); Text("Text")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusPrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("Status Privacy") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text("Coming soon", modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 64.dp))
        }
    }
}
