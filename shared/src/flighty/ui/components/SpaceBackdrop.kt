package flighty.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.imageResource
import shared.generated.resources.Res
import shared.generated.resources.earth_tex
import flighty.model.Flight
import flighty.model.FlightStatus
import flighty.ui.FlightyColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Orthographic (true globe) projection: the earth as a sphere seen from space.
 * Works in whatever length unit its center/radius were given in.
 */
internal class GlobeView(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val centerLonDeg: Double,
    val centerLatDeg: Double,
) {
    private val lam0 = centerLonDeg * PI / 180
    private val phi0 = centerLatDeg * PI / 180
    private val sinPhi0 = sin(phi0)
    private val cosPhi0 = cos(phi0)

    /** [visible] is false on the far hemisphere (point clamped to the limb). */
    fun project(lonDeg: Double, latDeg: Double): Triple<Float, Float, Boolean> {
        val lam = lonDeg * PI / 180 - lam0
        val phi = latDeg * PI / 180
        val cosC = sinPhi0 * sin(phi) + cosPhi0 * cos(phi) * cos(lam)
        var x = (cos(phi) * sin(lam)).toFloat()
        var y = (cosPhi0 * sin(phi) - sinPhi0 * cos(phi) * cos(lam)).toFloat()
        if (cosC < 0) {
            val len = sqrt(x * x + y * y).coerceAtLeast(1e-6f)
            x /= len
            y /= len
        }
        return Triple(centerX + radius * x, centerY - radius * y, cosC >= 0)
    }
}

internal fun globeViewFor(
    flight: Flight?,
    width: Float,
    height: Float,
    lonOffset: Double = 0.0,
): GlobeView {
    val (centerLon, centerLat) = if (flight != null) {
        val route = greatCircle(flight, samples = 16)
        val mid = route[route.size / 2]
        // Look "below" the route so it lands in the visible upper part of the sphere.
        mid.first to (mid.second - 26.0).coerceIn(-60.0, 60.0)
    } else {
        // North America + Atlantic + Europe across the top, drifting westward.
        (-50.0 - lonOffset) to 22.0
    }
    val radius = width * 0.80f
    return GlobeView(
        centerX = width / 2f,
        centerY = height * 0.055f + radius,
        radius = radius,
        centerLonDeg = centerLon,
        centerLatDeg = centerLat,
    )
}

private class Star(val x: Float, val y: Float, val r: Float, val alpha: Float)

/** Everything expensive, prebuilt in pixel space and drawn as-is each frame. */
private class GlobeGeometry(
    val view: GlobeView,
    val disk: Path,
    val graticule: Path,
    val land: Path,
    val cityLights: List<Star>,
    val routePath: Path?,
    val routeStart: Offset?,
    val routeEnd: Offset?,
    val plane: Offset?,
    /** Screen-space heading of the route at the plane, degrees from +x. */
    val planeAngleDeg: Float,
)

/**
 * Draw-phase cache: the globe is rasterized into a bitmap and re-rendered only
 * when the quantized spin angle changes. Skiko (iOS/desktop) has no Android-style
 * render-node caching, so without this every sheet-drag frame would re-fill the
 * 3.5k-point coastline path; with it, each frame is a single texture blit.
 */
private class EarthTexture(val pixels: IntArray, val width: Int, val height: Int)

private class GlobeBitmapCache {
    var key: Float = Float.NaN
    var bitmap: ImageBitmap? = null

    // Textured-sphere layer, rendered at half resolution and upscaled.
    var warp: GlobeWarp? = null
    var sphere: ImageBitmap? = null
    var sphereBuf: IntArray? = null
}

