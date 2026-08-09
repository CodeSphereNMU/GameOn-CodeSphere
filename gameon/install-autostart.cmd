@echo off
REM ============================================================
REM GameOn - Install Auto-Start (runs at Windows login)
REM Run this ONCE as Administrator to register the task.
REM After this, GameOn starts automatically when you log in.
REM Just open http://localhost:8080 in your browser.
REM ============================================================

echo.
echo  ============================
echo   GAME ON - Install AutoStart
echo  ============================
echo.

REM Check for admin privileges
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo  ERROR: Please run this script as Administrator!
    echo  Right-click ^> Run as administrator
    echo.
    pause
    exit /b 1
)

set SCRIPT_DIR=%~dp0
set VBS_PATH=%SCRIPT_DIR%start-background.vbs

REM Create scheduled task that runs at logon
schtasks /create /tn "GameOn Server" /tr "wscript.exe \"%VBS_PATH%\"" /sc onlogon /rl highest /f

if %errorlevel% equ 0 (
    echo.
    echo  SUCCESS! GameOn will now start automatically when you log in.
    echo.
    echo  Starting server now...
    wscript.exe "%VBS_PATH%"
    echo.
    echo  Server is starting in the background.
    echo  Wait ~10 seconds, then open: http://localhost:8080
    echo.
    echo  To stop:    run stop-server.cmd
    echo  To remove:  run uninstall-autostart.cmd as Administrator
    echo.
) else (
    echo  ERROR: Failed to create scheduled task.
)

pause
