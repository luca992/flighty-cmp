package flighty.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
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
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Position
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
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
            // The backdrop already defers mounting this map until the entrance
            // animation settles; this small extra beat gives the surface/style
            // init room before the camera starts moving (jank-prone on Android).
            delay(150.milliseconds)
            camera.animateTo(
                finalPosition = CameraPosition(
                    target = Position(longitude = mid.first, latitude = mid.second),
                    zoom = targetZoom,
                ),
                duration = 1200.milliseconds,
            )
        }
        MaplibreMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight),
            baseStyle = BaseStyle.Uri(MAP_STYLE_DARK),
            cameraState = camera,
            // The lambda's MapLibre target is declared explicitly: the Native
            // compiler drops the @MaplibreComposable marker from the library's
            // deserialized signature, so without this it infers a UI target.
            content = @MaplibreComposable { RouteLayers(flight = flight, route = route) },
        )

        // The aircraft is a Compose overlay: registering a style image would be
        // the native way, but maplibre-compose 0.13's iOS path crashes against
        // Compose 1.12's Skiko (Image.encodeToData was removed — IrLinkageError).
        // It appears only after the fly-in settles, so the overlay never has to
        // chase the animating camera; afterwards it re-places itself in the
        // layout placement phase (the offset lambda) as the user pans.
        // Visible from the first frame: the offset lambda reads the camera in
        // the placement phase, so the icon rides the route through the fly-in
        // without forcing recomposition.
        val plane = remember(flight.id) { planePosition(flight, route) }
        val bearing = remember(flight.id) { routeBearing(flight, route) }
        val planePainter = rememberVectorPainter(AppIcons.Plane)
        Spacer(
            modifier = Modifier
                .offset {
                    camera.position
                    val loc = camera.projection?.screenLocationFromPosition(
                        Position(longitude = plane.first, latitude = plane.second),
                    ) ?: DpOffset(x = (-100).dp, y = (-100).dp)
                    IntOffset((loc.x - 13.dp).roundToPx(), (loc.y - 13.dp).roundToPx())
                }
                .size(26.dp)
                .drawBehind {
                    rotate(degrees = bearing) {
                        val outline = ColorFilter.tint(Color(0xCC1A2433))
                        for ((ox, oy) in listOf(-1f to 0f, 1f to 0f, 0f to -1f, 0f to 1f)) {
                            translate(left = ox.dp.toPx(), top = oy.dp.toPx()) {
                                with(planePainter) { draw(size, colorFilter = outline) }
                            }
                        }
                        with(planePainter) {
                            draw(size, colorFilter = ColorFilter.tint(Color.White))
                        }
                    }
                },
        )
    }
}

private fun planePosition(flight: Flight, route: List<Pair<Double, Double>>): Pair<Double, Double> {
    val progress = when (flight.status) {
        FlightStatus.InAir -> flight.progress
        FlightStatus.Landed -> 1f
        else -> 0f
    }
    val index = ((route.size - 1) * progress).toInt().coerceIn(0, route.size - 1)
    return route[index]
}

/**
 * Route line, endpoints, and the aircraft symbol. Declared with the library's
 * [MaplibreComposable] target marker: layer composables run in the map's own
 * applier, and declaring that explicitly (rather than relying on cross-klib
 * target inference, which the Native compiler gets wrong) is what keeps the
 * applier-mismatch diagnostics away.
 */
@Composable
@MaplibreComposable
private fun RouteLayers(flight: Flight, route: List<Pair<Double, Double>>) {
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
        id = "flighty-plane-anchor",
        source = planeSource,
        color = const(FlightyColors.Route),
        radius = const(2.dp),
    )
}

/** Heading of the route at the plane's position, in degrees clockwise from north. */
private fun routeBearing(flight: Flight, route: List<Pair<Double, Double>>): Float {
    val progress = when (flight.status) {
        FlightStatus.InAir -> flight.progress
        FlightStatus.Landed -> 1f
        else -> 0f
    }
    val i = ((route.size - 1) * progress).toInt().coerceIn(0, route.size - 2)
    val (lon1, lat1) = route[i]
    val (lon2, lat2) = route[i + 1]
    val toRad = PI / 180.0
    val dLon = (lon2 - lon1) * toRad
    val y = sin(dLon) * cos(lat2 * toRad)
    val x = cos(lat1 * toRad) * sin(lat2 * toRad) -
        sin(lat1 * toRad) * cos(lat2 * toRad) * cos(dLon)
    return (atan2(y, x) / toRad).toFloat()
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
