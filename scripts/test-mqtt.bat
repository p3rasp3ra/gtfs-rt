@echo off
REM Quick start script for local MQTT testing

echo ============================================================
echo Starting MQTT Test Environment
echo ============================================================
echo.

echo [1/3] Starting MQTT broker (Mosquitto)...
docker-compose up -d mqtt-broker
timeout /t 3 /nobreak >nul

echo.
echo [2/3] Installing Python dependencies...
pip install -q gtfs-realtime-bindings paho-mqtt protobuf

echo.
echo [3/3] Starting test publisher...
echo Publishing test messages every 5 seconds (Ctrl+C to stop)
echo.
python mqtt_test_publisher.py --broker localhost --interval 5

pause

