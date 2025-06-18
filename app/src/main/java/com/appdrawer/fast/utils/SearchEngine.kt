package com.appdrawer.fast.utils

import com.appdrawer.fast.models.AppInfo
import com.appdrawer.fast.models.MatchType
import com.appdrawer.fast.models.SearchResult
import java.util.*
import kotlin.math.max
import kotlin.math.min

class SearchEngine {
    
    // T9 keypad mapping
    private val t9Map = mapOf(
        '2' to "abc", '3' to "def", '4' to "ghi", '5' to "jkl",
        '6' to "mno", '7' to "pqrs", '8' to "tuv", '9' to "wxyz"
    )
    
    fun search(query: String, apps: List<AppInfo>): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        
        val results = mutableListOf<SearchResult>()
        val queryLower = query.lowercase()
        
        for (app in apps) {
            if (app.isHidden) continue
            
            val appNameLower = app.appName.lowercase()
            val packageNameLower = app.packageName.lowercase()
            val aliasLower = app.alias?.lowercase()
            
            // Exact name match (highest priority)
            if (appNameLower == queryLower) {
                results.add(SearchResult(app, MatchType.EXACT_NAME, 1.0f))
                continue
            }
            
            // Alias match
            if (aliasLower == queryLower) {
                results.add(SearchResult(app, MatchType.ALIAS, 0.95f))
                continue
            }
            
            // Start of name match
            if (appNameLower.startsWith(queryLower)) {
                val score = 0.9f - (queryLower.length.toFloat() / appNameLower.length * 0.1f)
                results.add(SearchResult(app, MatchType.START_NAME, score))
                continue
            }
            
            // Contains in name
            if (appNameLower.contains(queryLower)) {
                val score = 0.8f - (queryLower.length.toFloat() / appNameLower.length * 0.2f)
                results.add(SearchResult(app, MatchType.CONTAINS_NAME, score))
                continue
            }
            
            // Package name match
            if (packageNameLower.contains(queryLower)) {
                results.add(SearchResult(app, MatchType.PACKAGE_NAME, 0.7f))
                continue
            }
            
            // T9 search
            if (query.all { it.isDigit() }) {
                val t9Score = getT9Score(query, appNameLower)
                if (t9Score > 0.5f) {
                    results.add(SearchResult(app, MatchType.T9, t9Score * 0.6f))
                    continue
                }
            }
            
            // Fuzzy matching (lowest priority)
            val fuzzyScore = getFuzzyScore(queryLower, appNameLower)
            if (fuzzyScore > 0.4f) {
                results.add(SearchResult(app, MatchType.FUZZY, fuzzyScore * 0.5f))
            }
        }
        
        // Sort by score (descending) and then by usage frequency
        return results.sortedWith(compareByDescending<SearchResult> { it.matchScore }
            .thenByDescending { it.app.useCount }
            .thenByDescending { it.app.lastUsed })
    }
    
    private fun getT9Score(query: String, appName: String): Float {
        if (query.length > appName.length) return 0f
        
        var queryIndex = 0
        var appIndex = 0
        var matches = 0
        
        while (queryIndex < query.length && appIndex < appName.length) {
            val digit = query[queryIndex]
            val char = appName[appIndex].lowercaseChar()
            
            val validChars = t9Map[digit] ?: ""
            if (validChars.contains(char)) {
                matches++
                queryIndex++
            }
            appIndex++
        }
        
        return if (queryIndex == query.length) {
            matches.toFloat() / query.length
        } else {
            0f
        }
    }
    
    private fun getFuzzyScore(query: String, target: String): Float {
        val distance = levenshteinDistance(query, target)
        val maxLength = max(query.length, target.length)
        return if (maxLength == 0) 1f else 1f - (distance.toFloat() / maxLength)
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }
} 