# Android Development Reference Guide

## 📚 Table of Contents
1. [GitHub Actions CI/CD Issues](#github-actions-cicd-issues)
2. [Gradle Build Issues](#gradle-build-issues)
3. [Gradle Cache & Build Cache Issues](#gradle-cache--build-cache-issues)
4. [Git & Version Control Issues](#git--version-control-issues)
5. [Kotlin-Specific Issues](#kotlin-specific-issues)
6. [Android Studio & IDE Issues](#android-studio--ide-issues)
7. [Android Lint & Code Quality](#android-lint--code-quality)
8. [APK Signing & Release](#apk-signing--release)
9. [UI Design Best Practices](#ui-design-best-practices)
10. [Project Structure](#project-structure)
11. [Memory & Performance Issues](#memory--performance-issues)
12. [Testing Issues](#testing-issues)
13. [Common Error Solutions](#common-error-solutions)
14. [Checklist for New Projects](#checklist-for-new-projects)

---

## 🔧 GitHub Actions CI/CD Issues

### ❌ Common Problems:
1. **Deprecated Actions** - Using outdated action versions
2. **Missing Permissions** - Insufficient workflow permissions
3. **Artifact Download Failures** - Artifacts not found or inaccessible
4. **Test Report Issues** - Wrong test report paths
5. **Repository Configuration Conflicts** - Settings vs project repos

### ✅ Solutions & Best Practices:

#### 1. Use Updated Actions (2024+)
```yaml
# ✅ CORRECT - Updated versions
- uses: actions/checkout@v4
- uses: actions/setup-java@v4
- uses: actions/upload-artifact@v4
- uses: actions/download-artifact@v4
- uses: softprops/action-gh-release@v1
- uses: github/codeql-action/init@v3

# ❌ AVOID - Deprecated versions
- uses: actions/upload-artifact@v3
- uses: actions/create-release@v1
- uses: github/codeql-action/init@v2
```

#### 2. Proper Workflow Permissions
```yaml
permissions:
  contents: write      # For releases and repo access
  checks: write        # For test reports
  pull-requests: write # For PR comments
  security-events: write # For CodeQL
```

#### 3. Artifact Handling Best Practices
```yaml
# Upload with explicit paths
- name: Upload release APK
  uses: actions/upload-artifact@v4
  with:
    name: release-apk
    path: app/build/outputs/apk/release/*.apk

# Download with path specification
- name: Download release APK
  uses: actions/download-artifact@v4
  with:
    name: release-apk
    path: ./release-artifacts

# Always verify artifacts exist
- name: Verify APK exists
  run: |
    ls -la ./release-artifacts/
    if [ ! -f "./release-artifacts/app-release.apk" ] && [ ! -f "./release-artifacts/app-release-unsigned.apk" ]; then
      echo "No APK found!"
      exit 1
    fi
```

#### 4. Test Report Configuration
```yaml
# Correct test report paths for Android
- name: Generate test report
  uses: dorny/test-reporter@v1
  if: success() || failure()
  with:
    name: Unit Test Results
    path: 'app/build/test-results/testDebugUnitTest/TEST-*.xml'
    reporter: java-junit
```

---

## 🏗️ Gradle Build Issues

### ❌ Common Problems:
1. **Repository Configuration Conflicts** - FAIL_ON_PROJECT_REPOS vs PREFER_SETTINGS
2. **Missing Gradle Wrapper** - gradle-wrapper.jar not found
3. **Version Compatibility Issues** - Mismatched Gradle/Plugin versions
4. **Signing Configuration** - Missing or incorrect signing setup

### ✅ Solutions & Best Practices:

#### 1. Repository Configuration
```gradle
// settings.gradle - Use PREFER_SETTINGS instead of FAIL_ON_PROJECT_REPOS
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

// build.gradle (project level)
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

#### 2. Gradle Wrapper Setup
```bash
# Always ensure gradle wrapper exists in CI/CD
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
  gradle wrapper --gradle-version 8.0
fi
chmod +x gradlew
```

#### 3. Version Compatibility Matrix (2024)
```gradle
// ✅ RECOMMENDED VERSIONS
android {
    compileSdk 34
    defaultConfig {
        minSdk 21        # Covers 99%+ of devices
        targetSdk 34     # Latest for Play Store
    }
}

// Build tools
buildscript {
    ext.kotlin_version = "1.9.10"
    dependencies {
        classpath "com.android.tools.build:gradle:8.1.2"
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

// Dependencies
dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.room:room-runtime:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'
}
```

---

## 💾 Gradle Cache & Build Cache Issues

### ❌ Common Problems:
1. **Corrupted Build Cache** - Intermittent build failures
2. **Gradle Daemon Issues** - Memory leaks and performance problems
3. **Outdated Cache Files** - Stale dependencies causing conflicts
4. **Configuration Cache Problems** - Incompatible plugins

### ✅ Solutions & Best Practices:

#### 1. Cache Management Commands
```bash
# ❌ Nuclear option - Clean everything
./gradlew clean
rm -rf ~/.gradle/caches/
rm -rf ~/.gradle/daemon/
rm -rf .gradle/

# ✅ Better approach - Targeted cleaning
./gradlew clean --refresh-dependencies
./gradlew build --rerun-tasks

# Stop gradle daemons
./gradlew --stop

# Clear specific caches
rm -rf ~/.gradle/caches/modules-2/
rm -rf ~/.gradle/caches/transforms-*/
```

#### 2. Gradle Properties Configuration
```properties
# gradle.properties - Optimize caching and performance
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
org.gradle.daemon=true

# Build cache configuration
android.enableBuildCache=true
android.useAndroidX=true
android.enableJetifier=true

# Kotlin optimization
kotlin.incremental=true
kotlin.caching.enabled=true
kotlin.parallel.tasks.in.project=true
```

#### 3. CI/CD Cache Strategy
```yaml
# GitHub Actions - Cache Gradle dependencies
- name: Cache Gradle packages
  uses: actions/cache@v3
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
    restore-keys: |
      ${{ runner.os }}-gradle-

# Clear cache when needed
- name: Clear Gradle cache
  run: |
    ./gradlew --stop
    rm -rf ~/.gradle/caches/
```

#### 4. Common Cache Errors & Fixes
```bash
# Error: "Could not determine the dependencies of task"
# Solution: Clear transform cache
rm -rf ~/.gradle/caches/transforms-*/

# Error: "Gradle daemon disappeared unexpectedly"
# Solution: Increase daemon memory
echo "org.gradle.jvmargs=-Xmx4g" >> gradle.properties

# Error: "Configuration cache problems"
# Solution: Disable configuration cache temporarily
./gradlew build --no-configuration-cache
```

---

## 🔀 Git & Version Control Issues

### ❌ Common Problems:
1. **Line Ending Issues** - Windows vs Unix line endings
2. **Large Binary Files** - APKs committed to Git
3. **Merge Conflicts** - Auto-generated files conflicts
4. **Branch Management** - Outdated branches and merge issues

### ✅ Solutions & Best Practices:

#### 1. Git Configuration for Android
```bash
# Set up proper line endings
git config core.autocrlf input  # On Unix/Mac
git config core.autocrlf true   # On Windows

# Set up .gitattributes
echo "* text=auto" > .gitattributes
echo "*.bat text eol=crlf" >> .gitattributes
echo "*.sh text eol=lf" >> .gitattributes
```

#### 2. Essential .gitignore for Android
```gitignore
# Built application files
*.apk
*.aar
*.ap_
*.aab

# Files for the ART/Dalvik VM
*.dex

# Java class files
*.class

# Generated files
bin/
gen/
out/
build/

# Gradle files
.gradle/
app/build/
gradle-app.setting
!gradle-wrapper.jar

# Android Studio
.idea/
*.iml
*.ipr
*.iws
.navigation/
captures/
.externalNativeBuild/
.cxx/

# Keystore files (NEVER commit these)
*.jks
*.keystore
release.properties

# Version control
.svn/

# Cache and temp files
*.tmp
*.temp
.DS_Store
Thumbs.db

# NDK
obj/

# Android Profiling
*.hprof
```

#### 3. Git Hooks for Android Projects
```bash
# pre-commit hook - Run lint before commit
#!/bin/sh
echo "Running Android lint..."
./gradlew lintDebug
if [ $? -ne 0 ]; then
    echo "Lint failed. Fix issues before committing."
    exit 1
fi

# pre-push hook - Run tests before push
#!/bin/sh
echo "Running unit tests..."
./gradlew test
if [ $? -ne 0 ]; then
    echo "Tests failed. Fix tests before pushing."
    exit 1
fi
```

#### 4. Common Git Error Solutions
```bash
# Error: "LF will be replaced by CRLF"
# Solution: Configure line endings properly
git config core.autocrlf input

# Error: "Large files detected"
# Solution: Use git-lfs for large files
git lfs track "*.apk"
git lfs track "*.aar"

# Error: "Merge conflict in auto-generated files"
# Solution: Regenerate files after merge
./gradlew clean build

# Error: "Repository too large"
# Solution: Use BFG to remove large files from history
java -jar bfg.jar --strip-blobs-bigger-than 50M .git
```

---

## 🔧 Kotlin-Specific Issues

### ❌ Common Problems:
1. **Compilation Errors** - Type inference failures
2. **KAPT Issues** - Annotation processing problems
3. **Coroutines Errors** - Threading and context issues
4. **Null Safety Violations** - Platform types causing crashes

### ✅ Solutions & Best Practices:

#### 1. Kotlin Compiler Configuration
```gradle
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = '1.8'
        freeCompilerArgs += [
            '-Xjvm-default=all',
            '-opt-in=kotlin.RequiresOptIn'
        ]
    }
}

// Enable incremental compilation
kotlin {
    incremental = true
}
```

#### 2. KAPT Optimization
```gradle
kapt {
    correctErrorTypes = true
    useBuildCache = true
    includeCompileClasspath = false
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }
}

// Consider migrating from KAPT to KSP
plugins {
    id 'com.google.devtools.ksp' version '1.9.10-1.0.13'
}

dependencies {
    ksp 'androidx.room:room-compiler:2.6.1'  // Instead of kapt
}
```

#### 3. Common Kotlin Patterns & Fixes
```kotlin
// ❌ AVOID - Nullable platform types
val text = intent.getStringExtra("key")  // String? (platform type)

// ✅ BETTER - Explicit null handling
val text = intent.getStringExtra("key") ?: ""

// ❌ AVOID - Blocking coroutines
runBlocking {
    repository.getData()
}

// ✅ BETTER - Proper coroutine scope
viewLifecycleOwner.lifecycleScope.launch {
    repository.getData()
}

// ❌ AVOID - Memory leaks with context
class MyRepository(private val context: Context) {
    // This holds reference to Activity
}

// ✅ BETTER - Use application context
class MyRepository(private val context: Context) {
    private val appContext = context.applicationContext
}
```

#### 4. Kotlin Error Solutions
```kotlin
// Error: "Type inference failed"
// Solution: Provide explicit types
val list: List<String> = emptyList()

// Error: "Unresolved reference"
// Solution: Check imports and dependencies
import kotlinx.coroutines.launch

// Error: "lateinit property has not been initialized"
// Solution: Use lazy initialization or nullable types
private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

// Error: "Suspend function called from wrong context"
// Solution: Use proper coroutine scope
lifecycleScope.launch { /* suspend function */ }
```

---

## 🖥️ Android Studio & IDE Issues

### ❌ Common Problems:
1. **Slow Indexing** - IDE freezing during project sync
2. **Memory Issues** - OutOfMemoryError in IDE
3. **Plugin Conflicts** - Incompatible plugin versions
4. **Gradle Sync Failures** - Project not syncing properly

### ✅ Solutions & Best Practices:

#### 1. Android Studio Optimization
```properties
# studio.vmoptions - Increase IDE memory
-Xms2048m
-Xmx8192m
-XX:ReservedCodeCacheSize=1024m
-XX:+UseConcMarkSweepGC
-XX:SoftRefLRUPolicyMSPerMB=50
```

#### 2. Project Structure Best Practices
```gradle
// Modularize large projects
include ':app'
include ':core:common'
include ':core:network'
include ':feature:search'
include ':feature:favorites'

// Use composite builds for large projects
includeBuild '../shared-library'
```

#### 3. Common IDE Issues & Solutions
```bash
# Issue: "Gradle sync failed"
# Solution: Clear IDE caches
File > Invalidate Caches and Restart

# Issue: "Cannot resolve symbol"
# Solution: Reimport project
File > Sync Project with Gradle Files

# Issue: "Build tools version mismatch"
# Solution: Update build tools in build.gradle
android {
    buildToolsVersion "34.0.0"
}

# Issue: "IDE freezing during indexing"
# Solution: Exclude unnecessary folders from indexing
File > Settings > Project Structure > Modules > Sources > Excluded
```

---

## 🔍 Android Lint & Code Quality

### ❌ Common Problems:
1. **API Level Compatibility Errors** - Using newer APIs on older min SDK
2. **Deprecation Warnings** - Using deprecated methods/classes
3. **Lint Blocking Builds** - Strict lint settings failing builds

### ✅ Solutions & Best Practices:

#### 1. Lint Configuration
```gradle
android {
    lint {
        abortOnError false           # Don't fail builds on lint errors
        warningsAsErrors false       # Don't treat warnings as errors
        disable 'NewApi', 'Deprecation'  # Disable problematic checks
        checkReleaseBuilds false     # Skip lint on release builds
        baseline = file("lint-baseline.xml")  # Use baseline for existing issues
    }
}
```

#### 2. API Level Compatibility
```xml
<!-- values/themes.xml - Base theme for minSdk -->
<style name="Theme.App" parent="Theme.Material3.DayNight">
    <item name="colorPrimary">@color/primary</item>
    <!-- Don't include API 23+ attributes here -->
</style>

<!-- values-v23/themes.xml - API 23+ specific attributes -->
<style name="Theme.App" parent="Theme.Material3.DayNight">
    <item name="colorPrimary">@color/primary</item>
    <item name="android:windowLightStatusBar">false</item>
</style>
```

#### 3. Fix Deprecated Methods
```kotlin
// ❌ DEPRECATED
override fun onBackPressed() {
    super.onBackPressed()
}

// ✅ MODERN APPROACH
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

// ❌ DEPRECATED
val position = adapterPosition

// ✅ MODERN APPROACH
val position = bindingAdapterPosition
```

---

## 📱 APK Signing & Release

### ❌ Common Problems:
1. **Unsigned APK Generation** - Gets `app-release-unsigned.apk` instead of `app-release.apk`
2. **Wrong APK Names** - Hardcoded paths expecting specific names
3. **Missing Signing Config** - No signing configuration for release builds

### ✅ Solutions & Best Practices:

#### 1. Debug Signing for Open Source Projects
```gradle
android {
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            // Use debug signing for open source projects
            // This ensures we get app-release.apk instead of app-release-unsigned.apk
            signingConfig signingConfigs.debug
        }
    }
}
```

#### 2. Dynamic APK Detection
```bash
# Handle both signed and unsigned APKs
if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
    echo "Found signed release APK"
elif [ -f "app/build/outputs/apk/release/app-release-unsigned.apk" ]; then
    APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
    echo "Found unsigned release APK"
else
    echo "No release APK found!"
    exit 1
fi
```

#### 3. Automatic Release Creation
```yaml
# Auto-generate releases from version info
- name: Get version from build.gradle
  id: get_version
  run: |
    VERSION_NAME=$(grep "versionName" app/build.gradle | cut -d '"' -f2)
    VERSION_CODE=$(grep "versionCode" app/build.gradle | awk '{print $2}')
    echo "tag_name=v${VERSION_NAME}-${VERSION_CODE}" >> $GITHUB_OUTPUT

- name: Create Release with APKs
  uses: softprops/action-gh-release@v1
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  with:
    tag_name: ${{ steps.get_version.outputs.tag_name }}
    files: |
      ./release-artifacts/*.apk
      ./debug-artifacts/*.apk
```

---

## 🎨 UI Design Best Practices

### ❌ Common Problems:
1. **Poor Layout Structure** - Using outdated layout patterns
2. **Inconsistent Spacing** - No design system for margins/padding
3. **Non-responsive Design** - Not adapting to different screen sizes

### ✅ Solutions & Best Practices:

#### 1. Modern Layout Structure
```xml
<!-- Use NestedScrollView for complex scrollable layouts -->
<androidx.core.widget.NestedScrollView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <LinearLayout
        android:orientation="vertical"
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <!-- Section headers with proper typography -->
        <TextView
            android:text="SECTION TITLE"
            android:textSize="12sp"
            android:textStyle="bold"
            android:letterSpacing="0.1"
            android:textColor="@color/text_secondary" />

        <!-- RecyclerView with nestedScrollingEnabled="false" -->
        <androidx.recyclerview.widget.RecyclerView
            android:nestedScrollingEnabled="false"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />

    </LinearLayout>
</androidx.core.widget.NestedScrollView>
```

#### 2. Material Design Components
```xml
<!-- Use MaterialCardView for modern cards -->
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp">

<!-- Circular icons for favorite apps -->
<com.google.android.material.card.MaterialCardView
    android:layout_width="56dp"
    android:layout_height="56dp"
    app:cardCornerRadius="28dp"
    app:cardElevation="2dp">
```

---

## 📂 Project Structure

### ✅ Recommended Structure:
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/package/
│   │   │   ├── data/          # Data layer
│   │   │   │   ├── database/  # Room database
│   │   │   │   ├── network/   # API clients
│   │   │   │   └── repository/ # Repository pattern
│   │   │   ├── domain/        # Business logic
│   │   │   │   ├── model/     # Domain models
│   │   │   │   └── usecase/   # Use cases
│   │   │   ├── presentation/  # UI layer
│   │   │   │   ├── ui/        # Activities/Fragments
│   │   │   │   ├── adapter/   # RecyclerView adapters
│   │   │   │   └── viewmodel/ # ViewModels
│   │   │   └── di/            # Dependency injection
│   │   └── res/               # Resources
│   └── test/                  # Unit tests
└── build.gradle
```

---

## 🚀 Memory & Performance Issues

### ❌ Common Problems:
1. **Memory Leaks** - Activities not being garbage collected
2. **ANR (Application Not Responding)** - Long operations on main thread
3. **Excessive Memory Usage** - Large bitmaps and data structures

### ✅ Solutions & Best Practices:

#### 1. Memory Leak Prevention
```kotlin
// ❌ CAUSES MEMORY LEAK
class MainActivity : AppCompatActivity() {
    private lateinit var handler: Handler
    
    override fun onCreate(savedInstanceState: Bundle?) {
        handler = Handler(Looper.getMainLooper())
        handler.postDelayed({ /* work */ }, 10000) // Holds reference to Activity
    }
}

// ✅ PREVENT MEMORY LEAK
class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable { /* work */ }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        handler.postDelayed(runnable, 10000)
    }
    
    override fun onDestroy() {
        handler.removeCallbacks(runnable) // Clean up
        super.onDestroy()
    }
}
```

#### 2. Performance Optimization
```kotlin
// ❌ EXPENSIVE OPERATION ON MAIN THREAD
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val data = loadLargeDataset() // Blocks UI
    updateUI(data)
}

// ✅ BACKGROUND PROCESSING
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    lifecycleScope.launch {
        val data = withContext(Dispatchers.IO) {
            loadLargeDataset() // Background thread
        }
        updateUI(data) // Back on main thread
    }
}
```

---

## 🧪 Testing Issues

### ❌ Common Problems:
1. **Test Dependencies Missing** - Test dependencies not configured
2. **MockK Issues** - Mocking framework conflicts
3. **Instrumentation Test Failures** - Tests failing on CI/CD

### ✅ Solutions & Best Practices:

#### 1. Test Dependencies Setup
```gradle
dependencies {
    // Unit testing
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:5.7.0'
    testImplementation 'org.mockito.kotlin:mockito-kotlin:5.1.0'
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
    testImplementation 'androidx.arch.core:core-testing:2.2.0'
    
    // Instrumentation testing
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.test:runner:1.5.2'
    androidTestImplementation 'androidx.test:rules:1.5.0'
}
```

#### 2. Testing Best Practices
```kotlin
// ✅ PROPER UNIT TEST STRUCTURE
@ExtendWith(MockitoExtension::class)
class MainViewModelTest {
    
    @Mock
    private lateinit var repository: AppRepository
    
    private lateinit var viewModel: MainViewModel
    
    @Before
    fun setup() {
        viewModel = MainViewModel(repository)
    }
    
    @Test
    fun `searchApps should update apps list`() = runTest {
        // Given
        val expectedApps = listOf(mockApp1, mockApp2)
        whenever(repository.searchApps("test")).thenReturn(expectedApps)
        
        // When
        viewModel.searchApps("test")
        
        // Then
        assertEquals(expectedApps, viewModel.apps.value)
    }
}
```

---

## 🚨 Common Error Solutions

### 1. Gradle Build Errors
```bash
# Error: "Build was configured to prefer settings repositories"
# Solution: Update settings.gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
}

# Error: "Could not find gradle-wrapper.jar"
# Solution: Generate wrapper
gradle wrapper --gradle-version 8.0

# Error: "Execution failed for task ':app:processDebugResources'"
# Solution: Clean and rebuild
./gradlew clean build
```

### 2. Kotlin Compilation Errors
```bash
# Error: "Cannot access class X. Check your module classpath"
# Solution: Add missing dependency and sync project

# Error: "Unresolved reference"
# Solution: Check imports and clean project
./gradlew clean
File > Sync Project with Gradle Files

# Error: "Type mismatch: inferred type is X but Y was expected"
# Solution: Add explicit type casting or check nullability
```

### 3. Runtime Errors
```bash
# Error: "java.lang.OutOfMemoryError"
# Solution: Increase heap size
echo "org.gradle.jvmargs=-Xmx4g" >> gradle.properties

# Error: "AndroidX Fragment exception"
# Solution: Use FragmentContainerView instead of fragment tag

# Error: "NetworkOnMainThreadException"
# Solution: Use coroutines or AsyncTask for network calls
```

### 4. Lint Errors
```bash
# Error: "windowLightStatusBar requires API level 23"
# Solution: Create values-v23/themes.xml with API 23+ attributes

# Error: "onBackPressed deprecated"
# Solution: Use OnBackPressedDispatcher instead

# Error: "Hardcoded string should use @string resource"
# Solution: Move strings to strings.xml
```

### 5. GitHub Actions Errors
```bash
# Error: "Artifact not found: release-apk"
# Solution: Use explicit paths and verify artifacts exist

# Error: "Resource not accessible by integration"
# Solution: Add 'contents: write' permission to workflow

# Error: "Node.js 16 actions are deprecated"
# Solution: Update to actions/checkout@v4, actions/setup-java@v4
```

### 6. APK Issues
```bash
# Error: "app-release.apk not found" (but app-release-unsigned.apk exists)
# Solution: Configure debug signing or handle both filenames dynamically

# Error: "App not installed" on device
# Solution: Uninstall previous version or check signing configuration

# Error: "APK too large for Play Store"
# Solution: Enable R8/ProGuard minification and use App Bundles (.aab)
```

### 7. Cache & Performance Issues
```bash
# Error: "Gradle daemon disappeared unexpectedly"
# Solution: Increase daemon memory and restart
./gradlew --stop
echo "org.gradle.jvmargs=-Xmx4g" >> gradle.properties

# Error: "Configuration cache problems"
# Solution: Disable configuration cache or fix incompatible plugins
./gradlew build --no-configuration-cache

# Error: "Build cache corruption"
# Solution: Clear cache and rebuild
rm -rf ~/.gradle/caches/
./gradlew clean build
```

---

## ✅ Checklist for New Projects

### Before Starting:
- [ ] Choose latest stable Android Gradle Plugin version (8.1.2+)
- [ ] Set appropriate minSdk (21+ covers 99% of devices)
- [ ] Use latest compileSdk and targetSdk (34+)
- [ ] Configure proper signing for releases
- [ ] Set up .gitignore and .gitattributes files
- [ ] Configure gradle.properties for performance

### During Development:
- [ ] Create API-specific resource folders (values-v23/, etc.)
- [ ] Use modern Material Design 3 components
- [ ] Implement proper MVVM architecture
- [ ] Add comprehensive unit tests
- [ ] Configure lint baseline for existing issues
- [ ] Set up proper dependency injection
- [ ] Implement proper error handling and logging

### GitHub Actions Setup:
- [ ] Use latest action versions (v4, not v3)
- [ ] Add proper permissions to workflows (contents: write)
- [ ] Set up automatic release creation
- [ ] Configure artifact handling with explicit paths
- [ ] Add test report generation with correct paths
- [ ] Set up caching strategy for Gradle dependencies

### UI Best Practices:
- [ ] Use sectioned layouts for better organization
- [ ] Implement proper spacing and typography
- [ ] Use Grid layouts for app lists (4 columns)
- [ ] Add proper search functionality
- [ ] Ensure responsive design with NestedScrollView
- [ ] Test on different screen sizes and orientations

### Code Quality:
- [ ] Set up static analysis tools (lint, detekt)
- [ ] Configure pre-commit hooks
- [ ] Add unit and instrumentation tests
- [ ] Set up continuous integration
- [ ] Document public APIs and complex logic

---

## 🔗 Quick Commands Reference

```bash
# Project setup
gradle wrapper --gradle-version 8.0
chmod +x gradlew

# Build commands
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew build

# Testing
./gradlew test
./gradlew connectedAndroidTest
./gradlew testDebugUnitTest

# Code quality
./gradlew lintDebug
./gradlew detekt
./gradlew updateLintBaseline

# Cache management
./gradlew clean
./gradlew --stop
./gradlew build --refresh-dependencies
./gradlew build --rerun-tasks

# Dependency analysis
./gradlew dependencies
./gradlew dependencyInsight --dependency <dependency-name>

# Performance analysis
./gradlew --profile build
./gradlew --scan build

# Git commands for Android
git config core.autocrlf input
git lfs track "*.apk"
git lfs track "*.aar"
```

---

## 🔧 Emergency Troubleshooting

When everything fails, try these in order:

1. **Clean everything**:
```bash
./gradlew clean
./gradlew --stop
rm -rf ~/.gradle/caches/
rm -rf .gradle/
```

2. **Invalidate IDE caches**:
   - File > Invalidate Caches and Restart

3. **Check versions compatibility**:
   - Gradle version vs AGP version
   - Kotlin version vs Gradle version
   - Target SDK vs compile SDK

4. **Reset Git state** (if needed):
```bash
git stash
git clean -fd
git reset --hard HEAD
```

5. **Recreate project** (last resort):
   - Export settings and dependencies
   - Create new project with Android Studio
   - Copy source files manually

---

**📅 Last Updated**: December 2024  
**⚙️ Versions**: Android Gradle Plugin 8.1.2, Kotlin 1.9.10, Target SDK 34, Min SDK 21

> 💡 **Usage**: Reference this file before starting new Android projects and when encountering any errors. Update it whenever you discover new solutions!

> 🔄 **Maintenance**: Review and update this file quarterly to keep up with new Android/Kotlin versions and emerging best practices. 