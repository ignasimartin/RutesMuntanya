package com.exemple.rutesmuntanya

import org.osmdroid.util.GeoPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Guia el seguiment d'una ruta ja carregada (el track del .gpx).
 * No calcula rutes: només indica cap on continuar per la línia i si te'n surts.
 * Detecta sol el sentit de la marxa segons com avances.
 */
class RouteGuidance(private val points: List<GeoPoint>) {

    enum class Kind { STRAIGHT, LEFT, RIGHT, ARRIVE, OFF_ROUTE }

    data class State(
        val kind: Kind,
        val distanceToTurnMeters: Double,
        val crossTrackMeters: Double,
        val progressMeters: Double,
        val forward: Boolean
    )

    private val n = points.size
    private val cum = DoubleArray(n)                       // distància acumulada a cada vèrtex
    private val total: Double
    private data class Maneuver(val dist: Double, val side: Int) // +1 dreta, -1 esquerra
    private val maneuvers = ArrayList<Maneuver>()

    private var lastProgress = Double.NaN
    private var forward = true

    // Llindars (metres / graus)
    private val offRouteMeters = 30.0
    private val arriveMeters = 20.0
    private val straightUntilMeters = 120.0
    private val turnThresholdDeg = 35.0

    init {
        for (i in 1 until n) cum[i] = cum[i - 1] + points[i - 1].distanceToAsDouble(points[i])
        total = if (n > 0) cum[n - 1] else 0.0
        buildManeuvers()
    }

    private fun buildManeuvers() {
        if (n < 3) return
        val raw = ArrayList<Maneuver>()
        for (i in 1 until n - 1) {
            val d = cum[i]
            val before = interpPoint(d - 15.0)
            val after = interpPoint(d + 15.0)
            val turn = normDelta(bearing(points[i], after) - bearing(before, points[i]))
            if (abs(turn) >= turnThresholdDeg) raw.add(Maneuver(d, if (turn > 0) 1 else -1))
        }
        // Fusiona girs molt propers (< 20 m) en un de sol.
        var idx = 0
        while (idx < raw.size) {
            var j = idx
            while (j + 1 < raw.size && raw[j + 1].dist - raw[idx].dist < 20.0) j++
            maneuvers.add(raw[idx])
            idx = j + 1
        }
    }

    /** Actualitza la guia amb la posició actual. */
    fun update(me: GeoPoint): State {
        val near = nearest(me)
        val progress = near.progress
        val cross = near.cross

        if (!lastProgress.isNaN()) {
            val delta = progress - lastProgress
            if (abs(delta) > 1.5) forward = delta > 0
        }
        lastProgress = progress

        if (cross > offRouteMeters) {
            return State(Kind.OFF_ROUTE, cross, cross, progress, forward)
        }

        val next = nextManeuver(progress)
        if (next == null) {
            val remaining = if (forward) total - progress else progress
            return if (remaining <= arriveMeters)
                State(Kind.ARRIVE, 0.0, cross, progress, forward)
            else
                State(Kind.STRAIGHT, remaining, cross, progress, forward)
        }

        val distToTurn = abs(next.dist - progress)
        var side = next.side
        if (!forward) side = -side // si anem al revés, els girs s'inverteixen
        return when {
            distToTurn > straightUntilMeters -> State(Kind.STRAIGHT, distToTurn, cross, progress, forward)
            side > 0 -> State(Kind.RIGHT, distToTurn, cross, progress, forward)
            else -> State(Kind.LEFT, distToTurn, cross, progress, forward)
        }
    }

    private fun nextManeuver(progress: Double): Maneuver? {
        if (maneuvers.isEmpty()) return null
        return if (forward) {
            maneuvers.firstOrNull { it.dist > progress + 2.0 }
        } else {
            maneuvers.lastOrNull { it.dist < progress - 2.0 }
        }
    }

    // ---------------- geometria ----------------

    private class Near(val progress: Double, val cross: Double)

    /** Punt més proper de la ruta (distància transversal + distància recorreguda). */
    private fun nearest(me: GeoPoint): Near {
        var bestCross = Double.MAX_VALUE
        var bestProgress = 0.0
        val mLat = 111320.0
        val mLon = 111320.0 * cos(Math.toRadians(me.latitude))
        for (i in 0 until n - 1) {
            val ax = (points[i].longitude - me.longitude) * mLon
            val ay = (points[i].latitude - me.latitude) * mLat
            val bx = (points[i + 1].longitude - me.longitude) * mLon
            val by = (points[i + 1].latitude - me.latitude) * mLat
            val dx = bx - ax
            val dy = by - ay
            val segLen2 = dx * dx + dy * dy
            var t = if (segLen2 > 0) -(ax * dx + ay * dy) / segLen2 else 0.0
            if (t < 0.0) t = 0.0
            if (t > 1.0) t = 1.0
            val px = ax + t * dx
            val py = ay + t * dy
            val cross = sqrt(px * px + py * py)
            if (cross < bestCross) {
                bestCross = cross
                val segLen = points[i].distanceToAsDouble(points[i + 1])
                bestProgress = cum[i] + t * segLen
            }
        }
        return Near(bestProgress, bestCross)
    }

    private fun interpPoint(dInput: Double): GeoPoint {
        val d = dInput.coerceIn(0.0, total)
        var lo = 0
        var hi = n - 1
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (cum[mid] < d) lo = mid + 1 else hi = mid
        }
        val i = if (lo > 0) lo - 1 else 0
        val segLen = cum[i + 1] - cum[i]
        val t = if (segLen > 0) (d - cum[i]) / segLen else 0.0
        val lat = points[i].latitude + t * (points[i + 1].latitude - points[i].latitude)
        val lon = points[i].longitude + t * (points[i + 1].longitude - points[i].longitude)
        return GeoPoint(lat, lon)
    }

    private fun bearing(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    private fun normDelta(x: Double): Double = (x + 540.0) % 360.0 - 180.0
}
