package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.ui.theme.text

/**
 * Confirmation dialog. Outside click / Esc only hides it
 * (`showClearConfirm = false`) without calling [onDismiss] — same as Android.
 * Pass a non-null [dontShowAgain] to show the "DON'T SHOW AGAIN" checkbox.
 */
@Composable
fun CustomAlertBox(
    showClearConfirm: MutableState<Boolean>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = {},
    title: String,
    body: String,
    yes: String,
    no: String,
    dontShowAgain: Boolean? = null,
    onDontShowAgainChange: (Boolean) -> Unit = {},
) {
    MusitifyDialog(
        onDismissRequest = { showClearConfirm.value = false },
        title = title.uppercase(),
        confirmButton = {
            DialogDangerButton(
                label = yes.uppercase(),
                onClick = {
                    showClearConfirm.value = false
                    onConfirm()
                },
                pill = true,
            )
        },
        dismissButton = {
            DialogCancelButton(
                label = no.uppercase(),
                onClick = {
                    showClearConfirm.value = false
                    onDismiss()
                },
                pill = true,
            )
        },
    ) {
        Column {
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = text
            )

            dontShowAgain?.let {
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = onDontShowAgainChange,
                        modifier = Modifier.handCursor()
                    )
                    Text(
                        text = "DON'T SHOW AGAIN",
                        color = text,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
