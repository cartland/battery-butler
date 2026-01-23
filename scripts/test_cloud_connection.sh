#!/bin/bash
set -ea

echo "Testing Cloud Connectivity..."

# Clear logs
adb logcat -c

# Launch App to ensure MainActivity is running (and DebugNetworkReceiver is registered)
adb shell monkey -p com.chriscartland.batterybutler -c android.intent.category.LAUNCHER 1
sleep 5

# Start capturing logs in background
adb logcat > connectivity_test.log &
LOG_PID=$!

# 1. Set Network Mode to GRPC_AWS (AWS Cloud)
echo "Broadcasting GRPC_AWS mode..."
adb shell am broadcast \
  -p com.chriscartland.batterybutler \
  -a com.chriscartland.batterybutler.SET_NETWORK_MODE \
  --es mode GRPC_AWS

# 2. Reset Server URL to default
adb shell am broadcast \
  -n com.chriscartland.batterybutler/com.chriscartland.batterybutler.composeapp.ServerUrlReceiver \
  -a com.chriscartland.batterybutler.SET_SERVER_URL \
  --esn url

echo "App configured for Cloud Connectivity (AWS Mode)."
echo "Monitoring logs for success confirmation..."

# Wait for success log
FOUND=0
for i in {1..30}; do
    if grep -q "RoomDeviceRepository received update! Size=" connectivity_test.log; then
        echo "Log found: $(grep -m 1 "RoomDeviceRepository received update! Size=" connectivity_test.log)"
        FOUND=1
        break
    fi
    if grep -q "Network mode set to GRPC_AWS via UseCase" connectivity_test.log; then
        echo "Mode switch verified..."
    fi
    sleep 1
done

# Cleanup
kill $LOG_PID
# Remove log file
# rm connectivity_test.log

if [ $FOUND -eq 1 ]; then
    echo "SUCCESS: Data received from Cloud!"
else
    echo "FAILURE: Timeout waiting for data."
    cat connectivity_test.log | grep -E "BatteryButler|Grpc" | tail -n 20
    exit 1
fi
