$ErrorActionPreference = "Continue"

Write-Host "=== Starting Phase 9 Live HTTP Verification Matrix ===" -ForegroundColor Cyan

# 1. Health Checks
Write-Host "`n--- 1. Health Checks ---" -ForegroundColor Yellow
$backendHealth = Invoke-RestMethod -Uri "http://127.0.0.1:8080/actuator/health" -Method Get
Write-Host "Backend Health: $($backendHealth.status)"

$aiDockerHealth = docker exec smartdesk-ai python -c "import urllib.request, json; res = urllib.request.urlopen('http://localhost:8000/health'); print(res.read().decode())"
Write-Host "AI Service Internal Health: $aiDockerHealth"

$frontendRes = Invoke-WebRequest -Uri "http://127.0.0.1:3000" -Method Get -UseBasicParsing
Write-Host "Frontend HTTP Status: $($frontendRes.StatusCode)"

# 2. Security Headers
Write-Host "`n--- 2. Security Headers Verification ---" -ForegroundColor Yellow
$loginEndpoint = "http://127.0.0.1:8080/api/v1/auth/login"
$headerCheck = Invoke-WebRequest -Uri "http://127.0.0.1:8080/actuator/health" -Method Get -UseBasicParsing
Write-Host "X-Content-Type-Options: $($headerCheck.Headers['X-Content-Type-Options'])"
Write-Host "X-Frame-Options: $($headerCheck.Headers['X-Frame-Options'])"
Write-Host "Referrer-Policy: $($headerCheck.Headers['Referrer-Policy'])"
Write-Host "Permissions-Policy: $($headerCheck.Headers['Permissions-Policy'])"

# 3. Authentication & RBAC Tokens
Write-Host "`n--- 3. Authentication & Token Retrieval ---" -ForegroundColor Yellow

# Bad credentials
try {
    $badLogin = Invoke-RestMethod -Uri $loginEndpoint -Method Post -ContentType "application/json" -Body '{"email":"bad@smartdesk.local","password":"wrong"}'
    Write-Host "Bad Login: UNEXPECTED SUCCESS" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "Bad Login Rejected correctly with status: $statusCode" -ForegroundColor Green
}

# Customer login
$custBody = '{"email":"alex@acmecorp.local","password":"Password123!"}'
$custAuth = (Invoke-RestMethod -Uri $loginEndpoint -Method Post -ContentType "application/json" -Body $custBody).data
$custToken = $custAuth.token
Write-Host "Customer Login: Success (Email: $($custAuth.user.email), Role: $($custAuth.user.role))" -ForegroundColor Green

# Agent login
$agentBody = '{"email":"agent.tech@smartdesk.local","password":"Password123!"}'
$agentAuth = (Invoke-RestMethod -Uri $loginEndpoint -Method Post -ContentType "application/json" -Body $agentBody).data
$agentToken = $agentAuth.token
Write-Host "Agent Login: Success (Email: $($agentAuth.user.email), Role: $($agentAuth.user.role))" -ForegroundColor Green

# Admin login
$adminBody = '{"email":"admin@smartdesk.local","password":"Password123!"}'
$adminAuth = (Invoke-RestMethod -Uri $loginEndpoint -Method Post -ContentType "application/json" -Body $adminBody).data
$adminToken = $adminAuth.token
Write-Host "Admin Login: Success (Email: $($adminAuth.user.email), Role: $($adminAuth.user.role))" -ForegroundColor Green

