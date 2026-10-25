package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yatharth.musitify.ui.theme.secondaryBg

/**
 * The app's dialog frame: the same outlined card with a hard offset shadow
 * as everything else, instead of Material's very rounded, borderless sheet.
 * Same slots as AlertDialog so dialogs can switch over one-for-one.
 */
@Composable
fun MusitifyDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        ShadowBox(
            modifier = modifier.padding(end = 4.dp, bottom = 4.dp),
            bgColor = secondaryBg,
            rounding = 5,
            alignment = Alignment.TopStart
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                title?.let {
                    it()
                    Spacer(Modifier.height(16.dp))
                }
                text?.let {
                    it()
                    Spacer(Modifier.height(20.dp))
                }
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
