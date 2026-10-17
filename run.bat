@echo off
call build.bat
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo ===================================================
echo   Launching PulseGuard-Z Server on http://localhost:8080
echo ===================================================

java -cp target\classes com.ibmz.pulseguard.Main 8080
