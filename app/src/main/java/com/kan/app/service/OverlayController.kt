package com.kan.app.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * Owns the floating screen-time pill: creation, drag/snap behavior, and text updates.
 * Stateful — call [show], [updateText], [remove], [persistPosition] from a single thread.
 */
class OverlayController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onPositionChanged: (x: Int, y: Int) -> Unit,
) {
    private var view: TextView? = null
    private var params: WindowManager.LayoutParams? = null

    fun show(initialText: String, initialX: Int, initialY: Int) {
        if (!Settings.canDrawOverlays(context)) return
        if (view != null) {
            updateText(initialText)
            return
        }

        val textView = buildPillView(initialText)
        val layoutParams = buildLayoutParams(initialX, initialY)

        installDragBehavior(textView, layoutParams)
        view = textView
        params = layoutParams
        windowManager.addView(textView, layoutParams)
    }

    fun updateText(text: String) {
        view?.text = text
    }

    fun remove() {
        view?.let { windowManager.removeView(it) }
        view = null
        params = null
    }

    private fun buildPillView(initialText: String): TextView = TextView(context).apply {
        text = initialText
        setTextColor(Color.rgb(18, 18, 18))
        textSize = 13f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        letterSpacing = 0.08f
        includeFontPadding = false
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(8), dp(14), dp(8))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(22).toFloat()
            setColor(Color.argb(190, 248, 244, 237))
            setStroke(dp(1), Color.argb(68, 15, 15, 15))
        }
        elevation = dp(10).toFloat()
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

    private fun installDragBehavior(target: TextView, layoutParams: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        target.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = (initialY + (event.rawY - initialTouchY).toInt()).coerceAtLeast(0)
                    windowManager.updateViewLayout(view, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    snapToNearestEdge(view, layoutParams)
                    onPositionChanged(layoutParams.x, layoutParams.y)
                    true
                }
                else -> false
            }
        }
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
}