private fun buildGlobeGeometry(
    flight: Flight?,
    route: List<Pair<Double, Double>>?,
    widthPx: Float,
    heightPx: Float,
    lonOffset: Float,
): GlobeGeometry {
    val view = globeViewFor(flight, widthPx, heightPx, lonOffset.toDouble())
    val cx = view.centerX
    val cy = view.centerY
    val r = view.radius

    val disk = Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }

    val graticule = Path()
    for (lonLine in -180 until 180 step 30) {
        var started = false
        for (lat in -85..85 step 5) {
            val (x, y, visible) = view.project(lonLine.toDouble(), lat.toDouble())
            if (!visible) { started = false; continue }
            if (!started) { graticule.moveTo(x, y); started = true } else graticule.lineTo(x, y)
        }
    }
    for (latLine in -60..75 step 15) {
        var started = false
        for (lon in -180..180 step 5) {
            val (x, y, visible) = view.project(lon.toDouble(), latLine.toDouble())
            if (!visible) { started = false; continue }
            if (!started) { graticule.moveTo(x, y); started = true } else graticule.lineTo(x, y)
        }
    }

    // All landmasses merged into a single multi-contour path: one fill + one
    // stroke per frame instead of two draws per polygon. Polygons entirely on
    // the far hemisphere are dropped, not just left unclosed.
    val land = Path()
    val contour = Path()
    for (poly in WORLD_LAND) {
        contour.reset()
        var anyVisible = false
        var i = 0
        var moved = false
        while (i < poly.size) {
            val (x, y, visible) = view.project(poly[i].toDouble(), poly[i + 1].toDouble())
            if (visible) anyVisible = true
            if (!moved) { contour.moveTo(x, y); moved = true } else contour.lineTo(x, y)
            i += 2
        }
        if (!anyVisible) continue
        contour.close()
        land.addPath(contour)
    }

    var lightSeed = 977
    fun nextLight(): Float {
        lightSeed = (lightSeed * 1664525 + 1013904223) and 0x7FFFFFFF
        return (lightSeed % 10_000) / 10_000f
    }
    val cityLights = ArrayList<Star>(160)
    for (poly in WORLD_LAND) {
        var i = 0
        while (i < poly.size) {
            if (nextLight() > 0.88f) {
                val (x, y, visible) = view.project(poly[i].toDouble(), poly[i + 1].toDouble())
                if (visible) {
                    cityLights += Star(x, y, 0.8f + nextLight(), 0.10f + nextLight() * 0.30f)
                }
            }
            i += 8
        }
    }

    var routePath: Path? = null
    var routeStart: Offset? = null
    var routeEnd: Offset? = null
    var plane: Offset? = null
    var planeAngleDeg = 0f
    if (route != null && flight != null) {
        val arc = Path()
        var started = false
        for ((lon, lat) in route) {
            val (x, y, visible) = view.project(lon, lat)
            if (!visible) { started = false; continue }
            if (!started) { arc.moveTo(x, y); started = true } else arc.lineTo(x, y)
        }
        routePath = arc
        val (x1, y1, v1) = view.project(route.first().first, route.first().second)
        if (v1) routeStart = Offset(x1, y1)
        val (x2, y2, v2) = view.project(route.last().first, route.last().second)
        if (v2) routeEnd = Offset(x2, y2)
        val t = when (flight.status) {
            FlightStatus.InAir -> flight.progress
            FlightStatus.Landed -> 1f
            else -> 0f
        }
        val planeIndex = ((route.size - 1) * t).toInt().coerceIn(0, route.size - 1)
        val p = route[planeIndex]
        val (px, py, pv) = view.project(p.first, p.second)
        if (pv) {
            plane = Offset(px, py)
            val next = route[(planeIndex + 1).coerceAtMost(route.size - 1)]
            val (nx, ny, nv) = view.project(next.first, next.second)
            if (nv && (nx != px || ny != py)) {
                planeAngleDeg = (atan2(ny - py, nx - px) * 180.0 / PI).toFloat()
            }
        }
    }

    return GlobeGeometry(
        view, disk, graticule, land, cityLights, routePath, routeStart, routeEnd, plane, planeAngleDeg,
    )
}

/**
 * The earth from space, drawn in common code with an orthographic projection
 * of real Natural Earth coastlines. Idle screens drift slowly; the animation
 * only invalidates the draw phase, and projected geometry is cached and
 * rebuilt ~15 times per second (0.1° spin steps ≈ 1px of surface motion).
 */
// Perf A/B switch — false renders a plain dark backdrop with no globe.
// Verified 2026-07-21: sheet-fling jank on iOS is identical with the globe
// disabled, so the backdrop is not a factor.
private const val GLOBE_ENABLED = true

