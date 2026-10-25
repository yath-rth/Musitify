package com.yatharth.musitify.ui.elements.display

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.ui.elements.CustomCircularButton
import com.yatharth.musitify.ui.elements.handCursor
import com.yatharth.musitify.ui.elements.onRightClickOrLongPress
import com.yatharth.musitify.ui.theme.cardBorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

/**
 * How far (in item pitches) from the centre a card reaches the full side-card transform.
 * Android normalised by half the *screen* width (≈1.1 pitches on a phone, with a 0.5 dead zone);
 * a fixed pitch-based falloff keeps the same look on any window width.
 */
private const val FALLOFF_PITCHES = 1.5f

/**
 * Index of the visible item whose centre is closest to the viewport centre — the one true
 * "centred" card (Android's `findCenterItemIndex`, measured against the real viewport centre).
 */
fun findCenterItemIndex(listState: LazyListState): Int? {
    val layoutInfo = listState.layoutInfo
    val centre = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.width / 2f
    return layoutInfo.visibleItemsInfo.minByOrNull { item ->
        abs(item.offset + item.size / 2f - centre)
    }?.index
}

/**
 * Shared 3D cover-flow row used by [MusicCarousel] and [PlaylistCarousel].
 *
 * Same transforms as Android — scale 1 → 0.75, rotationY 0 → ±45°, alpha 1 → 0.5,
 * camera distance 12·density — but driven by a smooth signed distance (no 0.5 dead zone).
 * Tapping the centred card calls [onSelect]; tapping a side card scrolls it to the centre
 * (unless [selectsOffCentre] says otherwise). Long press or right click calls [onContext]
 * for items where [hasContext] is true.
 *
 * Desktop input: mouse wheel (vertical or horizontal) steps one card per notch,
 * click-and-drag scrolls with a snapping fling, and ‹ › buttons appear on hover.
 * When [focusIndex] changes the row animates to that index.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun CoverFlowCarousel(
    itemCount: Int,
    itemWidth: Dp,
    itemHeight: Dp,
    spacing: Dp,
    focusIndex: Int?,
    key: (Int) -> Any,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onContext: (Int) -> Unit = {},
    hasContext: (Int) -> Boolean = { true },
    selectsOffCentre: (Int) -> Boolean = { false },
    itemContent: @Composable (Int) -> Unit,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (focusIndex ?: 0).coerceIn(0, (itemCount - 1).coerceAtLeast(0))
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    val falloffPx = with(localDensity) { (itemWidth + spacing).toPx() } * FALLOFF_PITCHES
    val count by rememberUpdatedState(itemCount)

    var scrollJob by remember { mutableStateOf<Job?>(null) }
    var scrollTarget by remember { mutableStateOf<Int?>(null) }

    fun scrollTo(index: Int) {
        if (count == 0) return
        val target = index.coerceIn(0, count - 1)
        scrollTarget = target
        scrollJob?.cancel()
        scrollJob = scope.launch {
            listState.animateScrollToItem(target)
            scrollTarget = null
        }
    }

    fun step(direction: Int) {
        val base = scrollTarget ?: findCenterItemIndex(listState) ?: 0
        scrollTo(base + direction)
    }

    LaunchedEffect(focusIndex) {
        if (focusIndex != null && itemCount > 0) {
            listState.animateScrollToItem(focusIndex.coerceIn(0, itemCount - 1))
        }
    }

    /** Signed distance from the viewport centre, -1 (left) … 0 (centred) … +1 (right). */
    fun distanceOf(index: Int): Float {
        val layoutInfo = listState.layoutInfo
        val info = layoutInfo.visibleItemsInfo.find { it.index == index } ?: return 1f
        val centre = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.width / 2f
        val itemCentre = info.offset + info.size / 2f
        return ((itemCentre - centre) / falloffPx).coerceIn(-1f, 1f)
    }

    val hoverSource = remember { MutableInteractionSource() }
    val hovered by hoverSource.collectIsHoveredAsState()
    val centredIndex by remember { derivedStateOf { findCenterItemIndex(listState) ?: 0 } }

    var wheelAccumulator by remember { mutableStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(hoverSource)
    ) {
        val sidePadding = ((maxWidth - itemWidth) / 2).coerceAtLeast(0.dp)

        val dragState = rememberDraggableState { delta -> listState.dispatchRawDelta(-delta) }

        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            userScrollEnabled = false, // input handled below (mouse drag, wheel, touch)
            contentPadding = PaddingValues(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .onPointerEvent(PointerEventType.Scroll, PointerEventPass.Initial) { event ->
                    val change = event.changes.firstOrNull() ?: return@onPointerEvent
                    val delta = change.scrollDelta
                    val d = if (abs(delta.y) >= abs(delta.x)) delta.y else delta.x
                    if (d == 0f) return@onPointerEvent
                    event.changes.forEach { it.consume() }
                    // Touchpads send fractional deltas; step one card per whole notch.
                    wheelAccumulator += d
                    if (abs(wheelAccumulator) >= 1f) {
                        step(sign(wheelAccumulator).toInt())
                        wheelAccumulator = 0f
                    }
                }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = {
                        scrollJob?.cancel()
                        scrollTarget = null
                    },
                    onDragStopped = { velocity ->
                        listState.scroll { with(flingBehavior) { performFling(-velocity) } }
                    }
                )
        ) {
            items(count = itemCount, key = key) { index ->
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(itemHeight)
                        .graphicsLayer {
                            val d = distanceOf(index)
                            val a = abs(d)
                            val scale = lerp(1f, 0.75f, a)
                            scaleX = scale
                            scaleY = scale
                            rotationY = lerp(0f, 45f, d)
                            alpha = lerp(1f, 0.5f, a)
                            cameraDistance = 12f * density
                        }
                        .handCursor()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (selectsOffCentre(index) || findCenterItemIndex(listState) == index) {
                                onSelect(index)
                            } else {
                                scrollTo(index)
                            }
                        }
                        // Inner to clickable so a long press consumes the release first.
                        .onRightClickOrLongPress(enabled = hasContext(index)) { onContext(index) }
                ) {
                    itemContent(index)
                }
            }
        }

        AnimatedVisibility(
            visible = hovered && itemCount > 1 && centredIndex > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
        ) {
            CustomCircularButton(onClick = { step(-1) }) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous", tint = cardBorder)
            }
        }

        AnimatedVisibility(
            visible = hovered && itemCount > 1 && centredIndex < itemCount - 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
        ) {
            CustomCircularButton(onClick = { step(1) }) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Next", tint = cardBorder)
            }
        }
    }
}
