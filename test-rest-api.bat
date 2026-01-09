@echo off
echo Testing Kafka Connect REST API accessibility...
echo.

REM First, let's see if the container is even running
echo Checking container status...
docker ps -a --filter "name=kafka-connect" --format "{{.Names}} - {{.Status}}"
echo.

REM Check if we can exec into the container and curl from inside
echo Testing REST API from INSIDE the container...
docker exec kafka-connect curl -s http://localhost:8082/ 2>&1
echo.

REM Check if we can reach it from the host
echo Testing REST API from HOST machine...
curl -s http://localhost:8082/ 2>&1
echo.

echo.
echo If the inside test works but outside test fails, it's a port binding issue.
echo If both fail, the REST API isn't starting properly.
pause

