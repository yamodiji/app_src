package com.appdrawer.fast.models

import android.graphics.drawable.Drawable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppInfo(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val activityName: String,
    var isFavorite: Boolean = false,
    var isHidden: Boolean = false,
    var alias: String? = null,
    var lastUsed: Long = 0L,
    var useCount: Int = 0,
    var customIconPath: String? = null
) {
    // Non-persisted fields
    @androidx.room.Ignore
    var icon: Drawable? = null
    
    @androidx.room.Ignore
    var searchScore: Float = 0f
}

data class SearchResult(
    val app: AppInfo,
    val matchType: MatchType,
    val matchScore: Float
)

enum class MatchType {
    EXACT_NAME,
    START_NAME,
    CONTAINS_NAME,
    ALIAS,
    PACKAGE_NAME,
    T9,
    FUZZY
} 