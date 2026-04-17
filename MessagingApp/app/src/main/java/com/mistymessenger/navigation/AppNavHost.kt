package com.mistymessenger.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mistymessenger.auth.ui.OTPVerificationScreen
import com.mistymessenger.auth.ui.PhoneEntryScreen
import com.mistymessenger.auth.ui.ProfileSetupScreen
import com.mistymessenger.chat.ui.ArchivedChatsScreen
import com.mistymessenger.chat.ui.BroadcastScreen
import com.mistymessenger.chat.ui.ChatDetailScreen
import com.mistymessenger.chat.ui.ContactInfoScreen
import com.mistymessenger.chat.ui.CreateGroupScreen
import com.mistymessenger.chat.ui.ForwardMessageScreen
import com.mistymessenger.chat.ui.GlobalSearchScreen
import com.mistymessenger.chat.ui.GroupInfoScreen
import com.mistymessenger.chat.ui.MediaGalleryScreen
import com.mistymessenger.chat.ui.MediaViewerScreen
import com.mistymessenger.chat.ui.NewChatScreen
import com.mistymessenger.chat.ui.StarredMessagesScreen
import com.mistymessenger.core.ui.main.MainScreen
import com.mistymessenger.settings.ui.*
import com.mistymessenger.status.ui.StatusCreatorScreen
import com.mistymessenger.status.ui.StatusPrivacyScreen
import com.mistymessenger.status.ui.StatusViewerScreen
import com.mistymessenger.call.ui.VideoCallScreen
import com.mistymessenger.call.ui.VoiceCallScreen
import com.mistymessenger.chat.ui.AutoReplyScreen
import com.mistymessenger.chat.ui.ScheduledMessagesScreen

sealed class Screen(val route: String) {
    // Auth
    object PhoneEntry : Screen("phone_entry")
    object OTPVerification : Screen("otp_verification/{phone}") {
        fun createRoute(phone: String) = "otp_verification/$phone"
    }
    object ProfileSetup : Screen("profile_setup")

    // Main (bottom nav)
    object Main : Screen("main")

    // Chat
    object NewChat : Screen("new_chat")
    object ChatDetail : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    object CreateGroup : Screen("create_group")
    object GroupInfo : Screen("group_info/{chatId}") {
        fun createRoute(chatId: String) = "group_info/$chatId"
    }
    object ContactInfo : Screen("contact_info/{userId}") {
        fun createRoute(userId: String) = "contact_info/$userId"
    }
    object MediaViewer : Screen("media_viewer/{url}/{mimeType}") {
        fun createRoute(url: String, mimeType: String) = "media_viewer/$url/$mimeType"
    }
    object MediaGallery : Screen("media_gallery/{chatId}") {
        fun createRoute(chatId: String) = "media_gallery/$chatId"
    }
    object StarredMessages : Screen("starred_messages")
    object ArchivedChats : Screen("archived_chats")
    object Broadcast : Screen("broadcast")
    object ForwardMessage : Screen("forward/{messageId}") {
        fun createRoute(messageId: String) = "forward/$messageId"
    }
    object GlobalSearch : Screen("search")
    object ScheduledMessages : Screen("scheduled_messages/{chatId}") {
        fun createRoute(chatId: String) = "scheduled_messages/$chatId"
    }
    object AutoReply : Screen("auto_reply")

    // Status
    object StatusViewer : Screen("status_viewer/{userId}") {
        fun createRoute(userId: String) = "status_viewer/$userId"
    }
    object StatusCreator : Screen("status_creator")
    object StatusPrivacy : Screen("status_privacy")

    // Calls
    object VoiceCall : Screen("voice_call/{chatId}") {
        fun createRoute(chatId: String) = "voice_call/$chatId"
    }
    object VideoCall : Screen("video_call/{chatId}") {
        fun createRoute(chatId: String) = "video_call/$chatId"
    }

