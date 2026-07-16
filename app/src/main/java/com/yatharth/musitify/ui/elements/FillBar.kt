package com.yatharth.musitify.ui.elements

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.secondaryBg
import com.yatharth.musitify.ui.theme.text
import kotlin.math.abs

@Composable
fun FillBar(
    modifier: Modifier = Modifier,
    label: String? = null,
    secondaryLabel: String? = "",
    color: Color = accentColor1,
    value: Double,
    max: Double,
    remaining: Double? = null,
    emptyColor: Color = secondaryBg,
    secondaryFillColor: Color = accentColor1,
    bgColor: Color? = null
) {
    val spent = remaining?.toInt() ?: (max - abs(value)).coerceAtLeast(0.0).toInt()
    val progress = if (max > 0) (abs(value) / max).coerceIn(0.0, 1.0) else 1.0
//    Log.d("test", "$value $max ${value/max}")

    val animatedProgress by animateFloatAsState(
        targetValue = progress.toFloat(),
        label = "budgetProgress"
    )

    val fillColor = when {
        progress > .80 -> color
        progress > .50 -> secondaryFillColor
        else -> emptyColor
    }

    Column(
        modifier = modifier,
    )
    {
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically
        )
        {
            if (label != null) {
                Text(
                    text = label,
                    color = text,
                    style = adaptiveTypography().bodySmall,
                    fontWeight = FontWeight.Medium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.weight(1f))

//            if (secondaryLabel != null) {
//                Text(
//                    text = "₹${spent} left of ${Formater.getFormattedMoneyNoSign(max)}",
//                    color = text,
//                    style = adaptiveTypography().bodySmall,
//                    fontWeight = FontWeight.SemiBold
//                )
//            }
        }

        Spacer(modifier = Modifier.padding(1.dp))

        Box(
            modifier = Modifier
                .height(12.dp)
        )
        {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 2.dp, y = 3.dp)
                    .background(
                        shape = RoundedCornerShape(20),
                        color = cardBorder
                    )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
            ) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.5.dp,
                            color = cardBorder,
                            shape = RoundedCornerShape(20)
                        )
                        .clip(RoundedCornerShape(20))
                        .background(secondaryBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20))
                            .background(bgColor ?: fillColor.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .background(
                                    color = accentColor1,
                                )
                        )
                    }
                }
            }
        }
    }
}