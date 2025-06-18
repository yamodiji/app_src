package com.appdrawer.fast.database

import androidx.room.*
import androidx.lifecycle.LiveData
import com.appdrawer.fast.models.AppInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps WHERE isHidden = 0 ORDER BY isFavorite DESC, lastUsed DESC")
    fun getAllVisibleApps(): Flow<List<AppInfo>>
    
    @Query("SELECT * FROM apps WHERE isFavorite = 1 ORDER BY lastUsed DESC")
    fun getFavoriteApps(): Flow<List<AppInfo>>
    
    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    suspend fun getApp(packageName: String): AppInfo?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppInfo)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppInfo>)
    
    @Update
    suspend fun updateApp(app: AppInfo)
    
    @Query("UPDATE apps SET isFavorite = :isFavorite WHERE packageName = :packageName")
    suspend fun updateFavoriteStatus(packageName: String, isFavorite: Boolean)
    
    @Query("UPDATE apps SET isHidden = :isHidden WHERE packageName = :packageName")
    suspend fun updateHiddenStatus(packageName: String, isHidden: Boolean)
    
    @Query("UPDATE apps SET alias = :alias WHERE packageName = :packageName")
    suspend fun updateAlias(packageName: String, alias: String?)
    
    @Query("UPDATE apps SET lastUsed = :timestamp, useCount = useCount + 1 WHERE packageName = :packageName")
    suspend fun updateUsage(packageName: String, timestamp: Long)
    
    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)
    
    @Query("DELETE FROM apps WHERE packageName NOT IN (:installedPackages)")
    suspend fun removeUninstalledApps(installedPackages: List<String>)
}

@Database(
    entities = [AppInfo::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 