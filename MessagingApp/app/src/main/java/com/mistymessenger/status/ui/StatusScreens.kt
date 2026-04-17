package com.mistymessenger.status.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mistymessenger.core.db.entity.StatusEntity
import com.mistymessenger.core.ui.components.AvatarImage
import com.mistymessenger.status.viewmodel.StatusViewModel
import kotlinx.coroutines.delay

private val StatusBgColors = listOf(
    Color(0xFF075E54), Color(0xFF1E3A5F), Color(0xFF6A0DAD),
    Color(0xFFB22222), Color(0xFF1A1A1A), Color(0xFFD4A017),
    Color(0xFF2E8B57), Color(0xFF8B4513)
)

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
            ListItem(
                modifier = Modifier.clickable { onCreateStatus() },
                headlineContent = { Text(if (myStatuses.isEmpty()) "Add status" else "My status") },
                supportingContent = {
                    Text(
                        if (myStatuses.isEmpty()) "Tap to add status update"
                        else "${myStatuses.size} update${if (myStatuses.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Box {
                        AvatarImage(url = viewModel.myAvatarUrl, name = viewModel.myName.ifBlank { "Me" }, size = 52.dp)
                        if (myStatuses.isEmpty()) {
                            Box(
                                modifier = Modifier.size(18.dp).align(Alignment.BottomEnd)
                                    .clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp), tint = Color.White) }
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
        items(contactStatuses) { (userId, statuses) ->
            StatusContactItem(userId = userId, statuses = statuses, onClick = { onStatusClick(userId) })
        }
    }
}

@Composable
private fun StatusContactItem(userId: String, statuses: List<StatusEntity>, onClick: () -> Unit) {
    val firstName = statuses.firstOrNull()?.userId ?: userId
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(firstName) },
        supportingContent = {
            val time = statuses.maxByOrNull { it.createdAt }?.createdAt ?: 0L
            Text(formatStatusTime(time), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(firstName.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium)
            }
        },
        trailingContent = {
            Text("${statuses.size}", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    )
}

