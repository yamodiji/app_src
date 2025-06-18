package com.appdrawer.fast

import com.appdrawer.fast.models.AppInfo
import com.appdrawer.fast.models.MatchType
import com.appdrawer.fast.utils.SearchEngine
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

class SearchEngineTest {
    
    private lateinit var searchEngine: SearchEngine
    private lateinit var testApps: List<AppInfo>
    
    @Before
    fun setUp() {
        searchEngine = SearchEngine()
        
        // Create test apps
        testApps = listOf(
            AppInfo("com.facebook.katana", "Facebook", "com.facebook.katana.MainActivity"),
            AppInfo("com.google.android.gm", "Gmail", "com.google.android.gm.MainActivity"),
            AppInfo("com.whatsapp", "WhatsApp", "com.whatsapp.MainActivity"),
            AppInfo("com.spotify.music", "Spotify", "com.spotify.music.MainActivity", alias = "music"),
            AppInfo("com.android.calculator2", "Calculator", "com.android.calculator2.MainActivity"),
            AppInfo("com.instagram.android", "Instagram", "com.instagram.android.MainActivity")
        )
    }
    
    @Test
    fun testExactMatch() {
        val results = searchEngine.search("Facebook", testApps)
        
        assertTrue("Should have results", results.isNotEmpty())
        assertEquals("Should match Facebook", "Facebook", results[0].app.appName)
        assertEquals("Should be exact match", MatchType.EXACT_NAME, results[0].matchType)
        assertEquals("Should have score 1.0", 1.0f, results[0].matchScore, 0.01f)
    }
    
    @Test
    fun testStartMatch() {
        val results = searchEngine.search("Face", testApps)
        
        assertTrue("Should have results", results.isNotEmpty())
        assertEquals("Should match Facebook", "Facebook", results[0].app.appName)
        assertEquals("Should be start match", MatchType.START_NAME, results[0].matchType)
    }
    
    @Test
    fun testContainsMatch() {
        val results = searchEngine.search("book", testApps)
        
        assertTrue("Should have results", results.isNotEmpty())
        assertEquals("Should match Facebook", "Facebook", results[0].app.appName)
        assertEquals("Should be contains match", MatchType.CONTAINS_NAME, results[0].matchType)
    }
    
    @Test
    fun testAliasMatch() {
        val results = searchEngine.search("music", testApps)
        
        assertTrue("Should have results", results.isNotEmpty())
        assertEquals("Should match Spotify", "Spotify", results[0].app.appName)
        assertEquals("Should be alias match", MatchType.ALIAS, results[0].matchType)
    }
    
    @Test
    fun testPackageNameMatch() {
        val results = searchEngine.search("android.gm", testApps)
        
        assertTrue("Should have results", results.isNotEmpty())
        assertEquals("Should match Gmail", "Gmail", results[0].app.appName)
        assertEquals("Should be package match", MatchType.PACKAGE_NAME, results[0].matchType)
    }
    
    @Test
    fun testT9Search() {
        // 4624 = GMAIL on T9 keypad
        val results = searchEngine.search("4624", testApps)
        
        assertTrue("Should have results", results.isNotEmpty())
        assertEquals("Should match Gmail", "Gmail", results[0].app.appName)
        assertEquals("Should be T9 match", MatchType.T9, results[0].matchType)
    }
    
    @Test
    fun testFuzzySearch() {
        // Typo in Facebook
        val results = searchEngine.search("Facbook", testApps)
        
        assertTrue("Should have results", results.isNotEmpty())
        assertEquals("Should match Facebook", "Facebook", results[0].app.appName)
        assertEquals("Should be fuzzy match", MatchType.FUZZY, results[0].matchType)
    }
    
    @Test
    fun testEmptyQuery() {
        val results = searchEngine.search("", testApps)
        assertTrue("Should have no results for empty query", results.isEmpty())
    }
    
    @Test
    fun testNoMatch() {
        val results = searchEngine.search("xyz123", testApps)
        assertTrue("Should have no results for non-matching query", results.isEmpty())
    }
    
    @Test
    fun testHiddenAppsExcluded() {
        val hiddenApp = AppInfo("com.hidden.app", "Hidden App", "com.hidden.app.MainActivity", isHidden = true)
        val appsWithHidden = testApps + hiddenApp
        
        val results = searchEngine.search("Hidden", appsWithHidden)
        assertTrue("Hidden apps should not appear in results", results.isEmpty())
    }
    
    @Test
    fun testResultsSortedByScore() {
        val results = searchEngine.search("Gm", testApps)
        
        assertTrue("Should have results", results.isNotEmpty())
        
        // Results should be sorted by score in descending order
        for (i in 0 until results.size - 1) {
            assertTrue(
                "Results should be sorted by score",
                results[i].matchScore >= results[i + 1].matchScore
            )
        }
    }
    
    @Test
    fun testCaseInsensitiveSearch() {
        val lowerCaseResults = searchEngine.search("facebook", testApps)
        val upperCaseResults = searchEngine.search("FACEBOOK", testApps)
        val mixedCaseResults = searchEngine.search("FaceBook", testApps)
        
        assertTrue("Lowercase should have results", lowerCaseResults.isNotEmpty())
        assertTrue("Uppercase should have results", upperCaseResults.isNotEmpty())
        assertTrue("Mixed case should have results", mixedCaseResults.isNotEmpty())
        
        assertEquals("Should match same app", lowerCaseResults[0].app.appName, upperCaseResults[0].app.appName)
        assertEquals("Should match same app", lowerCaseResults[0].app.appName, mixedCaseResults[0].app.appName)
    }
} 