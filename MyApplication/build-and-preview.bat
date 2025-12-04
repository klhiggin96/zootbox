@echo off
REM Build and Install Script for Android Preview
REM Builds the debug APK and installs it to a running emulator

echo ========================================
echo Android Build and Preview
echo ========================================
echo.

set ERROR_OCCURRED=0

REM Check if Gradle wrapper exists
if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat not found. Are you in the project root directory?
    exit /b 1
)

REM Check if adb is available
where adb >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [WARNING] adb not found in PATH. Trying to find it...
    
    REM Try to read SDK path from local.properties
    set SDK_PATH=
    if exist "local.properties" (
        for /f "tokens=2 delims==" %%a in ('findstr /C:"sdk.dir=" local.properties') do (
            set SDK_PATH=%%a
            set SDK_PATH=!SDK_PATH:\=/!
        )
    )
    
    REM Try common locations
    if "!SDK_PATH!"=="" (
        if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
            set SDK_PATH=%LOCALAPPDATA%\Android\Sdk
        ) else if exist "%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
            set SDK_PATH=%USERPROFILE%\AppData\Local\Android\Sdk
        )
    )
    
    if not "!SDK_PATH!"=="" (
        set "PATH=%SDK_PATH%\platform-tools;%PATH%"
        echo Found SDK at: !SDK_PATH!
    ) else (
        echo [ERROR] Could not find Android SDK. Please add platform-tools to PATH.
        exit /b 1
    )
)

REM Check for running emulator
echo Checking for running emulator...
adb devices | findstr /R "device$" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] No emulator is currently running.
    echo.
    echo Please start an emulator first:
    echo   1. Run: start-emulator.bat
    echo   2. Or start from Android Studio: Tools ^> Device Manager
    echo.
    exit /b 1
)

echo [OK] Emulator found
echo.

REM Build the debug APK
echo ========================================
echo Step 1: Building debug APK...
echo ========================================
call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build failed. Please check the errors above.
    set ERROR_OCCURRED=1
    goto :end
)

echo.
echo [OK] Build successful
echo.

REM Install to emulator
echo ========================================
echo Step 2: Installing to emulator...
echo ========================================
call gradlew.bat installDebug
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Installation failed. Please check the errors above.
    set ERROR_OCCURRED=1
    goto :end
)

echo.
echo [OK] Installation successful
echo.

REM Launch the app
echo ========================================
echo Step 3: Launching app...
echo ========================================
adb shell monkey -p com.example.myapplication -c android.intent.category.LAUNCHER 1
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [OK] App launched successfully!
) else (
    echo.
    echo [WARNING] Could not automatically launch app. You can open it manually from the emulator launcher.
)

:end
echo.
echo ========================================
if %ERROR_OCCURRED% EQU 1 (
    echo Build/Install completed with ERRORS
    echo Please fix the errors above.
) else (
    echo Build/Install completed successfully!
    echo.
    echo The app should now be visible on your emulator.
    echo If it didn't launch automatically, find it in the app launcher.
)
echo ========================================

exit /b %ERROR_OCCURRED%

