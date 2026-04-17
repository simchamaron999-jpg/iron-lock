package com.mistymessenger.chat.ui

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mistymessenger.chat.viewmodel.AutoReplyViewModel
import com.mistymessenger.core.db.entity.AutoReplyEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoReplyScreen(
    navController: NavHostController,
    viewModel: AutoReplyViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = { Text("Auto Reply") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, "Add rule")
            }
        }
    ) { padding ->
        if (rules.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Reply, null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("No auto-reply rules", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("When triggered, replies are sent automatically", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(rules, key = { it.id }) { rule ->
                    AutoReplyRuleItem(
                        rule = rule,
                        onToggle = { viewModel.toggleRule(rule.id, it) },
                        onDelete = { viewModel.deleteRule(rule) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showCreate) {
        CreateRuleDialog(
            onCreate = { trigger, response -> viewModel.addRule(trigger, response); showCreate = false },
            onDismiss = { showCreate = false }
        )
    }
}

@Composable
private fun AutoReplyRuleItem(
    rule: AutoReplyEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = { Text("When: \"${rule.trigger}\"") },
        supportingContent = {
            Column {
                Text("Reply: \"${rule.response}\"", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Scope: ${rule.scope}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = rule.isEnabled, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

@Composable
private fun CreateRuleDialog(onCreate: (String, String) -> Unit, onDismiss: () -> Unit) {
    var trigger by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var scope by remember { mutableStateOf("all") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New auto-reply rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    label = { Text("Trigger keyword") },
                    placeholder = { Text("e.g. hello") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = response,
                    onValueChange = { response = it },
                    label = { Text("Reply message") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Text("Scope", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "Everyone", "contact" to "Contacts", "group" to "Groups").forEach { (key, label) ->
                        FilterChip(
                            selected = scope == key,
                            onClick = { scope = key },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (trigger.isNotBlank() && response.isNotBlank()) onCreate(trigger.trim(), response.trim()) },
                enabled = trigger.isNotBlank() && response.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
