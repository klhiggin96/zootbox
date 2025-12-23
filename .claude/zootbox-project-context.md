# ZootBox Vending Machine App - Project Context

## Project Overview
**App Name**: ZootBox
**Package**: com.example.myapplication
**Platform**: Android (Kotlin)
**Min SDK**: 24 | Target SDK: 34
**Location**: `c:\dev\MyApplication\`
adb install -r c:/dev/MyApplication/app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.example.myapplication/.MainActivity
Android vending machine application with gradient UI, product grid, video backgrounds, and custom animations.

---

## 🏗️ Project Structure

### Critical Files & Locations

**Main Source Files**:
- `app/src/main/java/com/example/myapplication/MainActivity.kt` - Main activity, app entry point
- `app/src/main/java/com/example/myapplication/ProductAdapter.kt` - Product grid adapter
- `app/src/main/java/com/example/myapplication/FullScreenVideoView.kt` - Custom video view for backgrounds
- `app/src/main/java/com/example/myapplication/ZootBoxGradientView.kt` - Custom 3D glass text view (NOT currently used)

**Layout Files**:
- `app/src/main/res/layout/activity_main.xml` - Main screen layout with video backgrounds and product grid
- `app/src/main/res/layout/zootbox_title.xml` - **CURRENTLY USING IMAGE** (title.png, not custom view)
- `app/src/main/res/layout/product_card.xml` - Individual product card layout

**Assets**:
- `app/src/main/res/drawable/title.png` - ZOOTBOX title image (1080×360px)
- `app/src/main/res/drawable/product_*.png` - Product images
- `app/src/main/res/font/archivo_black.ttf` - Title font
- `app/src/main/res/font/oswald.ttf` - Product names
- `app/src/main/res/font/space_grotesk.ttf` - Product descriptions
- `app/src/main/res/raw/*.mp4` - Background video files

**Build Output**:
- `app/build/outputs/apk/debug/app-debug.apk` - **Built APK location**

---

## 📐 Current Layout Configuration

### Title Section (zootbox_title.xml)
```xml
- ImageView displaying @drawable/title
- Height: 300dp
- Container padding: 20dp top, 40dp bottom
- ScaleType: centerInside
- Total space: ~360dp
```

### Product Grid (activity_main.xml)
```xml
- RecyclerView with 2 columns
- paddingTop: 220dp (clears title area)
- paddingStart/End: 8dp
- paddingBottom: 24dp
```

### Background System
```xml
- Two FullScreenVideoView layers
- Crossfade animation every 6 seconds
- Seamless video loop transitions
```

---

## 🎨 Design Specs

### Screen Dimensions
- Device: 1080×1920 @ 280dpi
- Title container: 1080×360px
- Title image: 1080×360px (recommended)
- Safe text area: 1000px wide (40px padding each side)

### Color Palette
- Navy Dark: `#0f1423`
- Lavender: `#dcc2f5`
- Cerulean: `#1f80bb`
- Turquoise: `#38a1b5`
- Fuchsia: `#b9448c`

### Typography
- Title: Archivo Black (bold, heavy)
- Product Names: Oswald
- Product Details: Space Grotesk

---

## 🔨 Build & Deploy Commands

### Standard Build & Install
```bash
cd /c/dev/MyApplication && ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Clean Build
```bash
cd /c/dev/MyApplication && ./gradlew clean && ./gradlew assembleDebug
```

### Device Commands
```bash
adb devices                  # List connected devices
adb shell wm size           # Get screen resolution
adb shell wm density        # Get screen density
adb logcat                  # View logs
```

---

## ⚙️ Common Modifications

### 1. Change Title Image
**File**: `app/src/main/res/drawable/title.png`
- Replace with new 1080×360px PNG
- Layout in: `app/src/main/res/layout/zootbox_title.xml`
- Adjust `android:layout_height` if needed

### 2. Adjust Title Spacing
**File**: `app/src/main/res/layout/zootbox_title.xml`
- `android:paddingTop` - Space from screen top
- `android:paddingBottom` - Space before product cards

**File**: `app/src/main/res/layout/activity_main.xml`
- Line 36: `android:paddingTop="220dp"` - Product grid start position

### 3. Add/Edit Products
**File**: `app/src/main/java/com/example/myapplication/MainActivity.kt`
- Method: `createProductData()` (around line 60-150)
```kotlin
Product(
    name = "Product Name",
    price = "$X.XX",
    description = "Description",
    imageRes = R.drawable.product_image,
    ccCode = "CC XXX",
    requiresAgeVerification = true/false
)
```

### 4. Change Grid Layout
**File**: `MainActivity.kt`
```kotlin
// Line 50: Change column count
layoutManager = GridLayoutManager(this, 2) // Change 2 to desired columns

// Line 55: Adjust card spacing
addItemDecoration(GridSpacingItemDecoration(2, 16, true))
```

### 5. Modify Background Videos
**Location**: `app/src/main/res/raw/*.mp4`
**Reference**: `MainActivity.kt` - `videoResources` list
- Format: MP4 (H.264), 1080×1920, under 10MB

---

## 🎯 Key Implementation Details

### Title Display
- **Currently**: Uses ImageView with title.png
- **Alternative**: Custom ZootBoxGradientView.kt exists (3D glass text effect) but not in use
- To switch back to custom view: Replace ImageView in zootbox_title.xml with:
  ```xml
  <com.example.myapplication.ZootBoxGradientView ... />
  ```

### Video Crossfade System
- Two video layers (`bg_video_view_1`, `bg_video_view_2`)
- Alpha crossfade animation (6000ms duration)
- Implemented in `MainActivity.kt` - `setupBackgroundVideos()` method

### Product Card Features
- Rounded corners with gradient background
- Product image with overlay
- Name, price, description
- Age verification badge (conditional)
- CC code display

---

## 📝 File Naming Conventions

- **Layouts**: `activity_*.xml`, `*_card.xml`, `*_title.xml`
- **Drawables**: `bg_*.xml` (backgrounds), `rounded_*.xml` (shapes), `product_*.png`
- **Fonts**: Lowercase with underscores (e.g., `archivo_black.ttf`)
- **Colors**: Descriptive (e.g., `navy_dark`, `lavender`)
- **No spaces or special characters in resource names**

---

## 🐛 Known Issues & Notes

1. **ZootBoxGradientView.kt** - 3D glass text implementation exists but replaced with image for flexibility
2. **Title positioning** - Currently uses 20dp top, 40dp bottom padding for optimal spacing
3. **Product grid offset** - 220dp top padding to avoid title overlap
4. **Video performance** - Keep videos under 10MB for smooth playback
5. **Device specs** - Optimized for 1080×1920 @ 280dpi

---

## 🚀 Deployment

### APK Location
```
c:\dev\MyApplication\app\build\outputs\apk\debug\app-debug.apk
```

### Installation
```bash
adb install -r app-debug.apk
```

### Troubleshooting
```bash
# App won't install
adb uninstall com.example.myapplication
adb install app-debug.apk

# Build fails
./gradlew clean
./gradlew assembleDebug

# No devices
adb kill-server
adb start-server
adb devices
```

---

## 📦 Dependencies & Configuration

**Build Files**:
- `app/build.gradle.kts` - App-level dependencies
- `build.gradle.kts` - Project-level configuration
- `gradle/` - Gradle wrapper

**Key Dependencies**:
- AndroidX Core, AppCompat, ConstraintLayout
- Material Components
- RecyclerView
- Kotlin stdlib

**Manifest**:
- `app/src/main/AndroidManifest.xml`
- Package: `com.example.myapplication`
- Permissions: Standard Android permissions

---

## 💡 Quick Tips

1. **Always read files before editing** - Use Read tool first
2. **Build incrementally** - Test after each change
3. **Check device connection** - `adb devices` before installing
4. **Use proper paths** - `/c/dev/...` for Git Bash on Windows
5. **Image format** - PNG with transparency for best results
6. **Video format** - MP4 H.264 for compatibility

---

## 🔄 Git Status (as of session start)

**Current Branch**: master
**Modified Files**:
- `.gitignore`
- `MyApplication/.idea/vcs.xml`
- `MyApplication/app/src/main/java/com/example/myapplication/MainActivity.kt`
- `MyApplication/app/src/main/java/com/example/myapplication/ZootBoxGradientView.kt`
- `MyApplication/app/src/main/res/layout/activity_main.xml`
- `MyApplication/app/src/main/res/layout/zootbox_title.xml`
- `MyApplication/shippable_apk/VendingClient.apk`

**Untracked**:
- `.claude/`
- `MyApplication/app/src/main/java/com/example/myapplication/FullScreenVideoView.kt`
- `MyApplication/app/src/main/res/drawable/title.png`
- `MyApplication/app/src/main/res/raw/*.mp4`

---

*This context file is maintained for consistent Claude memory across sessions.*
*Last Updated: 2025-12-05*
*ZootBox Vending Machine App v1.0*
