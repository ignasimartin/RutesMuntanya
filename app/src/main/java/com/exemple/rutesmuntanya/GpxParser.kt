package com.exemple.rutesmuntanya

import org.osmdroid.util.GeoPoint
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

/**
 * Analitzador senzill de fitxers GPX.
 * Llegeix els punts de la traça (trkpt), la ruta (rtept) o els punts solts (wpt)
 * i calcula la distància total i el desnivell acumulat.
 */
object GpxParser {

    data class GpxResult(
        val name: String?,
        val points: List<GeoPoint>,
        val elevations: List<Double>,   // paral·lel a points; pot contenir NaN
        val totalDistanceMeters: Double,
        val elevationGainMeters: Double,
        val elevationLossMeters: Double,
        val minElevation: Double,   // NaN si no hi ha dades d'altitud
        val maxElevation: Double    // NaN si no hi ha dades d'altitud
    )

    fun parse(input: InputStream): GpxResult {
        val points = ArrayList<GeoPoint>()
        val elevations = ArrayList<Double>() // paral·lel a points; pot contenir NaN

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(input, null)

        var event = parser.eventType
        var lat = 0.0
        var lon = 0.0
        var currentEle = Double.NaN
        var inPoint = false
        var currentTag = ""
        var routeName: String? = null

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "trkpt" || currentTag == "rtept" || currentTag == "wpt") {
                        val la = parser.getAttributeValue(null, "lat")
                        val lo = parser.getAttributeValue(null, "lon")
                        if (la != null && lo != null) {
                            lat = la.toDoubleOrNull() ?: 0.0
                            lon = lo.toDoubleOrNull() ?: 0.0
                            currentEle = Double.NaN
                            inPoint = true
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    if (inPoint && currentTag == "ele") {
                        val text = parser.text?.trim()
                        if (!text.isNullOrEmpty()) {
                            currentEle = text.toDoubleOrNull() ?: Double.NaN
                        }
                    } else if (!inPoint && currentTag == "name" && routeName == null) {
                        val text = parser.text?.trim()
                        if (!text.isNullOrEmpty()) routeName = text
                    }
                }

                XmlPullParser.END_TAG -> {
                    val end = parser.name
                    if (end == "trkpt" || end == "rtept" || end == "wpt") {
                        if (inPoint) {
                            points.add(
                                if (!currentEle.isNaN()) GeoPoint(lat, lon, currentEle)
                                else GeoPoint(lat, lon)
                            )
                            elevations.add(currentEle)
                            inPoint = false
                        }
                    }
                    currentTag = ""
                }
            }
            event = parser.next()
        }

        return GpxResult(
            name = routeName,
            points = points,
            elevations = elevations,
            totalDistanceMeters = computeDistance(points),
            elevationGainMeters = computeGain(elevations, positive = true),
            elevationLossMeters = computeGain(elevations, positive = false),
            minElevation = elevations.filter { !it.isNaN() }.minOrNull() ?: Double.NaN,
            maxElevation = elevations.filter { !it.isNaN() }.maxOrNull() ?: Double.NaN
        )
    }

    private fun computeDistance(points: List<GeoPoint>): Double {
        var total = 0.0
        for (i in 1 until points.size) {
            total += points[i - 1].distanceToAsDouble(points[i])
        }
        return total
    }

    /**
     * Suma els desnivells positius (pujada) o negatius (baixada).
     * Aplica un llindar de 2 m per reduir el soroll de l'altímetre GPS.
     */
    private fun computeGain(elevations: List<Double>, positive: Boolean): Double {
        val threshold = 2.0
        var accumulated = 0.0
        var lastValid = Double.NaN
        for (e in elevations) {
            if (e.isNaN()) continue
            if (!lastValid.isNaN()) {
                val diff = e - lastValid
                if (positive && diff > threshold) accumulated += diff
                if (!positive && diff < -threshold) accumulated += -diff
                if (kotlin.math.abs(diff) > threshold) lastValid = e
            } else {
                lastValid = e
            }
        }
        return accumulated
    }
}
