package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.text

@Composable
fun ShadowBox(
    modifier: Modifier = Modifier,
    bgColor: Color = itemsBg,
    shadowOffsetX: Dp = 5.dp,
    shadowOffsetY: Dp = 5.dp,
    borderThickness: Dp = 2.dp,
    rounding: Int = 5,
    offset: Dp = 0.dp,
    alignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit,
    ) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if(shadowOffsetX != 0.dp || shadowOffsetY != 0.dp)
        {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(shadowOffsetX, shadowOffsetY)
                    .background(cardBorder, RoundedCornerShape(rounding))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(offset, offset)
                .background(bgColor, RoundedCornerShape(rounding))
                .border(borderThickness, text, RoundedCornerShape(rounding)),
            contentAlignment = alignment
        ) {
            content()
        }
    }
}
