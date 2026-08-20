package com.exemple.rutesmuntanya

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

/**
 * Utilitats de dibuix: icona d'ubicació (punt blau + fletxa) i color segons pendent.
 */
object Graphics {

    private const val BLUE = "#1E88E5"

    /** Punt blau amb vora blanca per marcar la posició actual. */
    fun blueDot(density: Float): Bitmap {
        val size = (24 * density).toInt().coerceAtLeast(24)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = size / 2f
        val cy = size / 2f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.WHITE
        c.drawCircle(cx, cy, size * 0.46f, p)
        p.color = Color.parseColor(BLUE)
        c.drawCircle(cx, cy, size * 0.34f, p)
        return bmp
    }

    /**
     * Fletxa que indica cap on estàs orientat. Apunta cap amunt (nord) i es rota
     * segons la brúixola. Es dibuixa centrada sobre el punt blau.
     */
    fun facingArrow(density: Float): Bitmap {
        val size = (48 * density).toInt().coerceAtLeast(48)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = size / 2f
        val cy = size / 2f

        val path = Path()
        path.moveTo(cx, cy - size * 0.42f)            // punta (amunt)
        path.lineTo(cx - size * 0.17f, cy - size * 0.08f)
        path.lineTo(cx + size * 0.17f, cy - size * 0.08f)
        path.close()

        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        fill.color = Color.parseColor(BLUE)
        fill.style = Paint.Style.FILL
        c.drawPath(path, fill)

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG)
        stroke.color = Color.WHITE
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = density * 1.5f
        c.drawPath(path, stroke)
        return bmp
    }

    /**
     * Color segons el pendent (grade = desnivell / distància).
     * Verd = baixada forta, groc = pla, vermell = pujada forta.
     */
    fun slopeColor(grade: Double): Int {
        val maxGrade = 0.20 // ±20 %
        val g = grade.coerceIn(-maxGrade, maxGrade)
        val t = ((g + maxGrade) / (2 * maxGrade)).toFloat() // 0 = baixada forta, 1 = pujada forta
        val hue = (1f - t) * 120f // baixada -> verd (120), pla -> groc (60), pujada -> vermell (0)
        val saturation = 0.85f
        val value = 0.80f + 0.15f * (1f - t) // el verd (baixada) surt una mica més clar
        return Color.HSVToColor(floatArrayOf(hue, saturation, value.coerceIn(0f, 1f)))
    }

    /** Punt taronja per marcar el punt seleccionat des del perfil d'altitud. */
    fun selectionDot(density: Float): Bitmap {
        val size = (18 * density).toInt().coerceAtLeast(18)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = size / 2f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.WHITE
        c.drawCircle(cx, cx, size * 0.46f, p)
        p.color = Color.parseColor("#FF6D00")
        c.drawCircle(cx, cx, size * 0.32f, p)
        return bmp
    }

    /** Color per a trams sense dades d'altitud. */
    fun neutralColor(): Int = Color.parseColor("#78909C")
}
