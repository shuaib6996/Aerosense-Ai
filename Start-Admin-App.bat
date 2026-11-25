@echo off
title AeroSense AI Admin Launcher
cd /d "%~dp0"

echo =======================================================
echo    AeroSense AI - Admin Cloud Command Center
echo    Starting Local Server & Desktop App Mode...
echo =======================================================

:: Start Python HTTP Server in background if not already running
netstat -ano | findstr :5050 >nul
if %errorlevel% neq 0 (
    start /b python -m http.server 5050 --bind 127.0.0.1 >nul 2>&1
    timeout /t 1 /nobreak >nul
)

:: Try Chrome first, then Edge, then default browser
set CHROME_PATH="C:\Program Files\Google\Chrome\Application\chrome.exe"
set CHROME_PATH_X86="C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"
set EDGE_PATH="C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
set EDGE_PATH_64="C:\Program Files\Microsoft\Edge\Application\msedge.exe"

if exist %CHROME_PATH% (
    start "" %CHROME_PATH% --app=http://127.0.0.1:5050/admin.html --window-size=1280,820
    exit /b
)
if exist %CHROME_PATH_X86% (
    start "" %CHROME_PATH_X86% --app=http://127.0.0.1:5050/admin.html --window-size=1280,820
    exit /b
)
if exist %EDGE_PATH% (
    start "" %EDGE_PATH% --app=http://127.0.0.1:5050/admin.html --window-size=1280,820
    exit /b
)
if exist %EDGE_PATH_64% (
    start "" %EDGE_PATH_64% --app=http://127.0.0.1:5050/admin.html --window-size=1280,820
    exit /b
)

:: Fallback to default browser
start http://127.0.0.1:5050/admin.html
exit /b
