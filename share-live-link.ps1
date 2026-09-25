# SmartDesk Live Public Link Generator (Cloudflare Tunnel)
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "     SmartDesk Reviewer Live Link Share      " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

# 1. Ensure smartdesk services are running
$running = docker compose ps --status running -q
if (-not $running) {
    Write-Host "Starting SmartDesk services first..." -ForegroundColor Yellow
    .\start.ps1
}

# 2. Ensure gateway container is running
$gateway = docker ps -q -f name=smartdesk-gateway
if (-not $gateway) {
    Write-Host "Starting Unified Gateway (Port 80)..." -ForegroundColor Yellow
    docker rm -f smartdesk-gateway 2>$null | Out-Null
    docker run -d --name smartdesk-gateway --network smartdesk-network -p 80:80 -v "${PSScriptRoot}/scratch/nginx-gateway.conf:/etc/nginx/nginx.conf:ro" nginx:1.27-alpine | Out-Null
}

# 3. Launch Cloudflare Tunnel
Write-Host "Starting Cloudflare Public Tunnel..." -ForegroundColor Green
Write-Host "Press Ctrl+C to stop sharing when done." -ForegroundColor Gray
Write-Host ""

& "${PSScriptRoot}\scratch\cloudflared.exe" tunnel --no-autoupdate --url http://localhost:80
