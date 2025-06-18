# Build Instructions for Fast App Drawer

This document provides step-by-step instructions to build the Fast App Drawer Android application.

## 📋 Prerequisites

Before building the project, ensure you have:

### Required Software
- **Android Studio**: Latest stable version (Arctic Fox or newer)
- **JDK**: Java Development Kit 17 or higher
- **Git**: For version control
- **Android SDK**: API level 34 (Android 14)

### Environment Setup
1. **Install Android Studio** from [developer.android.com](https://developer.android.com/studio)
2. **Configure Android SDK** with these components:
   - Android SDK Platform 34
   - Android SDK Build-Tools 34.0.0
   - Android SDK Platform-Tools
   - Android SDK Tools

## 🚀 Quick Start

### Option 1: Using Android Studio (Recommended)

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yamodiji/app_src.git
   cd app_src
   ```

2. **Generate Gradle Wrapper** (if gradle/wrapper/gradle-wrapper.jar is missing):
   ```bash
   # If you have Gradle installed globally:
   gradle wrapper --gradle-version 8.0
   
   # OR download and extract a Gradle distribution and run:
   # ./gradle-8.0/bin/gradle wrapper
   ```

3. **Open in Android Studio**:
   - Launch Android Studio
   - Choose "Open an existing Android Studio project"
   - Navigate to the cloned directory and select it
   - Wait for Gradle sync to complete

4. **Build the project**:
   - Click "Build" → "Make Project" (Ctrl+F9)
   - Or use "Build" → "Generate Signed Bundle/APK"

### Option 2: Command Line Build

1. **Setup environment variables**:
   ```bash
   export JAVA_HOME=/path/to/jdk17
   export ANDROID_HOME=/path/to/android-sdk
   export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools
   ```

2. **Generate Gradle Wrapper** (if missing):
   ```bash
   # Method 1: If you have Gradle installed
   gradle wrapper --gradle-version 8.0
   
   # Method 2: Download wrapper manually
   curl -L https://services.gradle.org/distributions/gradle-8.0-bin.zip -o gradle.zip
   unzip gradle.zip
   ./gradle-8.0/bin/gradle wrapper
   rm -rf gradle-8.0 gradle.zip
   ```

3. **Make gradlew executable**:
   ```bash
   chmod +x gradlew
   ```

4. **Build the project**:
   ```bash
   # Debug build
   ./gradlew assembleDebug
   
   # Release build
   ./gradlew assembleRelease
   
   # Run tests
   ./gradlew test
   
   # Run all checks
   ./gradlew check
   ```

## 🔧 Build Variants

### Debug Build
- **Command**: `./gradlew assembleDebug`
- **Output**: `app/build/outputs/apk/debug/app-debug.apk`
- **Features**: 
  - Debuggable
  - No obfuscation
  - Faster build time

### Release Build
- **Command**: `./gradlew assembleRelease`
- **Output**: `app/build/outputs/apk/release/app-release.apk`
- **Features**:
  - Optimized and minified
  - ProGuard/R8 obfuscation
  - Smaller APK size

## 🧪 Testing

### Unit Tests
```bash
# Run all unit tests
./gradlew test

# Run tests with coverage
./gradlew testDebugUnitTestCoverage

# View test results
open app/build/reports/tests/testDebugUnitTest/index.html
```

### Instrumented Tests
```bash
# Connect Android device or start emulator first
adb devices

# Run instrumented tests
./gradlew connectedAndroidTest

# View test results
open app/build/reports/androidTests/connected/index.html
```

### Lint Checks
```bash
# Run Android lint
./gradlew lintDebug

# View lint results
open app/build/reports/lint-results-debug.html
```

## 📱 Installing APK

### Install Debug APK
```bash
# Build and install debug version
./gradlew installDebug

# Or manually install
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Install Release APK
```bash
# Build and install release version
./gradlew installRelease

# Or manually install
adb install app/build/outputs/apk/release/app-release.apk
```

## 🔐 Signing Configuration (For Production)

### Generate Keystore
```bash
keytool -genkey -v -keystore fastappdrawer.keystore -alias fastappdrawer -keyalg RSA -keysize 2048 -validity 10000
```

### Configure Signing
1. Create `keystore.properties` in project root:
   ```properties
   storeFile=fastappdrawer.keystore
   storePassword=your_store_password
   keyAlias=fastappdrawer
   keyPassword=your_key_password
   ```

2. Update `app/build.gradle`:
   ```gradle
   android {
       signingConfigs {
           release {
               def keystorePropertiesFile = rootProject.file("keystore.properties")
               def keystoreProperties = new Properties()
               keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
               
               keyAlias keystoreProperties['keyAlias']
               keyPassword keystoreProperties['keyPassword']
               storeFile file(keystoreProperties['storeFile'])
               storePassword keystoreProperties['storePassword']
           }
       }
       
       buildTypes {
           release {
               signingConfig signingConfigs.release
               // ... other configurations
           }
       }
   }
   ```

## 🏗️ GitHub Actions CI/CD

The project includes automated build pipelines:

### Automatic Builds
- **Push to main/develop**: Builds debug APK
- **Pull requests**: Runs full test suite
- **Releases**: Builds signed release APK

### Manual Trigger
1. Go to repository → Actions tab
2. Select workflow
3. Click "Run workflow"

### Download Artifacts
- **Debug APKs**: Available in Actions → Workflow runs → Artifacts
- **Release APKs**: Attached to GitHub releases

## 🐛 Troubleshooting

### Common Issues

#### Gradle Wrapper Missing
```bash
# Generate wrapper
gradle wrapper --gradle-version 8.0

# Or download manually
wget https://services.gradle.org/distributions/gradle-8.0-bin.zip
unzip gradle-8.0-bin.zip
./gradle-8.0/bin/gradle wrapper
```

#### Build Fails with Kotlin Errors
```bash
# Clean and rebuild
./gradlew clean build
```

#### SDK Not Found
```bash
# Set ANDROID_HOME
export ANDROID_HOME=/path/to/android-sdk
echo 'export ANDROID_HOME=/path/to/android-sdk' >> ~/.bashrc
```

#### Permission Denied on gradlew
```bash
chmod +x gradlew
```

#### OutOfMemoryError
```bash
# Increase Gradle heap size
export GRADLE_OPTS="-Xmx4g -Xms1g"
```

### Build Cache Issues
```bash
# Clear Gradle cache
./gradlew clean
rm -rf ~/.gradle/caches

# Clear Android Studio cache
rm -rf ~/.android/build-cache
```

## 📊 Performance Optimization

### Faster Builds
1. **Enable Gradle Daemon**: Add to `gradle.properties`:
   ```properties
   org.gradle.daemon=true
   org.gradle.parallel=true
   org.gradle.configureondemand=true
   ```

2. **Increase Memory**: Add to `gradle.properties`:
   ```properties
   org.gradle.jvmargs=-Xmx4g -XX:MaxPermSize=512m
   ```

3. **Use Build Cache**:
   ```properties
   org.gradle.caching=true
   ```

## 📱 Testing on Different API Levels

### Create AVD (Android Virtual Device)
```bash
# List available system images
sdkmanager --list | grep system-images

# Create AVD for API 34
avdmanager create avd -n FastAppDrawer_API34 -k "system-images;android-34;google_apis;x86_64"

# Start emulator
emulator -avd FastAppDrawer_API34
```

### Test on Multiple Devices
```bash
# Run tests on all connected devices
./gradlew connectedAndroidTest

# Run on specific device
adb -s device_id install app-debug.apk
```

## 🎯 Next Steps

1. **Build the app**: Follow the Quick Start instructions
2. **Run tests**: Ensure all tests pass
3. **Install on device**: Test the app functionality
4. **Create pull request**: Contribute improvements
5. **Create release**: Use GitHub Actions for distribution

---

## 📞 Support

If you encounter issues:
1. Check the troubleshooting section above
2. Review GitHub Actions logs for CI/CD issues
3. Open an issue in the repository
4. Check Android Studio build logs for detailed error messages

Happy building! 🚀 