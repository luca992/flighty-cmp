package flighty.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import flighty.model.Flight

internal actual val hasNativeRouteMap: Boolean = false

@Composable
internal actual fun NativeRouteMap(flight: Flight, mapHeightFraction: Float, modifier: Modifier) {
    // Never reached: hasNativeRouteMap gates all call sites.
}
