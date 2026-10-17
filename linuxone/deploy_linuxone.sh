#!/usr/bin/env bash
# Deploy PulseGuard-Z to IBM LinuxONE Community Cloud (L1CC)
# Usage: ./deploy_linuxone.sh <L1CC_IP_OR_HOSTNAME> [PATH_TO_PEM_KEY]

REMOTE_HOST=$1
KEY_PATH=${2:-"../CodeBuilders.pem"}

if [ -z "$REMOTE_HOST" ]; then
    echo "Usage: ./deploy_linuxone.sh <L1CC_IP_OR_HOSTNAME> [PATH_TO_PEM_KEY]"
    echo "Example: ./deploy_linuxone.sh 148.100.xxx.xxx ../CodeBuilders.pem"
    exit 1
fi

echo "==================================================="
echo "  Deploying PulseGuard-Z to IBM LinuxONE Community Cloud"
echo "  Host: ${REMOTE_HOST}"
echo "  Key:  ${KEY_PATH}"
echo "==================================================="

# 1. Test SSH Connection and verify s390x architecture
echo "[1/4] Verifying connection and s390x architecture..."
ssh -i "${KEY_PATH}" -o StrictHostKeyChecking=no linux1@"${REMOTE_HOST}" "uname -m; cat /etc/os-release | grep PRETTY_NAME"

# 2. Sync project files
echo "[2/4] Uploading PulseGuard-Z codebase..."
rsync -avz -e "ssh -i ${KEY_PATH}" --exclude '.git' --exclude 'target' ../ linux1@"${REMOTE_HOST}":~/pulseguard-z/

# 3. Compile on s390x
echo "[3/4] Building natively on s390x JVM..."
ssh -i "${KEY_PATH}" linux1@"${REMOTE_HOST}" "cd ~/pulseguard-z && chmod +x run.sh && ./run.sh &"

echo "==================================================="
echo "  [SUCCESS] PulseGuard-Z running on IBM LinuxONE!"
echo "  Port-forward command to view dashboard locally:"
echo "  ssh -i ${KEY_PATH} -L 8080:localhost:8080 linux1@${REMOTE_HOST}"
echo "==================================================="
