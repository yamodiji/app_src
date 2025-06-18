package com.appdrawer.fast

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.OnBackPressedCallback
import com.appdrawer.fast.adapters.AppAdapter
import com.appdrawer.fast.database.AppDatabase
import com.appdrawer.fast.models.AppInfo
import com.appdrawer.fast.repository.AppRepository
import com.appdrawer.fast.utils.SearchEngine
import com.appdrawer.fast.viewmodels.MainViewModel
import com.appdrawer.fast.viewmodels.MainViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var appAdapter: AppAdapter
    private lateinit var viewModel: MainViewModel
    private lateinit var searchEngine: SearchEngine
    
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
    }
    
    private fun setupViews() {
        searchEditText = findViewById(R.id.searchEditText)
        recyclerView = findViewById(R.id.recyclerView)
        
        // Set up toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)
        
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
        appAdapter = AppAdapter(
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app -> showAppOptions(app) }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = appAdapter
        }
        
        // Observe all apps
        viewModel.allApps.observe(this) { apps ->
            if (searchEditText.text.isEmpty()) {
                // Show favorite apps when no search query
                val favoriteApps = apps.filter { it.isFavorite }
                if (favoriteApps.isNotEmpty()) {
                    appAdapter.submitList(favoriteApps)
                } else {
                    // Show recent apps if no favorites
                    appAdapter.submitList(apps.take(10))
                }
            }
        }
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
                // Show favorites or recent apps
                val favoriteApps = apps.filter { it.isFavorite }
                if (favoriteApps.isNotEmpty()) {
                    appAdapter.submitList(favoriteApps)
                } else {
                    appAdapter.submitList(apps.take(10))
                }
            } else {
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
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                lifecycleScope.launch {
                    viewModel.refreshApps()
                    Toast.makeText(this@MainActivity, "Apps refreshed", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
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