package com.appdrawer.fast.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appdrawer.fast.R
import com.appdrawer.fast.adapters.AppAdapter
import com.appdrawer.fast.database.AppDatabase
import com.appdrawer.fast.repository.AppRepository
import com.appdrawer.fast.utils.SearchEngine
import com.appdrawer.fast.viewmodels.MainViewModel
import com.appdrawer.fast.viewmodels.MainViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AppDrawerOverlay(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onDismiss: () -> Unit
) : ViewModelStoreOwner {
    
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isShowing = false
    
    // UI Components
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var closeButton: ImageView
    private lateinit var appAdapter: AppAdapter
    
    // ViewModel and data
    private lateinit var viewModel: MainViewModel
    private val searchEngine = SearchEngine()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    // ViewModelStore for proper lifecycle management
    private val viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = this.viewModelStore
    
    fun show() {
        if (isShowing) return
        
        overlayView = createOverlayView()
        layoutParams = createLayoutParams()
        
        try {
            windowManager.addView(overlayView, layoutParams)
            isShowing = true
            
            // Show keyboard and focus search
            showKeyboard()
            
            // Animate in
            animateIn()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun hide() {
        if (!isShowing || overlayView == null) return
        
        hideKeyboard()
        animateOut {
            try {
                windowManager.removeView(overlayView)
                isShowing = false
                coroutineScope.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun createOverlayView(): View {
        val view = LayoutInflater.from(context).inflate(R.layout.app_drawer_overlay, null)
        
        setupViews(view)
        setupViewModel()
        setupRecyclerView()
        setupSearch()
        
        return view
    }
    
    private fun setupViews(view: View) {
        searchEditText = view.findViewById(R.id.searchEditText)
        recyclerView = view.findViewById(R.id.recyclerView)
        closeButton = view.findViewById(R.id.closeButton)
        
        closeButton.setOnClickListener { 
            onDismiss()
        }
        
        // Handle background tap to dismiss
        view.setOnClickListener { 
            onDismiss()
        }
        
        // Prevent clicks on content area from dismissing
        view.findViewById<View>(R.id.contentArea).setOnClickListener { }
    }
    
    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(context)
        val repository = AppRepository(context, database.appDao())
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        appAdapter = AppAdapter(
            onAppClick = { app -> 
                launchApp(app.packageName)
                onDismiss()
            },
            onAppLongClick = { app -> 
                // Handle long click (app options)
            }
        )
        
        recyclerView.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = appAdapter
        }
        
        // Observe apps (simplified for overlay)
        coroutineScope.launch {
            val database = AppDatabase.getDatabase(context)
            val apps = database.appDao().getAllVisibleApps()
            apps.collect { appList ->
                if (searchEditText.text.isEmpty()) {
                    appAdapter.submitList(appList.sortedBy { it.appName })
                } else {
                    performSearch(searchEditText.text.toString().trim(), appList)
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
                coroutineScope.launch {
                    val database = AppDatabase.getDatabase(context)
                    val apps = database.appDao().getAllVisibleApps()
                    apps.collect { appList ->
                        performSearch(query, appList)
                    }
                }
            }
        })
    }
    
    private fun performSearch(query: String, apps: List<com.appdrawer.fast.models.AppInfo>) {
        if (query.isEmpty()) {
            appAdapter.submitList(apps.sortedBy { it.appName })
        } else {
            val searchResults = searchEngine.search(query, apps)
            val resultApps = searchResults.map { it.app }
            appAdapter.submitList(resultApps)
        }
    }
    
    private fun launchApp(packageName: String) {
        coroutineScope.launch {
            try {
                val pm = context.packageManager
                val launchIntent = pm.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
    }
    
    private fun showKeyboard() {
        searchEditText.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
    }
    
    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
    }
    
    private fun animateIn() {
        overlayView?.let { view ->
            view.alpha = 0f
            view.scaleX = 0.9f
            view.scaleY = 0.9f
            
            view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .start()
        }
    }
    
    private fun animateOut(onComplete: () -> Unit) {
        overlayView?.let { view ->
            view.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(150)
                .withEndAction(onComplete)
                .start()
        } ?: onComplete()
    }
} 