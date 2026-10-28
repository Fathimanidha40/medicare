@echo off
title MediCare Web Server
echo ============================================================
echo Starting MediCare Healthcare System...
echo ============================================================
echo.
cd /d "%~dp0medicare-backend"

set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.1"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Checking Java version...
java -version
echo.
echo Starting Spring Boot server on port 8080...
echo Once started, open your browser and go to:
echo http://localhost:8080/
echo.
echo Press Ctrl+C to stop the server at any time.
echo ============================================================
echo.
java -jar target\medicare-backend-1.0.0.jar
pause
