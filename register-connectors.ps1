Write-Host "=========================================="
Write-Host "Registering Kafka Connect MQTT Connectors"
Write-Host "=========================================="
Write-Host ""

$connectUrl = "http://localhost:8082"
$maxRetries = 5

Write-Host "Testing Kafka Connect REST API availability..."
$attempt = 0
$apiAvailable = $false

while ($attempt -lt $maxRetries) {
    $attempt++
    try {
        $response = Invoke-WebRequest -Uri "$connectUrl/" -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
        Write-Host "[$attempt/$maxRetries] Kafka Connect REST API is available!"
        $apiAvailable = $true
        break
    } catch {
        Write-Host "[$attempt/$maxRetries] Waiting for Kafka Connect REST API... retrying in 10 seconds"
        Start-Sleep -Seconds 10
    }
}

if (-not $apiAvailable) {
    Write-Host "ERROR: Kafka Connect REST API is not accessible after $maxRetries attempts"
    Write-Host "Make sure Docker containers are running: docker ps"
    Write-Host "Check Kafka Connect logs: docker logs kafka-connect"
    pause
    exit 1
}

Write-Host ""
Write-Host "=========================================="
Write-Host "Registering Connectors"
Write-Host "=========================================="
Write-Host ""

Write-Host "Deleting existing connectors if any..."
@("mqtt-vehicle-position-source", "mqtt-trip-update-source", "mqtt-service-alert-source") | ForEach-Object {
    try {
        Invoke-WebRequest -Uri "$connectUrl/connectors/$_" -Method Delete -ErrorAction SilentlyContinue | Out-Null
    } catch {}
}
Start-Sleep -Seconds 3

Write-Host ""
Write-Host "[1/3] Registering mqtt-vehicle-position-source..."
$vpConfig = Get-Content -Path "config\kafka-connect\mqtt-vp-source.json" -Raw
$response = Invoke-WebRequest -Uri "$connectUrl/connectors" -Method Post -Body $vpConfig -ContentType "application/json" -UseBasicParsing
Write-Host $response.Content
Write-Host ""

Write-Host "[2/3] Registering mqtt-trip-update-source..."
$tuConfig = Get-Content -Path "config\kafka-connect\mqtt-tu-source.json" -Raw
$response = Invoke-WebRequest -Uri "$connectUrl/connectors" -Method Post -Body $tuConfig -ContentType "application/json" -UseBasicParsing
Write-Host $response.Content
Write-Host ""

Write-Host "[3/3] Registering mqtt-service-alert-source..."
$saConfig = Get-Content -Path "config\kafka-connect\mqtt-sa-source.json" -Raw
$response = Invoke-WebRequest -Uri "$connectUrl/connectors" -Method Post -Body $saConfig -ContentType "application/json" -UseBasicParsing
Write-Host $response.Content
Write-Host ""

Write-Host ""
Write-Host "=========================================="
Write-Host "Verifying Connector Status"
Write-Host "=========================================="
Write-Host ""

Write-Host "Registered connectors:"
$response = Invoke-WebRequest -Uri "$connectUrl/connectors" -UseBasicParsing
Write-Host $response.Content
Write-Host ""

@("mqtt-vehicle-position-source", "mqtt-trip-update-source", "mqtt-service-alert-source") | ForEach-Object {
    Write-Host ""
    Write-Host "Status for $_:"
    $response = Invoke-WebRequest -Uri "$connectUrl/connectors/$_/status" -UseBasicParsing
    Write-Host $response.Content
    Write-Host ""
}

Write-Host ""
Write-Host "=========================================="
Write-Host "Registration Complete"
Write-Host "=========================================="
pause

