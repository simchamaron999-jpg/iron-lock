package com.mistymessenger.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.google.android.material.color.MaterialColors
import androidx.compose.material3.dynamicColorScheme

// Composition locals so any composable can read theme extras
data class ChatThemeExtras(
    val outgoingBubble: Color,
    val incomingBubble: Color,
    val tickRead: Color = TickRead,
    val tickSent: Color = TickSent
)

val LocalChatThemeExtras = staticCompositionLocalOf {
    ChatThemeExtras(OutgoingBubbleLight, IncomingBubbleLight)
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    seedColor: Color = MistyGreen,
    fontFamily: FontFamily = AppFonts.Roboto,
    useDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme: ColorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> buildDarkScheme(seedColor)
        else -> buildLightScheme(seedColor)
    }

    val chatExtras = if (darkTheme) {
        ChatThemeExtras(OutgoingBubbleDark, IncomingBubbleDark)
    } else {
        ChatThemeExtras(OutgoingBubbleLight, IncomingBubbleLight)
    }

    CompositionLocalProvider(LocalChatThemeExtras provides chatExtras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildTypography(fontFamily),
            content = content
        )
    }
}

private fun buildLightScheme(seed: Color) = lightColorScheme(
    primary = seed,
    onPrimary = Color.White,
    primaryContainer = seed.copy(alpha = 0.2f),
    secondary = MistyGreenDark,
    tertiary = Color(0xFF80CBC4)
)

private fun buildDarkScheme(seed: Color) = darkColorScheme(
    primary = seed,
    onPrimary = Color.Black,
    primaryContainer = seed.copy(alpha = 0.3f),
    secondary = MistyGreenDark,
    surface = DarkSurface,
    background = DarkBackground
)
