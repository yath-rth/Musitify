package com.yatharth.musitify.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

val DarkColorScheme = darkColorScheme(
    primary = background,
    secondary = secondaryBg,
    background = background
)

val LightColorScheme = lightColorScheme(
    primary = background,
    secondary = secondaryBg,
    background = background
)

@Composable
fun textFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = text,
    unfocusedTextColor = text,
    focusedContainerColor = itemsBg,
    unfocusedContainerColor = itemsBg,
    focusedIndicatorColor = text.copy(alpha = 0.2f),
    unfocusedIndicatorColor = text.copy(alpha = 0.1f),
)

@Composable
fun checkBoxColors() = CheckboxDefaults.colors(
    checkedColor = accentColor2.copy(alpha = 0.6f),
    uncheckedColor = text,
    checkmarkColor = accentColor2
)

@Composable
fun MusitifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = adaptiveTypography(),
        content = content
    )
}