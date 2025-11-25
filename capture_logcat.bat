@echo off
REM Capture Logcat for Food Recommendations debugging
echo Capturing Logcat output for Food Recommendations...
echo This will capture the next 2 minutes of logs
echo.
echo Instructions:
echo 1. Make sure your device/emulator is connected (adb devices should show it)
echo 2. Open the user app and navigate to Food Recommendations
echo 3. Press Ctrl+C when done capturing
echo.
pause

REM Clear existing logs first
adb logcat -c

REM Capture logs filtered for FoodRecommendations
echo Capturing logs... Press Ctrl+C to stop
adb logcat -s FoodRecommendations:D AndroidRuntime:E > logcat_food_recommendations.txt

echo.
echo Logs saved to: logcat_food_recommendations.txt
pause

