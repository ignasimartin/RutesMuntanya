package com.exemple.rutesmuntanya

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import kotlin.math.atan2

/**
 * Dibuixa petites fletxes al llarg de la ruta indicant-ne el sentit de la marxa.
 * [arrowIndices] són índexs i tals que la fletxa apunta de points[i] cap a points[i+1].
 */
class RouteArrowsOverlay(
    private val points: List<GeoPoint>,
    private val arrowIndices: List<Int>,
    density: Float
) : Overlay() {

    private val pA = Point()
    private val pB = Point()
    private val path = Path()
    private val size = density * 7f

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AA000000")
        style = Paint.Style.STROKE
        strokeWidth = density * 1.2f
    }

    override fun draw(canvas: Canvas, projection: Projection) {
        for (i in arrowIndices) {
            if (i + 1 >= points.size) continue
            projection.toPixels(points[i], pA)
            projection.toPixels(points[i + 1], pB)
            val dx = (pB.x - pA.x).toFloat()
            val dy = (pB.y - pA.y).toFloat()
            if (dx == 0f && dy == 0f) continue

            val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            val mx = (pA.x + pB.x) / 2f
            val my = (pA.y + pB.y) / 2f

            path.reset()
            path.moveTo(mx + size, my)                 // punta (cap a +x)
            path.lineTo(mx - size * 0.8f, my - size * 0.75f)
            path.lineTo(mx - size * 0.8f, my + size * 0.75f)
            path.close()

            canvas.save()
            canvas.rotate(angle, mx, my)
            canvas.drawPath(path, fill)
            canvas.drawPath(path, stroke)
            canvas.restore()
        }
    }
}
