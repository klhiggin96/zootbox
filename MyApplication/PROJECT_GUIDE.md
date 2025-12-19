# ZootBox Vending Machine App - Project Guide

## Project Overview
**App Name**: ZootBox
**Package**: com.example.myapplication
**Platform**: Android (Kotlin)
**Min SDK**: 24
**Target SDK**: 34

This is an Android vending machine application with a gradient UI, product grid, video backgrounds, and custom animations.

---

## Project Structure

### Key Directories

```
MyApplication/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/myapplication/
│   │   │   ├── MainActivity.kt              # Main activity - app entry point
│   │   │   ├── ProductAdapter.kt            # RecyclerView adapter for product grid
│   │   │   ├── ZootBoxGradientView.kt       # Custom view for 3D glass text (not currently used)
│   │   │   └── FullScreenVideoView.kt       # Custom VideoView for background videos
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml        # Main screen layout
│   │   │   │   ├── zootbox_title.xml        # Title header layout
│   │   │   │   └── product_card.xml         # Individual product card layout
│   │   │   ├── drawable/
│   │   │   │   ├── title.png                # ZOOTBOX title image
│   │   │   │   ├── product_*.png            # Product images
│   │   │   │   ├── bg_*.xml                 # Background gradients/shapes
│   │   │   │   └── rounded_*.xml            # Rounded corner drawables
│   │   │   ├── font/
│   │   │   │   ├── archivo_black.ttf        # Main title font
│   │   │   │   ├── oswald.ttf               # Product names font
│   │   │   │   ├── space_grotesk.ttf        # Product details font
│   │   │   │   └── ...                      # Other fonts
│   │   │   ├── raw/
│   │   │   │   └── *.mp4                    # Background video files
│   │   │   └── values/
│   │   │       ├── colors.xml               # App color palette
│   │   │       ├── strings.xml              # Text strings
│   │   │       └── themes.xml               # App themes
│   │   └── AndroidManifest.xml              # App configuration & permissions
│   ├── build/
│   │   └── outputs/apk/debug/
│   │       └── app-debug.apk                # ⭐ Built APK file (for installation)
│   └── build.gradle.kts                     # App-level build configuration
├── gradle/                                  # Gradle wrapper files
└── build.gradle.kts                         # Project-level build configuration
```

---

## Common Edit Locations

### 1. Title / Branding
**File**: `app/src/main/res/layout/zootbox_title.xml`
- Replace `@drawable/title` with new image
- Adjust `android:layout_height` to change title size
- Modify `android:paddingTop` and `android:paddingBottom` for spacing

**Current Title Image**: `app/src/main/res/drawable/title.png`
- Dimensions: 1080×360px recommended
- Format: PNG with transparency

### 2. Product Grid Spacing
**File**: `app/src/main/res/layout/activity_main.xml`
- Line 36: `android:paddingTop="220dp"` - Space between title and products
- Line 34-35: `android:paddingStart/End="8dp"` - Side margins
- Line 37: `android:paddingBottom="24dp"` - Bottom spacing

### 3. Background Videos
**Files**: `app/src/main/res/raw/*.mp4`
- Add video files to `res/raw/` folder
- Reference in `MainActivity.kt` (setupBackgroundVideos method)
- Videos crossfade every 6 seconds

**Current Setup**: Two FullScreenVideoView layers for seamless crossfade

### 4. Product Cards
**Layout**: `app/src/main/res/layout/product_card.xml`
- Product image, name, price, description
- Rounded corners with gradient backgrounds
- Age verification badge

**Data**: `MainActivity.kt` - Line 60-150 (createProductData method)
```kotlin
Product(
    name = "Product Name",
    price = "$X.XX",
    description = "Description text",
    imageRes = R.drawable.product_image,
    ccCode = "CC XXX",
    requiresAgeVerification = true/false
)
```

### 5. Fonts
**Location**: `app/src/main/res/font/`
- `archivo_black.ttf` - Title font (bold, heavy weight)
- `oswald.ttf` - Product names
- `space_grotesk.ttf` - Product descriptions

**Usage**: Reference in XML with `@font/font_name` or in code with `ResourcesCompat.getFont()`

### 6. Colors
**File**: `app/src/main/res/values/colors.xml`
```xml
<color name="navy_dark">#0f1423</color>
<color name="lavender">#dcc2f5</color>
<!-- etc -->
```

### 7. Background Gradients
**Files**: `app/src/main/res/drawable/bg_*.xml`
- `bg_header_fade.xml` - Header fade gradient
- `bg_product_card.xml` - Product card background
- Edit `<gradient>` tags to change colors/angles

---

## Building & Shipping

### Development Build (Debug APK)

