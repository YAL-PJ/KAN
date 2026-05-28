package com.kan.app.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.kan.app.core.OverlayStyle
import com.kan.app.ui.MainActivity
import kotlin.math.abs
import kotlin.math.min

/**
 * Owns the floating screen-time pill: builds one of three visual styles (bar / dots / ring),
 * tracks budget progress with a color shift, distinguishes drag from tap (tap launches the
 * app), and snaps to the nearest screen edge. Stateful — call from a single thread.
 */
class OverlayController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onPositionChanged: (x: Int, y: Int) -> Unit,
) {
    data class Payload(
        val seconds: Long,
        val budgetSeconds: Long,
        val style: OverlayStyle,
    )

    private var root: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var currentStyle: OverlayStyle? = null
    private var applyPayload: ((Payload) -> Unit)? = null

    fun isShowing(): Boolean = root != null

    fun show(payload: Payload, initialX: Int, initialY: Int) {
        if (!Settings.canDrawOverlays(context)) return
        if (root != null) {
            update(payload)
            return
        }
        val layoutParams = buildLayoutParams(initialX, initialY)
        params = layoutParams
        installStyle(payload, layoutParams)
        applyPayload?.invoke(payload)
    }

    fun update(payload: Payload) {
        val view = root ?: return
        val layoutParams = params ?: return
        if (currentStyle != payload.style) {
            windowManager.removeView(view)
            root = null
            applyPayload = null
            installStyle(payload, layoutParams)
        }
        applyPayload?.invoke(payload)
    }

    fun remove() {
        root?.let { windowManager.removeView(it) }
        root = null
        params = null
        currentStyle = null
        applyPayload = null
    }

    private fun installStyle(payload: Payload, layoutParams: WindowManager.LayoutParams) {
        val (view, updater) = when (payload.style) {
            OverlayStyle.Bar -> buildBarPill()
            OverlayStyle.Dots -> buildDotsPill()
            OverlayStyle.Ring -> buildRingPill()
        }
        installInteraction(view, layoutParams)
        root = view
        currentStyle = payload.style
        applyPayload = updater
        windowManager.addView(view, layoutParams)
    }

    // --- Styles --------------------------------------------------------------

    private fun buildBarPill(): Pair<View, (Payload) -> Unit> {
        val container = pillContainer().apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(7), dp(14), dp(8))
        }
        val label = labelView("SCREEN TIME")
        val time = timeView(13f)
        val limitBar = HorizontalBarView(context)
        val minuteBar = HorizontalBarView(context)
        val secondBar = HorizontalBarView(context)

        container.addView(label)
        container.addView(time, LinearLayout.LayoutParams(LP_WRAP, LP_WRAP).apply { topMargin = dp(1) })
        container.addView(
            limitBar,
            LinearLayout.LayoutParams(dp(78), dp(3)).apply { topMargin = dp(5) },
        )
        container.addView(
            minuteBar,
            LinearLayout.LayoutParams(dp(78), dp(2)).apply { topMargin = dp(3) },
        )
        container.addView(
            secondBar,
            LinearLayout.LayoutParams(dp(78), dp(2)).apply { topMargin = dp(2) },
        )

        val updater: (Payload) -> Unit = { p ->
            val ratio = budgetRatio(p)
            val color = colorForRatio(ratio)
            time.text = clockText(p)
            time.setTextColor(color)
            limitBar.setProgress(ratio, color)
            minuteBar.setProgress(minutePulseProgress(p), color)
            secondBar.setProgress(secondPulseProgress(p), color)
        }
        return container to updater
    }

    private fun buildDotsPill(): Pair<View, (Payload) -> Unit> {
        val container = pillContainer().apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(7), dp(14), dp(8))
        }
        val label = labelView("SCREEN TIME")
        val time = timeView(13f)
        val limitDots = DotsRowView(context, dotCount = 6)
        val minuteDots = DotsRowView(context, dotCount = 6)
        val secondDots = DotsRowView(context, dotCount = 6)

        container.addView(label)
        container.addView(time, LinearLayout.LayoutParams(LP_WRAP, LP_WRAP).apply { topMargin = dp(1) })
        container.addView(
            limitDots,
            LinearLayout.LayoutParams(LP_WRAP, dp(7)).apply { topMargin = dp(5) },
        )
        container.addView(
            minuteDots,
            LinearLayout.LayoutParams(LP_WRAP, dp(5)).apply { topMargin = dp(3) },
        )
        container.addView(
            secondDots,
            LinearLayout.LayoutParams(LP_WRAP, dp(5)).apply { topMargin = dp(2) },
        )

        val updater: (Payload) -> Unit = { p ->
            val ratio = budgetRatio(p)
            val color = colorForRatio(ratio)
            time.text = clockText(p)
            time.setTextColor(color)
            limitDots.setProgress(ratio, color)
            minuteDots.setProgress(minutePulseProgress(p), color)
            secondDots.setProgress(secondPulseProgress(p), color)
        }
        return container to updater
    }

    private fun buildRingPill(): Pair<View, (Payload) -> Unit> {
        val container = pillContainer().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(11), dp(7), dp(14), dp(7))
        }
        val ring = RingView(context)
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val label = labelView("SCREEN TIME")
        val time = timeView(13f)
        column.addView(label)
        column.addView(time, LinearLayout.LayoutParams(LP_WRAP, LP_WRAP).apply { topMargin = dp(1) })

        container.addView(
            ring,
            LinearLayout.LayoutParams(dp(22), dp(22)).apply { rightMargin = dp(8) },
        )
        container.addView(column)

        val updater: (Payload) -> Unit = { p ->
            val ratio = budgetRatio(p)
            val color = colorForRatio(ratio)
            time.text = clockText(p)
            time.setTextColor(color)
            ring.setProgress(ratio, minutePulseProgress(p), secondPulseProgress(p), color)
        }
        return container to updater
    }

    // --- View primitives -----------------------------------------------------

    private fun pillContainer(): LinearLayout = LinearLayout(context).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(20).toFloat()
            setColor(Color.argb(220, 10, 13, 19))
            setStroke(dp(1), Color.argb(165, 194, 200, 209))
        }
        elevation = dp(14).toFloat()
    }

    private fun labelView(text: String): TextView = TextView(context).apply {
        this.text = text
        setTextColor(Color.argb(200, 140, 148, 158))
        textSize = 7.5f
        letterSpacing = 0.22f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        includeFontPadding = false
    }

    private fun timeView(sizeSp: Float): TextView = TextView(context).apply {
        textSize = sizeSp
        setTextColor(Color.rgb(233, 236, 241))
        letterSpacing = 0.04f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        includeFontPadding = false
    }

    private fun clockText(p: Payload): String {
        val hoursUsed = p.seconds / 3_600L
        val minutesUsed = (p.seconds % 3_600L) / 60L
        val secondsUsed = p.seconds % 60L
        val budgetHours = p.budgetSeconds / 3_600L
        val budgetMinutes = (p.budgetSeconds % 3_600L) / 60L
        val budgetDisplaySeconds = p.budgetSeconds % 60L
        val left = "%d:%02d:%02d".format(hoursUsed, minutesUsed, secondsUsed)
        val right = "%d:%02d:%02d".format(budgetHours, budgetMinutes, budgetDisplaySeconds)
        return "$left / $right"
    }

    // --- Interaction ---------------------------------------------------------

    private fun installInteraction(target: View, layoutParams: WindowManager.LayoutParams) {
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        var startX = 0
        var startY = 0
        var startTouchX = 0f
        var startTouchY = 0f
        var moved = false

        target.setOnClickListener { launchApp() }
        target.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = layoutParams.x
                    startY = layoutParams.y
                    startTouchX = event.rawX
                    startTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startTouchX
                    val dy = event.rawY - startTouchY
                    if (!moved && (abs(dx) > touchSlop || abs(dy) > touchSlop)) moved = true
                    if (moved) {
                        layoutParams.x = startX + dx.toInt()
                        layoutParams.y = (startY + dy.toInt()).coerceAtLeast(0)
                        windowManager.updateViewLayout(view, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        snapToNearestEdge(view, layoutParams)
                        onPositionChanged(layoutParams.x, layoutParams.y)
                    } else {
                        view.performClick()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (moved) {
                        snapToNearestEdge(view, layoutParams)
                        onPositionChanged(layoutParams.x, layoutParams.y)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun launchApp() {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        // PendingIntent works around foreground-launch restrictions when sent from a
        // service-owned overlay on newer Android versions.
        PendingIntent.getActivity(
            context,
            REQUEST_LAUNCH_FROM_OVERLAY,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        ).send()
    }

    private fun buildLayoutParams(initialX: Int, initialY: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }

    private fun snapToNearestEdge(view: View, layoutParams: WindowManager.LayoutParams) {
        val bounds = currentWindowBounds()
        val maxX = (bounds.width() - view.width).coerceAtLeast(0)
        val maxY = (bounds.height() - view.height).coerceAtLeast(0)
        layoutParams.x = if (layoutParams.x + view.width / 2 < bounds.width() / 2) 0 else maxX
        layoutParams.y = layoutParams.y.coerceIn(0, maxY)
        windowManager.updateViewLayout(view, layoutParams)
    }

    private fun currentWindowBounds(): Rect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        windowManager.currentWindowMetrics.bounds
    } else {
        @Suppress("DEPRECATION")
        val display = windowManager.defaultDisplay
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    private fun overlayWindowType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private fun budgetRatio(p: Payload): Float =
        if (p.budgetSeconds <= 0L) 0f else (p.seconds.toFloat() / p.budgetSeconds.toFloat())

    private fun minutePulseProgress(p: Payload): Float = ((p.seconds / 60L) % 60L).toFloat() / 60f

    private fun secondPulseProgress(p: Payload): Float = (p.seconds % 60L).toFloat() / 60f

    private fun colorForRatio(ratio: Float): Int = when {
        ratio < 0.6f -> COLOR_OK
        ratio < 0.85f -> COLOR_WARN
        ratio < 1f -> lerpColor(COLOR_WARN, COLOR_DANGER, (ratio - 0.85f) / 0.15f)
        else -> COLOR_DANGER
    }

    companion object {
        private const val LP_WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
        private const val REQUEST_LAUNCH_FROM_OVERLAY = 5

        // Steel / Gold / Red — sourced from KanColors.
        private val COLOR_OK = Color.rgb(233, 236, 241)
        private val COLOR_WARN = Color.rgb(242, 178, 58)
        private val COLOR_DANGER = Color.rgb(232, 93, 74)

        private fun lerpColor(from: Int, to: Int, t: Float): Int {
            val c = t.coerceIn(0f, 1f)
            val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * c).toInt()
            val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * c).toInt()
            val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * c).toInt()
            return Color.rgb(r, g, b)
        }
    }

    // --- Custom progress views ----------------------------------------------

    private class HorizontalBarView(context: Context) : View(context) {
        private var progress: Float = 0f
        private var fillColor: Int = COLOR_OK
        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 180, 188, 200)
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()

        fun setProgress(value: Float, color: Int) {
            progress = value.coerceAtLeast(0f)
            fillColor = color
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val r = height / 2f
            rect.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(rect, r, r, trackPaint)
            val filled = (width * min(progress, 1f))
            if (filled > 0f) {
                fillPaint.color = fillColor
                rect.set(0f, 0f, filled, height.toFloat())
                canvas.drawRoundRect(rect, r, r, fillPaint)
            }
        }
    }

    private class DotsRowView(context: Context, private val dotCount: Int) : View(context) {
        private var progress: Float = 0f
        private var fillColor: Int = COLOR_OK
        private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 180, 188, 200)
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            // Ask for enough horizontal space for dotCount dots + gaps.
            val density = context.resources.displayMetrics.density
            minimumWidth = ((dotCount * 7 + (dotCount - 1) * 4) * density).toInt()
        }

        fun setProgress(value: Float, color: Int) {
            progress = value.coerceAtLeast(0f)
            fillColor = color
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val n = dotCount
            val radius = height / 2f
            val totalDots = n.toFloat()
            val gap = (width - totalDots * height) / (totalDots - 1).coerceAtLeast(1f)
            val filledExact = progress * n
            for (i in 0 until n) {
                val cx = i * (height + gap) + radius
                val cy = radius
                val paint = if (i < filledExact) {
                    fillPaint.apply { color = fillColor }
                } else {
                    emptyPaint
                }
                canvas.drawCircle(cx, cy, radius, paint)
            }
        }
    }

    private class RingView(context: Context) : View(context) {
        private var limitProgress: Float = 0f
        private var minuteProgress: Float = 0f
        private var secondProgress: Float = 0f
        private var ringColor: Int = COLOR_OK
        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(70, 180, 188, 200)
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val rect = RectF()

        fun setProgress(limitValue: Float, minuteValue: Float, secondValue: Float, color: Int) {
            limitProgress = limitValue.coerceAtLeast(0f)
            minuteProgress = minuteValue.coerceAtLeast(0f)
            secondProgress = secondValue.coerceAtLeast(0f)
            ringColor = color
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val outerStroke = height * 0.14f
            val middleStroke = height * 0.11f
            val innerStroke = height * 0.08f

            trackPaint.strokeWidth = outerStroke
            fillPaint.strokeWidth = outerStroke
            var inset = outerStroke / 2f
            rect.set(inset, inset, width - inset, height - inset)
            canvas.drawArc(rect, 0f, 360f, false, trackPaint)
            fillPaint.color = ringColor
            val limitSweep = (min(limitProgress, 1f) * 360f)
            if (limitSweep > 0f) canvas.drawArc(rect, -90f, limitSweep, false, fillPaint)

            trackPaint.strokeWidth = middleStroke
            fillPaint.strokeWidth = middleStroke
            inset = outerStroke + middleStroke
            rect.set(inset, inset, width - inset, height - inset)
            canvas.drawArc(rect, 0f, 360f, false, trackPaint)
            val minuteSweep = (min(minuteProgress, 1f) * 360f)
            if (minuteSweep > 0f) canvas.drawArc(rect, -90f, minuteSweep, false, fillPaint)

            trackPaint.strokeWidth = innerStroke
            fillPaint.strokeWidth = innerStroke
            inset = outerStroke + middleStroke + innerStroke
            rect.set(inset, inset, width - inset, height - inset)
            canvas.drawArc(rect, 0f, 360f, false, trackPaint)
            val secondSweep = (min(secondProgress, 1f) * 360f)
            if (secondSweep > 0f) canvas.drawArc(rect, -90f, secondSweep, false, fillPaint)
        }
    }
}
