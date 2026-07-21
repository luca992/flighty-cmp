package flighty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flighty.model.Airline
import flighty.model.FlightStatus
import flighty.ui.FlightyColors
import flighty.ui.label
import flighty.ui.tint

private fun materialGlyph(
    name: String,
    block: PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.White), pathFillType = PathFillType.NonZero, pathBuilder = block)
}.build()

/**
 * The few Material Design glyphs the app needs, built by hand from their path data
 * (Apache 2.0) — the toolchain's compose catalog doesn't expose material-icons-core,
 * and this keeps the dependency list at zero extras.
 */
object AppIcons {

    val Plane: ImageVector = materialGlyph("Plane") {
        moveTo(21f, 16f)
        verticalLineToRelative(-2f)
        lineToRelative(-8f, -5f)
        verticalLineTo(3.5f)
        curveTo(13f, 2.67f, 12.33f, 2f, 11.5f, 2f)
        reflectiveCurveTo(10f, 2.67f, 10f, 3.5f)
        verticalLineTo(9f)
        lineToRelative(-8f, 5f)
        verticalLineToRelative(2f)
        lineToRelative(8f, -2.5f)
        verticalLineTo(19f)
        lineToRelative(-2f, 1.5f)
        verticalLineTo(22f)
        lineToRelative(3.5f, -1f)
        lineToRelative(3.5f, 1f)
        verticalLineToRelative(-1.5f)
        lineTo(13f, 19f)
        verticalLineToRelative(-5.5f)
        lineTo(21f, 16f)
        close()
    }

    val Back: ImageVector = materialGlyph("Back") {
        moveTo(20f, 11f)
        horizontalLineTo(7.83f)
        lineToRelative(5.59f, -5.59f)
        lineTo(12f, 4f)
        lineToRelative(-8f, 8f)
        lineToRelative(8f, 8f)
        lineToRelative(1.41f, -1.41f)
        lineTo(7.83f, 13f)
        horizontalLineTo(20f)
        verticalLineToRelative(-2f)
        close()
    }

    val Close: ImageVector = materialGlyph("Close") {
        moveTo(19f, 6.41f)
        lineTo(17.59f, 5f)
        lineTo(12f, 10.59f)
        lineTo(6.41f, 5f)
        lineTo(5f, 6.41f)
        lineTo(10.59f, 12f)
        lineTo(5f, 17.59f)
        lineTo(6.41f, 19f)
        lineTo(12f, 13.41f)
        lineTo(17.59f, 19f)
        lineTo(19f, 17.59f)
        lineTo(13.41f, 12f)
        close()
    }

