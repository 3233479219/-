package com.example.bluefish

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class FishView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onMove: ((Float, Float) -> Unit)? = null
    var onTap: (() -> Unit)? = null

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val rx = 70f * d
    private val ry = 54f * d

    private val bodyPath = Path()
    private val tailPath = Path()
    private val dorsalPath = Path()
    private val pectPath = Path()
    private val mouthPath = Path()

    private var t = 0f
    private var happy = 0f
    private var blinkCd = 2.5f
    private var blinkT = 0f
    private val blinkDur = 0.16f
    private var bubbleCd = 1.2f
    private var lastFrameNs = 0L

    private val bubbles = ArrayList<Bubble>()

    private class Bubble(
        var x: Float, var y: Float,
        val r: Float, val speed: Float,
        val phase: Float, var life: Float
    )

    init {
        bodyPath.addOval(-rx, -ry, rx, ry, Path.Direction.CW)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension((300 * d).toInt(), (280 * d).toInt())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastFrameNs = System.nanoTime()
        postOnAnimation(tick)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow) return
            val now = System.nanoTime()
            var dt = (now - lastFrameNs) / 1_000_000_000f
            lastFrameNs = now
            if (dt > 0.05f) dt = 0.05f
            t += dt
            update(dt)
            invalidate()
            postOnAnimation(this)
        }
    }

    private fun update(dt: Float) {
        happy = max(0f, happy - dt * 2.2f)

        if (blinkT > 0f) {
            blinkT -= dt
            if (blinkT < 0f) blinkT = 0f
        } else {
            blinkCd -= dt
            if (blinkCd <= 0f) {
                blinkT = blinkDur
                blinkCd = 2.2f + Random.nextFloat() * 3.5f
            }
        }

        bubbleCd -= dt
        if (bubbleCd <= 0f) {
            bubbleCd = 0.9f + Random.nextFloat() * 1.6f
            bubbles.add(
                Bubble(
                    x = width / 2f + rx * 0.55f + Random.nextFloat() * 10f * d,
                    y = height / 2f + ry * 0.15f + Random.nextFloat() * 12f * d,
                    r = (3f + Random.nextFloat() * 4.5f) * d,
                    speed = (26f + Random.nextFloat() * 40f) * d,
                    phase = Random.nextFloat() * 6.2832f,
                    life = 1f
                )
            )
        }

        val it = bubbles.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.y -= b.speed * dt
            b.life -= dt * 0.22f
            if (b.life <= 0f || b.y < -40f * d) it.remove()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f

        val bob = sin(t * 1.7f) * 6f * d
        val breathe = 1f + sin(t * 1.7f + 0.6f) * 0.015f
        val pop = (1f + happy * 0.07f) * breathe

        canvas.save()
        canvas.translate(cx, cy + bob)
        canvas.scale(pop, pop)

        drawTail(canvas)
        drawFins(canvas)
        drawBody(canvas)
        drawFace(canvas)

        canvas.restore()

        drawBubbles(canvas)
    }

    private fun drawTail(c: Canvas) {
        tailPath.reset()
        tailPath.moveTo(-rx * 0.50f, -ry * 0.55f)
        tailPath.cubicTo(
            -rx * 1.05f, -ry * 0.85f,
            -rx * 1.65f, -ry * 0.95f,
            -rx * 1.70f, -ry * 0.45f
        )
        tailPath.cubicTo(
            -rx * 1.40f, -ry * 0.12f,
            -rx * 1.40f, ry * 0.12f,
            -rx * 1.70f, ry * 0.45f
        )
        tailPath.cubicTo(
            -rx * 1.65f, ry * 0.95f,
            -rx * 1.05f, ry * 0.85f,
            -rx * 0.50f, ry * 0.55f
        )
        tailPath.close()

        c.save()
        c.rotate(sin(t * 2.2f) * 9f, -rx * 0.6f, 0f)
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            -rx * 1.70f, 0f, -rx * 0.40f, 0f,
            0xFF2E7FD4.toInt(), 0xFF74C6FF.toInt(),
            Shader.TileMode.CLAMP
        )
        c.drawPath(tailPath, paint)
        paint.shader = null
        c.restore()
    }

    private fun drawFins(c: Canvas) {
        dorsalPath.reset()
        dorsalPath.moveTo(-rx * 0.42f, -ry * 0.85f)
        dorsalPath.cubicTo(
            -rx * 0.45f, -ry * 1.35f,
            rx * 0.05f, -ry * 1.45f,
            rx * 0.28f, -ry * 0.86f
        )
        dorsalPath.close()
        paint.style = Paint.Style.FILL
        paint.color = 0xFF3E90E0.toInt()
        c.drawPath(dorsalPath, paint)

        pectPath.reset()
        pectPath.moveTo(-rx * 0.15f, ry * 0.45f)
        pectPath.cubicTo(
            -rx * 0.45f, ry * 0.95f,
            rx * 0.00f, ry * 1.20f,
            rx * 0.32f, ry * 0.72f
        )
        pectPath.close()

        c.save()
        c.rotate(sin(t * 2.2f + 1.2f) * 12f, rx * 0.05f, ry * 0.55f)
        paint.color = 0xFF66B8F8.toInt()
        c.drawPath(pectPath, paint)
        c.restore()
    }

    private fun drawBody(c: Canvas) {
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            -rx * 0.25f, -ry * 0.35f, rx * 1.55f,
            intArrayOf(0xFF9BD8FF.toInt(), 0xFF4FA6F0.toInt(), 0xFF2A6CC0.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawPath(bodyPath, paint)
        paint.shader = null

        c.save()
        c.clipPath(bodyPath)
        paint.color = 0x55C6ECFF
        c.drawOval(-rx * 0.75f, ry * 0.05f, rx * 0.85f, ry * 1.05f, paint)
        paint.color = 0x33FFFFFF
        c.drawOval(-rx * 0.95f, -ry * 0.95f, rx * 0.35f, -ry * 0.10f, paint)
        c.restore()
    }

    private fun drawFace(c: Canvas) {
        val ex = rx * 0.38f
        val ey = -ry * 0.22f
        val er = 16f * d

        var bs = 1f
        if (blinkT > 0f) {
            val p = 1f - blinkT / blinkDur
            bs = max(0.08f, abs(cos(p * Math.PI)).toFloat())
        }

        c.save()
        c.translate(ex, ey)
        c.scale(1f, bs)

        paint.style = Paint.Style.FILL
        paint.color = 0x33000000
        c.drawCircle(0f, 0f, er + 2.5f * d, paint)
        paint.color = Color.WHITE
        c.drawCircle(0f, 0f, er, paint)
        paint.color = 0xFF1D2B3F.toInt()
        c.drawCircle(er * 0.22f, 0f, er * 0.60f, paint)
        paint.color = Color.WHITE
        c.drawCircle(er * 0.45f, -er * 0.30f, er * 0.22f, paint)
        c.drawCircle(-er * 0.15f, er * 0.35f, er * 0.10f, paint)

        c.restore()

        paint.style = Paint.Style.FILL
        paint.color = 0x55FF8FA3
        c.drawOval(ex - er * 0.40f, ey + er * 0.95f, ex + er * 1.15f, ey + er * 1.60f, paint)

        mouthPath.reset()
        mouthPath.moveTo(rx * 0.55f, ry * 0.12f)
        mouthPath.quadraticTo(rx * 0.72f, ry * 0.30f, rx * 0.50f, ry * 0.42f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.2f * d
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = 0xFF1D2B3F.toInt()
        c.drawPath(mouthPath, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawBubbles(c: Canvas) {
        for (b in bubbles) {
            val a = (b.life.coerceIn(0f, 1f) * 200).toInt()
            val x = b.x + sin(t * 2f + b.phase) * 5f * d

            paint.style = Paint.Style.FILL
            paint.color = Color.argb(a, 255, 255, 255)
            c.drawCircle(x, b.y, b.r, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f * d
            paint.color = Color.argb((a * 0.9f).toInt(), 160, 215, 255)
            c.drawCircle(x, b.y, b.r, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX; downY = event.rawY
                lastX = downX; lastY = downY
                dragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastX
                val dy = event.rawY - lastY
                if (!dragging) {
                    val ddx = event.rawX - downX
                    val ddy = event.rawY - downY
                    if (sqrt(ddx * ddx + ddy * ddy) > 12f * d) dragging = true
                }
                if (dragging) {
                    onMove?.invoke(dx, dy)
                    lastX = event.rawX
                    lastY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!dragging) {
                    happy = 1f
                    onTap?.invoke()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
