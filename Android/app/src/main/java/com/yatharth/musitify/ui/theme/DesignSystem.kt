package com.yatharth.musitify.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design System Tool
 * 
 * A reusable utility class that encapsulates the Tracknix UI design patterns.
 * Use this to create consistent UI elements with the same design across any language or framework.
 * 
 * Design DNA:
 * - Hard shadow boxes (offset background, not drop shadow)
 * - Thick borders (1.5-2dp) in text color
 * - Sage green color palette
 * - Minimal rounding (3-8dp)
 * - ExtraLight labels, ExtraBold values
 */

// ============================================================================
// CONFIGURATION
// ============================================================================

/** Shadow box offset for depth effect (4dp diagonal) */
const val SHADOW_OFFSET_DP = 4

/** Card border thickness (2dp) */
const val BORDER_THICKNESS_DP = 2

/** Card corner radius (5dp) */
const val CARD_RADIUS_DP = 5

/** Button press animation duration (50ms) */
const val BUTTON_ANIMATION_MS = 50

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Creates a shadow box wrapper with offset shadow and border
 * @param modifier The modifier for the container
 * @param bgColor Background color for the content box (default: light sage)
 * @param shadowOffset Offset for the shadow box (default: 4dp)
 * @param borderThickness Border thickness (default: 2dp)
 * @param rounding Corner radius (default: 5dp)
 * @param content The content to display
 */
@Composable
fun designSystemShadowBox(
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xFFF8FFFF),
    shadowOffset: Int = SHADOW_OFFSET_DP,
    borderThickness: Int = BORDER_THICKNESS_DP,
    rounding: Int = CARD_RADIUS_DP,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Shadow offset box - creates depth
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(shadowOffset.dp, shadowOffset.dp)
                .background(Color(0xFF1A1A2E), RoundedCornerShape(rounding.dp))
        )
        // Content box with border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(0.dp, 0.dp)
                .background(bgColor, RoundedCornerShape(rounding.dp))
                .border(borderThickness.dp, Color(0xFF1A1A2E), RoundedCornerShape(rounding.dp))
        ) {
            content()
        }
    }
}

/**
 * Creates a clickable button with press animation and border
 * @param modifier The modifier for the button
 * @param onClick Click handler
 * @param bgColor Background color (default: light sage)
 * @param text Text to display
 * @param borderRadius Corner radius (default: 10dp)
 * @param padding Button padding (horizontal, vertical)
 * @param textColor Text color (default: near black)
 */
@Composable
fun designSystemButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    bgColor: Color = Color(0xFFF8FFFF),
    text: String,
    borderRadius: Int = 10,
    padding: Pair<Float, Float> = Pair(16f, 12f),
    textColor: Color = Color(0xFF1A1A2E)
) {
    val offset by animateDpAsState(
        targetValue = 4.dp,
        animationSpec = tween(BUTTON_ANIMATION_MS),
        label = ""
    )

    Box(
        modifier = modifier
            .wrapContentWidth()
            .wrapContentHeight()
            .offset(x = offset, y = offset)
            .clickable(onClick = onClick)
    ) {
        // Shadow offset box
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(Color(0xFF1A1A2E), RoundedCornerShape(4.dp))
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            shape = RoundedCornerShape(borderRadius.dp),
            border = BorderStroke(1.5.dp, Color(0xFF1A1A2E))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor.copy(alpha = 0.8f))
            ) {
                Text(
                    text = text,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(padding.first.dp, padding.second.dp)
                )
            }
        }
    }
}

/**
 * Creates a switch toggle with press animation
 * @param checked Whether the switch is on
 * @param onCheckedChange Callback when state changes
 * @param trackHeight Height of the track (default: 26dp)
 * @param thumbSize Size of the thumb (default: 18dp)
 */
@Composable
fun designSystemSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    trackHeight: Int = 26,
    thumbSize: Int = 18
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(120),
        label = "switchThumb"
    )

    val trackColor = if (checked) Color(0xFF52AB2E).copy(alpha = 0.15f)
    else Color(0xFFF8FFFF)

    Box(
        modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)
    ) {
        // Container shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(Color(0xFF1A1A2E), RoundedCornerShape(4.dp))
        )
        // Track
        Box(
            modifier = Modifier
                .size(48.dp)
                .height(trackHeight.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(trackColor)
                .border(2.dp, Color(0xFF1A1A2E), RoundedCornerShape(4.dp))
                .clickable { onCheckedChange(!checked) }
                .padding(4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Thumb
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumbSize.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (checked) Color(0xFF52AB2E)
                        else Color(0xFF1A1A2E).copy(alpha = 0.2f)
                    )
                    .border(1.5.dp, Color(0xFF1A1A2E), RoundedCornerShape(2.dp))
            )
        }
    }
}

/**
 * Creates a segmented control for mode selection
 * @param selected The currently selected option
 * @param onSelect Callback when selection changes
 * @param options List of available options
 * @param selectedColor Color for selected state (default: coral red)
 */
