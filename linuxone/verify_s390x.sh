#!/usr/bin/env bash
# Hardware & Enterprise Verification for IBM Z / LinuxONE Submission
echo "==================================================="
echo "  IBM Z / LinuxONE Enterprise Verification Report  "
echo "==================================================="
echo "Timestamp:    $(date -u)"
echo "Architecture: $(uname -m)"
echo "Kernel:       $(uname -r)"
echo "OS Version:   $(cat /etc/os-release | grep PRETTY_NAME | cut -d= -f2)"
echo "CPU Details:  $(lscpu | grep 'Model name\|Architecture\|CPU(s):' 2>/dev/null || cat /proc/cpuinfo | head -n 10)"
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "==================================================="
if [ "$(uname -m)" = "s390x" ]; then
    echo "VERIFICATION: PASSED (Native IBM Z / LinuxONE s390x)"
else
    echo "VERIFICATION: RUNNING IN WORKSTATION EMULATION MODE"
fi
echo "==================================================="
