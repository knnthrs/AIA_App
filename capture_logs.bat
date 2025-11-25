@echo off
REM Quick Logcat Capture Script

echo ================================
echo Food Recommendations Logcat Capture
echo ================================
echo.

REM Try to find ADB in common locations
set ADB_PATH=
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set ADB_PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
)
if exist "%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
    set ADB_PATH=%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe
)

if "%ADB_PATH%"=="" (
    echo ERROR: Could not find adb.exe
    echo.
    echo Please use Android Studio Logcat instead:
    echo 1. Open Android Studio
    echo 2. Bottom toolbar, click "Logcat"
    echo 3. In the search/filter box, type: FoodRecommendations
    echo 4. Run your app and navigate to Food Recommendations
    echo 5. Copy the log output and paste it here
    echo.
    pause
    exit /b
)

echo Found ADB at: %ADB_PATH%
echo.

REM Check if device is connected
"%ADB_PATH%" devices
echo.

echo Instructions:
echo 1. Open the user app
echo 2. Navigate to Food Recommendations
echo 3. Come back here and press any key
echo 4. Logs will be captured for 30 seconds
echo.
pause

REM Clear old logs
"%ADB_PATH%" logcat -c

echo.
echo Capturing logs for 30 seconds...
echo.

REM Capture logs with timeout
"%ADB_PATH%" logcat -s FoodRecommendations:D AndroidRuntime:E -d > logcat_output.txt

echo.
echo ===== LOGCAT OUTPUT =====
type logcat_output.txt
echo.
echo ===== END LOGCAT =====
echo.
echo Logs saved to: logcat_output.txt
echo.
pause

