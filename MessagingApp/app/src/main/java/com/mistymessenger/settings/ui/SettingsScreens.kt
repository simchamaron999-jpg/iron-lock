package com.mistymessenger.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mistymessenger.navigation.Screen
import com.mistymessenger.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController, viewModel: SettingsViewModel = hiltViewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("Settings") }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                SettingsItem(Icons.Default.AccountCircle, "Account", "Privacy, security, phone number") { navController.navigate(Screen.AccountSettings.route) }
                SettingsItem(Icons.Default.Lock, "Privacy", "Block contacts, last seen, status") { navController.navigate(Screen.PrivacySettings.route) }
                SettingsItem(Icons.Default.Notifications, "Notifications", "Message, group & call tones") { navController.navigate(Screen.NotificationSettings.route) }
                SettingsItem(Icons.Default.Palette, "Themes", "Colors, fonts, wallpapers") { navController.navigate(Screen.ThemeSettings.route) }
                SettingsItem(Icons.Default.PhoneAndroid, "App lock", "Biometric & PIN lock") { navController.navigate(Screen.AppLock.route) }
                SettingsItem(Icons.Default.People, "Accounts", "Switch between accounts") { navController.navigate(Screen.MultipleAccounts.route) }
                SettingsItem(Icons.Default.Storage, "Storage & Data", "Manage storage, network usage") { navController.navigate(Screen.StorageSettings.route) }
                SettingsItem(Icons.Default.Block, "Blocked contacts") { navController.navigate(Screen.BlockedContacts.route) }
                SettingsItem(Icons.Default.Info, "About") { /* show about dialog */ }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = if (subtitle.isNotEmpty()) ({ Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }) else null,
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline) }
    )
    HorizontalDivider()
}

// ─── Privacy Settings ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(navController: NavHostController, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.privacyState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("Privacy") }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                SectionHeader("Visibility")
                SwitchSettingsItem("Show last seen", state.showLastSeen) { viewModel.setShowLastSeen(it) }
                SwitchSettingsItem("Show online status", state.showOnlineStatus) { viewModel.setShowOnlineStatus(it) }
                SwitchSettingsItem("Show read receipts (blue ticks)", state.showReadReceipts) { viewModel.setShowReadReceipts(it) }
                SwitchSettingsItem("Show typing indicator", state.showTyping) { viewModel.setShowTyping(it) }

                SectionHeader("WhatsApp Plus Privacy")
                SwitchSettingsItem("Ghost mode (appear offline)", state.ghostMode) { viewModel.setGhostMode(it) }
                SwitchSettingsItem("Freeze last seen", state.freezeLastSeen) { viewModel.setFreezeLastSeen(it) }
                SwitchSettingsItem("Anti-delete messages", state.antiDelete) { viewModel.setAntiDelete(it) }
                SwitchSettingsItem("Anti-revoke messages", state.antiRevoke) { viewModel.setAntiRevoke(it) }

                SectionHeader("Contacts")
                ListItem(
                    headlineContent = { Text("Blocked contacts") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) }
                )
            }
        }
    }
}

// ─── Theme Settings ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(navController: NavHostController, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.themePrefs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("Themes") }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                SwitchSettingsItem("Dark mode", state.isDark) { viewModel.setDarkMode(it) }
                SwitchSettingsItem("Use device dynamic color", state.useDynamicColor) { viewModel.setDynamicColor(it) }
                Spacer(Modifier.height(16.dp))
                Text("Accent color", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                SeedColorPicker(current = state.seedColorHex, onPick = { viewModel.setSeedColor(it) })
                Spacer(Modifier.height(16.dp))
                Text("Font", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FontPicker(current = state.fontName, onPick = { viewModel.setFont(it) })
            }
        }
    }
}

@Composable
fun SeedColorPicker(current: String, onPick: (String) -> Unit) {
    val colors = listOf("#00BFA5", "#1976D2", "#7B1FA2", "#E53935", "#F57F17", "#388E3C")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { hex ->
            val color = android.graphics.Color.parseColor(hex)
            val composeColor = androidx.compose.ui.graphics.Color(color)
            Surface(
                modifier = Modifier.size(40.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = composeColor,
                onClick = { onPick(hex) },
                border = if (hex == current) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null
            ) {}
        }
    }
}

@Composable
fun FontPicker(current: String, onPick: (String) -> Unit) {
    val fonts = listOf("Roboto", "Inter", "DM Sans", "Nunito", "Poppins")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        fonts.forEach { font ->
            FilterChip(selected = font == current, onClick = { onPick(font) }, label = { Text(font) })
        }
    }
}

// ─── App Lock ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(navController: NavHostController, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.appLockState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("App Lock") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            SwitchSettingsItem("Enable app lock", state.enabled) { viewModel.setAppLockEnabled(it) }
            if (state.enabled) {
                Spacer(Modifier.height(8.dp))
                Text("Lock type", style = MaterialTheme.typography.titleSmall)
                Row {
                    RadioButton(selected = state.useBiometric, onClick = { viewModel.setUseBiometric(true) })
                    Text("Biometric", modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = !state.useBiometric, onClick = { viewModel.setUseBiometric(false) })
                    Text("PIN", modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
        }
    }
}

// ─── Multiple Accounts ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultipleAccountsScreen(navController: NavHostController, viewModel: SettingsViewModel = hiltViewModel()) {
    val accounts by viewModel.accounts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("Accounts") }
            )
        },
        floatingActionButton = {
            if (accounts.size < 3) {
                FloatingActionButton(onClick = { /* add account flow */ }) {
                    Icon(Icons.Default.Add, "Add account")
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(accounts.size) { i ->
                val account = accounts[i]
                ListItem(
                    headlineContent = { Text(account.name) },
                    supportingContent = { Text(account.phone) },
                    trailingContent = {
                        if (account.isActive) {
                            Badge { Text("Active") }
                        } else {
                            TextButton(onClick = { viewModel.switchAccount(account.id) }) { Text("Switch") }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

// ─── Stub screens (Phase 2–9 implementations) ────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(navController: NavHostController) = StubSettingsScreen("Account Settings", navController)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(navController: NavHostController) = StubSettingsScreen("Notification Settings", navController)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(navController: NavHostController) = StubSettingsScreen("Storage & Data", navController)

// ChatWallpaperScreen lives in ChatWallpaperScreen.kt
// ChatLockScreen lives in ChatLockScreen.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedContactsScreen(navController: NavHostController) = StubSettingsScreen("Blocked Contacts", navController)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatExportScreen(chatId: String, navController: NavHostController) = StubSettingsScreen("Export Chat", navController)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StubSettingsScreen(title: String, navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text(title) }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("Coming soon", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun SwitchSettingsItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
    HorizontalDivider()
}
