package flighty.ui.components

import flighty.model.Flight
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Sampled great-circle path as (lon, lat) pairs with unwrapped longitudes. */
internal fun greatCircle(flight: Flight, samples: Int = 48): List<Pair<Double, Double>> {
    val o = flight.origin
    val d = flight.destination
    fun rad(x: Double) = x * PI / 180
    fun deg(x: Double) = x * 180 / PI
    fun vec(latR: Double, lonR: Double) =
        doubleArrayOf(cos(latR) * cos(lonR), cos(latR) * sin(lonR), sin(latR))

    val a = vec(rad(o.latitude), rad(o.longitude))
    val b = vec(rad(d.latitude), rad(d.longitude))
    val dot = (a[0] * b[0] + a[1] * b[1] + a[2] * b[2]).coerceIn(-1.0, 1.0)
    val omega = acos(dot)
    val sinOmega = sin(omega)
    if (sinOmega < 1e-9) {
        return listOf(o.longitude to o.latitude, d.longitude to d.latitude)
    }

    val points = ArrayList<Pair<Double, Double>>(samples + 1)
    for (i in 0..samples) {
        val t = i.toDouble() / samples
        val s1 = sin((1 - t) * omega) / sinOmega
        val s2 = sin(t * omega) / sinOmega
        val x = s1 * a[0] + s2 * b[0]
        val y = s1 * a[1] + s2 * b[1]
        val z = s1 * a[2] + s2 * b[2]
        points += deg(atan2(y, x)) to deg(asin(z.coerceIn(-1.0, 1.0)))
    }
    // Unwrap longitudes so antimeridian crossings stay continuous.
    val unwrapped = ArrayList<Pair<Double, Double>>(points.size)
    var prevLon = points.first().first
    var offset = 0.0
    for ((lon, lat) in points) {
        var l = lon + offset
        if (l - prevLon > 180) { offset -= 360; l -= 360 }
        if (l - prevLon < -180) { offset += 360; l += 360 }
        unwrapped += l to lat
        prevLon = l
    }
    return unwrapped
}
