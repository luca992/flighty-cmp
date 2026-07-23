package flighty.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap

/**
 * GPU path for the textured globe: the same inverse-orthographic mapping the
 * CPU sampler does, evaluated per pixel per frame by a runtime shader. No
 * keyframes, no sampling loop, no drift-compensation slide — the spin is
 * just a uniform. Skia RuntimeEffect (SkSL) serves desktop/iOS/web; Android
 * 13+ runs the identical source as AGSL. Hosts without it (Android < 13, or
 * a failed compile) return null and keep the CPU keyframe pipeline.
 */
internal interface GlobeShaderRenderer {
    /** Brush rendering the sphere for this view; call per frame with the live spin. */
    fun brush(
        centerX: Float,
        centerY: Float,
        radius: Float,
        centerLatDeg: Double,
        centerLonDeg: Double,
    ): Brush
}

internal expect fun createGlobeShaderRenderer(texture: ImageBitmap): GlobeShaderRenderer?

/** SkSL and AGSL share this source. */
internal const val GLOBE_SKSL = """
uniform shader tex;
uniform float2 c;
uniform float r;
uniform float sinPhi0;
uniform float cosPhi0;
uniform float lam0;
uniform float2 texSize;

const float PI = 3.14159265358979;

half4 main(float2 p) {
    float nx = (p.x - c.x) / r;
    float ny = (c.y - p.y) / r;
    float rho2 = nx * nx + ny * ny;
    if (rho2 > 1.0) {
        return half4(0.0);
    }
    float cosC = sqrt(1.0 - rho2);
    float phi = asin(clamp(cosC * sinPhi0 + ny * cosPhi0, -1.0, 1.0));
    float lam = atan(nx, cosPhi0 * cosC - ny * sinPhi0);
    float u = fract((lam0 + lam) / (2.0 * PI) + 0.5);
    float v = 0.5 - phi / PI;
    return tex.eval(float2(u * texSize.x, v * texSize.y));
}
"""
