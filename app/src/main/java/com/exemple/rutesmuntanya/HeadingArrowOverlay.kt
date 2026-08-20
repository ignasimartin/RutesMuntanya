package com.exemple.rutesmuntanya

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

/**
 * Dibuixa una fletxa a la posició actual, rotada segons l'orientació (brúixola).
 * La posició es llegeix en viu a través de [locationProvider].
 */
class HeadingArrowOverlay(
    private val arrow: Bitmap,
    private val locationProvider: () -> GeoPoint?
) : Overlay() {

    /** Graus respecte al nord (sentit horari). */
    var headingDeg: Float = 0f

    private val screenPoint = Point()

    override fun draw(canvas: Canvas, projection: Projection) {
        val loc = locationProvider() ?: return
        projection.toPixels(loc, screenPoint)
        val x = screenPoint.x.toFloat()
        val y = screenPoint.y.toFloat()
        canvas.save()
        canvas.rotate(headingDeg, x, y)
        canvas.drawBitmap(arrow, x - arrow.width / 2f, y - arrow.height / 2f, null)
        canvas.restore()
    }
}
