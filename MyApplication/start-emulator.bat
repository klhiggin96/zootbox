@echo off
REM Start Android Emulator Script
REM Automatically detects and starts an available AVD

echo ========================================
echo Android Emulator Launcher
echo ========================================
echo.

setlocal enabledelayedexpansion

REM Check if emulator is already running
where adb >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    adb devices | findstr /R "device$" >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo [OK] An emulator is already running.
        adb devices
        echo.
        echo If you want to start a different emulator, please stop the current one first.
        exit /b 0
    )
) else (
    echo [WARNING] adb not found in PATH. Will try to locate SDK...
)

REM Find SDK path
set SDK_PATH=
if exist "local.properties" (
    for /f "tokens=2 delims==" %%a in ('findstr /C:"sdk.dir=" local.properties') do (
        set SDK_PATH=%%a
        set SDK_PATH=!SDK_PATH:\=/!
    )
)

REM Try common locations
if "!SDK_PATH!"=="" (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set SDK_PATH=%LOCALAPPDATA%\Android\Sdk
    ) else if exist "%USERPROFILE%\AppData\Local\Android\Sdk" (
        set SDK_PATH=%USERPROFILE%\AppData\Local\Android\Sdk
    )
)

if "!SDK_PATH!"=="" (
    echo [ERROR] Could not find Android SDK path.
    echo Please ensure Android Studio is installed or set ANDROID_HOME environment variable.
    exit /b 1
)

echo [OK] Found SDK at: !SDK_PATH!
echo.

REM Add emulator to PATH
set "PATH=!SDK_PATH!\emulator;!SDK_PATH!\platform-tools;!PATH!"

REM Check if emulator command is available
where emulator >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] emulator command not found. Is the Android SDK properly installed?
    exit /b 1
)

REM List available AVDs
echo Listing available Android Virtual Devices...
echo.
emulator -list-avds > "%TEMP%\avd_list.txt" 2>&1

set AVD_COUNT=0
set FIRST_AVD=

for /f "delims=" %%a in ('type "%TEMP%\avd_list.txt"') do (
    set LINE=%%a
    if not "!LINE!"=="" (
        if !AVD_COUNT! EQU 0 (
            set FIRST_AVD=!LINE!
        )
        echo   [!AVD_COUNT!] !LINE!
        set /a AVD_COUNT+=1
    )
)

if %AVD_COUNT% EQU 0 (
    echo.
    echo [ERROR] No AVDs found. Please create an AVD first:
    echo   1. Open Android Studio
    echo   2. Go to: Tools ^> Device Manager
    echo   3. Click: Create Device
    echo.
    del "%TEMP%\avd_list.txt" >nul 2>&1
    exit /b 1
)

del "%TEMP%\avd_list.txt" >nul 2>&1

echo.
if %AVD_COUNT% EQU 1 (
    echo [OK] Found 1 AVD. Starting: !FIRST_AVD!
    set SELECTED_AVD=!FIRST_AVD!
) else (
    echo Found %AVD_COUNT% AVD(s). Starting the first one: !FIRST_AVD!
    echo.
    echo To start a different AVD, run:
    echo   emulator -avd ^<AVD_NAME^>
    echo.
    set SELECTED_AVD=!FIRST_AVD!
)

echo.
echo ========================================
echo Starting emulator: !SELECTED_AVD!
echo ========================================
echo.
echo This may take a minute or two. The emulator window will open when ready.
echo You can close this window once the emulator is running.
echo.
echo To check when the emulator is ready, run: adb devices
echo.

REM Start emulator in background
start "Android Emulator" emulator -avd "!SELECTED_AVD!"

echo Emulator is starting in the background...
echo.
echo You can check emulator status by running: adb devices
echo.
echo Once the emulator shows "device" status, you can build and install with:
echo   build-and-preview.bat
echo.

timeout /t 3 >nul 2>&1

endlocal

