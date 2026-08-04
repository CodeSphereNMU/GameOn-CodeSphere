@echo off
REM ============================================================
REM GameOn - Quick Run Script
REM Runs the application with the 'local' profile (H2 in-memory DB)
REM No SQL Server required!
REM ============================================================
REM
REM After startup, open: http://localhost:8080
REM
REM Test accounts (password: Test123):
REM   Zane, Lihlumelo, Gerard, Robert
REM
REM Admin accounts (password: Admin123):
REM   Moderator, Admin
REM
REM H2 Console: http://localhost:8080/h2-console
REM   JDBC URL: jdbc:h2:mem:GameOnDb
REM   Username: sa  |  Password: (leave empty)
REM ============================================================

set JAVA_HOME=%~dp0Java\jdk-21

echo.
echo  ============================
echo   GAME ON - Starting Server
echo  ============================
echo.
echo  Profile: local (H2 in-memory database)
echo  URL:     http://localhost:8080
echo  Java:    %JAVA_HOME%
echo.

call "%~dp0mvnw.cmd" spring-boot:run -Dspring-boot.run.profiles=local
