package com.appdrawer.fast

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.activity.OnBackPressedCallback
import com.appdrawer.fast.adapters.AppAdapter
import com.appdrawer.fast.adapters.FavoriteAppAdapter
import com.appdrawer.fast.database.AppDatabase
import com.appdrawer.fast.models.AppInfo
import com.appdrawer.fast.overlay.FloatingWidgetService
import com.appdrawer.fast.overlay.GestureDetectionService
import com.appdrawer.fast.repository.AppRepository
import com.appdrawer.fast.utils.SearchEngine
import com.appdrawer.fast.viewmodels.MainViewModel
import com.appdrawer.fast.viewmodels.MainViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
    }
    
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var favoriteAppsRecyclerView: RecyclerView
    private lateinit var recentAppsRecyclerView: RecyclerView
    private lateinit var appAdapter: AppAdapter
    private lateinit var favoriteAppAdapter: FavoriteAppAdapter
    private lateinit var recentAppAdapter: AppAdapter
    private lateinit var viewModel: MainViewModel
    private lateinit var searchEngine: SearchEngine
    private lateinit var settingsIcon: ImageView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupViews()
        setupViewModel()
        setupRecyclerView()
        setupSearch()
        setupBackPress()
        
        // Load apps on first launch
        lifecycleScope.launch {
            viewModel.refreshApps()
        }
        
        // Check and start overlay services
        checkAndStartServices()
    }
    
    private fun checkAndStartServices() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        
        // Check if overlay permission is granted
        if (hasOverlayPermission()) {
            // Start floating widget if enabled
            val floatingWidgetEnabled = preferences.getBoolean("floating_widget_enabled", true)
            if (floatingWidgetEnabled) {
                FloatingWidgetService.startService(this)
            }
            
            // Start gesture detection if enabled
            val gestureEnabled = preferences.getBoolean("swipe_gesture_enabled", true)
            if (gestureEnabled) {
                GestureDetectionService.startService(this)
            }
        } else {
            // Show permission dialog for first-time users
            if (preferences.getBoolean("first_launch", true)) {
                showOverlayPermissionDialog()
                preferences.edit().putBoolean("first_launch", false).apply()
            }
        }
    }
    
    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Floating Widget")
            .setMessage("Fast App Drawer can show a floating search widget and detect swipe gestures. This requires permission to draw over other apps.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestOverlayPermission()
            }
            .setNegativeButton("Maybe Later") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (hasOverlayPermission()) {
                Toast.makeText(this, "Permission granted! Starting services...", Toast.LENGTH_SHORT).show()
                checkAndStartServices()
            } else {
                Toast.makeText(this, "Permission denied. You can enable it later in Settings.", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun setupViews() {
        searchEditText = findViewById(R.id.searchEditText)
        recyclerView = findViewById(R.id.recyclerView)
        favoriteAppsRecyclerView = findViewById(R.id.favoriteAppsRecyclerView)
        recentAppsRecyclerView = findViewById(R.id.recentAppsRecyclerView)
        settingsIcon = findViewById(R.id.settingsIcon)
        
        // Set up settings click
        settingsIcon.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // Focus on search box when activity starts
        searchEditText.requestFocus()
    }
    
    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(this)
        val repository = AppRepository(this, database.appDao())
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
        searchEngine = SearchEngine()
    }
    
    private fun setupRecyclerView() {
        // Setup favorite apps adapter with grid layout
        favoriteAppAdapter = FavoriteAppAdapter(
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app -> showAppOptions(app) }
        )
        
        favoriteAppsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 4)
            adapter = favoriteAppAdapter
        }
        
        // Setup recent apps adapter with grid layout
        recentAppAdapter = AppAdapter(
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app -> showAppOptions(app) }
        )
        
        recentAppsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 4)
            adapter = recentAppAdapter
        }
        
        // Setup all apps adapter
        appAdapter = AppAdapter(
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app -> showAppOptions(app) }
        )
        
        recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 4)
            adapter = appAdapter
        }
        
        // Observe all apps and organize into sections
        viewModel.allApps.observe(this) { apps ->
            if (searchEditText.text.isEmpty()) {
                organizeSections(apps)
            }
        }
    }
    
    private fun organizeSections(apps: List<AppInfo>) {
        val favoriteApps = apps.filter { it.isFavorite }
        val recentApps = apps.filter { !it.isFavorite && it.lastUsed > 0 }
            .sortedByDescending { it.lastUsed }
            .take(8)
        val otherApps = apps.filter { !it.isFavorite && !recentApps.contains(it) }
            .sortedBy { it.appName }
        
        favoriteAppAdapter.submitList(favoriteApps)
        recentAppAdapter.submitList(recentApps)
        appAdapter.submitList(otherApps)
        
        // Show/hide sections based on content
        findViewById<LinearLayout>(R.id.favoriteSection).visibility = 
            if (favoriteApps.isNotEmpty()) View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.recentSection).visibility = 
            if (recentApps.isNotEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                performSearch(query)
            }
        })
    }
    
    private fun performSearch(query: String) {
        viewModel.allApps.value?.let { apps ->
            if (query.isEmpty()) {
                // Show organized sections
                organizeSections(apps)
                // Show all sections
                findViewById<LinearLayout>(R.id.favoriteSection).visibility = 
                    if (apps.any { it.isFavorite }) View.VISIBLE else View.GONE
                findViewById<LinearLayout>(R.id.recentSection).visibility = 
                    if (apps.any { !it.isFavorite && it.lastUsed > 0 }) View.VISIBLE else View.GONE
                findViewById<LinearLayout>(R.id.allAppsSection).visibility = View.VISIBLE
            } else {
                // Hide sections and show search results
                findViewById<LinearLayout>(R.id.favoriteSection).visibility = View.GONE
                findViewById<LinearLayout>(R.id.recentSection).visibility = View.GONE
                findViewById<LinearLayout>(R.id.allAppsSection).visibility = View.VISIBLE
                
                // Perform search
                val searchResults = searchEngine.search(query, apps)
                val resultApps = searchResults.map { it.app }
                appAdapter.submitList(resultApps)
            }
        }
    }
    
    private fun launchApp(app: AppInfo) {
        lifecycleScope.launch {
            val success = viewModel.launchApp(app.packageName)
            if (!success) {
                Toast.makeText(this@MainActivity, "Failed to launch ${app.appName}", Toast.LENGTH_SHORT).show()
            } else {
                // Close app drawer after launching
                finish()
            }
        }
    }
    
    private fun showAppOptions(app: AppInfo) {
        val options = mutableListOf<String>()
        
        if (app.isFavorite) {
            options.add("Remove from favorites")
        } else {
            options.add("Add to favorites")
        }
        
        if (app.isHidden) {
            options.add("Show app")
        } else {
            options.add("Hide app")
        }
        
        options.add("Set alias")
        options.add("App info")
        
        AlertDialog.Builder(this)
            .setTitle(app.appName)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Add to favorites", "Remove from favorites" -> {
                        lifecycleScope.launch {
                            viewModel.toggleFavorite(app.packageName)
                        }
                    }
                    "Hide app", "Show app" -> {
                        lifecycleScope.launch {
                            viewModel.toggleHidden(app.packageName)
                        }
                    }
                    "Set alias" -> {
                        showAliasDialog(app)
                    }
                    "App info" -> {
                        showAppInfo(app)
                    }
                }
            }
            .show()
    }
    
    private fun showAliasDialog(app: AppInfo) {
        val editText = EditText(this)
        editText.setText(app.alias ?: "")
        
        AlertDialog.Builder(this)
            .setTitle("Set alias for ${app.appName}")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val alias = editText.text.toString().trim()
                lifecycleScope.launch {
                    viewModel.updateAlias(app.packageName, alias.ifEmpty { null })
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAppInfo(app: AppInfo) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:${app.packageName}")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open app info", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (searchEditText.text.isNotEmpty()) {
                    searchEditText.text.clear()
                } else {
                    finish()
                }
            }
        })
    }
} 