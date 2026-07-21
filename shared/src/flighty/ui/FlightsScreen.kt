package flighty.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flighty.model.Flight
import flighty.model.FlightStatus
import flighty.platformName
import flighty.ui.components.AirlineBadge
import flighty.ui.components.AppIcons
import flighty.ui.components.FlightContextMenu
import flighty.ui.components.LocalNativeFlightMenuHost
import flighty.ui.components.ScreenHeader
import flighty.vm.FlightsUiState

@Composable
fun FlightsScreen(
    state: FlightsUiState,
    scrollState: ScrollState,
    scrollEnabled: Boolean,
    onFlightClick: (Flight) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "My Flights",
            profile = state.profile,
            modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 18.dp),
        )
        // Scrolling only engages once the sheet is expanded: the per-frame
        // nested-scroll arbitration between an active inner scrollable and the
        // sheet drag is what made sheet gestures stutter on iOS.
        // Flighty lists live, upcoming, and past flights as one flat run of
        // compact rows — no day/section headers.
        Column(
            modifier = Modifier
                .verticalScroll(scrollState, enabled = scrollEnabled)
                .padding(top = 4.dp, bottom = 96.dp),
        ) {
            (state.upcoming + state.past).forEach { flight ->
                FlightRow(flight = flight, onClick = { onFlightClick(flight) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlightRow(flight: Flight, onClick: () -> Unit) {
    val nativeMenuHost = LocalNativeFlightMenuHost.current
    if (nativeMenuHost != null) {
        // A native host owns the long-press: report where this row sits so its
        // system context-menu interaction can hit-test and snapshot the row.
        DisposableEffect(flight.id) {
            onDispose { nativeMenuHost.removeRow(flight.id) }
        }
        Box(
            modifier = Modifier.onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                nativeMenuHost.updateRowBounds(
                    flight, bounds.left, bounds.top, bounds.right, bounds.bottom,
                )
            },
        ) {
            FlightRowBody(flight = flight, onClick = onClick, onLongClick = {})
        }
    } else {
        var showMenu by remember { mutableStateOf(false) }
        Box {
            FlightContextMenu(flight = flight, expanded = showMenu, onDismiss = { showMenu = false })
            FlightRowBody(flight = flight, onClick = onClick, onLongClick = { showMenu = true })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FlightRowBody(flight: Flight, onClick: () -> Unit, onLongClick: () -> Unit) {
    // The right-click watcher is desktop-only: it must not sit in the touch
    // gesture path on mobile, where every drag already threads through the
    // sheet drag + list scroll + clickable stack.
    val rightClickModifier = if (platformName() != "Desktop JVM") Modifier
    else Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                    onLongClick()
                }
            }
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .then(rightClickModifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        StatusBadge(flight.status)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AirlineBadge(flight.airline, size = 15)
                Text(
                    text = flight.number,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = FlightyColors.TextGray,
                    modifier = Modifier.padding(start = 6.dp),
                )
                Spacer(Modifier.weight(1f))
                val (label, value, valueTint) = trailingStatus(flight)
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = FlightyColors.TextGray,
                )
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = valueTint,
                )
            }
            Text(
                text = flight.cityRoute,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlightyColors.TextDark,
                modifier = Modifier.padding(top = 2.dp),
            )
            val timeTint = when {
                flight.late || flight.status == FlightStatus.Delayed -> FlightyColors.RedTime
                else -> FlightyColors.GreenTime
            }
            Row(modifier = Modifier.padding(top = 3.dp)) {
                EndpointStamp(flight.origin.code, flight.departTime, timeTint)
                Spacer(Modifier.width(12.dp))
                EndpointStamp(
                    code = flight.destination.code,
                    time = flight.arriveTime + if (flight.arrivesNextDay) " ⁺¹" else "",
                    tint = timeTint,
                )
            }
        }
    }
}

/** The trailing "Landing in 2h 31m" / "Arrived 7:33 PM" fragment of a row. */
private fun trailingStatus(flight: Flight): Triple<String, String, Color> = when (flight.status) {
    FlightStatus.InAir -> Triple(
        "Landing in ",
        flight.landingIn.orEmpty(),
        if (flight.late) FlightyColors.RedTime else FlightyColors.GreenTime,
    )
    FlightStatus.Landed -> Triple("Arrived ", flight.arriveTime, FlightyColors.TextDark)
    FlightStatus.Delayed -> Triple("Now departs ", flight.departTime, FlightyColors.RedTime)
    FlightStatus.Scheduled -> Triple("Departs ", flight.departTime, FlightyColors.GreenTime)
}

@Composable
private fun EndpointStamp(code: String, time: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(7.dp).background(tint, CircleShape))
        Text(
            text = "$code $time",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun StatusBadge(status: FlightStatus) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(46.dp)) {
        Box(
            modifier = Modifier.size(34.dp).background(FlightyColors.ChipBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (status) {
                FlightStatus.InAir -> Icon(
                    imageVector = AppIcons.Plane,
                    contentDescription = null,
                    tint = FlightyColors.Blue,
                    modifier = Modifier.size(18.dp),
                )
                FlightStatus.Landed -> Text(
                    text = "✓",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlightyColors.GreenTime,
                )
                FlightStatus.Delayed -> Text(
                    text = "!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlightyColors.RedTime,
                )
                FlightStatus.Scheduled -> Icon(
                    imageVector = AppIcons.Plane,
                    contentDescription = null,
                    tint = FlightyColors.TextGray,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            text = when (status) {
                FlightStatus.InAir -> "IN AIR"
                FlightStatus.Landed -> "ARRIVED"
                FlightStatus.Delayed -> "DELAYED"
                FlightStatus.Scheduled -> "ON TIME"
            },
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            color = FlightyColors.TextGray,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}
