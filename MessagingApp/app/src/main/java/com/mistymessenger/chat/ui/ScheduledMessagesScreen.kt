package com.mistymessenger.chat.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mistymessenger.chat.viewmodel.ScheduledMessageViewModel
import com.mistymessenger.core.db.entity.ScheduledMessageEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledMessagesScreen(
    chatId: String,
    navController: NavHostController,
    viewModel: ScheduledMessageViewModel = hiltViewModel()
) {
    val messages by viewModel.pendingMessages.collectAsState()
    val chatMessages = messages.filter { it.chatId == chatId }
    var showScheduler by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = { Text("Scheduled messages") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showScheduler = true }) {
                Icon(Icons.Default.Add, "Schedule message")
            }
        }
    ) { padding ->
        if (chatMessages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("No scheduled messages", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Tap + to schedule a message", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(chatMessages, key = { it.id }) { msg ->
                    ScheduledMessageItem(msg = msg, onCancel = { viewModel.cancel(msg.id) })
                    HorizontalDivider()
                }
            }
        }
    }

    if (showScheduler) {
        ScheduleMessageDialog(
            chatId = chatId,
            onSchedule = { content, ms -> viewModel.schedule(chatId, content, ms); showScheduler = false },
            onDismiss = { showScheduler = false }
        )
    }
}

@Composable
private fun ScheduledMessageItem(msg: ScheduledMessageEntity, onCancel: () -> Unit) {
    val fmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    ListItem(
        headlineContent = { Text(msg.content, maxLines = 2) },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text(fmt.format(Date(msg.scheduledAt)), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        },
        trailingContent = {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Cancel, "Cancel", tint = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleMessageDialog(
    chatId: String,
    onSchedule: (String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var scheduledMs by remember { mutableStateOf(System.currentTimeMillis() + 3600_000L) }
    val fmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    fun pickDateTime() {
        val cal = Calendar.getInstance().also { it.timeInMillis = scheduledMs }
        DatePickerDialog(context, { _, y, m, d ->
            TimePickerDialog(context, { _, h, min ->
                cal.set(y, m, d, h, min, 0)
                scheduledMs = cal.timeInMillis
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule message") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                OutlinedButton(onClick = ::pickDateTime, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Schedule, null)
                    Spacer(Modifier.width(8.dp))
                    Text(fmt.format(Date(scheduledMs)))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onSchedule(text, scheduledMs) },
                enabled = text.isNotBlank()
            ) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
