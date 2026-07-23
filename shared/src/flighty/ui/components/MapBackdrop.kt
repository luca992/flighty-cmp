package flighty.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import flighty.model.Flight
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * The backdrop behind the sheet, matching Flighty's two moods:
 * - Flight detail → a real MapLibre vector map with the route (Android/iOS;
 *   desktop MapLibre support is still early, so it keeps the globe).
 * - Tabs → the orthographic globe seen from space, with the live route.
 */
@Composable
fun MapBackdrop(
    flight: Flight?,
    detail: Boolean,
    mapHeightFraction: Float,
    modifier: Modifier = Modifier,
) {
    if (detail && flight != null && hasNativeRouteMap) {
        // The native map view only mounts once the detail entrance animation
        // has settled: its surface creation and style load land on the UI
        // thread, and mounting mid-transition visibly janks the slide-in
        // (worst on Android). The globe keeps rendering underneath meanwhile.
        var mountMap by remember(flight.id) { mutableStateOf(false) }
        LaunchedEffect(flight.id) {
            delay(500.milliseconds)
            mountMap = true
        }
        if (mountMap) {
            NativeRouteMap(flight, mapHeightFraction, modifier)
        } else {
            SpaceBackdrop(flight, mapHeightFraction, modifier)
        }
    } else {
        SpaceBackdrop(flight, mapHeightFraction, modifier)
    }
}
