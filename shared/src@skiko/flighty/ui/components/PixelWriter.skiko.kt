package flighty.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

internal actual fun createSphereSurface(width: Int, height: Int): SphereSurface =
    object : SphereSurface {
        // Skia N32 is BGRA little-endian; the texture is opaque so
        // premul == straight. Sampled directly in that layout.
        private val bytes = ByteArray(width * height * 4)
        private val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.PREMUL)

        override fun produce(
            warp: GlobeWarp,
            tex: IntArray,
            texW: Int,
            centerLonDeg: Double,
        ): ImageBitmap {
            sampleGlobeBgra(warp, tex, texW, centerLonDeg, bytes)
            val bmp = ImageBitmap(width, height)
            bmp.asSkiaBitmap().installPixels(info, bytes, width * 4)
            return bmp
        }
    }