private const val STATUS_DURATION_MS = 5000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusViewerScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: StatusViewModel = hiltViewModel()
) {
    val statuses by viewModel.getStatusesForUser(userId).collectAsState(emptyList())
    var currentIndex by remember { mutableIntStateOf(0) }
    var progressFraction by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }

    if (statuses.isEmpty()) { onBack(); return }
    val current = statuses.getOrNull(currentIndex) ?: return

    LaunchedEffect(currentIndex) {
        viewModel.markViewed(current.id)
        progressFraction = 0f
        val steps = 100
        val stepMs = STATUS_DURATION_MS / steps
        repeat(steps) {
            if (!isPaused) {
                delay(stepMs.toLong())
                progressFraction = (it + 1) / steps.toFloat()
            } else {
                while (isPaused) delay(50)
            }
        }
        if (currentIndex < statuses.size - 1) currentIndex++ else onBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background / content
        when (current.type) {
            "text" -> Box(
                modifier = Modifier.fillMaxSize().background(Color(current.bgColor.takeIf { it != 0L } ?: 0xFF075E54L)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    current.content,
                    color = Color(current.textColor.takeIf { it != 0L } ?: 0xFFFFFFFFL),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(32.dp)
                )
            }
            "image" -> AsyncImage(
                model = current.mediaUrl, contentDescription = null,
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentScale = ContentScale.Fit
            )
            "video" -> Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Videocam, null, modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.3f))
            }
        }

        // Progress bars
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 48.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            statuses.forEachIndexed { i, _ ->
                val fraction = animateFloatAsState(
                    targetValue = when {
                        i < currentIndex -> 1f
                        i == currentIndex -> progressFraction
                        else -> 0f
                    },
                    animationSpec = if (i == currentIndex)
                        tween(durationMillis = STATUS_DURATION_MS, easing = LinearEasing)
                    else tween(0),
                    label = "progress_$i"
                )
                LinearProgressIndicator(
                    progress = { fraction.value },
                    modifier = Modifier.weight(1f).height(2.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.35f)
                )
            }
        }

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 56.dp, start = 4.dp, end = 16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Text(userId.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer) }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(userId, color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text(formatStatusTime(current.createdAt), color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall)
            }
            if (current.mediaUrl.isNotBlank()) {
                IconButton(onClick = { viewModel.downloadStatus(current) }) {
                    Icon(Icons.Default.Download, "Save", tint = Color.White)
                }
            }
        }

        // Left/right tap to advance
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
fun StatusCreatorScreen(
    onPosted: () -> Unit,
    onBack: () -> Unit,
    viewModel: StatusViewModel = hiltViewModel()
) {
    val isPosting by viewModel.isPosting.collectAsState()

    var mode by remember { mutableStateOf<CreatorMode>(CreatorMode.Choose) }
    var textContent by remember { mutableStateOf("") }
    var selectedBgColor by remember { mutableStateOf(StatusBgColors[0]) }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaMime by remember { mutableStateOf("image/jpeg") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { mediaUri = uri; mediaMime = "image/jpeg"; mode = CreatorMode.Media }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { mediaUri = uri; mediaMime = "video/mp4"; mode = CreatorMode.Media }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (mode == CreatorMode.Choose) onBack()
                        else mode = CreatorMode.Choose
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                title = { Text(when (mode) {
                    CreatorMode.Choose -> "New Status"
                    CreatorMode.Text -> "Text Status"
                    CreatorMode.Media -> "Preview"
                }) },
                actions = {
                    if (mode == CreatorMode.Text && textContent.isNotBlank()) {
                        IconButton(onClick = {
                            viewModel.postTextStatus(
                                text = textContent,
                                bgColor = selectedBgColor.toArgb().toLong(),
                                textColor = Color.White.toArgb().toLong(),
                                fontIndex = 0
                            ) { onPosted() }
                        }, enabled = !isPosting) {
                            if (isPosting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Send, "Post")
                        }
                    }
                    if (mode == CreatorMode.Media && mediaUri != null) {
                        IconButton(onClick = {
                            viewModel.postMediaStatus(mediaUri!!, mediaMime) { onPosted() }
                        }, enabled = !isPosting) {
                            if (isPosting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Send, "Post")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (mode) {
            CreatorMode.Choose -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                ) {
                    CreatorOption(Icons.Default.TextFields, "Text status", "Share thoughts with a colored background") {
                        mode = CreatorMode.Text
                    }
                    CreatorOption(Icons.Default.Photo, "Photo", "Share a photo from your gallery") {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    CreatorOption(Icons.Default.Videocam, "Video", "Share a video from your gallery") {
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }
                }
            }

            CreatorMode.Text -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding)
                        .background(selectedBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    TextField(
                        value = textContent,
                        onValueChange = { textContent = it },
                        placeholder = { Text("Type a status...", color = Color.White.copy(alpha = 0.5f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(color = Color.White),
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                    // Color picker row at bottom
                    LazyRow(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(StatusBgColors) { color ->
                            Box(
                                modifier = Modifier.size(if (color == selectedBgColor) 40.dp else 32.dp)
                                    .clip(CircleShape).background(color)
                                    .clickable { selectedBgColor = color }
                            )
                        }
                    }
                }
            }

            CreatorMode.Media -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black)) {
                    if (mediaUri != null) {
                        AsyncImage(
                            model = mediaUri, contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusPrivacyScreen(onBack: () -> Unit) {
    var contactsOption by remember { mutableStateOf(0) } // 0=All, 1=Contacts, 2=Custom

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("Status privacy") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Who can see my status updates",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            listOf("My contacts", "My contacts except...", "Only share with...").forEachIndexed { i, label ->
                ListItem(
                    modifier = Modifier.clickable { contactsOption = i },
                    headlineContent = { Text(label) },
                    trailingContent = {
                        RadioButton(selected = contactsOption == i, onClick = { contactsOption = i })
                    }
                )
            }
        }
    }
}

private sealed class CreatorMode {
    object Choose : CreatorMode()
    object Text : CreatorMode()
    object Media : CreatorMode()
}

private fun formatStatusTime(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "Yesterday"
    }
}
