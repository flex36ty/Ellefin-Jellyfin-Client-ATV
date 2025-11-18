# Elefin - Jellyfin Client for Android TV

**Elefin** is a modern, feature-rich Android TV and mobile client for Jellyfin media servers, built with Jetpack Compose and Android TV Material3 design principles. It provides a beautiful, intuitive interface for browsing and streaming your media library on Android TV devices and mobile phones.

## 📱 About

Elefin connects to your self-hosted Jellyfin media server, allowing you to browse movies, TV shows, episodes, and other media content with a sleek, TV-optimized interface. The app is built with Jetpack Compose for TV, following Android TV Material3 design guidelines for the best possible viewing experience.

### Key Highlights

- **Cross-Platform Support**: Works on both Android TV and mobile devices
- **Modern UI**: Built with Jetpack Compose and Android TV Material3
- **Immersive Experience**: Dynamic backgrounds and immersive previews
- **Fast & Cached**: Optimized image loading with memory and disk caching
- **Customizable**: Extensive settings to tailor your viewing experience

## ✨ Features

### 🎬 Media Browsing

- **Home Screen**
  - Featured movies/shows carousel with synopsis
  - Continue Watching row with progress tracking
  - Recently Added Movies/Shows/Episodes rows per library
  - Recently Released Movies rows
  - Library content rows organized by library

- **Media Types**
  - Movies with poster artwork
  - TV Shows with season/episode browsing
  - Episodes with backdrop photos
  - Horizontal and vertical card layouts

- **Library Management**
  - Browse multiple Jellyfin libraries
  - Library sub-categories with grid view
  - Movie/show titles displayed below cards
  - Horizontal backdrop cards for episodes

### 🎥 Video Player

- Full-featured video playback
- Auto-play next episode countdown (7-second timer)
- Resume playback from last position
- Episode navigation with progress tracking
- Playback controls optimized for TV remote

### 🔍 Search & Discovery

- Global search across all media
- Search history with clear option
- Browse by categories and genres
- Recently added content discovery
- Favorites collection

### 🎨 Immersive Experience

- **Immersive List Preview**
  - Optional immersive preview with backdrop photos
  - Dynamic color extraction from media backdrops
  - Cinematic scrim effects following Android TV guidelines
  - Smooth transitions and animations

- **Dynamic Background Colors**
  - Per-pixel color extraction from focused media
  - Automatic background color matching
  - Smooth fade animations (800ms transitions)
  - Darkened backgrounds for better contrast

### ⚙️ Settings & Customization

- **Featured Carousel**: Toggle featured carousel on/off
- **Immersive List Preview**: Enable/disable immersive preview experience
- **Dynamic Background Color**: Enable dynamic color extraction
- **Black Background**: Toggle solid black background
- **Profile Management**: View and manage user account
- **Search History**: View and clear search history
- **Language Selection**: Choose interface language
- **Subtitle Settings**: Configure subtitle preferences

### 📱 Platform Support

- **Android TV**: Full D-pad navigation and remote control support
- **Mobile**: Touch-friendly interface with on-screen keyboard
- **Hybrid Support**: Works seamlessly on both platforms

### 🔐 Authentication

- Simple login with server IP, username, and password
- Automatic logout on 401 Unauthorized errors
- Persistent login state
- Server URL validation

### 🎯 User Interface

- **Navigation**
  - Top navigation bar with Home, Libraries, Movies, Shows, Favourites, and Search
  - Profile/settings section with sub-screens
  - Smooth transitions between screens

- **Material Design**
  - Dark theme optimized for TV viewing
  - Android TV Material3 components
  - Consistent typography using default Android TV fonts (Roboto)
  - Smooth animations and focus indicators

- **Components Used**
  - TabRow for top navigation
  - Carousel for featured content
  - ImmersiveList for preview rows
  - TvLazyRow and TvLazyColumn for efficient scrolling
  - TvVerticalGrid for library browsing
  - Buttons with scale, glow, and color animations
  - StandardCardContainer for media cards
  - Dialogs and switches for settings

### 🚀 Performance

- **Image Caching**
  - Memory cache (25% of available memory)
  - Disk cache (100 MB)
  - Cache policies for optimal performance
  - Preloading of carousel images

- **Data Loading**
  - Eager loading with cached data
  - Progressive data updates
  - Error handling with fallback to cached content

- **Optimizations**
  - Baseline profiles for faster app startup
  - Efficient lazy loading
  - Image format optimization

## 🏗️ Technical Details

### Architecture

- **MVVM Pattern**: ViewModels with StateFlow for reactive state management
- **Dependency Injection**: Hilt for dependency management
- **Navigation**: Jetpack Compose Navigation
- **Image Loading**: Coil with caching
- **Networking**: Retrofit for API calls
- **Data Persistence**: SharedPreferences for user preferences

### Key Technologies

