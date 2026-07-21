package flighty.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap

internal actual fun ImageBitmap.writePixels(pixels: IntArray, width: Int, height: Int) {
    asAndroidBitmap().setPixels(pixels, 0, width, 0, 0, width, height)
}
