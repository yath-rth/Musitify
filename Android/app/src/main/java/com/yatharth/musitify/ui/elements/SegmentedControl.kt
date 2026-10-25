package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.ui.theme.accentColor1
import com.yatharth.musitify.ui.theme.adaptiveTypography
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.itemsBg
import com.yatharth.musitify.ui.theme.text

/**
 * Pick one of a few options, with every option visible and labelled.
 * Replaces switches whose label changed to describe the current state
 * ("CREATE NEW" / "ADD TO EXISTING"), which made it unclear what the switch
 * did. Styled per the Tracknix segmented control: tinted selected segment.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = accentColor1,
) {
    val shape = RoundedCornerShape(4.dp)
    Box(modifier = modifier.padding(end = 3.dp, bottom = 3.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(3.dp, 3.dp)
                .background(cardBorder, shape)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(itemsBg)
                .border(2.dp, cardBorder, shape)
                .selectableGroup()
        ) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) selectedColor.copy(alpha = 0.12f) else Color.Transparent)
                        .then(
                            if (index > 0) Modifier.border(2.dp, cardBorder, RoundedCornerShape(0.dp))
                            else Modifier
                        )
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(index) }
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = adaptiveTypography().labelMedium,
                        color = if (selected) selectedColor else text.copy(alpha = 0.4f),
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.ExtraLight,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
