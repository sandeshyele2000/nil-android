/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.overlay

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.sandesh.nil.R
import com.sandesh.nil.ui.NILInspectorActivity
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

internal object NILFloatingButtonController {
    private var isRegistered = false
    private var appRef: WeakReference<Application>? = null
    private val attachedButtons = mutableMapOf<Int, View>()

    fun initialize(application: Application) {
        if (isRegistered) return
        appRef = WeakReference(application)
        application.registerActivityLifecycleCallbacks(callbacks)
        isRegistered = true
    }

    private val callbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityResumed(activity: Activity) {
            attachButton(activity)
        }

        override fun onActivityDestroyed(activity: Activity) {
            attachedButtons.remove(activity.hashCode())
        }
    }

    private fun attachButton(activity: Activity) {
        if (activity is NILInspectorActivity) return
        if (attachedButtons.containsKey(activity.hashCode())) return

        val root = activity.window?.decorView as? ViewGroup ?: return
        val button = buildButton(activity)
        root.addView(button)
        attachedButtons[activity.hashCode()] = button
    }

    private fun buildButton(activity: Activity): View {
        val sizePx = activity.dp(56)
        val startMargin = activity.dp(20)
        val topOffset = activity.dp(120)
        val fixedPrimary = 0xFFB2182B.toInt()
        val fixedOnPrimary = 0xFFFFFFFF.toInt()
        val fixedBorder = 0xFFEFEFEF.toInt()

        val iconButton = ImageView(activity).apply {
            setImageResource(R.drawable.ic_earth_mesh_24dp)
            setColorFilter(fixedOnPrimary)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(activity.dp(14), activity.dp(14), activity.dp(14), activity.dp(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(fixedPrimary)
                setStroke(activity.dp(1), fixedBorder)
            }
            elevation = activity.dp(6).toFloat()
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                leftMargin = startMargin
                topMargin = topOffset
            }
            setOnClickListener {
                activity.startActivity(Intent(activity, NILInspectorActivity::class.java))
            }
        }

        iconButton.setOnTouchListener(DragTouchListener(activity))
        return iconButton
    }

    private class DragTouchListener(
        private val activity: Activity
    ) : View.OnTouchListener {
        private var dX = 0f
        private var dY = 0f
        private var dragging = false

        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            val parent = view.parent as? ViewGroup ?: return false
            val params = view.layoutParams as FrameLayout.LayoutParams
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = motionEvent.rawX - params.leftMargin
                    dY = motionEvent.rawY - params.topMargin
                    dragging = false
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val maxX = (parent.width - view.width).coerceAtLeast(0)
                    val maxY = (parent.height - view.height).coerceAtLeast(0)

                    val nextX = (motionEvent.rawX - dX).roundToInt().coerceIn(0, maxX)
                    val nextY = (motionEvent.rawY - dY).roundToInt().coerceIn(0, maxY)

                    if (nextX != params.leftMargin || nextY != params.topMargin) {
                        dragging = true
                    }

                    params.leftMargin = nextX
                    params.topMargin = nextY
                    view.layoutParams = params
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        view.performClick()
                    }
                    return true
                }
            }
            return false
        }
    }

    private fun Activity.dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).roundToInt()
    }
}