@Composable
fun SpaceBackdrop(
    flight: Flight?,
    mapHeightFraction: Float,
    modifier: Modifier = Modifier,
) {
    if (!GLOBE_ENABLED) {
        Box(modifier = modifier.fillMaxSize().background(FlightyColors.Space))
        return
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(FlightyColors.Space)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // The sheet permanently covers everything below its peek position, so
        // only rasterize (and composite) the visible map band — roughly halves
        // the bitmap that gets drawn every frame.
        val cropFraction = (mapHeightFraction + 0.20f).coerceAtMost(0.60f)
        val cropDp = maxHeight * cropFraction
        val cropPx = with(density) { cropDp.toPx() }

        val route = remember(flight?.id) { flight?.let { greatCircle(it) } }
        val stars = remember(widthPx, cropPx) {
            var seed = 20260720
            fun next(): Float {
                seed = (seed * 1664525 + 1013904223) and 0x7FFFFFFF
                return (seed % 10_000) / 10_000f
            }
            List(90) {
                Star(
                    x = next() * widthPx,
                    y = next() * cropPx * 0.95f,
                    r = 0.5f + next() * 1.4f,
                    alpha = 0.18f + next() * 0.5f,
                )
            }
        }

        val spin = rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(240_000, easing = LinearEasing)),
        )
        val cache = remember(flight?.id, widthPx, cropPx) { GlobeBitmapCache() }
        val planePainter = rememberVectorPainter(AppIcons.Plane)

        // NASA Blue Marble (public domain), pre-darkened for the space look.
        // Copied once into a tight IntArray for the warp sampler.
        val earthImage = imageResource(Res.drawable.earth_tex)
        val earthTex = remember(earthImage) {
            val map = earthImage.toPixelMap()
            val w = map.width
            val h = map.height
            val buf = IntArray(w * h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    buf[y * w + x] = map[x, y].toArgb()
                }
            }
            EarthTexture(buf, w, h)
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(cropDp)) {
            // Read the animation here (draw phase) so spinning never recomposes;
            // quantize the spin so the cached raster is reused across frames.
            // 0.1° steps ≈ 1px of surface motion at ~15 rebuilds/s (1.5°/s spin)
            // — below that the idle drift visibly stutters on Friends/Passport.
            val lonOffset = if (flight == null) (spin.value * 10).toInt() / 10f else 0f
            val w = size.width.toInt().coerceAtLeast(1)
            val h = size.height.toInt().coerceAtLeast(1)
            var bitmap = cache.bitmap
            if (cache.key != lonOffset || bitmap == null || bitmap.width != w || bitmap.height != h) {
                // Geometry is projected in full-screen space; drawing into the
                // cropped bitmap simply clips everything below the map band.
                val geo = buildGlobeGeometry(flight, route, widthPx, heightPx, lonOffset)

                // Warp the earth texture onto the sphere at half resolution.
                // The warp table is spin-independent (spin is a pure column
                // shift in the texture), so it's built once per size.
                val w2 = (w + 1) / 2
                val h2 = (h + 1) / 2
                var warp = cache.warp
                if (warp == null || warp.width != w2 || warp.height != h2) {
                    warp = buildGlobeWarp(geo.view, w2, h2, 2f, earthTex.width, earthTex.height)
                    cache.warp = warp
                    cache.sphereBuf = IntArray(w2 * h2)
                    cache.sphere = ImageBitmap(w2, h2)
                }
                val sphereBuf = cache.sphereBuf!!
                val sphere = cache.sphere!!
                sampleGlobe(warp, earthTex.pixels, earthTex.width, geo.view.centerLonDeg, sphereBuf)
                sphere.writePixels(sphereBuf, w2, h2)

                val target = bitmap?.takeIf { it.width == w && it.height == h } ?: ImageBitmap(w, h)
                CanvasDrawScope().draw(this, layoutDirection, GraphicsCanvas(target), Size(size.width, size.height)) {
                    drawRect(FlightyColors.Space)
                    drawGlobe(geo, stars, planePainter, sphere)
                }
                bitmap = target
                cache.bitmap = target
                cache.key = lonOffset
            }
            drawImage(bitmap)
        }

        // Airport chips, projected with the same (static, route-framed) view.
        if (flight != null) {
            val chipView = remember(flight.id, widthPx, heightPx) {
                globeViewFor(flight, widthPx, heightPx)
            }
            val (ox, oy, ov) = chipView.project(flight.origin.longitude, flight.origin.latitude)
            if (ov) {
                AirportChip(
                    code = flight.origin.code,
                    modifier = Modifier.offset(
                        x = with(density) { ox.toDp() } - 18.dp,
                        y = with(density) { oy.toDp() } + 9.dp,
                    ),
                )
            }
            val (dx, dy, dv) = chipView.project(flight.destination.longitude, flight.destination.latitude)
            if (dv) {
                AirportChip(
                    code = flight.destination.code,
                    modifier = Modifier.offset(
                        x = with(density) { dx.toDp() } - 18.dp,
                        y = with(density) { dy.toDp() } + 9.dp,
                    ),
                )
            }
        }
    }
}

