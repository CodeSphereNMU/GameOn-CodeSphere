@echo off
REM ============================================================
REM GameOn - Remove Auto-Start
REM Run as Administrator to remove the scheduled task.
REM ============================================================

echo.
echo  Removing GameOn auto-start task...

net session >nul 2>&1
if %errorlevel% neq 0 (
    echo  ERROR: Please run this script as Administrator!
    pause
    exit /b 1
)

schtasks /delete /tn "GameOn Server" /f

if %errorlevel% equ 0 (
    echo  Done. GameOn will no longer start automatically.
) else (
    echo  Task not found or already removed.
)

echo.
pause