    val Person: ImageVector = materialGlyph("Person") {
        moveTo(12f, 12f)
        curveToRelative(2.21f, 0f, 4f, -1.79f, 4f, -4f)
        reflectiveCurveToRelative(-1.79f, -4f, -4f, -4f)
        reflectiveCurveToRelative(-4f, 1.79f, -4f, 4f)
        reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
        close()
        moveTo(12f, 14f)
        curveToRelative(-2.67f, 0f, -8f, 1.34f, -8f, 4f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(16f)
        verticalLineToRelative(-2f)
        curveToRelative(0f, -2.66f, -5.33f, -4f, -8f, -4f)
        close()
    }

    val People: ImageVector = materialGlyph("People") {
        moveTo(16f, 11f)
        curveToRelative(1.66f, 0f, 2.99f, -1.34f, 2.99f, -3f)
        reflectiveCurveTo(17.66f, 5f, 16f, 5f)
        curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
        reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
        close()
        moveTo(8f, 11f)
        curveToRelative(1.66f, 0f, 2.99f, -1.34f, 2.99f, -3f)
        reflectiveCurveTo(9.66f, 5f, 8f, 5f)
        curveTo(6.34f, 5f, 5f, 6.34f, 5f, 8f)
        reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
        close()
        moveTo(8f, 13f)
        curveToRelative(-2.33f, 0f, -7f, 1.17f, -7f, 3.5f)
        verticalLineTo(19f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(-2.5f)
        curveToRelative(0f, -2.33f, -4.67f, -3.5f, -7f, -3.5f)
        close()
        moveTo(16f, 13f)
        curveToRelative(-0.29f, 0f, -0.62f, 0.02f, -0.97f, 0.05f)
        curveToRelative(1.16f, 0.84f, 1.97f, 1.97f, 1.97f, 3.45f)
        verticalLineTo(19f)
        horizontalLineToRelative(6f)
        verticalLineToRelative(-2.5f)
        curveToRelative(0f, -2.33f, -4.67f, -3.5f, -7f, -3.5f)
        close()
    }

    val Search: ImageVector = materialGlyph("Search") {
        moveTo(15.5f, 14f)
        horizontalLineToRelative(-0.79f)
        lineToRelative(-0.28f, -0.27f)
        curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
        curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
        reflectiveCurveTo(3f, 5.91f, 3f, 9.5f)
        reflectiveCurveTo(5.91f, 16f, 9.5f, 16f)
        curveToRelative(1.61f, 0f, 3.09f, -0.59f, 4.23f, -1.57f)
        lineToRelative(0.27f, 0.28f)
        verticalLineToRelative(0.79f)
        lineToRelative(5f, 4.99f)
        lineTo(20.49f, 19f)
        lineToRelative(-4.99f, -5f)
        close()
        moveTo(9.5f, 14f)
        curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
        reflectiveCurveTo(7.01f, 5f, 9.5f, 5f)
        reflectiveCurveTo(14f, 7.01f, 14f, 9.5f)
        reflectiveCurveTo(11.99f, 14f, 9.5f, 14f)
        close()
    }

    val Share: ImageVector = materialGlyph("Share") {
        moveTo(18f, 16.08f)
        curveToRelative(-0.76f, 0f, -1.44f, 0.3f, -1.96f, 0.77f)
        lineTo(8.91f, 12.7f)
        curveToRelative(0.05f, -0.23f, 0.09f, -0.46f, 0.09f, -0.7f)
        reflectiveCurveToRelative(-0.04f, -0.47f, -0.09f, -0.7f)
        lineToRelative(7.05f, -4.11f)
        curveToRelative(0.54f, 0.5f, 1.25f, 0.81f, 2.04f, 0.81f)
        curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
        reflectiveCurveToRelative(-1.34f, -3f, -3f, -3f)
        reflectiveCurveToRelative(-3f, 1.34f, -3f, 3f)
        curveToRelative(0f, 0.24f, 0.04f, 0.47f, 0.09f, 0.7f)
        lineTo(8.04f, 9.81f)
        curveTo(7.5f, 9.31f, 6.79f, 9f, 6f, 9f)
        curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
        reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
        curveToRelative(0.79f, 0f, 1.5f, -0.31f, 2.04f, -0.81f)
        lineToRelative(7.12f, 4.16f)
        curveToRelative(-0.05f, 0.21f, -0.08f, 0.43f, -0.08f, 0.65f)
        curveToRelative(0f, 1.61f, 1.31f, 2.92f, 2.92f, 2.92f)
        curveToRelative(1.61f, 0f, 2.92f, -1.31f, 2.92f, -2.92f)
        reflectiveCurveToRelative(-1.31f, -2.92f, -2.92f, -2.92f)
        close()
    }
}

@Composable
fun AirlineBadge(airline: Airline, size: Int = 30, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(Color(airline.color), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = airline.code,
            color = Color.White,
            fontSize = (size * 0.36).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun StatusText(status: FlightStatus, modifier: Modifier = Modifier, text: String = status.label) {
    Text(
        text = text,
        color = status.tint,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

/** Flighty's yellow gate badge, e.g. "↗ B22". */
@Composable
fun GateChip(gate: String, departure: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = (if (departure) "↗ " else "↘ ") + gate,
        color = FlightyColors.GateChipText,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(FlightyColors.GateChip, RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Thin track with a plane marker sitting at [progress] along it. */
@Composable
fun FlightProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(progress.coerceIn(0.02f, 0.98f))
                .height(3.dp)
                .background(FlightyColors.Blue, RoundedCornerShape(50)),
        )
        Icon(
            imageVector = AppIcons.Plane,
            contentDescription = null,
            tint = FlightyColors.Blue,
            modifier = Modifier.size(16.dp).rotate(90f),
        )
        Box(
            modifier = Modifier
                .weight((1f - progress).coerceIn(0.02f, 0.98f))
                .height(3.dp)
                .background(FlightyColors.Divider, RoundedCornerShape(50)),
        )
    }
}
