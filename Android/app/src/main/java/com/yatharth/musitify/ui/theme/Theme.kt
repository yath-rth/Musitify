package com.yatharth.musitify.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme(
    primary = accentColor1,
    onPrimary = itemsBg,
    primaryContainer = accentColor1.copy(alpha = 0.2f),
    onPrimaryContainer = text,
    secondary = accentColor2,
    onSecondary = itemsBg,
    secondaryContainer = accentColor2.copy(alpha = 0.2f),
    onSecondaryContainer = text,
    tertiary = accentColor3,
    onTertiary = text,
    background = background,
    onBackground = text,
    surface = itemsBg,
    onSurface = text,
    surfaceVariant = secondaryBg,
    onSurfaceVariant = text,
    outline = cardBorder,
    outlineVariant = dividerColor,
)

val LightColorScheme = lightColorScheme(
    primary = accentColor1,
    onPrimary = itemsBg,
    primaryContainer = accentColor1.copy(alpha = 0.2f),
    onPrimaryContainer = text,
    secondary = accentColor2,
    onSecondary = itemsBg,
    secondaryContainer = accentColor2.copy(alpha = 0.2f),
    onSecondaryContainer = text,
    tertiary = accentColor3,
    onTertiary = text,
    background = background,
    onBackground = text,
    surface = itemsBg,
    onSurface = text,
    surfaceVariant = secondaryBg,
    onSurfaceVariant = text,
    outline = cardBorder,
    outlineVariant = dividerColor,
)

@Composable
fun textFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = text,
    unfocusedTextColor = text,
    focusedContainerColor = itemsBg,
    unfocusedContainerColor = itemsBg,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = accentColor2,
    focusedLeadingIconColor = accentColor2,
    unfocusedLeadingIconColor = text.copy(alpha = 0.4f),
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
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = adaptiveTypography(),
        content = content
    )
}