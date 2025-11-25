@echo off
echo ========================================
echo  UPLOADING FOODS TO FIREBASE
echo ========================================
echo.
echo Checking Node.js...
node --version
if %errorlevel% neq 0 (
    echo ERROR: Node.js not found! Please install Node.js from nodejs.org
    pause
    exit /b 1
)

echo.
echo Installing Firebase Admin SDK...
call npm install firebase-admin --silent

echo.
echo Starting upload...
node upload-500-foods-final.js

echo.
echo Upload completed!
pause
