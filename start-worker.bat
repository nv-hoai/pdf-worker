@echo off
REM PDF Worker Startup Script for Windows

set SERVER_HOST=localhost
set SERVER_PORT=7777
set JAR_FILE=pdf-worker-1.0.0-jar-with-dependencies.jar

if "%1"=="" (
    echo Usage: start-worker.bat [server-host] [server-port]
    echo Example: start-worker.bat 192.168.1.100 7777
    echo.
    echo Starting with default: %SERVER_HOST%:%SERVER_PORT%
) else (
    set SERVER_HOST=%1
)

if not "%2"=="" (
    set SERVER_PORT=%2
)

echo ========================================
echo PDF Conversion Worker
echo ========================================
echo Server: %SERVER_HOST%:%SERVER_PORT%
echo Mode: TCP File Transfer
echo ========================================
echo.

java -Xmx1024m -jar target\%JAR_FILE% %SERVER_HOST% %SERVER_PORT%

pause
