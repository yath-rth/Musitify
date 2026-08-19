package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.secondaryBg
import com.yatharth.musitify.ui.theme.text

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
    AlertDialog(
        onDismissRequest = { showClearConfirm.value = false }, title = {
            Text(
                text = title.uppercase(),
                style = adaptiveTypography().labelLarge,
                color = text,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        }, text = {
            Column {
                Text(
                    text = body,
                    style = adaptiveTypography().bodySmall,
                    color = text
                )

                dontShowAgain?.let {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = onDontShowAgainChange
                        )

                        Text(
                            text = "DON'T SHOW AGAIN",
                            color = text,
                            style = adaptiveTypography().labelSmall,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }, confirmButton = {
            CustomButton(
                onClick = {
                    showClearConfirm.value = false
                    onConfirm()
                }, bgColor = accentColor1.copy(alpha = 0.2f)
            ) {
                Text(
                    text = yes.uppercase(),
                    style = adaptiveTypography().labelMedium,
                    color = accentColor1,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier
                        .background(
                            shape = RoundedCornerShape(10.dp), color = itemsBg.copy(alpha = 0.8f)
                        )
                        .padding(10.dp)
                )
            }
        }, dismissButton = {
            CustomButton(
                onClick = {
                    showClearConfirm.value = false
                    onDismiss()
                }) {
                Text(
                    text = no.uppercase(),
                    style = adaptiveTypography().labelMedium,
                    color = text,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier
                        .background(
                            shape = RoundedCornerShape(10.dp), color = itemsBg.copy(alpha = 0.8f)
                        )
                        .padding(10.dp)
                )
            }
        }, containerColor = secondaryBg
    )
}