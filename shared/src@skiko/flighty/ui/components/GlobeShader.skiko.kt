package flighty.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeShader
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.SamplingMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal actual fun createGlobeShaderRenderer(texture: ImageBitmap): GlobeShaderRenderer? =
    runCatching {
        val effect = RuntimeEffect.makeForShader(GLOBE_SKSL.trimIndent())
        val image = Image.makeFromBitmap(texture.asSkiaBitmap())
        val texShader = image.makeShader(sampling = SamplingMode.LINEAR)
        val texW = texture.width.toFloat()
        val texH = texture.height.toFloat()
        object : GlobeShaderRenderer {
            private val builder = RuntimeShaderBuilder(effect).apply {
                child("tex", texShader)
                uniform("texSize", texW, texH)
            }

            override fun brush(
                centerX: Float,
                centerY: Float,
                radius: Float,
                centerLatDeg: Double,
                centerLonDeg: Double,
            ): Brush {
                val phi0 = centerLatDeg * PI / 180.0
                builder.uniform("c", centerX, centerY)
                builder.uniform("r", radius)
                builder.uniform("sinPhi0", sin(phi0).toFloat())
                builder.uniform("cosPhi0", cos(phi0).toFloat())
                builder.uniform("lam0", (centerLonDeg * PI / 180.0).toFloat())
                val shader = builder.makeShader().asComposeShader()
                return object : ShaderBrush() {
                    override fun createShader(size: Size): Shader = shader
                }
            }
        }
    }.getOrNull()
