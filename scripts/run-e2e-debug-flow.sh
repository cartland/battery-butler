#!/bin/bash
set -ex

# 1. Kill potentially conflicting processes
echo "[DEBUG] Killing old server processes..."
# Only kill the specific server task if possible, but pkill is safer for the agent
pkill -f ':server:app:run' || true 

# 2. Start Server
echo "[DEBUG] Starting Server in background..."
./gradlew :server:app:installDist
./server/app/build/install/app/bin/app > server_log.txt 2>&1 &
SERVER_PID=$!
echo "[DEBUG] Server PID: $SERVER_PID"

# 3. Install and Launch App
echo "[DEBUG] Installing App..."
./gradlew :compose-app:installDebug

echo "[DEBUG] Launching App..."
adb shell am start -n com.chriscartland.batterybutler/com.chriscartland.batterybutler.composeapp.MainActivity

# 4. Wait for App Launch & Server Ready
echo "[DEBUG] Clearing Logcat..."
adb logcat -c

echo "[DEBUG] Starting Logcat Monitor (capturing to logcat_output.txt)..."
# Capture both standard logs and our custom tag
adb logcat -v time -s BatteryButlerReceiver BatteryButlerGrpc BatteryButlerRepo BatteryButlerApp BatteryButlerServer System.out:I *:S > logcat_output.txt &
LOGCAT_PID=$!
echo "[DEBUG] Logcat Monitor PID: $LOGCAT_PID"

echo "[DEBUG] Waiting 15s for Server and App..."
sleep 15

# 5. Trigger Network Mode Switch (via Broadcast)
echo "[DEBUG] Triggering Network Switch to GRPC_LOCAL..."
adb shell am broadcast -a com.chriscartland.batterybutler.SET_NETWORK_MODE --es mode "GRPC_LOCAL"

echo "[DEBUG] Waiting 15s for data sync..."
sleep 15

# 6. Report
echo "[DEBUG] --- Server Log Tail ---"
tail -n 20 server_log.txt
echo "[DEBUG] --- Logcat Output ---"
# We expect various BatteryButler tags
cat logcat_output.txt | grep -E "BatteryButler"
echo "[DEBUG] --- End of Report ---"

# 7. Cleanup
echo "[DEBUG] Cleaning up..."
kill $SERVER_PID || true
kill $LOGCAT_PID || true
echo "[DEBUG] Done."
