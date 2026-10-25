package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.secondaryBg
import com.yatharth.musitify.ui.theme.text

/**
 * Shared dialog shell used by every Android dialog: Material3 AlertDialog with
 * `containerColor = secondaryBg`, title in labelLarge / ExtraBold / 2 sp tracking.
 * [title] is rendered as given (callers pass the UPPERCASE string, as on Android).
 */
@Composable
fun MusitifyDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    MusitifyDialog(
        onDismissRequest = onDismissRequest,
        titleContent = { DialogTitle(title) },
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        content = content,
    )
}

/** Variant with a custom title slot (e.g. the context-menu dialogs' art + name header). */
@Composable
fun MusitifyDialog(
    onDismissRequest: () -> Unit,
    titleContent: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = titleContent,
        text = content,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        containerColor = secondaryBg
    )
}

@Composable
fun DialogTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = text,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.sp
    )
}

private fun Modifier.dialogButtonText(pill: Boolean): Modifier =
    (if (pill) background(shape = RoundedCornerShape(10.dp), color = itemsBg.copy(alpha = 0.8f)) else this)
        .padding(10.dp)

/**
 * Confirm-style dialog button: `CustomButton(bg = color@0.2)` with
 * labelMedium / ExtraBold / 1.2 sp text in [color]. [pill] adds the inner
 * `itemsBg@0.8` 10 dp rounded pill used by CustomAlertBox & SleepTimerDialog.
 * Use `color = accentColor2` for the green variant.
 */
@Composable
fun DialogConfirmButton(
    label: String,
    onClick: () -> Unit,
    color: Color = accentColor1,
    pill: Boolean = false,
) {
    CustomButton(onClick = onClick, bgColor = color.copy(alpha = 0.2f)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.dialogButtonText(pill)
        )
    }
}

/** Cancel-style dialog button: plain CustomButton, labelMedium / ExtraLight text. */
@Composable
fun DialogCancelButton(
    onClick: () -> Unit,
    label: String = "CANCEL",
    pill: Boolean = false,
) {
    CustomButton(onClick = onClick) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = text,
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = 1.2.sp,
            modifier = Modifier.dialogButtonText(pill)
        )
    }
}
