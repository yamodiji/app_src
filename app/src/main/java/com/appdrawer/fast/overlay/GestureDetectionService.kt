package com.appdrawer.fast.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.appdrawer.fast.MainActivity
import com.appdrawer.fast.R
import kotlin.math.abs

class GestureDetectionService : Service() {
    
    private lateinit var windowManager: WindowManager
    private var gestureView: View? = null
    private lateinit var gestureDetector: GestureDetector
    private lateinit var preferences: SharedPreferences
    private lateinit var vibrator: Vibrator
    
    companion object {
        private const val CHANNEL_ID = "GestureDetectionChannel"
        private const val NOTIFICATION_ID = 2
        
        fun startService(context: Context) {
            val intent = Intent(context, GestureDetectionService::class.java)
            context.startForegroundService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, GestureDetectionService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        createNotificationChannel()
        setupGestureDetector()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        val isEnabled = preferences.getBoolean("swipe_gesture_enabled", true)
        if (isEnabled && gestureView == null) {
            createGestureOverlay()
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        removeGestureOverlay()
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gesture Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Detects swipe-up gestures to open app drawer"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fast App Drawer")
            .setContentText("Swipe gesture detection active")
            .setSmallIcon(R.drawable.ic_default_app)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val deltaY = e2.y - e1.y
                val deltaX = e2.x - e1.x
                val sensitivity = preferences.getFloat("gesture_sensitivity", 0.5f)
                val minDistance = 100 + (sensitivity * 200) // 100-300 pixels
                val minVelocity = 100 + (sensitivity * 400) // 100-500 pixels/sec
                
                // Check if it's a vertical swipe up
                if (abs(deltaY) > abs(deltaX) && // More vertical than horizontal
                    deltaY < -minDistance && // Swipe up (negative Y)
                    abs(velocityY) > minVelocity && // Fast enough
                    isOnHomeScreen()) { // Only from home screen
                    
                    vibrateIfEnabled()
                    openAppDrawerOverlay()
                    return true
                }
                
                return false
            }
        })
    }
    
    private fun createGestureOverlay() {
        if (gestureView != null) return
        
        gestureView = View(this).apply {
            setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                false // Allow other views to handle touch events
            }
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            200, // Bottom 200 pixels for gesture detection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        
        try {
            windowManager.addView(gestureView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun removeGestureOverlay() {
        gestureView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        gestureView = null
    }
    
    private fun isOnHomeScreen(): Boolean {
        // TODO: Implement proper home screen detection
        // For now, we'll assume gesture is enabled when this service is running
        // In a real implementation, you'd check if the current app is a launcher
        return true
    }
    
    private fun openAppDrawerOverlay() {
        val intent = Intent(this, AppDrawerOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }
    
    private fun vibrateIfEnabled() {
        if (preferences.getBoolean("vibration_enabled", true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }
    
    fun updateGestureDetection() {
        val isEnabled = preferences.getBoolean("swipe_gesture_enabled", true)
        if (isEnabled && gestureView == null) {
            createGestureOverlay()
        } else if (!isEnabled && gestureView != null) {
            removeGestureOverlay()
        }
    }
} 