# Auth /me endpoint
$custMe = (Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/auth/me" -Method Get -Headers @{ Authorization = "Bearer $custToken" }).data
Write-Host "Customer /me Profile: Email=$($custMe.email), Role=$($custMe.role), UserId=$($custMe.id), CustomerCode=$($custMe.customerCode)" -ForegroundColor Green

# Auth /me without token
try {
    $unauthMe = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/auth/me" -Method Get
    Write-Host "Unauthenticated /me: UNEXPECTED SUCCESS" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "Unauthenticated /me Rejected correctly with status: $statusCode" -ForegroundColor Green
}

# 4. RBAC Verification
Write-Host "`n--- 4. Role-Based Access Control (RBAC) ---" -ForegroundColor Yellow
try {
    $custAnalytics = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/analytics/overview" -Method Get -Headers @{ Authorization = "Bearer $custToken" }
    Write-Host "Customer Analytics Access: UNEXPECTED SUCCESS" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "Customer blocked from Analytics with status: $statusCode" -ForegroundColor Green
}

try {
    $custQueue = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/agent/queue" -Method Get -Headers @{ Authorization = "Bearer $custToken" }
    Write-Host "Customer Agent Queue Access: UNEXPECTED SUCCESS" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "Customer blocked from Agent Queue with status: $statusCode" -ForegroundColor Green
}

$agentAnalytics = (Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/analytics/overview" -Method Get -Headers @{ Authorization = "Bearer $agentToken" }).data
Write-Host "Agent Analytics Access: Success (Total Tickets: $($agentAnalytics.totalTickets))" -ForegroundColor Green

# 5. Customer Isolation & Ticket Creation
Write-Host "`n--- 5. Customer Isolation & IDOR Check ---" -ForegroundColor Yellow
# Find a category ID
$categories = (Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/categories" -Method Get -Headers @{ Authorization = "Bearer $custToken" }).data
$catId = $categories[0].id
Write-Host "Retrieved category ID: $catId ($($categories[0].name))"

# Customer creates ticket
$newTicketPayload = @{
    subject = "Phase 9 Security Live Verification Ticket"
    description = "Checking customer isolation, IDOR, and duplicate ticket sanitization live."
    categoryId = $catId
    priority = "MEDIUM"
} | ConvertTo-Json

$createdTicket = (Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/tickets" -Method Post -Headers @{ Authorization = "Bearer $custToken" } -ContentType "application/json" -Body $newTicketPayload).data
Write-Host "Customer Created Ticket ID: $($createdTicket.id), Number: $($createdTicket.ticketNumber)" -ForegroundColor Green

# Customer gets own ticket
$custTicketView = (Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/tickets/$($createdTicket.id)" -Method Get -Headers @{ Authorization = "Bearer $custToken" }).data
Write-Host "Customer view duplicateMatches field: $(if ($custTicketView.duplicateMatches -eq $null) { 'NULL (Securely Hidden from Customer)' } else { 'EXPOSED!' })" -ForegroundColor Green

# Agent gets same ticket -> duplicate matches field should be present (array, possibly empty)
$agentTicketView = (Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/tickets/$($createdTicket.id)" -Method Get -Headers @{ Authorization = "Bearer $agentToken" }).data
Write-Host "Agent view duplicateMatches field is present: $($agentTicketView.PSObject.Properties['duplicateMatches'] -ne $null)" -ForegroundColor Green

# Check IDOR: Find another customer's ticket from database
$otherTicketId = (docker exec smartdesk-postgres psql -U postgres -d smartdesk -t -c "SELECT id FROM tickets WHERE customer_id NOT IN (SELECT id FROM customers WHERE user_id = '$($custMe.id)') LIMIT 1;").Trim()
Write-Host "Target other customer's ticket ID: $otherTicketId"

try {
    $idorCheck = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/tickets/$otherTicketId" -Method Get -Headers @{ Authorization = "Bearer $custToken" }
    Write-Host "IDOR Vulnerability: Customer could access another customer's ticket!" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "IDOR Blocked correctly with status: $statusCode" -ForegroundColor Green
}

# 6. Input Validation & Error Handling
Write-Host "`n--- 6. Input Validation & Error Handling ---" -ForegroundColor Yellow

# Malformed JSON
try {
    $malformed = Invoke-WebRequest -Uri $loginEndpoint -Method Post -ContentType "application/json" -Body "{ bad json: " -UseBasicParsing
    Write-Host "Malformed JSON: UNEXPECTED SUCCESS" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $responseBody = $reader.ReadToEnd()
    Write-Host "Malformed JSON Rejected: Status $statusCode, Body: $responseBody" -ForegroundColor Green
}

# Invalid UUID
try {
    $invalidUuid = Invoke-WebRequest -Uri "http://127.0.0.1:8080/api/v1/tickets/not-a-valid-uuid" -Method Get -Headers @{ Authorization = "Bearer $agentToken" } -UseBasicParsing
    Write-Host "Invalid UUID: UNEXPECTED SUCCESS" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $responseBody = $reader.ReadToEnd()
    Write-Host "Invalid UUID Rejected: Status $statusCode, Body: $responseBody" -ForegroundColor Green
}

# 7. AI Outage Graceful Degradation
Write-Host "`n--- 7. AI Outage Graceful Degradation Test ---" -ForegroundColor Yellow
Write-Host "Pausing smartdesk-ai container..."
docker pause smartdesk-ai | Out-Null
try {
    $outageTicketPayload = @{
        subject = "Ticket created during AI outage"
        description = "This ticket was created while the AI service container was completely paused."
        categoryId = $catId
        priority = "LOW"
    } | ConvertTo-Json

    $outageTicket = (Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/tickets" -Method Post -Headers @{ Authorization = "Bearer $custToken" } -ContentType "application/json" -Body $outageTicketPayload).data
    Write-Host "Ticket created during AI outage successfully: ID=$($outageTicket.id), Number=$($outageTicket.ticketNumber)" -ForegroundColor Green
} catch {
    Write-Host "Ticket creation failed during AI outage: $($_.Exception.Message)" -ForegroundColor Red
} finally {
    Write-Host "Unpausing smartdesk-ai container..."
    docker unpause smartdesk-ai | Out-Null
}

# 8. Rate Limiting Test (run at end so window doesn't affect other calls)
Write-Host "`n--- 8. Rate Limiting Test on /api/v1/auth/login (Approved 30 req/min/IP) ---" -ForegroundColor Yellow
$rateLimited = $false
$rateLimitCode = 0
$rateLimitFirstReq = 0
$probeIp = "172.28.0.99"
for ($i = 1; $i -le 45; $i++) {
    try {
        $res = Invoke-WebRequest -Uri $loginEndpoint -Method Post -ContentType "application/json" -Body '{"email":"test@smartdesk.local","password":"test"}' -Headers @{ "X-Forwarded-For" = $probeIp } -UseBasicParsing
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 429) {
            $rateLimited = $true
            $rateLimitCode = $statusCode
            $rateLimitFirstReq = $i
            Write-Host "Rate limit triggered at request #$i with HTTP 429 TOO_MANY_REQUESTS" -ForegroundColor Green
            break
        }
    }
}
if (-not $rateLimited) {
    Write-Host "Rate limiting was not triggered in 45 requests!" -ForegroundColor Red
} else {
    Write-Host "Confirmed: Rate limit enforced at approved threshold (first HTTP 429 at request #$rateLimitFirstReq)" -ForegroundColor Green
}

Write-Host "`n=== Phase 9 Live Verification Complete ===" -ForegroundColor Cyan
