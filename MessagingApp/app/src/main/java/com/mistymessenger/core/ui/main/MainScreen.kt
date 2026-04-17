package com.mistymessenger.core.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import com.mistymessenger.chat.ui.ChatsScreen
import com.mistymessenger.call.ui.CallsScreen
import com.mistymessenger.navigation.Screen
import com.mistymessenger.status.ui.StatusScreen

enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CHATS("Chats", Icons.Filled.Chat, Icons.Outlined.Chat),
    STATUS("Status", Icons.Filled.Circle, Icons.Outlined.Circle),
    CALLS("Calls", Icons.Filled.Phone, Icons.Outlined.Phone)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController) {
    var selectedTab by remember { mutableStateOf(MainTab.CHATS) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                MainTab.CHATS -> ChatsScreen(
                    onChatClick = { chatId -> navController.navigate(Screen.ChatDetail.createRoute(chatId)) },
                    onNewChat = { navController.navigate(Screen.NewChat.route) },
                    onSettings = { navController.navigate(Screen.Settings.route) },
                    onSearch = { navController.navigate(Screen.GlobalSearch.route) }
                )
                MainTab.STATUS -> StatusScreen(
                    onStatusClick = { userId -> navController.navigate(Screen.StatusViewer.createRoute(userId)) },
                    onCreateStatus = { navController.navigate(Screen.StatusCreator.route) }
                )
                MainTab.CALLS -> CallsScreen(
                    onCallClick = { chatId, isVideo ->
                        if (isVideo) navController.navigate(Screen.VideoCall.createRoute(chatId))
                        else navController.navigate(Screen.VoiceCall.createRoute(chatId))
                    },
                    onNewCall = { navController.navigate(Screen.NewChat.route) }
                )
            }
        }
    }
}
