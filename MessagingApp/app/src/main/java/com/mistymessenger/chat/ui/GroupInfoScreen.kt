package com.mistymessenger.chat.ui

import androidx.compose.foundation.clickable
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
import com.mistymessenger.chat.viewmodel.GroupInfoViewModel
import com.mistymessenger.core.db.entity.ContactEntity
import com.mistymessenger.core.ui.components.AvatarImage
import com.mistymessenger.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    chatId: String,
    navController: NavHostController,
    viewModel: GroupInfoViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val chat = state.chat
    val currentUserId = viewModel.currentUserId
    val isAdmin = chat?.adminIds?.contains(currentUserId) == true

    var showLeaveDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf<ContactEntity?>(null) }

    LaunchedEffect(chatId) { viewModel.load(chatId) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = { Text("Group Info") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Group header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AvatarImage(url = chat?.avatarUrl ?: "", name = chat?.name ?: "", size = 80.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(chat?.name ?: "", style = MaterialTheme.typography.titleLarge)
                    if (chat?.description?.isNotBlank() == true) {
                        Spacer(Modifier.height(4.dp))
                        Text(chat.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${chat?.memberIds?.size ?: 0} members", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Media gallery shortcut
            item {
                ListItem(
                    modifier = Modifier.clickable { navController.navigate(Screen.MediaGallery.createRoute(chatId)) },
                    headlineContent = { Text("Media, links and docs") },
                    leadingContent = { Icon(Icons.Default.Photo, null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                )
                HorizontalDivider()
            }

            // Invite link
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Invite link", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    if (state.inviteLink.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(state.inviteLink, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.copyInviteLink() }) {
                                Icon(Icons.Default.ContentCopy, "Copy link")
                            }
                        }
                    } else if (isAdmin) {
                        TextButton(onClick = { viewModel.generateInviteLink(chatId) }) {
                            Icon(Icons.Default.Link, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Generate invite link")
                        }
                    }
                }
                HorizontalDivider()
            }

            // Members section header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${state.contacts.size} members", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (isAdmin) {
                        TextButton(onClick = { /* open contact picker to add member */ }) {
                            Icon(Icons.Default.PersonAdd, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }
            }

            // Member list
            items(state.contacts, key = { it.userId }) { contact ->
                val contactIsAdmin = chat?.adminIds?.contains(contact.userId) == true
                MemberRow(
                    contact = contact,
                    isAdmin = contactIsAdmin,
                    isCurrentUser = contact.userId == currentUserId,
                    canManage = isAdmin && contact.userId != currentUserId,
                    onRemove = { showRemoveDialog = contact },
                    onPromote = { viewModel.promoteAdmin(chatId, contact.userId) },
                    onDemote = { viewModel.demoteAdmin(chatId, contact.userId) },
                    onViewProfile = { navController.navigate(Screen.ContactInfo.createRoute(contact.userId)) }
                )
            }

            // Leave group
            item {
                Spacer(Modifier.height(8.dp))
                ListItem(
                    modifier = Modifier.clickable { showLeaveDialog = true },
                    headlineContent = { Text("Leave group", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.ExitToApp, null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave group?") },
            text = { Text("You will no longer receive messages from this group.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.leaveGroup(chatId) {
                        showLeaveDialog = false
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Main.route) { inclusive = false }
                        }
                    }
                }) { Text("Leave", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    showRemoveDialog?.let { contact ->
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            title = { Text("Remove ${contact.name}?") },
            text = { Text("${contact.name} will be removed from this group.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeMember(chatId, contact.userId)
                    showRemoveDialog = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MemberRow(
    contact: ContactEntity,
    isAdmin: Boolean,
    isCurrentUser: Boolean,
    canManage: Boolean,
    onRemove: () -> Unit,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onViewProfile: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        modifier = Modifier.clickable(onClick = onViewProfile),
        headlineContent = { Text(if (isCurrentUser) "${contact.name} (You)" else contact.name) },
        supportingContent = { Text(contact.phone, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { AvatarImage(url = contact.avatarUrl, name = contact.name, size = 40.dp) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAdmin) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Admin", style = MaterialTheme.typography.labelSmall) }
                    )
                }
                if (canManage) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, null)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (isAdmin) {
                                DropdownMenuItem(
                                    text = { Text("Remove as admin") },
                                    onClick = { onDemote(); showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.PersonRemove, null) }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Make admin") },
                                    onClick = { onPromote(); showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Remove from group", color = MaterialTheme.colorScheme.error) },
                                onClick = { onRemove(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.PersonRemove, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }
        }
    )
}
