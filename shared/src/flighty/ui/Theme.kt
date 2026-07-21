package flighty.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import flighty.model.FlightStatus

/**
 * Flighty look: a dark space/map backdrop with a light sheet floating over it.
 */
object FlightyColors {
    // Space / map backdrop
    val Space = Color(0xFF04060C)
    val SpaceGlow = Color(0xFF12264D)
    val Horizon = Color(0xFF3E6EDB)
    val Route = Color(0xFF4D8DFF)
    val CityLight = Color(0xFFE8C36A)

    // Light sheet
    val SheetBg = Color(0xFFF2F2F5)
    val CardBg = Color(0xFFFFFFFF)
    val Divider = Color(0xFFE7E7EB)
    val ChipBg = Color(0xFFE8E8EC)
    val TextDark = Color(0xFF121216)
    val TextGray = Color(0xFF85868C)

    // Semantics (Flighty: green = on time, red = late)
    val GreenTime = Color(0xFF13A452)
    val RedTime = Color(0xFFE53B41)
    val Blue = Color(0xFF2E6FF2)
    val GateChip = Color(0xFFFFD426)
    val GateChipText = Color(0xFF6B4E00)

    // Passport cards
    val IndigoTop = Color(0xFF46429B)
    val IndigoBottom = Color(0xFF232055)
    val RedCardTop = Color(0xFFB3122F)
    val RedCardBottom = Color(0xFF6E0A1E)
}

val FlightStatus.tint: Color
    get() = when (this) {
        FlightStatus.Scheduled -> FlightyColors.GreenTime
        FlightStatus.Delayed -> FlightyColors.RedTime
        FlightStatus.InAir -> FlightyColors.Blue
        FlightStatus.Landed -> FlightyColors.TextGray
    }

/** Color of the big departure/arrival times, Flighty-style. */
val FlightStatus.timeTint: Color
    get() = when (this) {
        FlightStatus.Scheduled -> FlightyColors.GreenTime
        FlightStatus.Delayed -> FlightyColors.RedTime
        FlightStatus.InAir -> FlightyColors.GreenTime
        FlightStatus.Landed -> FlightyColors.GreenTime
    }

val FlightStatus.label: String
    get() = when (this) {
        FlightStatus.Scheduled -> "On Time"
        FlightStatus.Delayed -> "Delayed"
        FlightStatus.InAir -> "In Air"
        FlightStatus.Landed -> "Landed"
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlightyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = FlightyColors.Blue,
            onPrimary = Color.White,
            background = FlightyColors.Space,
            onBackground = FlightyColors.TextDark,
            surface = FlightyColors.CardBg,
            onSurface = FlightyColors.TextDark,
            surfaceVariant = FlightyColors.SheetBg,
            onSurfaceVariant = FlightyColors.TextGray,
            outline = FlightyColors.Divider,
        ),
        content = content,
    )
}
