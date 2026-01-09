@echo off
REM Test script for GTFS-RT ASCII text format (development/debug)
echo ============================================================
echo GTFS-RT ASCII Text Format Testing
echo ============================================================
echo.

echo [1/3] Testing POST with ASCII text format FeedEntity...
echo.
curl -X POST http://localhost:8087/vp/f/ztp-feed/a/ztp-agency ^
  -H "Content-Type: text/plain" ^
  --data-binary @test_vehicle.txt ^
  -w "\nHTTP Status: %%{http_code}\n" ^
  -v

echo.
echo.
echo [2/3] Testing GET feed with ASCII text format (Accept: text/plain)...
echo.
curl -X GET "http://localhost:8087/gtfs-rt/feed.pb" ^
  -H "Accept: text/plain" ^
  -w "\nHTTP Status: %%{http_code}\n" ^
  -v

echo.
echo.
echo [3/3] Testing GET feed with binary format (Accept: application/x-protobuf)...
echo.
curl -X GET "http://localhost:8087/gtfs-rt/feed.pb" ^
  -H "Accept: application/x-protobuf" ^
  -w "\nHTTP Status: %%{http_code}\n" ^
  --output feed_binary.pb ^
  -v

echo.
echo ============================================================
echo Test complete
echo Binary feed saved to: feed_binary.pb
echo ============================================================

