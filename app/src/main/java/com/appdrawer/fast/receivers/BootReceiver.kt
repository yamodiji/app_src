package com.appdrawer.fast.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.appdrawer.fast.overlay.OverlayService

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                // Start overlay service after boot
                OverlayService.start(context)
            }
        }
    }
} 