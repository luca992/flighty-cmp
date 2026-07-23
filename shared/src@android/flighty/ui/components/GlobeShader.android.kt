package flighty.ui.components

import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader as AndroidShader
import android.os.Build
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal actual fun createGlobeShaderRenderer(texture: ImageBitmap): GlobeShaderRenderer? {
    if (Build.VERSION.SDK_INT < 33) return null   // AGSL needs 13+; CPU path below
    return runCatching {
        val shader = RuntimeShader(GLOBE_SKSL.trimIndent())
        shader.setInputShader(
            "tex",
            BitmapShader(
                texture.asAndroidBitmap(),
                AndroidShader.TileMode.CLAMP,
                AndroidShader.TileMode.CLAMP,
            ),
        )
        shader.setFloatUniform("texSize", texture.width.toFloat(), texture.height.toFloat())
        object : GlobeShaderRenderer {
            override fun brush(
                centerX: Float,
                centerY: Float,
                radius: Float,
                centerLatDeg: Double,
                centerLonDeg: Double,
            ): Brush {
                val phi0 = centerLatDeg * PI / 180.0
                shader.setFloatUniform("c", centerX, centerY)
                shader.setFloatUniform("r", radius)
                shader.setFloatUniform("sinPhi0", sin(phi0).toFloat())
                shader.setFloatUniform("cosPhi0", cos(phi0).toFloat())
                shader.setFloatUniform("lam0", (centerLonDeg * PI / 180.0).toFloat())
                return object : ShaderBrush() {
                    override fun createShader(size: Size): Shader = shader
                }
            }
        }
    }.getOrNull()
}
