package com.exemple.rutesmuntanya

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import org.osmdroid.util.GeoPoint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Perfil d'altitud de la ruta (distància a l'eix X, alçada a l'eix Y).
 * En tocar-lo, selecciona el punt més proper i ho notifica amb [onPointSelected].
 */
class ElevationProfileView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var dist = DoubleArray(0)
    private var ele = DoubleArray(0)
    private var minEle = 0.0
    private var maxEle = 0.0
    private var totalDist = 0.0
    private var hasData = false
    private var selectedIndex = -1

    /** Es crida amb l'índex del punt de la ruta seleccionat. */
    var onPointSelected: ((Int) -> Unit)? = null

    private val density = resources.displayMetrics.density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density * 2f
        color = Color.WHITE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#5581D4FA")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 11f * density
    }
    private val selLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density * 1.5f
        color = Color.parseColor("#FF6D00")
    }
    private val selDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF6D00")
    }

    private val path = Path()

    fun setData(points: List<GeoPoint>, elevations: List<Double>) {
        val n = points.size
        if (n < 2) {
            hasData = false
            invalidate()
            return
        }
        // Primer valor d'altitud vàlid.
        var firstValid = Double.NaN
        for (v in elevations) {
            if (!v.isNaN()) {
                firstValid = v
                break
            }
        }
        if (firstValid.isNaN()) {
            hasData = false
            invalidate()
            return
        }

        val d = DoubleArray(n)
        val e = DoubleArray(n)
        var acc = 0.0
        var prevValid = firstValid
        for (i in 0 until n) {
            if (i > 0) acc += points[i - 1].distanceToAsDouble(points[i])
            d[i] = acc
            val v = elevations.getOrElse(i) { Double.NaN }
            if (v.isNaN()) {
                e[i] = prevValid
            } else {
                e[i] = v
                prevValid = v
            }
        }
        dist = d
        ele = e
        totalDist = acc
        minEle = e.minOrNull() ?: 0.0
        maxEle = e.maxOrNull() ?: 1.0
        if (maxEle - minEle < 1.0) maxEle = minEle + 1.0
        hasData = true
        selectedIndex = -1
        invalidate()
    }

    fun clearSelection() {
        selectedIndex = -1
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val padL = 6f * density
        val padR = 6f * density
        val padT = 18f * density
        val padB = 16f * density

        if (!hasData) {
            canvas.drawText("Sense dades d'altitud", padL, h / 2f, textPaint)
            return
        }

        val plotW = w - padL - padR
        val plotH = h - padT - padB
        val n = dist.size
        val step = max(1, n / 1000)

        path.reset()
        var started = false
        var i = 0
        while (i < n) {
            val x = padL + (if (totalDist > 0) (dist[i] / totalDist).toFloat() else 0f) * plotW
            val y = padT + (1f - ((ele[i] - minEle) / (maxEle - minEle)).toFloat()) * plotH
            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
            if (i == n - 1) break
            i = min(i + step, n - 1)
        }

        val fill = Path(path)
        fill.lineTo(padL + plotW, padT + plotH)
        fill.lineTo(padL, padT + plotH)
        fill.close()
        canvas.drawPath(fill, fillPaint)
        canvas.drawPath(path, linePaint)

        canvas.drawText("${maxEle.roundToInt()} m", padL, padT - 4f * density, textPaint)
        canvas.drawText("${minEle.roundToInt()} m", padL, h - 4f * density, textPaint)

        if (selectedIndex in 0 until n) {
            val x = padL + (if (totalDist > 0) (dist[selectedIndex] / totalDist).toFloat() else 0f) * plotW
            val y = padT + (1f - ((ele[selectedIndex] - minEle) / (maxEle - minEle)).toFloat()) * plotH
            canvas.drawLine(x, padT, x, padT + plotH, selLinePaint)
            canvas.drawCircle(x, y, 4f * density, selDotPaint)

            val km = dist[selectedIndex] / 1000.0
            val label = "%d m · %.2f km".format(ele[selectedIndex].roundToInt(), km)
            val tw = textPaint.measureText(label)
            val lx = (x - tw / 2f).coerceIn(padL, (w - padR - tw).coerceAtLeast(padL))
            canvas.drawText(label, lx, padT - 4f * density, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!hasData) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val padL = 6f * density
                val padR = 6f * density
                val plotW = (width - padL - padR).coerceAtLeast(1f)
                val frac = ((event.x - padL) / plotW).coerceIn(0f, 1f)
                val target = frac * totalDist
                selectedIndex = nearestIndexByDistance(target.toDouble())
                onPointSelected?.invoke(selectedIndex)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun nearestIndexByDistance(target: Double): Int {
        if (dist.isEmpty()) return -1
        var lo = 0
        var hi = dist.size - 1
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (dist[mid] < target) lo = mid + 1 else hi = mid
        }
        if (lo > 0 && abs(dist[lo - 1] - target) <= abs(dist[lo] - target)) return lo - 1
        return lo
    }
}
