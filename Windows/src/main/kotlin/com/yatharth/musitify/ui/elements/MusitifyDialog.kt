package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yatharth.musitify.ui.theme.danger
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.primaryAction
import com.yatharth.musitify.ui.theme.secondaryBg
import com.yatharth.musitify.ui.theme.text
import java.util.concurrent.atomic.AtomicInteger

/**
 * Number of [MusitifyDialog]s currently shown. The window only maps Escape to "back"
 * while this is 0 — otherwise Escape belongs to the dialog (dismiss).
 */
object OpenDialogs {
    private val count = AtomicInteger(0)
    val isAnyOpen: Boolean get() = count.get() > 0
    internal fun opened() {
        count.incrementAndGet()
    }

    internal fun closed() {
        count.updateAndGet { (it - 1).coerceAtLeast(0) }
    }
}

/**
 * The app's dialog frame: the same outlined card with a hard offset shadow as everything
 * else, instead of Material's very rounded, borderless sheet (Android `MusitifyDialog`).
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
    DisposableEffect(Unit) {
        OpenDialogs.opened()
        onDispose { OpenDialogs.closed() }
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ShadowBox(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 560.dp)
                .padding(horizontal = 24.dp)
                .padding(end = 4.dp, bottom = 4.dp),
            bgColor = secondaryBg,
            rounding = 5,
            alignment = Alignment.TopStart
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                titleContent()
                Spacer(Modifier.height(16.dp))
                content()
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButton?.let {
                        it()
                        Spacer(Modifier.width(8.dp))
                    }
                    confirmButton()
                }
            }
        }
    }
}

@Composable
fun DialogTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = text,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

/** Small caps label above a field ("NAME", "SONGS", "YOUTUBE LINK"). */
@Composable
internal fun FieldLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = com.yatharth.musitify.ui.theme.textSecondary
    )
}

private fun Modifier.dialogButtonText(pill: Boolean): Modifier =
    (if (pill) background(shape = RoundedCornerShape(10.dp), color = itemsBg.copy(alpha = 0.8f)) else this)
        .padding(10.dp)

/**
 * Confirm-style dialog button: `CustomButton(bg = color@0.2)` with
 * labelMedium / ExtraBold / 1.2 sp text in [textColor] (defaults to [color]).
 * [pill] adds the inner `itemsBg@0.8` 10 dp rounded pill used by CustomAlertBox.
 * Green ([primaryAction]) is the main action; red ([danger]) is destructive only.
 */
@Composable
fun DialogConfirmButton(
    label: String,
    onClick: () -> Unit,
    color: Color = primaryAction,
    textColor: Color = color,
    pill: Boolean = false,
) {
    CustomButton(onClick = onClick, bgColor = color.copy(alpha = 0.2f)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.dialogButtonText(pill)
        )
    }
}

/** Destructive confirm button (delete). */
@Composable
fun DialogDangerButton(label: String, onClick: () -> Unit, pill: Boolean = false) =
    DialogConfirmButton(label = label, onClick = onClick, color = danger, pill = pill)

/** Cancel-style dialog button: plain CustomButton, labelMedium regular text. */
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
            letterSpacing = 1.2.sp,
            modifier = Modifier.dialogButtonText(pill)
        )
    }
}
