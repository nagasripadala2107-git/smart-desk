# SmartDesk 1-Click Startup Script (PowerShell)
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "         Starting SmartDesk Services         " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

# 1. Start containers in background
docker compose up -d

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[ERROR] Failed to start Docker containers. Make sure Docker Desktop is running." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "---------------------------------------------" -ForegroundColor Green
Write-Host "  SmartDesk is running!" -ForegroundColor Green
Write-Host "---------------------------------------------" -ForegroundColor Green
Write-Host ""
Write-Host "  Web App (PC):  http://localhost:3000" -ForegroundColor Yellow
Write-Host "  Mobile (Wi-Fi):http://192.168.1.5:3000" -ForegroundColor Yellow
Write-Host "  Backend API:   http://localhost:8080/api/v1" -ForegroundColor Gray
Write-Host "  Health Check:  http://localhost:8080/actuator/health" -ForegroundColor Gray
Write-Host ""
Write-Host "Default Demo Accounts (Password: Password123!):" -ForegroundColor Cyan
Write-Host "  - Admin:    admin@smartdesk.local"
Write-Host "  - Agent:    agent.tech@smartdesk.local"
Write-Host "  - Customer: alex@acmecorp.local"
Write-Host ""
Write-Host "Opening browser..." -ForegroundColor Cyan

# 2. Automatically launch browser
Start-Process "http://localhost:3000"

Write-Host "Done! Use 'docker compose down' or './stop.ps1' to stop." -ForegroundColor Green
