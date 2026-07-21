package flighty.ui.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
