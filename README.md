# Fast App Drawer

A lightning-fast, customizable app drawer for Android that lets you search and launch apps quickly and efficiently.

## Features

🔍 **Smart Search Engine**
- Fuzzy matching - find apps even with typos
- T9 search - use number keypad like old phones
- Package name matching
- Custom aliases for apps
- Multiple search algorithms with intelligent scoring

⭐ **App Management**
- Mark apps as favorites for quick access
- Hide unwanted apps from the drawer
- Usage tracking and smart suggestions
- Long-press for app options menu

🎨 **Customization**
- Material Design 3 with modern UI
- Light, dark, and auto themes
- Customizable search behavior
- Clean, ad-free interface

🚀 **Performance**
- Fast app launching with usage tracking
- Efficient database storage with Room
- Smooth animations and transitions
- Minimal resource usage

📱 **Integration**
- Home screen widget for quick access
- Assistant integration (digital assistant)
- No unnecessary permissions
- Completely free and open-source

## Key Components

### Search Engine (`SearchEngine.kt`)
Advanced search algorithm supporting:
- **Exact matches** (highest priority)
- **Alias matching** (custom shortcuts)
- **Start-of-name matching**
- **Contains matching**
- **Package name search**
- **T9 keypad search** (2=ABC, 3=DEF, etc.)
- **Fuzzy matching** with Levenshtein distance

### Database (`AppDatabase.kt`)
Room database for persistent storage:
- App metadata and preferences
- Favorites and hidden apps
- Usage statistics and last used timestamps
- Custom aliases and icon paths

### Repository (`AppRepository.kt`)
Data layer handling:
- Installed app discovery
- App launching and usage tracking
- Favorites and hidden app management
- Database synchronization

## Installation

1. Clone this repository
2. Open in Android Studio
3. Build and install on your Android device
4. Grant necessary permissions when prompted

## Permissions

- `QUERY_ALL_PACKAGES` - To discover all installed apps
- `DEVICE_POWER` - For assistant integration (optional)

## Usage

### Basic Search
- Type any part of an app name
- Use numbers for T9 search (e.g., 2662 for "AMOC")
- Search by package name (e.g., "com.android")

### App Management
- **Tap** - Launch app
- **Long press** - Show options menu
  - Add/remove favorites
  - Hide/show app
  - Set custom alias
  - View app info

### Aliases
Create custom shortcuts for apps:
- Set "fb" as alias for "Facebook"
- Set "gm" as alias for "Gmail"
- Use any text that's easier to remember

### Widget
Add the search widget to your home screen for instant access to the app drawer.

## Architecture

The app follows MVVM architecture with:
- **View**: Activities and Fragments
- **ViewModel**: Business logic and data binding
- **Repository**: Data source abstraction
- **Database**: Room for local storage
- **Utils**: Search engine and helper classes

## Customization

Access settings through the menu to customize:
- Search behavior (fuzzy, T9, package names)
- Display options (themes, favorite icons)
- App behavior (auto-close, recent apps)

## Performance Tips

- The app loads and caches installed apps on first launch
- Search results are scored and sorted by relevance and usage
- Database operations are optimized for fast queries
- UI updates use DiffUtil for efficient RecyclerView updates

## Contributing

This app is built to be similar to popular app launcher apps like "App Search: Launch apps fast" while adding modern Android development practices and additional features.

Feel free to:
- Report bugs
- Suggest features
- Submit pull requests
- Improve documentation

## License

This project is open-source and available under the MIT License.

## Technical Details

- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)
- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **Database**: Room
- **UI**: Material Design 3
- **Build System**: Gradle

---

*Created as a modern, efficient alternative to traditional app drawers with advanced search capabilities and customization options.* 