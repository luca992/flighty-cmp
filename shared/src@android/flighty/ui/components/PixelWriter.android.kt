package flighty.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap

internal actual fun createSphereSurface(width: Int, height: Int): SphereSurface =
    object : SphereSurface {
        private val buf = IntArray(width * height)

        override fun produce(
            warp: GlobeWarp,
            tex: IntArray,
            texW: Int,
            centerLonDeg: Double,
        ): ImageBitmap {
            sampleGlobe(warp, tex, texW, centerLonDeg, buf)
            val bmp = ImageBitmap(width, height)
            bmp.asAndroidBitmap().setPixels(buf, 0, width, 0, 0, width, height)
            return bmp
        }
    }
