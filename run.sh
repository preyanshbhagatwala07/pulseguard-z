#!/usr/bin/env bash
# PulseGuard-Z Linux / IBM LinuxONE (s390x) Execution Script
set -e

echo "==================================================="
echo "  PulseGuard-Z: LinuxONE / s390x Execution Engine  "
echo "  National Health & ABDM FHIR R4 Edition           "
echo "==================================================="

ARCH=$(uname -m)
echo "[INFO] Detected Architecture: ${ARCH}"
if [ "${ARCH}" = "s390x" ]; then
    echo "[SUCCESS] Running natively on IBM Z / LinuxONE enterprise mainframe!"
fi

mkdir -p target/classes

echo "[INFO] Compiling Java classes..."
javac -d target/classes -sourcepath src/main/java \
    src/main/java/com/ibmz/pulseguard/*.java \
    src/main/java/com/ibmz/pulseguard/abdm/*.java \
    src/main/java/com/ibmz/pulseguard/clinical/*.java \
    src/main/java/com/ibmz/pulseguard/audit/*.java \
    src/main/java/com/ibmz/pulseguard/teleicu/*.java \
    src/main/java/com/ibmz/pulseguard/model/*.java \
    src/main/java/com/ibmz/pulseguard/engine/*.java \
    src/main/java/com/ibmz/pulseguard/service/*.java \
    src/main/java/com/ibmz/pulseguard/server/*.java

echo "[INFO] Copying web assets..."
mkdir -p target/classes/static
cp -r src/main/resources/static/* target/classes/static/ 2>/dev/null || true

echo "==================================================="
echo "  Launching PulseGuard-Z Server on port 8080       "
echo "  Access Dashboard: http://localhost:8080          "
echo "==================================================="

exec java -XX:+UseZGC -XX:+AlwaysPreTouch -cp target/classes com.ibmz.pulseguard.Main 8080
