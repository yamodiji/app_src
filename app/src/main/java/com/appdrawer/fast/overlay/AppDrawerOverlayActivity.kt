package com.appdrawer.fast.overlay

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appdrawer.fast.R
import com.appdrawer.fast.adapters.AppAdapter
import com.appdrawer.fast.adapters.FavoriteAppAdapter
import com.appdrawer.fast.database.AppDatabase
import com.appdrawer.fast.models.AppInfo
import com.appdrawer.fast.repository.AppRepository
import com.appdrawer.fast.utils.SearchEngine
import com.appdrawer.fast.viewmodels.MainViewModel
import com.appdrawer.fast.viewmodels.MainViewModelFactory
import kotlinx.coroutines.launch

class AppDrawerOverlayActivity : AppCompatActivity() {
    
    private lateinit var searchEditText: EditText
    private lateinit var closeButton: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var favoriteAppsRecyclerView: RecyclerView
    private lateinit var recentAppsRecyclerView: RecyclerView
    private lateinit var appAdapter: AppAdapter
    private lateinit var favoriteAppAdapter: FavoriteAppAdapter
    private lateinit var recentAppAdapter: AppAdapter
    private lateinit var viewModel: MainViewModel
    private lateinit var searchEngine: SearchEngine
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_drawer_overlay)
        
        setupViews()
        setupViewModel()
        setupRecyclerView()
        setupSearch()
        setupBackPress()
        
        lifecycleScope.launch {
            viewModel.refreshApps()
        }
        
        searchEditText.requestFocus()
    }
    
    private fun setupViews() {
        searchEditText = findViewById(R.id.searchEditText)
        closeButton = findViewById(R.id.closeButton)
        recyclerView = findViewById(R.id.recyclerView)
        favoriteAppsRecyclerView = findViewById(R.id.favoriteAppsRecyclerView)
        recentAppsRecyclerView = findViewById(R.id.recentAppsRecyclerView)
        
        closeButton.setOnClickListener { finish() }
        findViewById<View>(R.id.rootContainer).setOnClickListener { finish() }
    }
    
    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(this)
        val repository = AppRepository(this, database.appDao())
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
        searchEngine = SearchEngine()
    }
    
    private fun setupRecyclerView() {
        favoriteAppAdapter = FavoriteAppAdapter(
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app -> showAppOptions(app) }
        )
        
        recentAppAdapter = AppAdapter(
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app -> showAppOptions(app) }
        )
        
        appAdapter = AppAdapter(
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app -> showAppOptions(app) }
        )
        
        favoriteAppsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@AppDrawerOverlayActivity, 4)
            adapter = favoriteAppAdapter
        }
        
        recentAppsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@AppDrawerOverlayActivity, 4)
            adapter = recentAppAdapter
        }
        
        recyclerView.apply {
            layoutManager = GridLayoutManager(this@AppDrawerOverlayActivity, 4)
            adapter = appAdapter
        }
        
        viewModel.allApps.observe(this) { apps ->
            if (searchEditText.text.isEmpty()) {
                organizeSections(apps)
            }
        }
    }
    
    private fun organizeSections(apps: List<AppInfo>) {
        val favoriteApps = apps.filter { it.isFavorite }
        val recentApps = apps.filter { !it.isFavorite && it.lastUsed > 0 }
            .sortedByDescending { it.lastUsed }.take(8)
        val otherApps = apps.filter { !it.isFavorite && !recentApps.contains(it) }
            .sortedBy { it.appName }
        
        favoriteAppAdapter.submitList(favoriteApps)
        recentAppAdapter.submitList(recentApps)
        appAdapter.submitList(otherApps)
        
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
                performSearch(s?.toString()?.trim() ?: "")
            }
        })
    }
    
    private fun performSearch(query: String) {
        viewModel.allApps.value?.let { apps ->
            if (query.isEmpty()) {
                organizeSections(apps)
                findViewById<LinearLayout>(R.id.favoriteSection).visibility = 
                    if (apps.any { it.isFavorite }) View.VISIBLE else View.GONE
                findViewById<LinearLayout>(R.id.recentSection).visibility = 
                    if (apps.any { !it.isFavorite && it.lastUsed > 0 }) View.VISIBLE else View.GONE
                findViewById<LinearLayout>(R.id.allAppsSection).visibility = View.VISIBLE
            } else {
                findViewById<LinearLayout>(R.id.favoriteSection).visibility = View.GONE
                findViewById<LinearLayout>(R.id.recentSection).visibility = View.GONE
                findViewById<LinearLayout>(R.id.allAppsSection).visibility = View.VISIBLE
                
                val searchResults = searchEngine.search(query, apps)
                appAdapter.submitList(searchResults.map { it.app })
            }
        }
    }
    
    private fun launchApp(app: AppInfo) {
        lifecycleScope.launch {
            val success = viewModel.launchApp(app.packageName)
            if (!success) {
                Toast.makeText(this@AppDrawerOverlayActivity, "Failed to launch ${app.appName}", Toast.LENGTH_SHORT).show()
            } else {
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
                    "Set alias" -> showAliasDialog(app)
                    "App info" -> showAppInfo(app)
                }
            }
            .show()
    }
    
    private fun showAliasDialog(app: AppInfo) {
        val editText = android.widget.EditText(this)
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