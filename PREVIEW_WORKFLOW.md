# Android Preview Workflow Guide

This guide explains how to preview Android code from Figma MCP on an emulator before shipping your app.

## Prerequisites

Before you begin, ensure you have:

1. **Android Studio** installed with:
   - Android SDK
   - Android Virtual Device (AVD) component
   - At least one AVD created

2. **Android SDK tools** accessible:
   - `adb` (Android Debug Bridge)
   - `emulator` command

3. **Project structure**:
   - This Android project in `MyApplication/`
   - Gradle wrapper (`gradlew` or `gradlew.bat`)

## Quick Start

### Step 1: Verify Your Setup

Run the verification script to check if everything is configured correctly:

**Windows:**
```powershell
.\verify-android-setup.ps1
```

**Mac/Linux:**
```bash
./verify-android-setup.sh
```

This script will:
- Detect your Android SDK path
- Verify `adb` and `emulator` tools are available
- List your available AVDs
- Check if an emulator is currently running
- Verify Gradle wrapper exists

**If you see errors:**
- Install Android Studio if not already installed
- Create an AVD: Android Studio → Tools → Device Manager → Create Device
- Add SDK tools to PATH (see Troubleshooting section)

### Step 2: Start an Emulator

Start an Android emulator where you'll preview your app:

**Windows:**
```cmd
.\start-emulator.bat
```

**Mac/Linux:**
```bash
./start-emulator.sh
```

Alternatively, you can start the emulator from Android Studio:
- Open Android Studio
- Go to: **Tools → Device Manager**
- Click the **Play** button next to your AVD

**Wait for the emulator to boot completely.** You'll know it's ready when:
- The emulator window shows the Android home screen
- Running `adb devices` shows the device status as "device"

### Step 3: Receive Code from Figma MCP

When you receive Android code from Figma MCP:

1. **Identify the file type:**
   - **Kotlin files** (`.kt`) → Go in `app/src/main/java/com/example/myapplication/`
   - **Layout XML files** (`.xml`) → Go in `app/src/main/res/layout/`
   - **Drawable resources** → Go in `app/src/main/res/drawable/`
   - **Values (colors, strings, etc.)** → Go in `app/src/main/res/values/`

2. **Place the code in the correct location:**
   - If it's a new file, create it in the appropriate directory
   - If it's an existing file, replace or update as needed
   - Ensure package names match if modifying Kotlin files

3. **Check for dependencies:**
   - If the Figma code references libraries not in your `build.gradle.kts`, add them
   - Sync your Gradle files if needed

### Step 4: Build and Install to Emulator

Build and install your app to the running emulator:

**Windows:**
```cmd
.\build-and-preview.bat
```

**Mac/Linux:**
```bash
./build-and-preview.sh
```

This script will:
1. Build the debug APK
2. Install it to the running emulator
3. Launch the app automatically (if possible)

**The app should now appear on your emulator!**

### Step 5: Preview and Iterate

1. **Test the UI** on the emulator
2. **Make adjustments** if needed:
   - Edit code files in Cursor
   - Re-run `build-and-preview.bat` or `build-and-preview.sh`
3. **Repeat** until satisfied

### Step 6: Ship Your Changes

Once you're happy with the preview:
- Commit your changes to version control
- Build release APK if needed: `gradlew assembleRelease`
- Deploy to your target device (vending machine tablet, etc.)

## Detailed Workflow

### Manual Build Commands

If you prefer to run Gradle commands manually:

**Build debug APK:**
```bash
# Windows
gradlew.bat assembleDebug

# Mac/Linux
./gradlew assembleDebug
```

**Install to emulator:**
```bash
# Windows
gradlew.bat installDebug

# Mac/Linux
./gradlew installDebug
```

**Check connected devices:**
```bash
adb devices
```

**Launch app manually:**
```bash
adb shell monkey -p com.example.myapplication -c android.intent.category.LAUNCHER 1
```

## Troubleshooting

### Problem: "adb not found" or "emulator not found"

**Solution:** Add Android SDK tools to your system PATH:

**Windows:**
1. Open System Properties → Environment Variables
2. Edit the "Path" variable
3. Add these paths (adjust SDK path as needed):
   - `C:\Users\YourName\AppData\Local\Android\Sdk\platform-tools`
   - `C:\Users\YourName\AppData\Local\Android\Sdk\emulator`

**Mac/Linux:**
Add to your `~/.bashrc` or `~/.zshrc`:
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk  # Mac
# OR
export ANDROID_HOME=$HOME/Android/Sdk          # Linux

export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/emulator
```

Then reload: `source ~/.bashrc` or `source ~/.zshrc`

### Problem: "No AVDs found"

**Solution:** Create an AVD:
1. Open Android Studio
2. Tools → Device Manager
3. Click "Create Device"
4. Choose a device (e.g., Pixel 6)
5. Download a system image if needed (e.g., Android 13/14)
6. Complete the wizard

### Problem: "No emulator currently running"

**Solution:** Start an emulator:
- Run `.\start-emulator.bat` (Windows) or `./start-emulator.sh` (Mac/Linux)
- Or start from Android Studio: Tools → Device Manager → Click Play button

### Problem: Build fails with dependency errors

**Solution:**
1. Check if required libraries are in `app/build.gradle.kts`
2. Sync Gradle: File → Sync Project with Gradle Files (in Android Studio)
3. Or run: `gradlew build --refresh-dependencies`

### Problem: App doesn't appear on emulator after install

**Solution:**
1. Check if installation succeeded: Look for "BUILD SUCCESSFUL" message
2. Check if app is installed: `adb shell pm list packages | grep myapplication`
3. Launch manually: Look for app icon in emulator's app drawer
4. Or launch via: `adb shell monkey -p com.example.myapplication -c android.intent.category.LAUNCHER 1`

### Problem: Emulator is slow or unresponsive

**Solution:**
1. **Enable hardware acceleration** (if not already):
   - Windows: Enable Hyper-V or Intel HAXM
   - Mac: Should be automatic
   - Linux: Install KVM

2. **Reduce emulator resources**:
   - Use a smaller system image
   - Reduce RAM allocation in AVD settings

3. **Use a physical device** for faster testing (connect via USB and enable USB debugging)

## Tips

1. **Keep emulator running:** Don't close it between builds - it's faster to reuse
2. **Use multiple emulators:** Test on different screen sizes and Android versions
3. **Hot reload limitations:** Gradle builds replace the entire app - not instant like Flutter/React Native
4. **Check logs:** Use `adb logcat` to see app logs and debug issues
5. **Clear app data:** If app behaves strangely, clear data: `adb shell pm clear com.example.myapplication`

## Integration with Figma MCP

When working with Figma MCP:

1. **Get design context:** Use Figma MCP to extract design details
2. **Get code:** Request Android code for specific components/screens
3. **Review code:** Check the generated code structure
4. **Integrate:** Place code in appropriate project directories
5. **Preview:** Use this workflow to preview on emulator
6. **Iterate:** Make adjustments based on preview and repeat

## Scripts Reference

| Script | Purpose | When to Use |
|--------|---------|-------------|
| `verify-android-setup.ps1` / `.sh` | Verify environment | First time setup, troubleshooting |
| `start-emulator.bat` / `.sh` | Start emulator | When emulator is not running |
| `build-and-preview.bat` / `.sh` | Build & install | After making code changes |

## Next Steps

- Set up continuous integration if needed
- Configure release signing for production builds
- Set up automated testing
- Document your specific app deployment process

---

**Need help?** Check the script outputs - they provide detailed error messages and next steps.

