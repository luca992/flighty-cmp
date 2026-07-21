package flighty.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

private var scratch: ByteArray = ByteArray(0)

internal actual fun ImageBitmap.writePixels(pixels: IntArray, width: Int, height: Int) {
    val byteCount = pixels.size * 4
    if (scratch.size < byteCount) scratch = ByteArray(byteCount)
    val bytes = scratch
    // Skia N32 is BGRA little-endian; the texture is opaque so premul == straight.
    for (i in pixels.indices) {
        val p = pixels[i]
        val j = i * 4
        bytes[j] = (p and 0xFF).toByte()            // B
        bytes[j + 1] = ((p shr 8) and 0xFF).toByte()  // G
        bytes[j + 2] = ((p shr 16) and 0xFF).toByte() // R
        bytes[j + 3] = ((p shr 24) and 0xFF).toByte() // A
    }
    asSkiaBitmap().installPixels(
        ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.PREMUL),
        bytes,
        width * 4L,
    )
}
