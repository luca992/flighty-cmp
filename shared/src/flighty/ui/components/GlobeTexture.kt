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
    /**
     * Mean of cos(lat)·cos(relLon) over the visible pixels: the band's true
     * average horizontal motion per degree of spin, as a fraction of the
     * equator rate. The draw-phase slide must use this — sliding at the
     * equator rate over-shoots the crown-heavy band and every keyframe swap
     * snaps the excess back, which reads as the texture shaking (worst on
     * big globes at low keyframe cadence).
     */
    val dxFactor: Float,
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
    /** Screen-x of the raster's left edge; negative overscans past the canvas. */
    xStartPx: Float = 0f,
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
    var dxSum = 0.0

    for (py in 0 until outH) {
        val fy = (py + 0.5f) * scale
        val ny = ((cy - fy) / r).toDouble()
        for (px in 0 until outW) {
            val fx = xStartPx + (px + 0.5f) * scale
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
            dxSum += cos(phi) * cos(relLam)
        }
    }
    return GlobeWarp(
        width = outW,
        height = outH,
        diskIndex = diskIndex.toIntArray(),
        rowBase = rowBase.toIntArray(),
        relCol = relCol.toIntArray(),
        dxFactor = (dxSum / diskIndex.size.coerceAtLeast(1)).toFloat(),
    )
}

/** Copies the texture through the warp for the given center longitude. */
internal fun sampleGlobe(
    warp: GlobeWarp,
    tex: IntArray,
    texW: Int,
    centerLonDeg: Double,
    out: IntArray,
    from: Int = 0,
    until: Int = warp.diskIndex.size,
) {
    // The view centers on centerLonDeg, and relCol already encodes +180°/2
    // (texture column 0 is longitude -180°). The shift is fractional: at this
    // texture resolution one column is ~2.6 screen px, so flooring it makes
    // surface features jump a whole column every ~120 ms of idle drift.
    // Blending the two neighboring columns keeps the motion continuous.
    var shiftF = (centerLonDeg / 360.0) * texW % texW
    if (shiftF < 0) shiftF += texW
    val shift = shiftF.toInt()
    // 0..256 fixed-point blend weight; packed two-lane multiply stays within
    // 16 bits per channel (0xFF * 256 = 0xFF00).
    val w1 = ((shiftF - shift) * 256).toInt()
    val w0 = 256 - w1
    val diskIndex = warp.diskIndex
    val rowBase = warp.rowBase
    val relCol = warp.relCol
    if (w1 == 0) {
        for (i in from until until) {
            var col = relCol[i] + shift
            if (col >= texW) col -= texW
            out[diskIndex[i]] = tex[rowBase[i] + col]
        }
        return
    }
    for (i in from until until) {
        var col = relCol[i] + shift
        if (col >= texW) col -= texW
        var col2 = col + 1
        if (col2 >= texW) col2 -= texW
        val base = rowBase[i]
        val c0 = tex[base + col]
        val c1 = tex[base + col2]
        val rb = ((c0 and 0x00FF00FF) * w0 + (c1 and 0x00FF00FF) * w1) shr 8 and 0x00FF00FF
        val ag = ((c0 ushr 8 and 0x00FF00FF) * w0 + (c1 ushr 8 and 0x00FF00FF) * w1) and
            0xFF00FF00.toInt()
        out[diskIndex[i]] = ag or rb
    }
}

/**
 * Same warp sampling as [sampleGlobe], but written straight into a BGRA byte
 * buffer — the layout Skia's installPixels wants — so Skiko targets skip a
 * whole ARGB-int-to-byte conversion pass per keyframe. Non-disk bytes stay
 * zero (transparent); only visible sphere pixels are ever touched.
 */
internal fun sampleGlobeBgra(
    warp: GlobeWarp,
    tex: IntArray,
    texW: Int,
    centerLonDeg: Double,
    out: ByteArray,
    from: Int = 0,
    until: Int = warp.diskIndex.size,
) {
    var shiftF = (centerLonDeg / 360.0) * texW % texW
    if (shiftF < 0) shiftF += texW
    val shift = shiftF.toInt()
    val w1 = ((shiftF - shift) * 256).toInt()
    val w0 = 256 - w1
    val diskIndex = warp.diskIndex
    val rowBase = warp.rowBase
    val relCol = warp.relCol
    if (w1 == 0) {
        for (i in from until until) {
            var col = relCol[i] + shift
            if (col >= texW) col -= texW
            val c = tex[rowBase[i] + col]
            val j = diskIndex[i] * 4
            out[j] = (c and 0xFF).toByte()
            out[j + 1] = (c shr 8 and 0xFF).toByte()
            out[j + 2] = (c shr 16 and 0xFF).toByte()
            out[j + 3] = (c shr 24 and 0xFF).toByte()
        }
        return
    }
    for (i in from until until) {
        var col = relCol[i] + shift
        if (col >= texW) col -= texW
        var col2 = col + 1
        if (col2 >= texW) col2 -= texW
        val base = rowBase[i]
        val c0 = tex[base + col]
        val c1 = tex[base + col2]
        val rb = ((c0 and 0x00FF00FF) * w0 + (c1 and 0x00FF00FF) * w1) shr 8 and 0x00FF00FF
        val ag = ((c0 ushr 8 and 0x00FF00FF) * w0 + (c1 ushr 8 and 0x00FF00FF) * w1) and
            0xFF00FF00.toInt()
        val c = ag or rb
        val j = diskIndex[i] * 4
        out[j] = (c and 0xFF).toByte()
        out[j + 1] = (c shr 8 and 0xFF).toByte()
        out[j + 2] = (c shr 16 and 0xFF).toByte()
        out[j + 3] = (c shr 24 and 0xFF).toByte()
    }
}
