package flighty.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flighty.data.MockFlights
import flighty.model.Flight
import flighty.model.FlightStatus
import flighty.platformName
import flighty.ui.components.AirlineBadge
import flighty.ui.components.FlightContextMenu
import flighty.ui.components.FlightProgressBar
import flighty.ui.components.GateChip
import flighty.ui.components.StatusText

@Composable
fun FlightsScreen(
    scrollState: ScrollState,
    scrollEnabled: Boolean,
    onFlightClick: (Flight) -> Unit,
) {
    var showPast by remember { mutableStateOf(false) }
    val flights = if (showPast) MockFlights.past else MockFlights.upcoming

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
        ) {
            Text(
                text = "My Flights",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = FlightyColors.TextDark,
                modifier = Modifier.weight(1f),
            )
            SegmentedToggle(showPast = showPast, onChange = { showPast = it })
        }
        // Scrolling only engages once the sheet is expanded: the per-frame
        // nested-scroll arbitration between an active inner scrollable and the
        // sheet drag is what made sheet gestures stutter on iOS.
        Column(
            modifier = Modifier
                .verticalScroll(scrollState, enabled = scrollEnabled)
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            var lastDay: String? = null
            flights.forEach { flight ->
                if (flight.dayLabel != lastDay) {
                    lastDay = flight.dayLabel
                    Text(
                        text = flight.dayLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        color = if (flight.dayLabel == "LIVE") FlightyColors.Blue else FlightyColors.TextGray,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                    )
                }
                FlightCard(flight = flight, onClick = { onFlightClick(flight) })
            }
        }
    }
}

@Composable
private fun SegmentedToggle(
    showPast: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(FlightyColors.ChipBg, RoundedCornerShape(50))
            .padding(3.dp),
    ) {
        SegmentedButton("Upcoming", selected = !showPast) { onChange(false) }
        SegmentedButton("Past", selected = showPast) { onChange(true) }
    }
}

@Composable
private fun SegmentedButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) FlightyColors.CardBg else FlightyColors.ChipBg,
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) FlightyColors.TextDark else FlightyColors.TextGray,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlightCard(flight: Flight, onClick: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        FlightContextMenu(flight = flight, expanded = showMenu, onDismiss = { showMenu = false })
        FlightCardBody(flight = flight, onClick = onClick, onLongClick = { showMenu = true })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FlightCardBody(flight: Flight, onClick: () -> Unit, onLongClick: () -> Unit) {
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
    Surface(
        color = FlightyColors.CardBg,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .then(rightClickModifier),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AirlineBadge(flight.airline, size = 26)
                Text(
                    text = "${flight.number} · ${flight.dateLabel}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = FlightyColors.TextGray,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                StatusText(flight.status)
            }

            Text(
                text = flight.cityRoute,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = FlightyColors.TextDark,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.height(10.dp))
            val timeTint = if (flight.late) FlightyColors.RedTime else flight.status.timeTint
            Row(verticalAlignment = Alignment.Top) {
                EndpointTime(
                    code = flight.origin.code,
                    terminal = flight.departTerminal,
                    time = flight.departTime,
                    strikethroughTime = flight.scheduledDepartTime,
                    tint = timeTint,
                    alignEnd = false,
                    modifier = Modifier.weight(1f),
                )
                EndpointTime(
                    code = flight.destination.code,
                    terminal = flight.arriveTerminal,
                    time = flight.arriveTime + if (flight.arrivesNextDay) " ⁺¹" else "",
                    strikethroughTime = null,
                    tint = timeTint,
                    alignEnd = true,
                    modifier = Modifier.weight(1f),
                )
            }

            if (flight.status == FlightStatus.InAir) {
                Spacer(Modifier.height(10.dp))
                FlightProgressBar(flight.progress)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Landing in ${flight.landingIn}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FlightyColors.Blue,
                )
            } else {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GateChip(flight.departGate, departure = true)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Terminal ${flight.departTerminal}",
                        fontSize = 12.sp,
                        color = FlightyColors.TextGray,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = flight.duration,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FlightyColors.TextGray,
                    )
                }
            }
        }
    }
}

@Composable
private fun EndpointTime(
    code: String,
    terminal: String,
    time: String,
    strikethroughTime: String?,
    tint: androidx.compose.ui.graphics.Color,
    alignEnd: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (strikethroughTime != null) {
                Text(
                    text = strikethroughTime,
                    fontSize = 13.sp,
                    color = FlightyColors.TextGray,
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
            Text(
                text = time,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
        }
        Text(
            text = "$code · Terminal $terminal",
            fontSize = 12.sp,
            color = FlightyColors.TextGray,
        )
    }
}