**Command**:
```bash
cd /c/dev/MyApplication
./gradlew assembleDebug
```

**Output Location**: `app/build/outputs/apk/debug/app-debug.apk`

**Install to Device**:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Production Build (Release APK)

**Command**:
```bash
./gradlew assembleRelease
```

**Output Location**: `app/build/outputs/apk/release/app-release.apk`

**Note**: Requires signing configuration in `app/build.gradle.kts`

### Check Connected Devices
```bash
adb devices
```

### Get Device Info
```bash
adb shell wm size        # Screen resolution
adb shell wm density     # Screen density
```

### View Logs
```bash
adb logcat | grep "MainActivity"
```

---

## APK Information

**Current Debug APK Name**: `app-debug.apk`
**Location**: `c:\dev\MyApplication\app\build\outputs\apk\debug\app-debug.apk`
**Size**: ~15-20 MB (with videos and images)

**Shippable APK**: Copy from output location to distribution folder
**Install Command**: `adb install -r app-debug.apk`

---

## Quick Reference Commands

### Build & Deploy (One Command)
```bash
cd /c/dev/MyApplication && ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Clean Build
```bash
./gradlew clean
./gradlew assembleDebug
```

### List All Tasks
```bash
./gradlew tasks
```

---

## Key Customization Points

### 1. Change App Name
**File**: `app/src/main/res/values/strings.xml`
```xml
<string name="app_name">ZootBox</string>
```

### 2. Change Package Name
**Files**: `app/build.gradle.kts` + refactor code package structure
```kotlin
namespace = "com.example.myapplication"
```

### 3. Add New Product
**File**: `MainActivity.kt` - Add to `createProductData()` method
```kotlin
Product(
    name = "New Product",
    price = "$XX.XX",
    description = "Description",
    imageRes = R.drawable.product_new,
    ccCode = "CC XXX",
    requiresAgeVerification = false
)
```

### 4. Change Grid Columns
**File**: `MainActivity.kt` - Line 50
```kotlin
layoutManager = GridLayoutManager(this, 2) // Change 2 to desired columns
```

### 5. Adjust Card Spacing
**File**: `MainActivity.kt` - Line 55
```kotlin
addItemDecoration(GridSpacingItemDecoration(2, 16, true)) // spanCount, spacing, includeEdge
```

---

## Video Background System

### How It Works
- Two `FullScreenVideoView` layers overlay each other
- Videos crossfade using alpha animation (6-second cycle)
- Seamless looping effect

### Add New Videos
1. Place `.mp4` files in `app/src/main/res/raw/`
2. Reference in `MainActivity.kt`:
```kotlin
private val videoResources = listOf(
    R.raw.video1,
    R.raw.video2,
    R.raw.new_video  // Add here
)
```

### Video Specs
- Format: MP4 (H.264)
- Resolution: 1080×1920 (portrait)
- Keep file size under 10MB for performance

---

## Troubleshooting

### App Won't Install
```bash
# Uninstall existing version
adb uninstall com.example.myapplication

# Reinstall
adb install app-debug.apk
```

### Build Fails
```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug
```

### No Devices Found
```bash
# Restart adb server
adb kill-server
adb start-server
adb devices
```

### Image Not Showing
- Verify image is in `app/src/main/res/drawable/`
- PNG/JPG formats supported
- No special characters in filename
- Reference as `@drawable/image_name` (no extension)

---

## File Naming Conventions

- **Layouts**: `activity_*.xml`, `*_card.xml`, `*_title.xml`
- **Drawables**: `bg_*.xml` (backgrounds), `rounded_*.xml` (shapes)
- **Images**: `product_*.png`, lowercase with underscores
- **Colors**: Descriptive names like `navy_dark`, `lavender`
- **Fonts**: Lowercase with underscores (e.g., `archivo_black.ttf`)

---

## Important Notes

1. **Title Container Height**: 300dp image + 20dp top + 40dp bottom = 360dp total
2. **Product Grid Padding**: 220dp from top to clear title area
3. **Screen Resolution**: 1080×1920 @ 280dpi (standard Android)
4. **Custom View**: `ZootBoxGradientView.kt` exists but not currently used (replaced with image)
5. **Video Crossfade**: 6000ms duration per cycle

---

## Git Workflow (if using version control)

```bash
# Current branch
git status

# Add changes
git add .

# Commit
git commit -m "Description of changes"

# Push to remote
git push origin master
```

**GitHub Token**: Use personal access token for HTTPS authentication

---

## Resources

- Android Documentation: https://developer.android.com
- Kotlin Reference: https://kotlinlang.org/docs/home.html
- Material Design: https://material.io/design

---

*Last Updated: 2025-12-05*
*App Version: 1.0*
*Project: ZootBox Vending Machine*