private fun DrawScope.drawGlobe(
    geo: GlobeGeometry,
    stars: List<Star>,
    planePainter: VectorPainter,
    sphere: ImageBitmap? = null,
) {
    val cx = geo.view.centerX
    val cy = geo.view.centerY
    val r = geo.view.radius

    for (star in stars) {
        val dx = star.x - cx
        val dy = star.y - cy
        if (dx * dx + dy * dy > (r + 12f) * (r + 12f)) {
            drawCircle(Color.White.copy(alpha = star.alpha), star.r, Offset(star.x, star.y))
        }
    }

    drawCircle(FlightyColors.Horizon.copy(alpha = 0.50f), r + 4f, Offset(cx, cy), style = Stroke(9f))
    drawCircle(FlightyColors.Horizon.copy(alpha = 0.18f), r + 16f, Offset(cx, cy), style = Stroke(26f))
    drawCircle(FlightyColors.Horizon.copy(alpha = 0.06f), r + 38f, Offset(cx, cy), style = Stroke(50f))

    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFF2C55A4),
                0.55f to Color(0xFF15294F),
                1.0f to Color(0xFF070E20),
            ),
            center = Offset(cx, cy - r * 0.85f),
            radius = r * 2.1f,
        ),
        radius = r,
        center = Offset(cx, cy),
    )

    if (sphere != null) {
        // Real earth imagery, warped onto the sphere. The disk clip keeps the
        // circular edge crisp despite the half-resolution upscale, and a limb
        // shading pass restores the sphere's depth over the flat texture.
        clipPath(geo.disk) {
            drawImage(
                image = sphere,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(sphere.width, sphere.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.72f to Color.Transparent,
                        1.0f to Color(0xB3040A18),
                    ),
                    center = Offset(cx, cy - r * 0.55f),
                    radius = r * 1.65f,
                ),
                radius = r,
                center = Offset(cx, cy),
            )
        }
    } else {
        clipPath(geo.disk) {
            drawPath(geo.graticule, Color(0xFF31497F).copy(alpha = 0.35f), style = Stroke(1f))
            drawPath(geo.land, Color(0xFF27466F))
            drawPath(geo.land, Color(0xFF46709F).copy(alpha = 0.85f), style = Stroke(1.2f))
            for (light in geo.cityLights) {
                drawCircle(
                    FlightyColors.CityLight.copy(alpha = light.alpha),
                    light.r,
                    Offset(light.x, light.y),
                )
            }
        }
    }

    geo.routePath?.let { arc ->
        drawPath(arc, FlightyColors.Route.copy(alpha = 0.30f), style = Stroke(10f))
        drawPath(
            arc,
            FlightyColors.Route,
            style = Stroke(3.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 12f))),
        )
    }
    geo.routeStart?.let { drawCircle(Color.White, 6f, it) }
    geo.routeEnd?.let { drawCircle(Color.White, 6f, it) }
    geo.plane?.let { center ->
        // The glyph points north, so screen-heading needs a +90° correction.
        rotate(degrees = geo.planeAngleDeg + 90f, pivot = center) {
            val s = 30f
            val outline = ColorFilter.tint(Color(0xCC1A2433))
            // Four offset passes of a dark tint form a light outline that keeps
            // the white glyph readable over bright parts of the globe.
            for ((ox, oy) in listOf(-1.5f to 0f, 1.5f to 0f, 0f to -1.5f, 0f to 1.5f)) {
                translate(left = center.x - s / 2 + ox, top = center.y - s / 2 + oy) {
                    with(planePainter) { draw(Size(s, s), colorFilter = outline) }
                }
            }
            translate(left = center.x - s / 2, top = center.y - s / 2) {
                with(planePainter) {
                    draw(Size(s, s), colorFilter = ColorFilter.tint(Color.White))
                }
            }
        }
    }
}

@Composable
private fun AirportChip(code: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Text(
            text = code,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(Color(0xFF2B62D9), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}
