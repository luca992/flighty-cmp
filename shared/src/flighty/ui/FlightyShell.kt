package flighty.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flighty.model.Flight
import flighty.platformName
import flighty.ui.components.AppIcons
import flighty.ui.components.MapBackdrop
import kotlinx.coroutines.launch

/**
 * The Flighty look shared by every host: map/globe backdrop, floating map
 * controls, and the persistent bottom sheet the content lives in. The
 * full-Compose app wraps its own navigation and tab bar around this; the
 * native-chrome iOS app instantiates one shell per SwiftUI destination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightyShell(
    backdropFlight: Flight?,
    detail: Boolean,
    contentAtTop: () -> Boolean,
    modifier: Modifier = Modifier,
    bottomOverlay: @Composable BoxScope.() -> Unit = {},
    sheetContent: @Composable (innerScrollEnabled: Boolean) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(FlightyColors.Space)) {
        val screenHeight = maxHeight
        val peekHeight = if (detail) screenHeight * 0.55f else screenHeight * 0.66f

        // Globe ↔ real map handoff switches instantly: the MapLibre map is
        // a native interop view, and Compose cannot alpha-composite interop
        // layers — crossfading it painted the whole backdrop black on iOS
        // for the duration of the fade. The map's own camera fly-in
        // animation carries the transition instead.
        MapBackdrop(
            flight = backdropFlight,
            detail = detail,
            mapHeightFraction = if (detail) 0.45f else 0.34f,
        )

        // Flighty's map controls sit over the backdrop but under the sheet:
        // they must disappear behind it as the sheet expands.
        MapControlsOverlay(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 6.dp, end = 10.dp),
        )

        val sheetState = rememberBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            // No Hidden: the sheet is structural chrome and must never be
            // dismissable (the new API's default enabledValues includes it).
            enabledValues = setOf(SheetValue.PartiallyExpanded, SheetValue.Expanded),
        )
        val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
        val scope = rememberCoroutineScope()

        // Material3's sheet only follows touch drags, and desktop wheel
        // events skip nested scroll entirely when the list can't consume
        // them (e.g. at the top). Raw scroll pointer events always arrive,
        // so drive the sheet from those: scroll up into the content
        // expands it, scroll down at the top collapses it. Desktop-only —
        // touch platforms must not pay for this in the drag hot path.
        val wheelSheetModifier = if (platformName() != "Desktop JVM") Modifier
        else Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type != PointerEventType.Scroll) continue
                    val dy = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                    if (dy < -0.2f &&
                        contentAtTop() &&
                        sheetState.currentValue == SheetValue.Expanded &&
                        sheetState.targetValue == SheetValue.Expanded
                    ) {
                        scope.launch { sheetState.partialExpand() }
                    } else if (dy > 0.2f &&
                        sheetState.currentValue == SheetValue.PartiallyExpanded &&
                        sheetState.targetValue == SheetValue.PartiallyExpanded
                    ) {
                        scope.launch { sheetState.expand() }
                    }
                }
            }
        }

        // Drag the sheet or scroll its content to grow it over the map,
        // Flighty-style; drag down to reveal more of the globe.
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = peekHeight,
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetContainerColor = FlightyColors.SheetBg,
            sheetShadowElevation = 0.dp,
            containerColor = Color.Transparent,
            sheetDragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(FlightyColors.Divider, RoundedCornerShape(50)),
                )
            },
            sheetContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight - 72.dp)
                        .then(wheelSheetModifier),
                ) {
                    // Inner scrolling engages only once the sheet is fully
                    // expanded: at peek, gestures are pure sheet drags.
                    // Gate on the SETTLED value only — flipping mid-gesture
                    // recomposes the scrollable out of the nested-scroll
                    // chain and makes collapse flings jump.
                    sheetContent(sheetState.currentValue == SheetValue.Expanded)
                }
            },
        ) { /* Map area — the backdrop behind the scaffold shows through. */ }

        bottomOverlay()
    }
}

@Composable
fun MapControlsOverlay(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        MapControlButton(AppIcons.Map, "Map style")
        MapControlButton(AppIcons.Cloud, "Weather")
        MapControlButton(AppIcons.Locate, "Live tracking")
    }
}

@Composable
private fun MapControlButton(icon: ImageVector, description: String) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(Color(0x66141922), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(15.dp),
        )
    }
}

/**
 * The share / alerts / more cluster plus the Add Return pill that floats over
 * the bottom of the flight-detail sheet, standing in for the tab bar there.
 */
@Composable
fun DetailActionBar(modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Surface(
            color = FlightyColors.CardBg,
            shape = RoundedCornerShape(50),
            shadowElevation = 6.dp,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                DetailActionIcon(AppIcons.Share, "Share")
                DetailActionIcon(AppIcons.BellOff, "Alerts")
                DetailActionIcon(AppIcons.More, "More")
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(
            color = FlightyColors.Blue,
            shape = RoundedCornerShape(50),
            shadowElevation = 6.dp,
        ) {
            Text(
                text = "Add Return",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 13.dp),
            )
        }
    }
}

@Composable
private fun DetailActionIcon(icon: ImageVector, description: String) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = FlightyColors.TextDark,
        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp).size(19.dp),
    )
}