- **Jetpack Compose for TV**: Modern declarative UI framework
- **Android TV Material3**: TV-optimized Material Design components
- **Coroutines & Flow**: Asynchronous data handling
- **Coil**: Image loading and caching
- **Retrofit**: REST API client
- **Palette**: Dynamic color extraction from images
- **Hilt**: Dependency injection

### Code Structure

```
jetstream/
├── data/
│   ├── entities/          # Data models
│   ├── repositories/      # Data layer
│   └── util/              # Utilities and converters
├── presentation/
│   ├── screens/           # Screen composables
│   │   ├── dashboard/     # Main navigation
│   │   ├── home/          # Home screen
│   │   ├── movies/        # Movie details
│   │   ├── shows/         # TV shows
│   │   ├── videoPlayer/   # Video playback
│   │   ├── categories/    # Library browsing
│   │   ├── library/       # Library details
│   │   ├── login/         # Authentication
│   │   ├── profile/       # Settings
│   │   └── search/        # Search functionality
│   ├── common/            # Shared components
│   └── theme/             # Theming and typography
└── di/                    # Dependency injection modules
```

## 💻 Requirements

- **Android Studio**: Latest version recommended
- **Min SDK**: Check `build.gradle.kts` for minimum requirements
- **Jellyfin Server**: Self-hosted Jellyfin media server
- **Network Access**: Ability to connect to Jellyfin server

## 🚀 Getting Started

### Building the Project

1. Clone this repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build and run on an Android TV device or emulator

### Configuration

1. Launch the app
2. Enter your Jellyfin server IP address (e.g., `192.168.1.100:8096`)
3. Enter your username and password
4. Tap/click Login to connect

### First Time Setup

- The app will automatically fetch your libraries and media
- Featured content will appear in the carousel (if enabled)
- Continue Watching will show partially watched items
- Browse libraries to access your media

## 🎨 UI Components Showcased

This app demonstrates the following Android TV Material3 components:

- **TabRow**: Top navigation bar
- **Carousel**: Featured content carousel with auto-advance
- **ImmersiveList**: Immersive preview rows with backdrop photos
- **TvLazyRow**: Horizontal scrolling rows
- **TvLazyColumn**: Vertical scrolling lists
- **TvVerticalGrid**: Grid layout for library browsing
- **Button**: Interactive buttons with animations
- **StandardCardContainer**: Media cards with focus states
- **Dialog**: Settings and confirmation dialogs
- **Switch**: Toggle switches for settings
- **ListItem**: Settings list items
- **Text**: Typography with TV-optimized fonts
- **Surface**: Interactive surfaces with borders

## 📸 Screenshots
![Home Screen](https://raw.githubusercontent.com/flex36ty/Ellefin-Jellyfin-Client-ATV/master/screenshots/homescreen.png)
![Home Screen](https://raw.githubusercontent.com/flex36ty/Ellefin-Jellyfin-Client-ATV/master/screenshots/screenshot2.png)
![List of screenshots from the Elefin application](./Screenshots.png)

## 🔧 Customization Options

Users can customize their experience through Settings:

1. **Show Featured Carousel**: Enable/disable the featured content carousel
2. **Immersive List Preview**: Enable immersive preview with backdrop photos
3. **Dynamic Background Color**: Enable automatic color extraction from media
4. **Black Background**: Toggle solid black background when dynamic colors are disabled

## 🎯 Design Principles

- **TV-First Design**: Optimized for 10-foot viewing experience
- **Focus Navigation**: Full D-pad and remote control support
- **Accessibility**: High contrast and clear focus indicators
- **Performance**: Efficient scrolling and image loading
- **Material Design**: Following Android TV Material3 guidelines

## 📊 Performance

### Benchmarks

The `benchmark` module contains sample tests written using [`Macrobenchmark`](https://developer.android.com/studio/profile/macrobenchmark) library. It also contains tests to generate baseline profiles for the Elefin app.

### Baseline Profiles

The baseline profile for this app is located at [`jetstream/src/main/baseline-prof.txt`](jetstream/src/main/baseline-prof.txt). It contains rules that enable AOT compilation of the critical user path taken during app launch. To learn more about baseline profiles, read [here](https://developer.android.com/studio/profile/baselineprofiles).

For more details on how to generate & apply baseline profiles, check [this document](baseline-profiles.md).

## 🤝 Contributing

This is a sample application demonstrating Jetpack Compose for TV. Contributions and improvements are welcome!

## 📄 License

```
Copyright 2023 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 🔗 Related Links

- [Jellyfin Project](https://jellyfin.org/)
- [Jetpack Compose for TV](https://developer.android.com/jetpack/androidx/releases/tv)
- [Android TV Material3](https://developer.android.com/reference/kotlin/androidx/tv/material3/package-summary)
- [Android TV Design Guidelines](https://developer.android.com/design/ui/tv)

## 📝 Notes

- This app requires a self-hosted Jellyfin server
- Internet connection is required for initial server connection
- Media streaming requires network access to your Jellyfin server
- All settings are stored locally on the device
