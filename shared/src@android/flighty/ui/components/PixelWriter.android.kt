package flighty.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlinx.coroutines.yield

internal actual fun createSphereSurface(width: Int, height: Int): SphereSurface =
    object : SphereSurface {
        private val buf = IntArray(width * height)

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
                sampleGlobe(warp, tex, texW, centerLonDeg, buf, i, end)
                i = end
                yield()
            }
            val bmp = ImageBitmap(width, height)
            bmp.asAndroidBitmap().setPixels(buf, 0, width, 0, 0, width, height)
            return bmp
        }
    }
