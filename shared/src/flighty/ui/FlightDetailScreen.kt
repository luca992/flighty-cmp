package flighty.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flighty.model.Flight
import flighty.model.FlightStatus
import flighty.ui.components.AirlineBadge
import flighty.ui.components.AppIcons
import flighty.ui.components.FlightProgressBar
import flighty.ui.components.GateChip

/**
 * The light sheet content for a flight — the map above it is drawn by the
 * shared [flighty.ui.components.SpaceBackdrop].
 */
@Composable
fun FlightDetailScreen(
    flight: Flight,
    scrollState: ScrollState,
    scrollEnabled: Boolean,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = scrollEnabled)
            .padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            AirlineBadge(flight.airline, size = 24)
            Text(
                text = "${flight.number} · ${flight.dateLabel}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = FlightyColors.TextGray,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(FlightyColors.ChipBg, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = "Close",
                    tint = FlightyColors.TextDark,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Text(
            text = flight.cityRoute,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = FlightyColors.TextDark,
            modifier = Modifier.padding(top = 6.dp),
        )
        StatusHeadline(flight)

        Spacer(Modifier.height(14.dp))
        val timeTint = if (flight.late) FlightyColors.RedTime else flight.status.timeTint
        Surface(color = FlightyColors.CardBg, shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                EndpointBlock(
                    airportLine = "${flight.origin.code} · ${flight.origin.name}",
                    time = flight.departTime,
                    strikethroughTime = flight.scheduledDepartTime,
                    note = flight.departNote,
                    tint = timeTint,
                    gate = flight.departGate,
                    departure = true,
                    terminalLine = "Terminal ${flight.departTerminal}",
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 12.dp),
                ) {
                    HorizontalDivider(color = FlightyColors.Divider, modifier = Modifier.weight(1f))
                    Text(
                        text = "  ${flight.duration} · ${flight.distanceKm} km · ${flight.aircraft}  ",
                        fontSize = 11.sp,
                        color = FlightyColors.TextGray,
                    )
                    HorizontalDivider(color = FlightyColors.Divider, modifier = Modifier.weight(1f))
                }

                EndpointBlock(
                    airportLine = "${flight.destination.code} · ${flight.destination.name}",
                    time = flight.arriveTime + if (flight.arrivesNextDay) " ⁺¹" else "",
                    strikethroughTime = null,
                    note = flight.arriveNote,
                    tint = timeTint,
                    gate = flight.arriveGate,
                    departure = false,
                    terminalLine = "Terminal ${flight.arriveTerminal}" +
                        (flight.baggageClaim?.let { " · Bag $it" } ?: ""),
                )

                if (flight.status == FlightStatus.InAir) {
                    Spacer(Modifier.height(12.dp))
                    FlightProgressBar(flight.progress)
                    flight.altitudeFt?.let { alt ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${alt} ft · ${flight.speedKmh} km/h ground speed",
                            fontSize = 11.sp,
                            color = FlightyColors.TextGray,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile("BOOKING CODE", flight.bookingCode ?: "—", Modifier.weight(1f))
            InfoTile("SEAT", flight.seat ?: "—", Modifier.weight(1f))
            AddReturnButton(Modifier.weight(1f))
        }

        flight.delayForecast?.let { forecast ->
            Spacer(Modifier.height(10.dp))
            DelayForecastCard(forecast)
        }

        flight.inboundNote?.let { note ->
            Spacer(Modifier.height(10.dp))
            WheresMyPlaneCard(flight = flight, note = note)
        }

        Spacer(Modifier.height(10.dp))
        DetailedTimetableCard(flight)

        Spacer(Modifier.height(10.dp))
        GoodToKnowCard(flight)

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Mock data · built with Compose Multiplatform",
            fontSize = 11.sp,
            color = FlightyColors.TextGray,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun DelayForecastCard(forecast: flighty.model.DelayForecast) {
    Surface(color = FlightyColors.CardBg, shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = "ON-TIME PERFORMANCE",
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlightyColors.TextGray,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForecastChip("↗ ${forecast.early}%")
                ForecastChip("⏱ ${forecast.avgDelayMin}m")
                ForecastChip("✈ ${forecast.observed}")
            }
            val rows = listOf(
                Triple("Early", forecast.early, FlightyColors.GreenTime),
                Triple("On Time", forecast.onTime, Color(0xFF63C97E)),
                Triple("15m late", forecast.late15, FlightyColors.GateChip),
                Triple("30m late", forecast.late30, Color(0xFFFF9F0A)),
                Triple("45m+ late", forecast.late45Plus, FlightyColors.RedTime),
                Triple("Canceled", forecast.canceled, Color(0xFF8E1F2F)),
                Triple("Diverted", forecast.diverted, FlightyColors.TextGray),
            )
            val maxPct = rows.maxOf { it.second }.coerceAtLeast(1)
            rows.forEach { (label, pct, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = FlightyColors.TextGray,
                        modifier = Modifier.width(64.dp),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (pct.toFloat() / maxPct).coerceIn(0.02f, 1f))
                                .height(8.dp)
                                .background(color, RoundedCornerShape(50)),
                        )
                    }
                    Text(
                        text = "$pct%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FlightyColors.TextDark,
                        modifier = Modifier.width(38.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun WheresMyPlaneCard(flight: Flight, note: String) {
    Surface(color = FlightyColors.CardBg, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                text = "WHERE'S MY PLANE?",
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlightyColors.TextGray,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(FlightyColors.ChipBg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Plane,
                        contentDescription = null,
                        tint = FlightyColors.Blue,
                        modifier = Modifier.size(24.dp).rotate(90f),
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = flight.aircraft,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FlightyColors.TextDark,
                    )
                    flight.aircraftInfo?.let {
                        Text(text = it, fontSize = 11.sp, color = FlightyColors.TextGray)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (flight.late) "⚠ MINOR DELAYS" else "✓ NO ISSUE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = Color.White,
                modifier = Modifier
                    .background(
                        if (flight.late) Color(0xFFE8A013) else FlightyColors.GreenTime,
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = note,
                fontSize = 12.sp,
                color = FlightyColors.TextGray,
            )
        }
    }
}

@Composable
private fun DetailedTimetableCard(flight: Flight) {
    Surface(color = FlightyColors.CardBg, shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "DETAILED TIMETABLE",
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlightyColors.TextGray,
            )
            Text(
                text = "Scheduled, Estimated, Predicted, and Actual",
                fontSize = 11.sp,
                color = FlightyColors.TextGray,
            )
            TimetableRow(
                label = "Departure ${flight.origin.code}",
                scheduled = flight.scheduledDepartTime ?: flight.departTime,
                actual = flight.departTime,
                tint = if (flight.late) FlightyColors.RedTime else FlightyColors.GreenTime,
            )
            TimetableRow(
                label = "Arrival ${flight.destination.code}",
                scheduled = flight.arriveTime,
                actual = flight.arriveTime,
                tint = if (flight.late) FlightyColors.RedTime else FlightyColors.GreenTime,
            )
        }
    }
}

@Composable
private fun TimetableRow(label: String, scheduled: String, actual: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = FlightyColors.TextDark,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = actual,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint,
            )
            if (scheduled != actual) {
                Text(
                    text = "Scheduled $scheduled",
                    fontSize = 10.sp,
                    color = FlightyColors.TextGray,
                )
            }
        }
    }
}

