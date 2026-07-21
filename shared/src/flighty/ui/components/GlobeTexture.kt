package flighty.ui.components

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Precomputed inverse-orthographic mapping from globe-band pixels to texture
 * texels. The key property making the idle spin cheap: rotating the globe only
 * changes the center longitude, which in an equirectangular texture is a pure
 * column shift — so the per-pixel latitude row and relative-longitude column
 * are constant, and each spin step is a single indexed copy.
 */
internal class GlobeWarp(
    val width: Int,
    val height: Int,
    /** Output buffer index of each visible sphere pixel. */
    val diskIndex: IntArray,
    /** texRow * texWidth for each visible sphere pixel. */
    val rowBase: IntArray,
    /** Texture column for the pixel's longitude relative to the view center. */
    val relCol: IntArray,
)

/**
 * Builds the warp for an output raster of [outW]x[outH] pixels representing
 * the same area as the full-resolution band scaled down by [scale].
 */
internal fun buildGlobeWarp(
    view: GlobeView,
    outW: Int,
    outH: Int,
    scale: Float,
    texW: Int,
    texH: Int,
): GlobeWarp {
    val phi0 = view.centerLatDeg * PI / 180
    val sinPhi0 = sin(phi0)
    val cosPhi0 = cos(phi0)
    val cx = view.centerX
    val cy = view.centerY
    val r = view.radius

    val diskIndex = ArrayList<Int>(outW * outH / 2)
    val rowBase = ArrayList<Int>(outW * outH / 2)
    val relCol = ArrayList<Int>(outW * outH / 2)

    for (py in 0 until outH) {
        val fy = (py + 0.5f) * scale
        val ny = ((cy - fy) / r).toDouble()
        for (px in 0 until outW) {
            val fx = (px + 0.5f) * scale
            val nx = ((fx - cx) / r).toDouble()
            val rho2 = nx * nx + ny * ny
            if (rho2 > 1.0) continue
            val cosC = sqrt(1.0 - rho2)
            // Standard orthographic inverse (sinC == rho cancels against /rho).
            val phi = asin((cosC * sinPhi0 + ny * cosPhi0).coerceIn(-1.0, 1.0))
            val relLam = atan2(nx, cosPhi0 * cosC - ny * sinPhi0)

            val row = (((0.5 - phi / PI) * texH).toInt()).coerceIn(0, texH - 1)
            val col = ((relLam / (2 * PI) + 0.5) * texW).toInt().coerceIn(0, texW - 1)
            diskIndex.add(py * outW + px)
            rowBase.add(row * texW)
            relCol.add(col)
        }
    }
    return GlobeWarp(
        width = outW,
        height = outH,
        diskIndex = diskIndex.toIntArray(),
        rowBase = rowBase.toIntArray(),
        relCol = relCol.toIntArray(),
    )
}

/** Copies the texture through the warp for the given center longitude. */
internal fun sampleGlobe(
    warp: GlobeWarp,
    tex: IntArray,
    texW: Int,
    centerLonDeg: Double,
    out: IntArray,
) {
    // The view centers on centerLonDeg, and relCol already encodes +180°/2
    // (texture column 0 is longitude -180°).
    var shift = ((centerLonDeg / 360.0) * texW).toInt() % texW
    if (shift < 0) shift += texW
    val diskIndex = warp.diskIndex
    val rowBase = warp.rowBase
    val relCol = warp.relCol
    for (i in diskIndex.indices) {
        var col = relCol[i] + shift
        if (col >= texW) col -= texW
        out[diskIndex[i]] = tex[rowBase[i] + col]
    }
}
