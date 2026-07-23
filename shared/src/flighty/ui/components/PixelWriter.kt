package flighty.ui.components

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Per-platform sphere-raster transport, each target on its fastest path:
 * Android samples ARGB ints straight into Bitmap.setPixels; Skiko targets
 * (desktop, iOS, web) sample straight into the BGRA byte layout that
 * installPixels wants, skipping a per-keyframe conversion pass.
 *
 * [produce] returns a FRESH bitmap each call: the UI may still be drawing
 * the previously published one frames later, and recycling buffers under it
 * desynchronizes the drift compensation (the globe visibly rocks).
 */
internal interface SphereSurface {
    fun produce(warp: GlobeWarp, tex: IntArray, texW: Int, centerLonDeg: Double): ImageBitmap
}

internal expect fun createSphereSurface(width: Int, height: Int): SphereSurface
