@echo off
echo Testing basic commands
set CONNECT_URL=http://localhost:8082
echo URL is: %CONNECT_URL%
curl -s %CONNECT_URL%/
pause

