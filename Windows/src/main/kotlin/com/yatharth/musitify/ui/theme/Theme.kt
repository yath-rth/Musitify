package com.yatharth.musitify.ui.theme

import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

// Android defines identical light & dark schemes; the app is always this palette.
val MusitifyColorScheme = lightColorScheme(
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
fun checkBoxColors() = CheckboxDefaults.colors(
    checkedColor = accentColor2.copy(alpha = 0.6f),
    uncheckedColor = text,
    checkmarkColor = accentColor2
)

@Composable
fun MusitifyTheme(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val widthPx = LocalWindowInfo.current.containerSize.width
    val scale = with(density) { uiScaleFor(widthPx.toDp()) }
    val typography = remember(scale) { adaptiveTypography(scale) }

    CompositionLocalProvider(LocalUiScale provides scale) {
        MaterialTheme(
            colorScheme = MusitifyColorScheme,
            typography = typography,
            content = content
        )
    }
}
