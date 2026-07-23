package flighty.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.coroutines.yield
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

internal actual fun createSphereSurface(width: Int, height: Int): SphereSurface =
    object : SphereSurface {
        // Skia N32 is BGRA little-endian; the texture is opaque so
        // premul == straight. Sampled directly in that layout.
        private val bytes = ByteArray(width * height * 4)
        private val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.PREMUL)

        override suspend fun produce(
            warp: GlobeWarp,
            tex: IntArray,
            texW: Int,
            centerLonDeg: Double,
        ): ImageBitmap {
            val n = warp.diskIndex.size
            var i = 0
            while (i < n) {
                val end = minOf(i + SPHERE_CHUNK, n)
                sampleGlobeBgra(warp, tex, texW, centerLonDeg, bytes, i, end)
                i = end
                yield()
            }
            val bmp = ImageBitmap(width, height)
            bmp.asSkiaBitmap().installPixels(info, bytes, width * 4)
            return bmp
        }
    }
