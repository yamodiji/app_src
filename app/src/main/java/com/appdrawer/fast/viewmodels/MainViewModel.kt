package com.appdrawer.fast.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.appdrawer.fast.repository.AppRepository
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AppRepository) : ViewModel() {
    
    val allApps = repository.getAllVisibleApps().asLiveData()
    val favoriteApps = repository.getFavoriteApps().asLiveData()
    
    suspend fun refreshApps() {
        repository.refreshInstalledApps()
    }
    
    suspend fun launchApp(packageName: String): Boolean {
        return repository.launchApp(packageName)
    }
    
    suspend fun toggleFavorite(packageName: String) {
        repository.toggleFavorite(packageName)
    }
    
    suspend fun toggleHidden(packageName: String) {
        repository.toggleHidden(packageName)
    }
    
    suspend fun updateAlias(packageName: String, alias: String?) {
        repository.updateAlias(packageName, alias)
    }
}

class MainViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 