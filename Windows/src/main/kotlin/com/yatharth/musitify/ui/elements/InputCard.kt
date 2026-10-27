package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.text
import com.yatharth.musitify.ui.theme.textSecondary

/**
 * Text field in the outlined-card style used across the app's forms (Android `OutlinedInput`):
 * a 5 dp card with a 1.5 dp border and a 4/4 hard shadow, no Material outline.
 *
 * Desktop: Enter calls [onSubmit] when given; focus is reported to the shortcut handler.
 */
@Composable
fun TracknixTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    onSubmit: (() -> Unit)? = null,
) {
    val currentOnSubmit by rememberUpdatedState(onSubmit)
    val shape = RoundedCornerShape(5.dp)

    Box(modifier = modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(4.dp, 4.dp)
                .background(cardBorder, shape)
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .background(itemsBg, shape)
                .border(1.5.dp, cardBorder, shape)
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
                Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = textSecondary)
            },
            singleLine = true,
            leadingIcon = leadingIcon,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = text,
                unfocusedTextColor = text,
                focusedContainerColor = itemsBg,
                unfocusedContainerColor = itemsBg,
                cursorColor = text,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
            )
        )
    }
}
