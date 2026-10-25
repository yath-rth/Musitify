package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.ui.theme.accentColor2
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.text

/**
 * The dialogs' "Tracknix input field": a 5 dp card (1.5 dp border) with a 4/4 hard shadow
 * holding a single-line [OutlinedTextField].
 *
 * [showOutline] = true keeps Material's inner outline (AddSongDialog / PlaylistDialog on
 * Android); false makes it transparent (AddToPlaylist / Rename dialogs).
 * Desktop: Enter calls [onSubmit] when given; focus is reported to the shortcut handler.
 */
@Composable
fun TracknixTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    showOutline: Boolean = true,
    onSubmit: (() -> Unit)? = null,
) {
    val currentOnSubmit by rememberUpdatedState(onSubmit)

    Box(modifier = modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(cardBorder, RoundedCornerShape(5.dp))
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = itemsBg),
            shape = RoundedCornerShape(5.dp),
            border = BorderStroke(1.5.dp, cardBorder)
        ) {
            val colors = if (showOutline) {
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = text,
                    unfocusedTextColor = text,
                    focusedContainerColor = itemsBg,
                    unfocusedContainerColor = itemsBg,
                    cursorColor = accentColor2,
                )
            } else {
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = text,
                    unfocusedTextColor = text,
                    focusedContainerColor = itemsBg,
                    unfocusedContainerColor = itemsBg,
                    cursorColor = accentColor2,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                )
            }
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .trackTextFieldFocus()
                    .onPreviewKeyEvent { e ->
                        val submit = currentOnSubmit
                        if (submit != null && e.type == KeyEventType.KeyDown &&
                            (e.key == Key.Enter || e.key == Key.NumPadEnter)
                        ) {
                            submit()
                            true
                        } else false
                    },
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = text.copy(alpha = 0.4f)
                    )
                },
                singleLine = true,
                colors = colors
            )
        }
    }
}
