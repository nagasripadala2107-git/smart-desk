# SmartDesk Shutdown Script (PowerShell)
Write-Host "Stopping SmartDesk services..." -ForegroundColor Yellow
docker compose down
docker rm -f smartdesk-gateway 2>$null | Out-Null
Write-Host "All services stopped." -ForegroundColor Green
