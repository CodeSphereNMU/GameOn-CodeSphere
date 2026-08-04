# ============================================================
# GameOn - Quick Run Script (PowerShell)
# Runs the application with the 'local' profile (H2 in-memory DB)
# No SQL Server required!
# ============================================================
#
# After startup, open: http://localhost:8080
#
# Test accounts (password: Test123):
#   Zane, Lihlumelo, Gerard, Robert
#
# Admin accounts (password: Admin123):
#   Moderator, Admin
#
# H2 Console: http://localhost:8080/h2-console
#   JDBC URL: jdbc:h2:mem:GameOnDb
#   Username: sa  |  Password: (leave empty)
# ============================================================

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$env:JAVA_HOME = Join-Path $scriptDir "Java\jdk-21"

Write-Host ""
Write-Host "  ============================" -ForegroundColor Red
Write-Host "   GAME ON - Starting Server" -ForegroundColor White
Write-Host "  ============================" -ForegroundColor Red
Write-Host ""
Write-Host "  Profile: local (H2 in-memory database)" -ForegroundColor Cyan
Write-Host "  URL:     http://localhost:8080" -ForegroundColor Green
Write-Host "  Java:    $env:JAVA_HOME" -ForegroundColor DarkGray
Write-Host ""

& "$scriptDir\mvnw.cmd" spring-boot:run "-Dspring-boot.run.profiles=local"
