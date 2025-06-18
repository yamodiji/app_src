package com.appdrawer.fast.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.appdrawer.fast.overlay.FloatingWidgetService
import com.appdrawer.fast.overlay.GestureDetectionService

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.PACKAGE_REPLACED" -> {
                if (intent.action == "android.intent.action.PACKAGE_REPLACED") {
                    val packageName = intent.dataString
                    if (packageName != "package:${context.packageName}") {
                        return
                    }
                }
                
                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                
                // Start floating widget service if enabled
                val floatingWidgetEnabled = preferences.getBoolean("floating_widget_enabled", true)
                if (floatingWidgetEnabled) {
                    try {
                        FloatingWidgetService.startService(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Start gesture detection service if enabled
                val gestureEnabled = preferences.getBoolean("swipe_gesture_enabled", true)
                if (gestureEnabled) {
                    try {
                        GestureDetectionService.startService(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
} 