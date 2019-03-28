package com.anwesh.uiprojects.linetosemicircleview

/**
 * Created by anweshmishra on 28/03/19.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.app.Activity
import android.content.Context

val nodes : Int = 5
val lines : Int = 2
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#673AB7")
val backColor : Int = Color.parseColor("#BDBDBD")
val delay : Long = 20
val lSizeFactor : Int = 4

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float = (1 - scaleFactor()) * a.inverse() + scaleFactor() * b.inverse()
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap
fun Int.sjf() : Float = 1f - 2 * this

fun Canvas.drawArrow(x : Float, sy : Float, dy : Float, size : Float, sc1 : Float, sc2 : Float, paint : Paint) {
    val lSize : Float = size / lSizeFactor
    save()
    translate(x, sy + (dy - sy) * sc1)
    drawLine(0f, 0f, size, 0f, paint)
    for (j in 0..(lines - 1)) {
        save()
        translate(size, 0f)
        rotate(75f * sc2.divideScale(j, lines) * j.sjf())
        drawLine(0f, 0f, -lSize, 0f, paint)
        restore()
    }
    restore()
}

fun Canvas.drawLTSNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    paint.style = Paint.Style.STROKE
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    drawArrow(0f, h, gap * (i + 1) - size, size, sc1, sc2, paint)
    save()
    translate(w / 2, gap * (i + 1))
    for (j in 0..(lines - 1)) {
        save()
        rotate(-180f * sc1 * j)
        translate(w / 2 * sc2.divideScale(j, lines), 0f)
        drawLine(0f, 0f, size, 0f, paint)
        restore()
    }
    drawArc(RectF(-size, -size, size, size), 180f + 180f * (1 - sc1), 180f * sc1, false, paint)
    restore()
}

class LineToSemiCircleView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, lines, 1)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class LTSCNode(var i : Int, val state : State = State()) {

        private var next : LTSCNode? = null
        private var prev : LTSCNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = LTSCNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawLTSNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : LTSCNode {
            var curr : LTSCNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class LineToSemiCircle(var i : Int) {

        private val root : LTSCNode = LTSCNode(0)
        private var curr : LTSCNode = root
        private var dir : Int = 1
        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : LineToSemiCircleView) {

        private val animator : Animator = Animator(view)
        private val ltsc : LineToSemiCircle = LineToSemiCircle(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            ltsc.draw(canvas, paint)
            animator.animate {
                ltsc.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            ltsc.startUpdating {
                animator.start()
            }
        }
    }
    companion object {
        fun create(activity : Activity) : LineToSemiCircleView {
            val view : LineToSemiCircleView = LineToSemiCircleView(activity)
            activity.setContentView(view)
            return view
        }
    }
}