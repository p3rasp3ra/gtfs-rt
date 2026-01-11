@echo off
REM Test script for GTFS-RT protobuf endpoint using cURL
REM This script assumes you have a test protobuf file named test_vehicle.pb

echo ============================================================
echo GTFS-RT Protobuf Test - Vehicle Position Endpoint
echo ============================================================
echo.

if not exist test_vehicle.pb (
    echo ERROR: test_vehicle.pb file not found!
    echo Please create a protobuf file first using the Java code or Python script.
    exit /b 1
)

echo Sending protobuf FeedEntity to endpoint...
echo.

curl -X POST http://localhost:8087/vp/f/ztp-feed/a/ztp-agency ^
  -H "Content-Type: application/x-protobuf" ^
  --data-binary @test_vehicle.pb ^
  -w "\n\nHTTP Status: %%{http_code}\n" ^
  -v

echo.
echo ============================================================
echo Test complete
echo ============================================================

