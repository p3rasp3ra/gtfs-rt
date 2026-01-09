@echo off
echo ==========================================
echo Kafka Connect Diagnostics
echo ==========================================
echo.

echo [1] Checking if Kafka Connect container is running...
docker ps --filter "name=kafka-connect" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo.

echo [2] Checking Kafka Connect logs (last 30 lines)...
docker logs kafka-connect --tail 30
echo.

echo [3] Testing port 8082 from host...
echo Testing with curl...
curl -v http://localhost:8082/ 2>&1 | findstr /C:"Connected" /C:"HTTP" /C:"failed" /C:"refused"
echo.

echo [4] Checking if port 8082 is listening...
netstat -an | findstr ":8082"
echo.

echo ==========================================
echo Diagnostics Complete
echo ==========================================
pause

