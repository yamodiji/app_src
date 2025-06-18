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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.appdrawer.fast.MainActivity
import com.appdrawer.fast.R
import com.appdrawer.fast.SettingsActivity

class FloatingWidgetService : Service() {
    
    private lateinit var windowManager: WindowManager
    private var floatingWidget: View? = null
    private var isWidgetVisible = false
    
    private lateinit var preferences: SharedPreferences
    private lateinit var vibrator: Vibrator
    
    companion object {
        private const val CHANNEL_ID = "FloatingWidgetChannel"
        private const val NOTIFICATION_ID = 1
        
        fun startService(context: Context) {
            val intent = Intent(context, FloatingWidgetService::class.java)
            context.startForegroundService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, FloatingWidgetService::class.java)
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
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        val isEnabled = preferences.getBoolean("floating_widget_enabled", true)
        if (isEnabled && !isWidgetVisible) {
            showFloatingWidget()
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        hideFloatingWidget()
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Widget",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Floating search widget overlay"
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
        
        val settingsIntent = Intent(this, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            this, 1, settingsIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fast App Drawer")
            .setContentText("Floating widget is active")
            .setSmallIcon(R.drawable.ic_default_app)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_favorite, "Settings", settingsPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    private fun showFloatingWidget() {
        if (isWidgetVisible || floatingWidget != null) return
        
        floatingWidget = LayoutInflater.from(this)
            .inflate(R.layout.floating_widget, null)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        
        // Set position based on preferences
        val position = preferences.getString("widget_position", "top_center")
        params.gravity = when (position) {
            "top_left" -> Gravity.TOP or Gravity.START
            "top_right" -> Gravity.TOP or Gravity.END
            "center" -> Gravity.CENTER
            "bottom_center" -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            else -> Gravity.TOP or Gravity.CENTER_HORIZONTAL // top_center default
        }
        
        params.x = preferences.getInt("widget_x", 0)
        params.y = preferences.getInt("widget_y", 100)
        
        setupWidgetInteractions()
        
        try {
            windowManager.addView(floatingWidget, params)
            isWidgetVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun setupWidgetInteractions() {
        val searchIcon = floatingWidget?.findViewById<ImageView>(R.id.searchIcon)
        val settingsIcon = floatingWidget?.findViewById<ImageView>(R.id.settingsIcon)
        val micIcon = floatingWidget?.findViewById<ImageView>(R.id.micIcon)
        
        // Search icon click - Open app drawer overlay
        searchIcon?.setOnClickListener {
            vibrateIfEnabled()
            openAppDrawerOverlay()
        }
        
        // Settings icon click
        settingsIcon?.setOnClickListener {
            vibrateIfEnabled()
            val intent = Intent(this, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }
        
        // Mic icon click - Voice search (placeholder)
        micIcon?.setOnClickListener {
            vibrateIfEnabled()
            // TODO: Implement voice search
        }
        
        // Long press to drag widget
        var isDragging = false
        var initialX = 0f
        var initialY = 0f
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        floatingWidget?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    initialX = (view.layoutParams as WindowManager.LayoutParams).x.toFloat()
                    initialY = (view.layoutParams as WindowManager.LayoutParams).y.toFloat()
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isDragging) {
                        val deltaX = Math.abs(event.rawX - initialTouchX)
                        val deltaY = Math.abs(event.rawY - initialTouchY)
                        if (deltaX > 10 || deltaY > 10) {
                            isDragging = true
                            vibrateIfEnabled()
                        }
                    }
                    
                    if (isDragging) {
                        val params = view.layoutParams as WindowManager.LayoutParams
                        params.x = (initialX + (event.rawX - initialTouchX)).toInt()
                        params.y = (initialY + (event.rawY - initialTouchY)).toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        // Save new position
                        val params = view.layoutParams as WindowManager.LayoutParams
                        preferences.edit()
                            .putInt("widget_x", params.x)
                            .putInt("widget_y", params.y)
                            .apply()
                    }
                    isDragging = false
                }
            }
            isDragging
        }
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
    
    private fun hideFloatingWidget() {
        floatingWidget?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        floatingWidget = null
        isWidgetVisible = false
    }
    
    fun updateWidgetVisibility() {
        val isEnabled = preferences.getBoolean("floating_widget_enabled", true)
        if (isEnabled && !isWidgetVisible) {
            showFloatingWidget()
        } else if (!isEnabled && isWidgetVisible) {
            hideFloatingWidget()
        }
    }
} 