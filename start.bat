@echo off
echo =============================================
echo          Starting SmartDesk Services
echo =============================================
echo.
docker compose up -d

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Failed to start Docker containers. Make sure Docker Desktop is running.
    pause
    exit /b %errorlevel%
)

echo.
echo ---------------------------------------------
echo   SmartDesk is running!
echo ---------------------------------------------
echo.
echo   Web App (PC):   http://localhost:3000
echo   Mobile (Wi-Fi): http://192.168.1.5:3000
echo   Backend API:    http://localhost:8080/api/v1
echo   Health Check:   http://localhost:8080/actuator/health
echo.
echo Default Demo Accounts (Password: Password123!):
echo   - Admin:    admin@smartdesk.local
echo   - Agent:    agent.tech@smartdesk.local
echo   - Customer: alex@acmecorp.local
echo.
echo Opening browser...
start http://localhost:3000
echo Done!
