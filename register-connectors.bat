@echo off
echo ==========================================
echo Registering Kafka Connect MQTT Connectors
echo ==========================================
echo.

set CONNECT_URL=http://localhost:8082

echo Testing Kafka Connect REST API availability...
curl -s -f %CONNECT_URL%/ >nul 2>&1
if not errorlevel 1 goto register_connectors
echo [1/5] Waiting for Kafka Connect REST API... retrying in 10 seconds
timeout /t 10 /nobreak >nul

curl -s -f %CONNECT_URL%/ >nul 2>&1
if not errorlevel 1 goto register_connectors
echo [2/5] Waiting for Kafka Connect REST API... retrying in 10 seconds
timeout /t 10 /nobreak >nul

curl -s -f %CONNECT_URL%/ >nul 2>&1
if not errorlevel 1 goto register_connectors
echo [3/5] Waiting for Kafka Connect REST API... retrying in 10 seconds
timeout /t 10 /nobreak >nul

curl -s -f %CONNECT_URL%/ >nul 2>&1
if not errorlevel 1 goto register_connectors
echo [4/5] Waiting for Kafka Connect REST API... retrying in 10 seconds
timeout /t 10 /nobreak >nul

curl -s -f %CONNECT_URL%/ >nul 2>&1
if not errorlevel 1 goto register_connectors
echo [5/5] Waiting for Kafka Connect REST API... retrying in 10 seconds
timeout /t 10 /nobreak >nul

curl -s -f %CONNECT_URL%/ >nul 2>&1
if not errorlevel 1 goto register_connectors

echo ERROR: Kafka Connect REST API is not accessible after 5 attempts
echo Make sure Docker containers are running: docker ps
echo Check Kafka Connect logs: docker logs kafka-connect
pause
exit /b 1

:register_connectors
echo Kafka Connect REST API is available!
echo.
echo ==========================================
echo Registering Connectors
echo ==========================================
echo.

echo Deleting existing connectors if any...
curl -s -X DELETE %CONNECT_URL%/connectors/mqtt-vehicle-position-source >nul 2>&1
curl -s -X DELETE %CONNECT_URL%/connectors/mqtt-trip-update-source >nul 2>&1
curl -s -X DELETE %CONNECT_URL%/connectors/mqtt-service-alert-source >nul 2>&1
timeout /t 3 /nobreak >nul

echo.
echo [1/3] Registering mqtt-vehicle-position-source...
curl -X POST -H "Content-Type: application/json" -d @config/kafka-connect/mqtt-vp-source.json %CONNECT_URL%/connectors
echo.

echo.
echo [2/3] Registering mqtt-trip-update-source...
curl -X POST -H "Content-Type: application/json" -d @config/kafka-connect/mqtt-tu-source.json %CONNECT_URL%/connectors
echo.

echo.
echo [3/3] Registering mqtt-service-alert-source...
curl -X POST -H "Content-Type: application/json" -d @config/kafka-connect/mqtt-sa-source.json %CONNECT_URL%/connectors
echo.

echo.
echo ==========================================
echo Verifying Connector Status
echo ==========================================
echo.
echo Registered connectors:
curl -s %CONNECT_URL%/connectors
echo.
echo.

echo Status for mqtt-vehicle-position-source:
curl -s %CONNECT_URL%/connectors/mqtt-vehicle-position-source/status
echo.
echo.

echo Status for mqtt-trip-update-source:
curl -s %CONNECT_URL%/connectors/mqtt-trip-update-source/status
echo.
echo.

echo Status for mqtt-service-alert-source:
curl -s %CONNECT_URL%/connectors/mqtt-service-alert-source/status
echo.
echo.

echo ==========================================
echo Registration Complete
echo ==========================================
pause

