package flighty.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import flighty.data.AppGraph
import flighty.model.AddFlightShortcut
import flighty.model.AddFlightSuggestion
import flighty.ui.components.AppIcons
import flighty.vm.AddFlightViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * The full Add Flight sheet: an M3 modal sheet rendered inside the Compose
 * canvas, used by every host. The native-chrome iOS app also uses this —
 * presenting a UIKit modal over a Compose canvas either snapshots the Metal
 * layer blank (.sheet: white flash) or permanently suspends its rendering
 * (fullScreenCover: white screen after dismiss) on the current CMP beta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFlightSheetHost(onDismiss: () -> Unit) {
    val addFlightViewModel = viewModel { AddFlightViewModel(AppGraph.flightRepository) }
    val state by addFlightViewModel.uiState.collectAsState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Flighty's search opens straight to a full-height sheet — no
        // partial stop, so the enabled values skip PartiallyExpanded.
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        containerColor = FlightyColors.SheetBg,
        // From google issue 467297218 (comment #4): the TOP window inset
        // makes near-full-height sheets oscillate on fast flings — keep
        // only the bottom inset so content clears the home indicator.
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        AddFlightContent(
            shortcuts = state.shortcuts,
            suggestions = state.suggestions,
            onDismiss = onDismiss,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * The Add Flight search content, matching the reference: smart shortcuts on
 * top, frequently-used entries, then Find by Route under MORE — with the
 * search field focused on open so the keyboard comes up.
 */
@Composable
fun AddFlightContent(
    shortcuts: List<AddFlightShortcut>,
    suggestions: List<AddFlightSuggestion>,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)) {
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

        // Real text field, focused on open — the reference comes up with the
        // keyboard already showing.
        var query by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            // Let the sheet's presentation settle before summoning the
            // keyboard, or the focus request can get dropped.
            delay(350.milliseconds)
            focusRequester.requestFocus()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .background(FlightyColors.ChipBg, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (query.isEmpty()) {
                Text(
                    text = "Flair, YYZ, or F8123",
                    fontSize = 14.sp,
                    color = FlightyColors.TextGray,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = FlightyColors.TextDark),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        }

        shortcuts.forEachIndexed { index, shortcut ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (index == 0) 14.dp else 0.dp, bottom = 10.dp),
            ) {
                Icon(
                    imageVector = if (index == 0) AppIcons.Return else AppIcons.Plane,
                    contentDescription = null,
                    tint = FlightyColors.TextDark,
                    modifier = Modifier.size(18.dp),
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = shortcut.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FlightyColors.TextDark,
                    )
                    Text(
                        text = shortcut.subtitle,
                        fontSize = 11.sp,
                        color = FlightyColors.TextGray,
                    )
                }
            }
        }

        Text(
            text = "FREQUENTLY USED",
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlightyColors.TextGray,
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
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
                        fontSize = when {
                            suggestion.badgeColor == null -> 15.sp
                            suggestion.badgeCode.length > 2 -> 8.sp
                            else -> 11.sp
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (suggestion.badgeColor != null) Color.White else FlightyColors.TextDark,
                    )
                }
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        text = suggestion.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FlightyColors.TextDark,
                    )
                    Text(text = suggestion.codes, fontSize = 11.sp, color = FlightyColors.TextGray)
                }
            }
        }

        Text(
            text = "MORE",
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlightyColors.TextGray,
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp),
        ) {
            Icon(
                imageVector = AppIcons.Plane,
                contentDescription = null,
                tint = FlightyColors.TextDark,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Find by Route",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlightyColors.TextDark,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
