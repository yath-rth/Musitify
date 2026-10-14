package com.yatharth.musitify.ui.elements

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg

@Composable
fun CustomButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    bgColor: Color = itemsBg,
    content: @Composable () -> Unit = {},
) {
    var pressed by remember { mutableStateOf(false) }

    val offset by animateDpAsState(
        targetValue = if (pressed) 2.dp else 0.dp,
        animationSpec = tween(20),
        label = ""
    )

    Box(
        modifier = modifier
            .wrapContentSize()
            .animateContentSize()
            .padding(4.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(
                    shape = RoundedCornerShape(4.dp),
                    color = cardBorder
                )
        )

        Card(
            Modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .offset(x = offset, y = offset),
            colors = CardDefaults.cardColors(
                containerColor = itemsBg
            ),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(
                1.5.dp,
                color = cardBorder
            ),
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .background(color = bgColor)
            ) { content() }
        }
    }
}

@Composable
fun CustomCircularButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    bgColor: Color = itemsBg,
    content: @Composable () -> Unit = {},
) {
    var pressed by remember { mutableStateOf(false) }

    val offset by animateDpAsState(
        targetValue = if (pressed) 3.dp else 0.dp,
        animationSpec = tween(20),
        label = ""
    )

    Box(
        modifier = modifier
            .wrapContentSize()
            .animateContentSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(
                    shape = CircleShape,
                    color = cardBorder
                )
        )

        Card(
            Modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .offset(x = offset, y = offset),
            colors = CardDefaults.cardColors(
                containerColor = itemsBg
            ),
            shape = CircleShape,
            border = BorderStroke(
                1.dp,
                color = cardBorder
            ),
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .background(color = bgColor),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier.padding(4.dp),
                    contentAlignment = Alignment.Center
                ) { content() }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun preview() {
    CustomCircularButton(
        onClick = {

        }
    ) {
        Icon(
            imageVector = Icons.Outlined.PlayArrow,
            contentDescription = null
        )
    }
}