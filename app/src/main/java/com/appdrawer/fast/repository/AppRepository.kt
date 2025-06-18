package com.appdrawer.fast.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.appdrawer.fast.database.AppDao
import com.appdrawer.fast.models.AppInfo

class AppRepository(
    private val context: Context,
    private val appDao: AppDao
) {
    
    fun getAllVisibleApps(): Flow<List<AppInfo>> = appDao.getAllVisibleApps()
    
    fun getFavoriteApps(): Flow<List<AppInfo>> = appDao.getFavoriteApps()
    
    suspend fun toggleFavorite(packageName: String) {
        val app = appDao.getApp(packageName)
        app?.let {
            appDao.updateFavoriteStatus(packageName, !it.isFavorite)
        }
    }
    
    suspend fun toggleHidden(packageName: String) {
        val app = appDao.getApp(packageName)
        app?.let {
            appDao.updateHiddenStatus(packageName, !it.isHidden)
        }
    }
    
    suspend fun updateAlias(packageName: String, alias: String?) {
        appDao.updateAlias(packageName, alias)
    }
    
    suspend fun recordAppUsage(packageName: String) {
        appDao.updateUsage(packageName, System.currentTimeMillis())
    }
    
    suspend fun refreshInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val installedApps = mutableListOf<AppInfo>()
        val installedPackages = mutableListOf<String>()
        
        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            val activityName = resolveInfo.activityInfo.name
            
            // Skip our own app
            if (packageName == context.packageName) continue
            
            try {
                val appName = resolveInfo.loadLabel(pm).toString()
                installedPackages.add(packageName)
                
                // Check if app already exists in database
                val existingApp = appDao.getApp(packageName)
                
                val appInfo = if (existingApp != null) {
                    // Update existing app name in case it changed
                    existingApp.copy(appName = appName, activityName = activityName)
                } else {
                    // Create new app entry
                    AppInfo(
                        packageName = packageName,
                        appName = appName,
                        activityName = activityName
                    )
                }
                
                // Load icon
                appInfo.icon = try {
                    resolveInfo.loadIcon(pm)
                } catch (e: Exception) {
                    null
                }
                
                installedApps.add(appInfo)
            } catch (e: Exception) {
                // Skip apps that can't be loaded
                continue
            }
        }
        
        // Insert/update apps in database
        appDao.insertApps(installedApps)
        
        // Remove uninstalled apps from database
        appDao.removeUninstalledApps(installedPackages)
    }
    
    suspend fun launchApp(packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                recordAppUsage(packageName)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
} 