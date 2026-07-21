package flighty.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import flighty.model.Flight
import flighty.model.FlightStatus
import flighty.ui.FlightyColors
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import kotlin.math.ln
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

/** Dark, key-free vector style (OpenFreeMap / OpenStreetMap data). */
private const val MAP_STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"

/**
 * A real MapLibre vector map showing the flight's great-circle route.
 * Used on Android and iOS; desktop MapLibre support is still early, so the
 * canvas backdrop is used there instead.
 */
@Composable
fun MaplibreRouteMap(
    flight: Flight,
    mapHeightFraction: Float,
    modifier: Modifier = Modifier,
) {
    val route = remember(flight.id) { greatCircle(flight) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Keep the native map view's overlap with the Compose sheet minimal:
        // on iOS, every frame of sheet drag pays interop-compositing cost over
        // the overlapped region.
        val mapHeight = maxHeight * (mapHeightFraction + 0.03f).coerceAtMost(1f)
        val mid = route[route.size / 2]
        val targetZoom = zoomFor(route)
        // Start pulled way out and fly into the route, like the reference video.
        val camera = rememberCameraState(
            firstPosition = CameraPosition(
                target = Position(longitude = mid.first, latitude = mid.second),
                zoom = (targetZoom - 2.6).coerceAtLeast(0.3),
            ),
        )
        LaunchedEffect(flight.id) {
            camera.animateTo(
                finalPosition = CameraPosition(
                    target = Position(longitude = mid.first, latitude = mid.second),
                    zoom = targetZoom,
                ),
                duration = 1400.milliseconds,
            )
        }
        MaplibreMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight),
            baseStyle = BaseStyle.Uri(MAP_STYLE_DARK),
            cameraState = camera,
        ) {
            val lineSource = rememberGeoJsonSource(
                GeoJsonData.JsonString(remember(flight.id) { lineFeature(route) }),
            )
            val endpointSource = rememberGeoJsonSource(
                GeoJsonData.JsonString(remember(flight.id) { endpointFeatures(route) }),
            )
            val planeSource = rememberGeoJsonSource(
                GeoJsonData.JsonString(remember(flight.id) { planeFeature(flight, route) }),
            )
            LineLayer(
                id = "flighty-route-glow",
                source = lineSource,
                color = const(FlightyColors.Route.copy(alpha = 0.35f)),
                width = const(7.dp),
            )
            LineLayer(
                id = "flighty-route",
                source = lineSource,
                color = const(FlightyColors.Route),
                width = const(3.dp),
            )
            CircleLayer(
                id = "flighty-route-endpoints",
                source = endpointSource,
                color = const(Color.White),
                radius = const(4.dp),
            )
            CircleLayer(
                id = "flighty-plane-halo",
                source = planeSource,
                color = const(FlightyColors.Route),
                radius = const(8.dp),
            )
            CircleLayer(
                id = "flighty-plane",
                source = planeSource,
                color = const(Color.White),
                radius = const(3.dp),
            )
        }
    }
}

private fun zoomFor(route: List<Pair<Double, Double>>): Double {
    var minLon = Double.MAX_VALUE
    var maxLon = -Double.MAX_VALUE
    var minLat = Double.MAX_VALUE
    var maxLat = -Double.MAX_VALUE
    for ((lon, lat) in route) {
        if (lon < minLon) minLon = lon
        if (lon > maxLon) maxLon = lon
        if (lat < minLat) minLat = lat
        if (lat > maxLat) maxLat = lat
    }
    val span = max(max(maxLon - minLon, (maxLat - minLat) * 2), 8.0)
    val zoom = ln(360.0 / (span * 1.8)) / ln(2.0)
    return zoom.coerceIn(0.5, 7.0)
}

private fun lineFeature(route: List<Pair<Double, Double>>): String {
    val line = route.joinToString(",") { (lon, lat) -> "[$lon,$lat]" }
    return """{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[$line]}}"""
}

private fun endpointFeatures(route: List<Pair<Double, Double>>): String {
    val first = route.first()
    val last = route.last()
    return """
        {"type":"FeatureCollection","features":[
          {"type":"Feature","properties":{},
           "geometry":{"type":"Point","coordinates":[${first.first},${first.second}]}},
          {"type":"Feature","properties":{},
           "geometry":{"type":"Point","coordinates":[${last.first},${last.second}]}}
        ]}
    """.trimIndent()
}

private fun planeFeature(flight: Flight, route: List<Pair<Double, Double>>): String {
    val progress = when (flight.status) {
        FlightStatus.InAir -> flight.progress
        FlightStatus.Landed -> 1f
        else -> 0f
    }
    val planeIndex = (route.size - 1) * progress
    val plane = route[planeIndex.toInt().coerceIn(0, route.size - 1)]
    return """{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[${plane.first},${plane.second}]}}"""
}
