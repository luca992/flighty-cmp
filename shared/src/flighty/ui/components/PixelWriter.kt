package flighty.ui.components

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Writes ARGB pixels into an [ImageBitmap] in place. Compose's common API can
 * read pixels ([androidx.compose.ui.graphics.toPixelMap]) but not write them,
 * so the textured globe's warp buffer needs this tiny platform hook.
 */
internal expect fun ImageBitmap.writePixels(pixels: IntArray, width: Int, height: Int)