@Composable
private fun ForecastChip(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = FlightyColors.TextDark,
        modifier = Modifier
            .background(FlightyColors.ChipBg, RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

@Composable
private fun GoodToKnowCard(flight: Flight) {
    Surface(color = FlightyColors.CardBg, shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "GOOD TO KNOW",
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlightyColors.TextGray,
            )
            GoodToKnowRow("Wi-Fi and power outlets available on this ${flight.aircraft}")
            GoodToKnowRow("Average taxi time at ${flight.destination.code} is 11 min")
            flight.baggageClaim?.let {
                GoodToKnowRow("Bags usually reach claim $it about 18 min after gate arrival")
            }
        }
    }
}

@Composable
private fun GoodToKnowRow(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "•",
            fontSize = 13.sp,
            color = FlightyColors.Blue,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = FlightyColors.TextDark,
        )
    }
}

@Composable
private fun StatusHeadline(flight: Flight) {
    val (prefix, value, tint) = when (flight.status) {
        FlightStatus.InAir -> Triple("Landing in ", flight.landingIn ?: "", FlightyColors.GreenTime)
        FlightStatus.Scheduled -> Triple("Departs at ", flight.departTime, FlightyColors.GreenTime)
        FlightStatus.Delayed -> Triple("Now departs at ", flight.departTime, FlightyColors.RedTime)
        FlightStatus.Landed -> Triple("Arrived ", flight.arriveTime, FlightyColors.GreenTime)
    }
    Row(modifier = Modifier.padding(top = 2.dp)) {
        Text(text = prefix, fontSize = 15.sp, color = FlightyColors.TextDark)
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
    }
}

@Composable
private fun EndpointBlock(
    airportLine: String,
    time: String,
    strikethroughTime: String?,
    note: String,
    tint: Color,
    gate: String?,
    departure: Boolean,
    terminalLine: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$airportLine ›",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlightyColors.TextGray,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (strikethroughTime != null) {
                    Text(
                        text = strikethroughTime,
                        fontSize = 16.sp,
                        color = FlightyColors.TextGray,
                        textDecoration = TextDecoration.LineThrough,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                Text(
                    text = time,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                )
            }
            Text(
                text = note,
                fontSize = 11.sp,
                color = if (tint == FlightyColors.RedTime) FlightyColors.RedTime else FlightyColors.TextGray,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (gate != null) {
                GateChip(gate, departure = departure)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = terminalLine,
                fontSize = 11.sp,
                color = FlightyColors.TextGray,
            )
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = FlightyColors.CardBg, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp,
                color = FlightyColors.TextGray,
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlightyColors.TextDark,
            )
            Text(
                text = "Tap to Edit",
                fontSize = 9.sp,
                color = FlightyColors.TextGray,
            )
        }
    }
}

@Composable
private fun AddReturnButton(modifier: Modifier = Modifier) {
    Surface(
        color = FlightyColors.Blue,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.clickable(onClick = { /* mock */ }),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 18.dp),
        ) {
            Text(
                text = "Add Return",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}
