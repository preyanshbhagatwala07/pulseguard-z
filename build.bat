@echo off
echo ===================================================
echo   Compiling PulseGuard-Z (Java 21+)
echo ===================================================

if not exist target\classes mkdir target\classes

javac -d target\classes -sourcepath src\main\java src\main\java\com\ibmz\pulseguard\*.java src\main\java\com\ibmz\pulseguard\abdm\*.java src\main\java\com\ibmz\pulseguard\clinical\*.java src\main\java\com\ibmz\pulseguard\audit\*.java src\main\java\com\ibmz\pulseguard\teleicu\*.java src\main\java\com\ibmz\pulseguard\model\*.java src\main\java\com\ibmz\pulseguard\engine\*.java src\main\java\com\ibmz\pulseguard\service\*.java src\main\java\com\ibmz\pulseguard\server\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Compilation completed successfully.
