package flighty.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import flighty.model.Flight

/**
 * Whether this platform has a real vector-map implementation. Platforms
 * without one (desktop, web — maplibre-compose ships no artifacts for them
 * yet) keep the canvas globe backdrop on the detail screen too.
 */
internal expect val hasNativeRouteMap: Boolean

@Composable
internal expect fun NativeRouteMap(flight: Flight, mapHeightFraction: Float, modifier: Modifier)
