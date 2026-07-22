package flighty.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import flighty.model.Flight
import flighty.ui.FlightyColors

/**
 * The long-press context menu on a flight, mirroring Flighty's: share and
 * airline actions on top, destructive actions at the bottom. All mock.
 */
@Composable
fun FlightContextMenu(
    flight: Flight,
    expanded: Boolean,
    onDismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = FlightyColors.CardBg,
    ) {
        FlightMenuItems(flight, onDismiss)
    }
}

/**
 * The same menu presented from the detail screen's bottom action bar.
 * DropdownMenu cannot sit flush against a bottom-of-screen anchor — when the
 * flipped-up menu violates its built-in 48dp window margin it falls back to a
 * fixed window-relative position that ignores the anchor entirely — so this
 * hosts the items in a Popup with an exact "bottom sits above the anchor"
 * position provider.
 */
@Composable
fun FlightMenuAboveAnchor(
    flight: Flight,
    expanded: Boolean,
    onDismiss: () -> Unit,
) {
    if (!expanded) return
    val gapPx = with(LocalDensity.current) { 10.dp.roundToPx() }
    val positionProvider = remember(gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val x = anchorBounds.left
                    .coerceAtMost(windowSize.width - popupContentSize.width)
                    .coerceAtLeast(0)
                val y = (anchorBounds.top - popupContentSize.height - gapPx)
                    .coerceAtLeast(0)
                return IntOffset(x, y)
            }
        }
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        // Mirror DropdownMenu's entrance so the popup swap is invisible,
        // growing from the bottom-left corner where the anchor sits.
        val shown = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(
            visibleState = shown,
            enter = fadeIn(tween(120)) +
                scaleIn(tween(120), initialScale = 0.85f, transformOrigin = TransformOrigin(0f, 1f)),
            exit = fadeOut(tween(90)) + scaleOut(tween(90)),
        ) {
            Surface(
                color = FlightyColors.CardBg,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp,
            ) {
                Column(Modifier.width(IntrinsicSize.Max)) {
                    FlightMenuItems(flight, onDismiss)
                }
            }
        }
    }
}

@Composable
private fun FlightMenuItems(flight: Flight, onDismiss: () -> Unit) {
    MenuItem("Share Flight", icon = AppIcons.Share, onClick = onDismiss)
    MenuItem("Alternate Flights", icon = AppIcons.Plane, onClick = onDismiss)
    MenuItem("Contact ${flight.airline.name}", onClick = onDismiss)
    HorizontalDivider(color = FlightyColors.Divider)
    MenuItem("Open to ${flight.origin.code}", onClick = onDismiss)
    MenuItem("Open to ${flight.destination.code}", onClick = onDismiss)
    HorizontalDivider(color = FlightyColors.Divider)
    MenuItem("Move to Friends", icon = AppIcons.People, onClick = onDismiss)
    MenuItem("Report Data Issue", onClick = onDismiss)
    MenuItem("Archive Flight", onClick = onDismiss)
    MenuItem("Delete Flight", tint = FlightyColors.RedTime, onClick = onDismiss)
}

@Composable
private fun MenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    tint: Color = FlightyColors.TextDark,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(text = label, fontSize = 14.sp, color = tint)
        },
        trailingIcon = icon?.let { vector ->
            {
                val base = Modifier.size(16.dp)
                Icon(
                    imageVector = vector,
                    contentDescription = null,
                    tint = tint,
                    modifier = if (vector == AppIcons.Plane) base.rotate(45f) else base,
                )
            }
        },
        onClick = onClick,
    )
}
