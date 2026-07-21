package flighty.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flighty.model.AddFlightSuggestion
import flighty.ui.components.AppIcons

/**
 * The Add Flight search content. The full-Compose app hosts it in a
 * ModalBottomSheet; the native-chrome iOS app hosts it in a system sheet,
 * which provides its own dismissal (pass a null [onDismiss] to hide the X).
 */
@Composable
fun AddFlightContent(
    suggestions: List<AddFlightSuggestion>,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Add Flight",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = FlightyColors.TextDark,
                modifier = Modifier.weight(1f),
            )
            if (onDismiss != null) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(FlightyColors.ChipBg, CircleShape)
                        .clickable(onClick = onDismiss),
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
        }
        Text(
            text = "Enter airline, airport, or flight",
            fontSize = 13.sp,
            color = FlightyColors.TextGray,
            modifier = Modifier.padding(top = 2.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .background(FlightyColors.ChipBg, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Flair, YYZ, or F8123",
                fontSize = 14.sp,
                color = FlightyColors.TextGray,
            )
        }
        Text(
            text = "FREQUENTLY USED",
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlightyColors.TextGray,
            modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
        )
        suggestions.forEach { suggestion ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            suggestion.badgeColor?.let { Color(it) } ?: FlightyColors.ChipBg,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = suggestion.badgeCode,
                        fontSize = if (suggestion.badgeCode.length > 2) 8.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (suggestion.badgeColor != null) Color.White else FlightyColors.TextDark,
                    )
                }
                Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(
                        text = suggestion.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FlightyColors.TextDark,
                    )
                    Text(text = suggestion.codes, fontSize = 11.sp, color = FlightyColors.TextGray)
                }
                Text(text = "›", fontSize = 16.sp, color = FlightyColors.TextGray)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
        ) {
            Icon(
                imageVector = AppIcons.Plane,
                contentDescription = null,
                tint = FlightyColors.Blue,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Find by Route",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlightyColors.Blue,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}
