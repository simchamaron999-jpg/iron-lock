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
import com.mistymessenger.chat.viewmodel.ChatDetailViewModel
import com.mistymessenger.core.db.entity.MessageEntity
import com.mistymessenger.core.network.TokenProvider
import com.mistymessenger.core.ui.components.AvatarImage
import com.mistymessenger.core.ui.components.MessageTickIcon
import com.mistymessenger.core.ui.theme.LocalChatThemeExtras
import com.mistymessenger.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    navController: NavHostController,
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val chat by viewModel.chat.collectAsState()
    val messages = viewModel.messages.collectAsLazyPagingItems()
    val typingUsers by viewModel.typingUsers.collectAsState()
    val replyTarget by viewModel.replyTarget.collectAsState()
    val themeExtras = LocalChatThemeExtras.current
    val currentUserId = viewModel.currentUserId

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chatId) { viewModel.init(chatId) }

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
                            else navController.navigate(Screen.ContactInfo.createRoute(chat?.memberIds?.firstOrNull { it != currentUserId } ?: ""))
                        }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarImage(url = chat?.avatarUrl ?: "", name = chat?.name ?: "", size = 36.dp)
                        Spacer(Modifier.width(8.dp))
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
                    IconButton(onClick = { /* show chat menu */ }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                replyTarget?.let { reply ->
                    ReplyBanner(message = reply, onDismiss = { viewModel.clearReply() })
                }
                ChatInputBar(
                    text = inputText,
                    onTextChange = {
                        inputText = it
                        viewModel.onTyping()
                    },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendTextMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    onAttach = { /* show attachment picker */ },
                    onVoice = { /* start voice recording */ },
                    onEmoji = { /* show emoji picker */ }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
            state = listState,
            reverseLayout = true
        ) {
            items(count = messages.itemCount, key = messages.itemKey { it.id }) { index ->
                val message = messages[index]
                if (message != null) {
                    MessageBubble(
                        message = message,
                        isOutgoing = message.senderId == currentUserId,
                        outgoingColor = themeExtras.outgoingBubble,
                        incomingColor = themeExtras.incomingBubble,
                        onLongPress = { viewModel.onMessageLongPress(message) },
                        onReplySwipe = { viewModel.setReplyTarget(message) },
                        onClick = {
                            if (message.type == "image" || message.type == "video") {
                                navController.navigate(Screen.MediaViewer.createRoute(message.id))
                            }
                        }
                    )
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isOutgoing: Boolean,
    outgoingColor: Color,
    incomingColor: Color,
    onLongPress: () -> Unit,
    onReplySwipe: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (isOutgoing) outgoingColor else incomingColor
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start
    val shape = if (isOutgoing) {
        RoundedCornerShape(12.dp, 2.dp, 12.dp, 12.dp)
    } else {
        RoundedCornerShape(2.dp, 12.dp, 12.dp, 12.dp)
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Show anti-delete indicator
    val isAntiDeleted = message.deletedByOther
    val isDeleted = message.isDeletedForEveryone

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .combinedClickable(onClick = onClick, onLongClick = onLongPress)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column {
                // Reply quote
                if (message.replyToId.isNotEmpty()) {
                    ReplyQuote(replyToId = message.replyToId)
                    Spacer(Modifier.height(4.dp))
                }
                when {
                    isDeleted -> Text(
                        "This message was deleted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    isAntiDeleted -> Column {
                        Text(
                            message.content,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Deleted by sender",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    message.type == "text" -> Text(message.content, style = MaterialTheme.typography.bodyMedium)
                    message.type == "image" -> MessageImageContent(message)
                    message.type == "video" -> MessageVideoContent(message)
                    message.type == "audio" -> MessageAudioContent(message)
                    message.type == "document" -> MessageDocumentContent(message)
                    message.type == "location" -> MessageLocationContent(message)
                    else -> Text(message.content, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (message.scheduledAt > 0 && message.status == "sending") {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        timeFormat.format(Date(message.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isOutgoing) {
                        MessageTickIcon(status = message.status)
                    }
                }
            }
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
            Box(modifier = Modifier.width(4.dp).height(36.dp).background(MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Reply", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(message.content, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
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
                maxLines = 4,
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

// Stub composables — implemented in Phase 4
@Composable fun ReplyQuote(replyToId: String) { /* Phase 3 */ }
@Composable fun MessageImageContent(message: MessageEntity) { /* Phase 4 */ }
@Composable fun MessageVideoContent(message: MessageEntity) { /* Phase 4 */ }
@Composable fun MessageAudioContent(message: MessageEntity) { /* Phase 4 */ }
@Composable fun MessageDocumentContent(message: MessageEntity) { /* Phase 4 */ }
@Composable fun MessageLocationContent(message: MessageEntity) { /* Phase 8 */ }
