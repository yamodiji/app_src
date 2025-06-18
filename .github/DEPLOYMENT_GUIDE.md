# GitHub Actions Deployment Guide

This guide explains how to set up and use the GitHub Actions CI/CD pipeline for the Fast App Drawer Android application.

## 🚀 Overview

The repository includes comprehensive GitHub Actions workflows that automatically:
- **Build APKs** on every push and pull request
- **Run tests** and quality checks
- **Generate releases** when you create GitHub releases
- **Perform security scans** and code analysis

## 📋 Prerequisites

Before pushing to your [GitHub repository](https://github.com/yamodiji/app_src), ensure you have:

1. **Android Development Environment**
   - Android Studio installed locally
   - Android SDK with API level 34
   - JDK 17 or higher

2. **Repository Secrets** (for signed releases)
   - `SIGNING_KEY_ALIAS` - Your keystore alias
   - `SIGNING_KEY_PASSWORD` - Your key password
   - `SIGNING_STORE_PASSWORD` - Your keystore password

## 🏗️ Workflow Structure

### 1. **Main Build Workflow** (`.github/workflows/android-build.yml`)

Triggers on:
- Push to `main` or `develop` branches
- Pull requests to `main`
- GitHub releases

**Jobs:**
- **Test**: Runs unit tests with JUnit reporting
- **Lint**: Performs Android lint checks
- **Build**: Creates debug APKs (and release APKs for releases)
- **Instrumented Tests**: Runs UI tests on Android emulator (PR only)
- **Release**: Uploads APK to GitHub releases

### 2. **Code Quality Workflow** (`.github/workflows/code-quality.yml`)

Triggers on:
- Push to `main` or `develop`
- Pull requests to `main`

**Jobs:**
- **Kotlin Lint**: Code style checking with ktlint
- **Detekt**: Static code analysis
- **Dependency Check**: Vulnerability scanning
- **CodeQL**: Security analysis

## 🔧 Setup Instructions

### Step 1: Push to GitHub

1. Create a new repository or use your existing one at [github.com/yamodiji/app_src](https://github.com/yamodiji/app_src)
2. Push all the project files:

```bash
git init
git add .
git commit -m "Initial commit: Fast App Drawer with GitHub Actions"
git branch -M main
git remote add origin https://github.com/yamodiji/app_src.git
git push -u origin main
```

### Step 2: Configure Repository Secrets (Optional - for signed releases)

Go to your repository → Settings → Secrets and variables → Actions

Add these secrets if you want signed release builds:
- `SIGNING_KEY_ALIAS`: Your keystore alias name
- `SIGNING_KEY_PASSWORD`: Password for your signing key
- `SIGNING_STORE_PASSWORD`: Password for your keystore file

### Step 3: Enable GitHub Actions

1. Go to your repository → Actions tab
2. GitHub Actions should be automatically enabled
3. The workflows will run automatically on the next push

## 📦 Build Process

### Automatic Builds

Every time you:
- **Push to main/develop**: Builds debug APK, runs tests and quality checks
- **Create pull request**: Runs full test suite including UI tests
- **Create GitHub release**: Builds signed release APK and attaches to release

### Manual Builds

You can also trigger builds manually:
1. Go to Actions tab in your repository
2. Select the workflow you want to run
3. Click "Run workflow"

## 📱 Generated Artifacts

### Debug Builds
- **APK Location**: Available as GitHub Actions artifacts
- **Download**: Go to Actions → Select workflow run → Download artifacts

### Release Builds
- **APK Location**: Attached to GitHub releases
- **Download**: Go to Releases → Select release → Download APK

## 🔍 Quality Checks

The CI pipeline includes:

### Tests
- **Unit Tests**: Kotlin/Java tests with JUnit
- **Instrumented Tests**: UI tests on Android emulator
- **Test Reports**: Viewable in GitHub Actions

### Code Quality
- **Android Lint**: Built-in Android static analysis
- **Ktlint**: Kotlin code style enforcement
- **Detekt**: Advanced static analysis for Kotlin

### Security
- **Dependency Check**: Scans for vulnerable dependencies
- **CodeQL**: GitHub's semantic code analysis
- **SARIF Reports**: Security findings in GitHub Security tab

## 🚀 Release Process

### Creating a Release

1. **Tag your code**:
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

2. **Create GitHub Release**:
   - Go to repository → Releases → "Create a new release"
   - Select your tag
   - Fill in release notes
   - Publish release

3. **Automatic Build**:
   - GitHub Actions will automatically build a signed APK
   - APK will be attached to the release
   - Users can download directly from GitHub

### Release APK Features
- **Optimized**: ProGuard/R8 optimized and minified
- **Signed**: Ready for distribution
- **Small Size**: Optimized for download and installation

## 🛠️ Customization

### Modifying Build Configuration

Edit `app/build.gradle` to:
- Change version numbers
- Modify build variants
- Add signing configurations
- Update dependencies

### Customizing Workflows

Edit workflow files in `.github/workflows/` to:
- Change trigger conditions
- Add new quality checks
- Modify build steps
- Add deployment targets

### Adding Signing Configuration

For production releases, add to `app/build.gradle`:

```gradle
android {
    signingConfigs {
        release {
            keyAlias System.getenv("SIGNING_KEY_ALIAS")
            keyPassword System.getenv("SIGNING_KEY_PASSWORD")
            storePassword System.getenv("SIGNING_STORE_PASSWORD")
            storeFile file("../keystore.jks")
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

## 📊 Monitoring Builds

### GitHub Actions Dashboard
- **View all runs**: Repository → Actions
- **Check status**: Green ✅ = success, Red ❌ = failure
- **View logs**: Click on any workflow run
- **Download artifacts**: Available after successful builds

### Notifications
- **Email**: GitHub sends emails on workflow failures
- **Status checks**: PRs show build status before merging
- **Badges**: Add build status badges to your README

## 🔧 Troubleshooting

### Common Issues

1. **Build Fails**:
   - Check Android SDK version compatibility
   - Verify Gradle wrapper permissions
   - Review error logs in Actions tab

2. **Tests Fail**:
   - Check test code for Android API compatibility
   - Verify emulator configuration for UI tests
   - Review test reports in artifacts

3. **Signing Issues**:
   - Verify repository secrets are correctly set
   - Check keystore file path and permissions
   - Validate signing configuration

### Getting Help

1. **Check Logs**: Always check the full workflow logs
2. **GitHub Docs**: [GitHub Actions for Android](https://docs.github.com/en/actions)
3. **Android Docs**: [Build and Test Android Apps](https://developer.android.com/studio/test)

---

## 🎯 Next Steps

1. **Push your code** to the repository
2. **Watch the build** in GitHub Actions
3. **Create your first release** to generate a production APK
4. **Share your app** with users via GitHub releases

Your Fast App Drawer is now ready for continuous integration and deployment! 🎉 