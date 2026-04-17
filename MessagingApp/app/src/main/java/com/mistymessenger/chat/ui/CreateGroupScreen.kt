package com.mistymessenger.chat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mistymessenger.chat.viewmodel.CreateGroupViewModel
import com.mistymessenger.core.db.entity.ContactEntity
import com.mistymessenger.core.ui.components.AvatarImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onGroupCreated: (chatId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel()
) {
    val contacts by viewModel.filteredContacts.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val groupName by viewModel.groupName.collectAsState()
    val avatarUri by viewModel.avatarUri.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()
    val error by viewModel.error.collectAsState()
    val query by viewModel.query.collectAsState()

    var step by remember { mutableIntStateOf(0) } // 0 = pick members, 1 = name/icon

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> viewModel.setAvatarUri(uri) }

    LaunchedEffect(error) {
        if (error != null) viewModel.clearError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = if (step == 0) onBack else { { step = 0 } }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = {
                    if (step == 0) {
                        Column {
                            Text("New Group")
                            Text("${selected.size} of 1023 selected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text("Group Info")
                    }
                }
            )
        },
        floatingActionButton = {
            if (step == 0 && selected.isNotEmpty()) {
                FloatingActionButton(onClick = { step = 1 }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next")
                }
            } else if (step == 1) {
                FloatingActionButton(
                    onClick = { viewModel.createGroup(onGroupCreated) },
                    containerColor = if (isCreating) MaterialTheme.colorScheme.surfaceVariant
                                     else MaterialTheme.colorScheme.primary
                ) {
                    if (isCreating) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else Icon(Icons.Default.Check, "Create group")
                }
            }
        }
    ) { padding ->
        when (step) {
            0 -> MemberPickerStep(
                contacts = contacts,
                selected = selected,
                query = query,
                onQueryChange = { viewModel.setQuery(it) },
                onToggle = { viewModel.toggleContact(it) },
                modifier = Modifier.padding(padding)
            )
            1 -> GroupInfoStep(
                groupName = groupName,
                onNameChange = { viewModel.setGroupName(it) },
                avatarUri = avatarUri?.toString(),
                onPickAvatar = {
                    avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                selectedCount = selected.size,
                modifier = Modifier.padding(padding)
            )
        }
    }

    error?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberPickerStep(
    contacts: List<ContactEntity>,
    selected: Set<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        // Search bar
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = {},
            active = false,
            onActiveChange = {},
            placeholder = { Text("Search contacts") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {}

        // Selected chips
        if (selected.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(selected.toList()) { userId ->
                    val contact = contacts.find { it.userId == userId }
                    InputChip(
                        selected = true,
                        onClick = { onToggle(userId) },
                        label = { Text(contact?.name ?: userId, style = MaterialTheme.typography.labelMedium) },
                        avatar = { AvatarImage(url = contact?.avatarUrl ?: "", name = contact?.name ?: "", size = 24.dp) },
                        trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
            HorizontalDivider()
        }

        LazyColumn {
            items(contacts, key = { it.userId }) { contact ->
                ContactPickerRow(
                    contact = contact,
                    isSelected = contact.userId in selected,
                    onToggle = { onToggle(contact.userId) }
                )
            }
        }
    }
}

@Composable
private fun ContactPickerRow(contact: ContactEntity, isSelected: Boolean, onToggle: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onToggle),
        headlineContent = { Text(contact.name) },
        supportingContent = { Text(contact.phone, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { AvatarImage(url = contact.avatarUrl, name = contact.name, size = 40.dp) },
        trailingContent = {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
        }
    )
}

@Composable
private fun GroupInfoStep(
    groupName: String,
    onNameChange: (String) -> Unit,
    avatarUri: String?,
    onPickAvatar: () -> Unit,
    selectedCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Avatar picker
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onPickAvatar),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUri != null) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Default.CameraAlt, "Pick avatar", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        OutlinedTextField(
            value = groupName,
            onValueChange = { if (it.length <= 50) onNameChange(it) },
            label = { Text("Group name") },
            singleLine = true,
            trailingIcon = {
                Text("${groupName.length}/50", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            "$selectedCount participants will be added",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
