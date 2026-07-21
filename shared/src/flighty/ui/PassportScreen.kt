package flighty.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flighty.model.Profile
import flighty.model.TravelStats
import flighty.ui.components.AppIcons
import flighty.ui.components.FlightyChip
import flighty.ui.components.ScreenHeader

@Composable
fun PassportScreen(
    stats: TravelStats,
    runningOn: String,
    profile: Profile,
    scrollState: ScrollState,
    scrollEnabled: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = scrollEnabled)
            .padding(horizontal = 16.dp),
    ) {
        ScreenHeader(title = "Passport", profile = profile, modifier = Modifier.padding(start = 4.dp, top = 18.dp))

        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlightyChip("All-Time", selected = true)
            FlightyChip("2026", selected = false)
        }

        Spacer(Modifier.height(14.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(FlightyColors.IndigoTop, FlightyColors.IndigoBottom)),
                    RoundedCornerShape(18.dp),
                )
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ALL-TIME FLIGHTY PASSPORT",
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(width = 8.dp, height = 10.dp)
                                .background(
                                    Color.White.copy(alpha = 0.55f),
                                    RoundedCornerShape(2.dp),
                                ),
                        )
                        Text(
                            text = " PASSPORT · PASS · PASAPORTE",
                            fontSize = 9.sp,
                            letterSpacing = 1.sp,
                            color = Color.White.copy(alpha = 0.55f),
                        )
                    }
                }
                Icon(
                    imageVector = AppIcons.Share,
                    contentDescription = "Share passport",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(15.dp),
                )
            }

            Spacer(Modifier.height(14.dp))
            Row {
                PassportStat("FLIGHTS", "${stats.flights}", "${stats.longHaul} Long Haul", Modifier.weight(1f))
                PassportStat("DISTANCE", "${formatThousands(stats.distanceKm)} km", stats.aroundWorld, Modifier.weight(1.4f))
            }
            Spacer(Modifier.height(12.dp))
            Row {
                PassportStat("FLIGHT TIME", stats.flightTime, null, Modifier.weight(1f))
                PassportStat("AIRPORTS", "${stats.airports}", null, Modifier.weight(1f))
                PassportStat("AIRLINES", "${stats.airlines}", null, Modifier.weight(1f))
            }

            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Text(
                    text = "All Flight Stats",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "›",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(FlightyColors.RedCardTop, FlightyColors.RedCardBottom)),
                    RoundedCornerShape(18.dp),
                )
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${stats.monthFlights}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = stats.monthLabel,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            Icon(
                imageVector = AppIcons.Share,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Running on $runningOn",
            fontSize = 11.sp,
            color = FlightyColors.TextGray,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 96.dp),
        )
    }
}

@Composable
private fun PassportStat(label: String, value: String, sub: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 9.sp,
            letterSpacing = 0.8.sp,
            color = Color.White.copy(alpha = 0.55f),
        )
        Text(
            text = value,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        if (sub != null) {
            Text(
                text = sub,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
    }
}

internal fun formatThousands(value: Int): String {
    val s = value.toString()
    val sb = StringBuilder()
    s.forEachIndexed { i, c ->
        sb.append(c)
        val remaining = s.length - 1 - i
        if (remaining > 0 && remaining % 3 == 0) sb.append(',')
    }
    return sb.toString()
}
