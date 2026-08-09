@echo off
REM ============================================================
REM GameOn - Stop Server
REM Kills the running GameOn Java process
REM ============================================================
echo Stopping GameOn server...
for /f "tokens=1" %%p in ('wmic process where "commandline like '%%gameon%%' and commandline like '%%spring-boot%%'" get processid 2^>nul ^| findstr /r "[0-9]"') do (
    taskkill /PID %%p /F >nul 2>&1
)
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    taskkill /PID %%p /F >nul 2>&1
)
echo GameOn server stopped.
pause
