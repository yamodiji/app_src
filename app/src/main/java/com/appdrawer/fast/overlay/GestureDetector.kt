package com.appdrawer.fast.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

class GestureDetector(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onSwipeUp: () -> Unit
) {
    private var gestureView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isShowing = false
    
    private var startY = 0f
    private var startTime = 0L
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    
    companion object {
        private const val SWIPE_THRESHOLD = 150f // minimum distance for swipe
        private const val SWIPE_VELOCITY_THRESHOLD = 800f // minimum velocity
        private const val GESTURE_AREA_HEIGHT = 100 // height of gesture detection area at bottom
    }
    
    fun show() {
        if (isShowing) return
        
        gestureView = createGestureView()
        layoutParams = createLayoutParams()
        
        try {
            windowManager.addView(gestureView, layoutParams)
            isShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun hide() {
        if (!isShowing || gestureView == null) return
        
        try {
            windowManager.removeView(gestureView)
            isShowing = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun createGestureView(): View {
        val view = View(context)
        
        view.setOnTouchListener { _, event ->
            handleGesture(event)
        }
        
        return view
    }
    
    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        val displayMetrics = context.resources.displayMetrics
        
        return WindowManager.LayoutParams(
            displayMetrics.widthPixels,
            GESTURE_AREA_HEIGHT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }
    }
    
    private fun handleGesture(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startY = event.rawY
                startTime = System.currentTimeMillis()
                
                // Enable touch events for this gesture
                layoutParams?.flags = layoutParams?.flags?.and(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                )
                gestureView?.let { windowManager.updateViewLayout(it, layoutParams) }
                
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaY = startY - event.rawY
                
                // Visual feedback could be added here (e.g., reveal animation)
                
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val deltaY = startY - event.rawY
                val deltaTime = System.currentTimeMillis() - startTime
                val velocity = if (deltaTime > 0) deltaY / deltaTime * 1000 else 0f
                
                // Disable touch events after gesture
                layoutParams?.flags = layoutParams?.flags?.or(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                )
                gestureView?.let { windowManager.updateViewLayout(it, layoutParams) }
                
                // Check if this was a valid swipe up
                if (deltaY > SWIPE_THRESHOLD && velocity > SWIPE_VELOCITY_THRESHOLD) {
                    triggerSwipeUp()
                }
                
                return true
            }
        }
        return false
    }
    
    private fun triggerSwipeUp() {
        // Vibration feedback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
        
        onSwipeUp()
    }
} 