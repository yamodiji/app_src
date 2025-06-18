package com.appdrawer.fast.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.view.isVisible
import com.appdrawer.fast.R

class FloatingWidget(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onTap: () -> Unit
) {
    private var floatingView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isShowing = false
    
    // Touch handling variables
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var lastClickTime = 0L
    
    companion object {
        private const val CLICK_THRESHOLD = 200L // ms
        private const val DRAG_THRESHOLD = 10f // pixels
    }
    
    fun show() {
        if (isShowing) return
        
        floatingView = createFloatingView()
        layoutParams = createLayoutParams()
        
        try {
            windowManager.addView(floatingView, layoutParams)
            isShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun hide() {
        if (!isShowing || floatingView == null) return
        
        try {
            windowManager.removeView(floatingView)
            isShowing = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun createFloatingView(): View {
        val view = LayoutInflater.from(context).inflate(R.layout.floating_widget, null)
        
        val searchIcon = view.findViewById<ImageView>(R.id.searchIcon)
        val settingsIcon = view.findViewById<ImageView>(R.id.settingsIcon)
        
        // Set up touch handling
        view.setOnTouchListener { v, event ->
            handleTouch(event)
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
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }
    }
    
    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams?.x ?: 0
                initialY = layoutParams?.y ?: 0
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                lastClickTime = System.currentTimeMillis()
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY
                
                if (!isDragging && (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD)) {
                    isDragging = true
                }
                
                if (isDragging) {
                    layoutParams?.x = initialX + deltaX.toInt()
                    layoutParams?.y = initialY + deltaY.toInt()
                    windowManager.updateViewLayout(floatingView, layoutParams)
                }
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                if (!isDragging && (System.currentTimeMillis() - lastClickTime) < CLICK_THRESHOLD) {
                    // This was a tap, not a drag
                    animateClick()
                    onTap()
                }
                snapToEdge()
                return true
            }
        }
        return false
    }
    
    private fun animateClick() {
        floatingView?.let { view ->
            val animator = ValueAnimator.ofFloat(1f, 1.1f, 1f)
            animator.duration = 150
            animator.addUpdateListener { 
                val scale = it.animatedValue as Float
                view.scaleX = scale
                view.scaleY = scale
            }
            animator.start()
        }
    }
    
    private fun snapToEdge() {
        layoutParams?.let { params ->
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            
            // Snap to left or right edge based on current position
            val targetX = if (params.x < screenWidth / 2) {
                20 // Left edge with margin
            } else {
                screenWidth - (floatingView?.width ?: 0) - 20 // Right edge with margin
            }
            
            // Animate to target position
            val startX = params.x
            val animator = ValueAnimator.ofInt(startX, targetX)
            animator.duration = 200
            animator.addUpdateListener { 
                params.x = it.animatedValue as Int
                windowManager.updateViewLayout(floatingView, params)
            }
            animator.start()
        }
    }
} 