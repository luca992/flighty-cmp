package flighty.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import flighty.model.Flight

internal actual val hasNativeRouteMap: Boolean = true

@Composable
internal actual fun NativeRouteMap(flight: Flight, mapHeightFraction: Float, modifier: Modifier) {
    MaplibreRouteMap(flight, mapHeightFraction, modifier)
}