@Composable
fun designSystemSegmentedControl(
    selected: String,
    onSelect: (String) -> Unit,
    options: List<String>,
    selectedColor: Color = Color(0xFFCB3D38)
) {
    val selectedTextColor = selectedColor
    val unselectedTextColor = Color(0xFF1A1A2E).copy(alpha = 0.4f)

    Box(
        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
    ) {
        // Shadow offset
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(Color(0xFF1A1A2E), RoundedCornerShape(4.dp))
        )
        // Segmented container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF8FFFF), RoundedCornerShape(4.dp))
                .border(2.dp, Color(0xFF1A1A2E), RoundedCornerShape(4.dp)),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selected == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) selectedColor.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .then(
                            if (index > 0) Modifier.border(
                                width = 2.dp,
                                color = Color(0xFF1A1A2E),
                                shape = RoundedCornerShape(0.dp)
                            ) else Modifier
                        )
                        .clickable(onClick = { onSelect(option) })
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.ExtraLight,
                        color = if (isSelected) selectedTextColor else unselectedTextColor,
                        letterSpacing = 1.2.sp,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Creates a progress bar for budget/progress tracking
 * @param value Current value
 * @param max Maximum value
 * @param label Optional label text
 * @param progressHeight Height of progress bar (default: 12dp)
 * @param accentColor Color for high usage (default: coral red)
 * @param secondaryColor Color for medium usage (default: golden amber)
 * @param emptyColor Color for low usage (default: sage green)
 */
@Composable
fun designSystemProgressBar(
    value: Float,
    max: Float,
    label: String? = null,
    progressHeight: Int = 12,
    accentColor: Color = Color(0xFFCB3D38),
    secondaryColor: Color = Color(0xFFE7C277),
    emptyColor: Color = Color(0xFFDDE3DB),
    labelColor: Color = Color(0xFF1A1A2E)
) {
    val progress = (value / max).coerceIn(0f, 1f)
    val fillColor = when {
        progress > 0.80f -> accentColor
        progress > 0.50f -> secondaryColor
        else -> emptyColor
    }

    Column {
        // Label
        label?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = it,
                    color = labelColor,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Spacer(modifier = Modifier.height(1.dp))
        // Progress bar container
        Box(
            modifier = Modifier.height(progressHeight.dp)
        ) {
            // Shadow offset
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 2.dp, y = 3.dp)
                    .background(Color(0xFF1A1A2E), CircleShape)
            )
            Row {
                // Background
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.5.dp,
                            Color(0xFF1A1A2E),
                            CircleShape
                        )
                        .clip(CircleShape)
                        .background(emptyColor)
                ) {
                    // Progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .background(fillColor)
                    )
                }
            }
        }
    }
}

/**
 * Creates a text field with edit/done icons
 * @param value Current text value
 * @param onValueChange Callback for text changes
 * @param label Label prefix
 * @param borderRadius Corner radius (default: 5dp)
 */
@Composable
fun designSystemTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    borderRadius: Int = 5
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Shadow offset
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(Color(0xFF1A1A2E), RoundedCornerShape(4.dp))
        )
        Card(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FFFF)),
            shape = RoundedCornerShape(borderRadius.dp),
            border = BorderStroke(1.5.dp, Color(0xFF1A1A2E))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Label prefix
                label?.let {
                    Text(
                        text = "$it: ",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF1A1A2E)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                }
                // Text field
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    interactionSource = interactionSource,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = if (isPressed) Color(0xFF52AB2E)
                        else Color(0xFF1A1A2E).copy(alpha = 0.7f)
                    ),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FFFF),
                        unfocusedContainerColor = Color(0xFFF8FFFF),
                        focusedIndicatorColor = Color(0xFFF8FFFF),
                        unfocusedIndicatorColor = Color(0xFFF8FFFF)
                    )
                )
                Spacer(modifier = Modifier.padding(8.dp))
                // Done icon when focused
                if (isPressed) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Done",
                        tint = Color(0xFF52AB2E),
                        modifier = Modifier.size(18.dp)
                            .clickable { onValueChange("") }
                    )
                }
                // Edit icon
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit",
                    tint = Color(0xFF1A1A2E),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ============================================================================
// EXAMPLE USAGE
// ============================================================================

/**
 * Example composable showing all design system components
 * Call this to preview the design system elements
 */
@Composable
fun DesignSystemPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Shadow Box Example
        designSystemShadowBox {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This is a shadow box", fontWeight = FontWeight.Bold)
                Text("With offset depth effect")
            }
        }
        // Button Example
        designSystemButton(
            onClick = {},
            text = "Click Me",
            textColor = Color(0xFF1A1A2E)
        )
        // Switch Example
        designSystemSwitch(
            checked = true,
            onCheckedChange = {}
        )
        // Segmented Control Example
        designSystemSegmentedControl(
            selected = "Monthly",
            onSelect = {},
            options = listOf("Daily", "Monthly", "Weekly")
        )
        // Progress Bar Example
        designSystemProgressBar(
            value = 65f,
            max = 100f,
            label = "65% Spent"
        )
        // Text Field Example
        designSystemTextField(
            value = "1000",
            onValueChange = {},
            label = "Budget"
        )
    }
}
