package com.mistymessenger.chat.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import com.mistymessenger.chat.ui.components.*
import com.mistymessenger.chat.viewmodel.ChatDetailViewModel
import com.mistymessenger.chat.viewmodel.ChatEvent
import com.mistymessenger.chat.viewmodel.GifViewModel
import com.mistymessenger.core.db.dao.MessageDao
import com.mistymessenger.core.db.entity.MessageEntity
import com.mistymessenger.core.media.RecorderState
import com.mistymessenger.core.ui.components.AvatarImage
import com.mistymessenger.core.ui.components.MessageTickIcon
import com.mistymessenger.core.ui.theme.LocalChatThemeExtras
import com.mistymessenger.navigation.Screen
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    navController: NavHostController,
    viewModel: ChatDetailViewModel = hiltViewModel(),
    gifViewModel: GifViewModel = hiltViewModel()
) {
    val chat by viewModel.chat.collectAsState()
    val messages = viewModel.messages.collectAsLazyPagingItems()
    val typingUsers by viewModel.typingUsers.collectAsState()
    val replyTarget by viewModel.replyTarget.collectAsState()
    val event by viewModel.event.collectAsState()
    val themeExtras = LocalChatThemeExtras.current
    val currentUserId = viewModel.currentUserId
    val recorderState by viewModel.voiceRecorder.state.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Shown modals
    var contextMenuMsg by remember { mutableStateOf<MessageEntity?>(null) }
    var emojiSheetMsg by remember { mutableStateOf<MessageEntity?>(null) }
    var deleteDialogMsg by remember { mutableStateOf<MessageEntity?>(null) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var showGifPicker by remember { mutableStateOf(false) }
    var showStickerSheet by remember { mutableStateOf(false) }
    var fullResEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(chatId) { viewModel.init(chatId) }

    // Handle events from ViewModel
    LaunchedEffect(event) {
        when (val e = event) {
            is ChatEvent.ShowContextMenu -> { contextMenuMsg = e.message; viewModel.clearEvent() }
            is ChatEvent.ShowEmojiSheet -> { emojiSheetMsg = e.message; viewModel.clearEvent() }
            is ChatEvent.ShowDeleteDialog -> { deleteDialogMsg = e.message; viewModel.clearEvent() }
            is ChatEvent.ShowForward -> { navController.navigate(Screen.ForwardMessage.createRoute(e.messageId)); viewModel.clearEvent() }
            ChatEvent.None -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.combinedClickable(onClick = {
                            if (chat?.type == "group") navController.navigate(Screen.GroupInfo.createRoute(chatId))
                            else navController.navigate(Screen.ContactInfo.createRoute(
                                chat?.memberIds?.firstOrNull { it != currentUserId } ?: ""
                            ))
                        }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarImage(url = chat?.avatarUrl ?: "", name = chat?.name ?: "", size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(chat?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (typingUsers.isNotEmpty()) {
                                Text("typing...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.VideoCall.createRoute(chatId)) }) {
                        Icon(Icons.Default.Videocam, "Video call")
                    }
                    IconButton(onClick = { navController.navigate(Screen.VoiceCall.createRoute(chatId)) }) {
                        Icon(Icons.Default.Call, "Voice call")
                    }
                    IconButton(onClick = { /* chat options menu */ }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                replyTarget?.let { reply ->
                    ReplyBanner(
                        message = reply,
                        onDismiss = { viewModel.clearReply() }
                    )
                }
                when (val rs = recorderState) {
                    is RecorderState.Recording -> VoiceRecorderBar(
                        durationMs = rs.durationMs,
                        amplitudes = rs.amplitudes,
                        onCancel = { viewModel.cancelRecording() },
                        onSend = { viewModel.stopAndSendVoice() }
                    )
                    else -> ChatInputBar(
                        text = inputText,
                        onTextChange = { inputText = it; viewModel.onTyping() },
                        onSend = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendTextMessage(inputText.trim())
                                inputText = ""
                            }
                        },
                        onAttach = { showAttachSheet = true },
                        onVoice = { viewModel.startRecording() },
                        onEmoji = { /* TODO: emoji keyboard */ }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
            state = listState,
            reverseLayout = true
        ) {
            items(count = messages.itemCount, key = messages.itemKey { it.id }) { index ->
                val message = messages[index] ?: return@items
                val prevMessage = if (index < messages.itemCount - 1) messages[index + 1] else null
                val showDateSeparator = prevMessage == null ||
                    !sameDay(message.createdAt, prevMessage.createdAt)

                Column {
                    if (showDateSeparator) {
                        DateSeparator(message.createdAt)
                    }
                    MessageBubble(
                        message = message,
                        isOutgoing = message.senderId == currentUserId,
                        outgoingColor = themeExtras.outgoingBubble,
                        incomingColor = themeExtras.incomingBubble,
                        currentUserId = currentUserId,
                        onLongPress = { viewModel.onMessageLongPress(message) },
                        onReplySwipe = { viewModel.setReplyTarget(message) },
                        onReactionClick = { emoji -> viewModel.sendReaction(message.id, emoji) },
                        onClick = {
                            if (message.type == "image" || message.type == "video" || message.type == "gif") {
                                val encodedUrl = URLEncoder.encode(message.mediaUrl ?: "", "UTF-8")
                                val mime = if (message.type == "video") "video/mp4" else "image/jpeg"
                                val encodedMime = URLEncoder.encode(mime, "UTF-8")
                                navController.navigate(Screen.MediaViewer.createRoute(encodedUrl, encodedMime))
                            }
                        },
                        viewModel = viewModel
                    )
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }

    // Context menu bottom sheet
    contextMenuMsg?.let { msg ->
        MessageContextMenu(
            message = msg,
            isOutgoing = msg.senderId == currentUserId,
            onReply = { viewModel.setReplyTarget(msg) },
            onReact = { emojiSheetMsg = msg },
            onStar = { viewModel.toggleStar(msg) },
            onForward = { navController.navigate(Screen.ForwardMessage.createRoute(msg.id)) },
            onCopy = { viewModel.copyMessage(msg.content) },
            onDeleteForMe = { deleteDialogMsg = msg },
            onDeleteForEveryone = { deleteDialogMsg = msg },
            onDismiss = { contextMenuMsg = null }
        )
    }

    // Emoji reaction sheet
    emojiSheetMsg?.let { msg ->
        EmojiReactionSheet(
            onEmojiSelected = { emoji ->
                viewModel.sendReaction(msg.id, emoji)
                emojiSheetMsg = null
            },
            onDismiss = { emojiSheetMsg = null }
        )
    }

    // Delete confirmation dialog
    deleteDialogMsg?.let { msg ->
        DeleteMessageDialog(
            isOutgoing = msg.senderId == currentUserId,
            onDeleteForMe = {
                viewModel.deleteForMe(msg.id)
                deleteDialogMsg = null
            },
            onDeleteForEveryone = {
                viewModel.deleteForEveryone(msg.id)
                deleteDialogMsg = null
            },
            onDismiss = { deleteDialogMsg = null }
        )
    }

    // Attachment sheet
    if (showAttachSheet) {
        AttachmentSheet(
            onDismiss = { showAttachSheet = false },
            onImageSelected = { uri, fr -> viewModel.sendMedia(uri, "image/jpeg", fr); showAttachSheet = false },
            onVideoSelected = { uri, fr -> viewModel.sendMedia(uri, "video/mp4", fr); showAttachSheet = false },
            onDocumentSelected = { uri -> viewModel.sendMedia(uri, "application/octet-stream", true); showAttachSheet = false },
            onCameraCapture = { showAttachSheet = false },
            onGifPick = { showAttachSheet = false; showGifPicker = true },
            onStickerPick = { showAttachSheet = false; showStickerSheet = true },
            onLocationShare = { showAttachSheet = false },
            onContactShare = { showAttachSheet = false },
            fullResEnabled = fullResEnabled,
            onToggleFullRes = { fullResEnabled = it }
        )
    }

    // GIF picker
    if (showGifPicker) {
        val gifs by gifViewModel.gifs.collectAsState()
        val gifLoading by gifViewModel.loading.collectAsState()
        GifPickerSheet(
            onDismiss = { showGifPicker = false },
            onGifSelected = { url -> viewModel.sendGif(url); showGifPicker = false },
            gifs = gifs,
            isLoading = gifLoading,
            onSearch = { gifViewModel.search(it) }
        )
    }

    // Sticker sheet
    if (showStickerSheet) {
        StickerSheet(
            onDismiss = { showStickerSheet = false },
            onStickerSelected = { url -> viewModel.sendSticker(url); showStickerSheet = false },
            packs = emptyList(),
            onDownloadMorePacks = {}
        )
    }
}

// ── Message bubble ────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    isOutgoing: Boolean,
    outgoingColor: Color,
    incomingColor: Color,
    currentUserId: String,
    onLongPress: () -> Unit,
    onReplySwipe: () -> Unit,
    onReactionClick: (String) -> Unit,
    onClick: () -> Unit,
    viewModel: ChatDetailViewModel,
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (isOutgoing) outgoingColor else incomingColor
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start
    val shape = if (isOutgoing) RoundedCornerShape(12.dp, 2.dp, 12.dp, 12.dp)
                else RoundedCornerShape(2.dp, 12.dp, 12.dp, 12.dp)
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(bubbleColor)
                .combinedClickable(onClick = onClick, onLongClick = onLongPress)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column {
                // Reply quote
                if (message.replyToId.isNotEmpty()) {
                    ReplyQuote(
                        replyToId = message.replyToId,
                        messageDao = viewModel.messageDao
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // Forward badge
                if (message.isForwarded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Icon(Icons.Default.Forward, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Forwarded", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Content
                when {
                    message.isDeletedForEveryone -> Text(
                        "This message was deleted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    message.deletedByOther -> Column {
                        Text(message.content, style = MaterialTheme.typography.bodyMedium)
                        Text("Deleted by sender", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                    message.type == "text" -> {
                        Text(message.content, style = MaterialTheme.typography.bodyMedium)
                        if (message.linkPreviewUrl.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            LinkPreviewCard(message)
                        }
                    }
                    message.type == "image" -> MessageImageContent(message)
                    message.type == "video" -> MessageVideoContent(message)
                    message.type == "audio" -> MessageAudioContent(message)
                    message.type == "document" -> MessageDocumentContent(message)
                    message.type == "location" -> MessageLocationContent(message)
                    else -> Text(message.content, style = MaterialTheme.typography.bodyMedium)
                }

                // Time + tick
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (message.isStarred) {
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        timeFormat.format(Date(message.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isOutgoing) MessageTickIcon(status = message.status)
                }
            }
        }

        // Reactions below bubble
        if (message.reactions.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            MessageReactions(
                reactions = message.reactions,
                currentUserId = currentUserId,
                onReactionClick = onReactionClick,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// ── Supporting composables ────────────────────────────────────────────────────

@Composable
private fun DateSeparator(timestampMs: Long) {
    val label = remember(timestampMs) {
        val cal = Calendar.getInstance()
        val today = Calendar.getInstance()
        cal.timeInMillis = timestampMs
        when {
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> "Yesterday"
            else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestampMs))
        }
    }
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ReplyBanner(message: MessageEntity, onDismiss: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(3.dp).height(36.dp).background(MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Reply", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    when (message.type) {
                        "image" -> "Photo"
                        "video" -> "Video"
                        "audio" -> "Voice message"
                        "document" -> "Document"
                        else -> message.content
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "Dismiss", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onVoice: () -> Unit,
    onEmoji: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = onEmoji) { Icon(Icons.Default.EmojiEmotions, "Emoji") }
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                maxLines = 5,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(onClick = onAttach) { Icon(Icons.Default.AttachFile, "Attach") }
            if (text.isBlank()) {
                IconButton(onClick = onVoice) { Icon(Icons.Default.Mic, "Voice") }
            } else {
                FloatingActionButton(
                    onClick = onSend,
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun DeleteMessageDialog(
    isOutgoing: Boolean,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete message?") },
        text = { Text("This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onDeleteForMe) { Text("Delete for me", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            Row {
                if (isOutgoing) {
                    TextButton(onClick = onDeleteForEveryone) {
                        Text("Delete for everyone", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun MessageImageContent(message: MessageEntity) {
    AsyncImage(
        model = message.mediaUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .widthIn(max = 240.dp)
            .heightIn(max = 240.dp)
            .clip(RoundedCornerShape(8.dp))
    )
}

@Composable
fun MessageVideoContent(message: MessageEntity) {
    Box(
        modifier = Modifier
            .widthIn(max = 240.dp)
            .heightIn(max = 180.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = message.thumbnailUrl ?: message.mediaUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Icon(
            Icons.Default.PlayCircle,
            null,
            tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(44.dp).align(Alignment.Center)
        )
    }
}

@Composable
fun MessageAudioContent(message: MessageEntity) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            message.mediaUrl?.let { setMediaItem(androidx.media3.common.MediaItem.fromUri(it)) }
            prepare()
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            player.play()
            while (player.isPlaying) {
                val dur = player.duration.coerceAtLeast(1)
                progress = player.currentPosition.toFloat() / dur
                kotlinx.coroutines.delay(200)
            }
            isPlaying = false
        } else {
            player.pause()
        }
    }

    Row(
        modifier = Modifier.width(200.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = { isPlaying = !isPlaying }, modifier = Modifier.size(36.dp)) {
            Icon(
                if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        WaveformPlayback(
            amplitudes = List(30) { (50 + (Math.random() * 200).toInt()) },
            progress = progress,
            modifier = Modifier.weight(1f).height(32.dp)
        )
    }
}

@Composable
fun MessageDocumentContent(message: MessageEntity) {
    val fileName = message.mediaUrl?.substringAfterLast("/")?.substringBefore("?") ?: "Document"
    Row(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Text(fileName, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun MessageLocationContent(message: MessageEntity) {
    Row(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
        Text(message.content.ifBlank { "Location" }, style = MaterialTheme.typography.bodySmall)
    }
}

private fun sameDay(ms1: Long, ms2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = ms1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = ms2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

