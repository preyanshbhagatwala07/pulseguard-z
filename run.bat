@echo off
call build.bat
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

if not exist target\classes\static mkdir target\classes\static
xcopy /E /I /Y src\main\resources\static target\classes\static >nul 2>&1

echo ===================================================
echo   Launching PulseGuard-Z Server
echo   Access Dashboard: http://localhost:8080 (or port 8085)
echo ===================================================

java -cp target\classes com.ibmz.pulseguard.Main %*