    // Settings
    object Settings : Screen("settings")
    object AccountSettings : Screen("settings/account")
    object PrivacySettings : Screen("settings/privacy")
    object ThemeSettings : Screen("settings/theme")
    object ChatWallpaper : Screen("settings/wallpaper/{chatId}") {
        fun createRoute(chatId: String) = "settings/wallpaper/$chatId"
    }
    object NotificationSettings : Screen("settings/notifications")
    object StorageSettings : Screen("settings/storage")
    object AppLock : Screen("settings/app_lock")
    object ChatLock : Screen("settings/chat_lock/{chatId}") {
        fun createRoute(chatId: String) = "settings/chat_lock/$chatId"
    }
    object MultipleAccounts : Screen("settings/accounts")
    object BlockedContacts : Screen("settings/blocked")
    object ChatExport : Screen("settings/export/{chatId}") {
        fun createRoute(chatId: String) = "settings/export/$chatId"
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {
        // Auth
        composable(Screen.PhoneEntry.route) {
            PhoneEntryScreen(onNavigateToOTP = { phone ->
                navController.navigate(Screen.OTPVerification.createRoute(phone))
            })
        }
        composable(
            Screen.OTPVerification.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { back ->
            val phone = back.arguments?.getString("phone") ?: ""
            OTPVerificationScreen(phone = phone, onVerified = {
                navController.navigate(Screen.ProfileSetup.route) {
                    popUpTo(Screen.PhoneEntry.route) { inclusive = true }
                }
            })
        }
        composable(Screen.ProfileSetup.route) {
            ProfileSetupScreen(onComplete = {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                }
            })
        }

        // Main
        composable(Screen.Main.route) {
            MainScreen(navController = navController)
        }

        // Chat
        composable(Screen.NewChat.route) {
            NewChatScreen(
                onChatSelected = { chatId -> navController.navigate(Screen.ChatDetail.createRoute(chatId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.ChatDetail.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { back ->
            val chatId = back.arguments?.getString("chatId") ?: ""
            ChatDetailScreen(
                chatId = chatId,
                navController = navController
            )
        }
        composable(Screen.CreateGroup.route) {
            CreateGroupScreen(
                onGroupCreated = { chatId ->
                    navController.navigate(Screen.ChatDetail.createRoute(chatId)) {
                        popUpTo(Screen.CreateGroup.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.GroupInfo.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { back ->
            GroupInfoScreen(
                chatId = back.arguments?.getString("chatId") ?: "",
                navController = navController
            )
        }
        composable(
            Screen.ContactInfo.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { back ->
            ContactInfoScreen(
                userId = back.arguments?.getString("userId") ?: "",
                navController = navController
            )
        }
        composable(
            Screen.MediaViewer.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("mimeType") { type = NavType.StringType }
            )
        ) { back ->
            val url = java.net.URLDecoder.decode(back.arguments?.getString("url") ?: "", "UTF-8")
            val mime = java.net.URLDecoder.decode(back.arguments?.getString("mimeType") ?: "image/jpeg", "UTF-8")
            MediaViewerScreen(url = url, mimeType = mime, navController = navController)
        }
        composable(
            Screen.MediaGallery.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { back ->
            MediaGalleryScreen(chatId = back.arguments?.getString("chatId") ?: "", navController = navController)
        }
        composable(Screen.StarredMessages.route) {
            StarredMessagesScreen(navController = navController)
        }
        composable(Screen.ArchivedChats.route) {
            ArchivedChatsScreen(navController = navController)
        }
        composable(Screen.Broadcast.route) {
            BroadcastScreen(navController = navController)
        }
        composable(
            Screen.ForwardMessage.route,
            arguments = listOf(navArgument("messageId") { type = NavType.StringType })
        ) { back ->
            ForwardMessageScreen(
                messageId = back.arguments?.getString("messageId") ?: "",
                onForwarded = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.GlobalSearch.route) {
            GlobalSearchScreen(navController = navController)
        }
        composable(
            Screen.ScheduledMessages.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { back ->
            ScheduledMessagesScreen(chatId = back.arguments?.getString("chatId") ?: "", navController = navController)
        }
        composable(Screen.AutoReply.route) {
            AutoReplyScreen(navController = navController)
        }

        // Status
        composable(
            Screen.StatusViewer.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { back ->
            StatusViewerScreen(userId = back.arguments?.getString("userId") ?: "", onBack = { navController.popBackStack() })
        }
        composable(Screen.StatusCreator.route) {
            StatusCreatorScreen(onPosted = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }
        composable(Screen.StatusPrivacy.route) {
            StatusPrivacyScreen(onBack = { navController.popBackStack() })
        }

        // Calls
        composable(
            Screen.VoiceCall.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { back ->
            VoiceCallScreen(chatId = back.arguments?.getString("chatId") ?: "", onEnd = { navController.popBackStack() })
        }
        composable(
            Screen.VideoCall.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { back ->
            VideoCallScreen(chatId = back.arguments?.getString("chatId") ?: "", onEnd = { navController.popBackStack() })
        }

        // Settings
        composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
        composable(Screen.AccountSettings.route) { AccountSettingsScreen(navController = navController) }
        composable(Screen.PrivacySettings.route) { PrivacySettingsScreen(navController = navController) }
        composable(Screen.ThemeSettings.route) { ThemeSettingsScreen(navController = navController) }
        composable(
            Screen.ChatWallpaper.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { back -> ChatWallpaperScreen(chatId = back.arguments?.getString("chatId") ?: "", navController = navController) }
        composable(Screen.NotificationSettings.route) { NotificationSettingsScreen(navController = navController) }
        composable(Screen.StorageSettings.route) { StorageSettingsScreen(navController = navController) }
        composable(Screen.AppLock.route) { AppLockScreen(navController = navController) }
        composable(
            Screen.ChatLock.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { back -> ChatLockScreen(chatId = back.arguments?.getString("chatId") ?: "", navController = navController) }
        composable(Screen.MultipleAccounts.route) { MultipleAccountsScreen(navController = navController) }
        composable(Screen.BlockedContacts.route) { BlockedContactsScreen(navController = navController) }
        composable(
            Screen.ChatExport.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { back -> ChatExportScreen(chatId = back.arguments?.getString("chatId") ?: "", navController = navController) }
    }
}
