@echo off
REM ============================================================
REM GameOn - Quick Run Script
REM Runs the application with the 'dev' profile against GameOnOfficial.
REM Set SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD
REM before running this script.
REM ============================================================
REM
REM After startup, open: http://localhost:8080
REM
REM Test accounts (password: Test123):
REM   Zane, Lihlumelo, Gerard, Robert
REM
REM Admin accounts (password: Admin123):
REM   Moderator, Admin
REM ============================================================

if "%JAVA_HOME%"=="" (
    echo  ERROR: JAVA_HOME is not set.
    echo  Install Java and set JAVA_HOME before running GameOn.
    exit /b 1
)

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo  ERROR: JAVA_HOME does not point to a valid Java installation.
    echo  Current JAVA_HOME: %JAVA_HOME%
    exit /b 1
)

if not exist "%~dp0mvnw.cmd" (
    echo  ERROR: Maven Wrapper not found at %~dp0mvnw.cmd
    exit /b 1
)

if "%SPRING_DATASOURCE_URL%"=="" (
    set "SPRING_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=GameOnOfficial;encrypt=true;trustServerCertificate=true"
)

if "%SPRING_DATASOURCE_USERNAME%"=="" (
    echo  ERROR: SPRING_DATASOURCE_USERNAME is not set.
    echo  Set it in this terminal before running GameOn.
    exit /b 1
)

if "%SPRING_DATASOURCE_PASSWORD%"=="" (
    echo  ERROR: SPRING_DATASOURCE_PASSWORD is not set.
    echo  Set it in this terminal before running GameOn.
    exit /b 1
)

set "SPRING_PROFILES_ACTIVE=dev"

echo.
echo  ============================
echo   GAME ON - Starting Server
echo  ============================
echo.
echo  Profile: dev (SQL Server)
echo  Database: GameOnOfficial
echo  URL:     http://localhost:8080
echo  Java:    %JAVA_HOME%
echo.

REM Compile and run the current source through the project Maven Wrapper.
pushd "%~dp0"
call "%~dp0mvnw.cmd" spring-boot:run
set "GAMEON_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %GAMEON_EXIT_CODE%
