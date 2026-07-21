package flighty.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import flighty.model.Flight
import flighty.platformName

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
    if (detail && flight != null && platformName() != "Desktop JVM") {
        MaplibreRouteMap(flight, mapHeightFraction, modifier)
    } else {
        SpaceBackdrop(flight, mapHeightFraction, modifier)
    }
}
