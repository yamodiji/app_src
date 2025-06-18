package com.appdrawer.fast

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.appdrawer.fast.overlay.FloatingWidgetService
import com.appdrawer.fast.overlay.GestureDetectionService

class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
            
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    class SettingsFragment : PreferenceFragmentCompat() {
        
        companion object {
            private const val REQUEST_OVERLAY_PERMISSION = 1001
        }
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            
            setupFloatingWidgetPreferences()
            setupGesturePreferences()
            setupAppearancePreferences()
            setupPermissionPreferences()
        }
        
        private fun setupFloatingWidgetPreferences() {
            val floatingWidgetEnabled = findPreference<SwitchPreferenceCompat>("floating_widget_enabled")
            floatingWidgetEnabled?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                if (enabled) {
                    if (hasOverlayPermission()) {
                        FloatingWidgetService.startService(requireContext())
                    } else {
                        requestOverlayPermission()
                        return@setOnPreferenceChangeListener false
                    }
                } else {
                    FloatingWidgetService.stopService(requireContext())
                }
                true
            }
            
            val widgetPosition = findPreference<ListPreference>("widget_position")
            widgetPosition?.setOnPreferenceChangeListener { _, _ ->
                // Restart service to apply new position
                if (floatingWidgetEnabled?.isChecked == true) {
                    FloatingWidgetService.stopService(requireContext())
                    FloatingWidgetService.startService(requireContext())
                }
                true
            }
            
            val widgetOpacity = findPreference<SeekBarPreference>("widget_opacity")
            widgetOpacity?.setOnPreferenceChangeListener { _, _ ->
                // Restart service to apply new opacity
                if (floatingWidgetEnabled?.isChecked == true) {
                    FloatingWidgetService.stopService(requireContext())
                    FloatingWidgetService.startService(requireContext())
                }
                true
            }
        }
        
        private fun setupGesturePreferences() {
            val gestureEnabled = findPreference<SwitchPreferenceCompat>("swipe_gesture_enabled")
            gestureEnabled?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                if (enabled) {
                    if (hasOverlayPermission()) {
                        GestureDetectionService.startService(requireContext())
                    } else {
                        requestOverlayPermission()
                        return@setOnPreferenceChangeListener false
                    }
                } else {
                    GestureDetectionService.stopService(requireContext())
                }
                true
            }
            
            val gestureSensitivity = findPreference<SeekBarPreference>("gesture_sensitivity")
            gestureSensitivity?.setOnPreferenceChangeListener { _, _ ->
                // Service will pick up new sensitivity automatically
                true
            }
        }
        
        private fun setupAppearancePreferences() {
            val appTheme = findPreference<ListPreference>("app_theme")
            appTheme?.setOnPreferenceChangeListener { _, _ ->
                // Restart activity to apply theme
                requireActivity().recreate()
                true
            }
            
            val gridViewEnabled = findPreference<SwitchPreferenceCompat>("grid_view_enabled")
            val gridColumns = findPreference<SeekBarPreference>("grid_columns")
            
            gridViewEnabled?.setOnPreferenceChangeListener { _, newValue ->
                gridColumns?.isEnabled = newValue as Boolean
                true
            }
            
            // Set initial state
            gridColumns?.isEnabled = gridViewEnabled?.isChecked ?: true
        }
        
        private fun setupPermissionPreferences() {
            val overlayPermission = findPreference<Preference>("overlay_permission")
            overlayPermission?.setOnPreferenceClickListener {
                requestOverlayPermission()
                true
            }
            
            val accessibilityPermission = findPreference<Preference>("accessibility_permission")
            accessibilityPermission?.setOnPreferenceClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                true
            }
            
            val usageStatsPermission = findPreference<Preference>("usage_stats_permission")
            usageStatsPermission?.setOnPreferenceClickListener {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
                true
            }
            
            updatePermissionStatus()
        }
        
        private fun hasOverlayPermission(): Boolean {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(requireContext())
            } else {
                true
            }
        }
        
        private fun requestOverlayPermission() {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${requireContext().packageName}")
                )
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            }
        }
        
        private fun updatePermissionStatus() {
            val overlayPermission = findPreference<Preference>("overlay_permission")
            overlayPermission?.summary = if (hasOverlayPermission()) {
                "✓ Granted"
            } else {
                "⚠ Required for floating widget"
            }
        }
        
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            
            if (requestCode == REQUEST_OVERLAY_PERMISSION) {
                if (hasOverlayPermission()) {
                    Toast.makeText(requireContext(), "Overlay permission granted", Toast.LENGTH_SHORT).show()
                    updatePermissionStatus()
                    
                    // Enable floating widget if it was requested
                    val floatingWidgetEnabled = findPreference<SwitchPreferenceCompat>("floating_widget_enabled")
                    if (floatingWidgetEnabled?.isChecked == true) {
                        FloatingWidgetService.startService(requireContext())
                    }
                    
                    // Enable gesture detection if it was requested
                    val gestureEnabled = findPreference<SwitchPreferenceCompat>("swipe_gesture_enabled")
                    if (gestureEnabled?.isChecked == true) {
                        GestureDetectionService.startService(requireContext())
                    }
                } else {
                    Toast.makeText(requireContext(), "Overlay permission denied", Toast.LENGTH_SHORT).show()
                    updatePermissionStatus()
                }
            }
        }
        
        override fun onResume() {
            super.onResume()
            updatePermissionStatus()
        }
    }
} 