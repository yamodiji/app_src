# Fast App Drawer

A powerful Android app drawer with floating widget, gesture controls, and smart search capabilities.

## 🚀 Features

- **🎯 Floating Widget**: Always-on-screen draggable search widget
- **🌊 Gesture Control**: Swipe-up from home screen to open app drawer
- **⚡ Smart Search**: Live filtering with advanced search algorithms
- **📱 Overlay Interface**: Full-screen transparent app drawer
- **⚙️ Comprehensive Settings**: Customizable appearance and behavior
- **🎨 Material Design 3**: Modern, beautiful user interface

## 🔧 GitHub Actions Workflows

This project uses **GitHub Actions for all building, testing, and deployment**. No local setup required!

### Available Workflows

#### 1. **Build and Test** (Automatic)
- **Triggers**: Push to `main`/`develop`, Pull Requests
- **What it does**: 
  - Runs unit tests
  - Performs Android lint checks
  - Builds debug APK
  - Builds & signs release APK (main branch only)
  - Creates GitHub releases automatically
- **Location**: `.github/workflows/build-and-test.yml`

#### 2. **Build APK (Manual)** 
- **Triggers**: Manual dispatch from GitHub Actions tab
- **What it does**: Quick APK building without tests
- **Options**: Choose debug or release build
- **Location**: `.github/workflows/build-apk.yml`

#### 3. **Test Only**
- **Triggers**: Manual dispatch or test file changes
- **What it does**: Runs only unit tests for quick feedback
- **Location**: `.github/workflows/test-only.yml`

## 📱 Getting the APK

### Method 1: GitHub Releases (Recommended)
1. Go to the [Releases](../../releases) page
2. Download the latest APK file
3. Install on your Android device

### Method 2: Build Manually
1. Go to [Actions](../../actions) tab
2. Click "Build APK (Manual)"
3. Click "Run workflow"
4. Choose debug or release
5. Download from Artifacts section

### Method 3: Automatic Builds
- Every push to `main` branch automatically creates a new release
- Check the [Actions](../../actions) tab for build status
- Releases appear in the [Releases](../../releases) section

## 🔍 Testing

### Running Tests
1. Go to [Actions](../../actions) tab
2. Click "Test Only" workflow
3. Click "Run workflow"
4. View test results in the workflow run

### Test Reports
- Unit test results are automatically published
- Test reports available in workflow artifacts
- Failed tests are highlighted in the workflow summary

## 🛠️ Development Workflow

### Making Changes
1. Create a new branch: `git checkout -b feature/your-feature`
2. Make your changes
3. Push to GitHub: `git push origin feature/your-feature`
4. Create a Pull Request
5. GitHub Actions will automatically test your changes
6. Merge to `main` when tests pass

### Viewing Build Status
- Check the [Actions](../../actions) tab for all workflow runs
- Green checkmark ✅ = Build successful
- Red X ❌ = Build failed (check logs for details)
- Yellow circle 🟡 = Build in progress

## 📋 Build Information

- **Target SDK**: Android 34 (API level 34)
- **Minimum SDK**: Android 24 (API level 24)
- **Architecture**: MVVM with Repository pattern
- **Database**: Room for local storage
- **UI**: Material Design 3 components
- **Language**: Kotlin 100%

## 🔐 Permissions Required

- `SYSTEM_ALERT_WINDOW`: For floating widget overlay
- `BIND_ACCESSIBILITY_SERVICE`: For gesture detection
- `PACKAGE_USAGE_STATS`: For app usage tracking
- `VIBRATE`: For haptic feedback
- `FOREGROUND_SERVICE`: For background services

## 📂 Project Structure

```
app/src/main/
├── java/com/appdrawer/fast/
│   ├── adapters/          # RecyclerView adapters
│   ├── database/          # Room database
│   ├── models/           # Data models
│   ├── overlay/          # Floating widget & overlay
│   ├── repository/       # Data repository
│   ├── utils/           # Utility classes
│   ├── viewmodels/      # MVVM ViewModels
│   └── MainActivity.kt   # Main activity
├── res/                 # Resources (layouts, drawables, etc.)
└── AndroidManifest.xml  # App configuration
```

## 🚦 Workflow Status

[![Build and Test](../../actions/workflows/build-and-test.yml/badge.svg)](../../actions/workflows/build-and-test.yml)
[![Test Only](../../actions/workflows/test-only.yml/badge.svg)](../../actions/workflows/test-only.yml)

## 💡 Usage Tips

1. **First Launch**: Grant overlay permission when prompted
2. **Gesture Setup**: Enable accessibility service for swipe gestures
3. **Widget Position**: Drag the floating widget to your preferred location
4. **Search**: Type to search, use space for multiple keywords
5. **App Options**: Long-press apps for favorite/hide options

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Push to GitHub (tests will run automatically)
5. Create a Pull Request
6. Wait for GitHub Actions to validate your changes
7. Merge when approved

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Note**: This project uses GitHub Actions for all development workflows. No local Android development environment is required for contributing! 