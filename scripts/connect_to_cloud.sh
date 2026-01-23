#!/bin/bash
set -e

echo "Connecting App to Cloud..."

# 1. Set Server URL to Production (Explicitly, although default should work)
# This uses the default production URL from SharedServerConfig.
# To override, uncomment and set specific URL.
# URL="http://battery-butler-nlb-847feaa773351518.elb.us-west-1.amazonaws.com:80"
# adb shell am broadcast \
#   -n com.chriscartland.batterybutler/com.chriscartland.batterybutler.composeapp.ServerUrlReceiver \
#   -a com.chriscartland.batterybutler.SET_SERVER_URL \
#   -e url "$URL"
#
# For now, we rely on the default production URL by resetting any overrides.
adb shell am broadcast \
  -n com.chriscartland.batterybutler/com.chriscartland.batterybutler.composeapp.ServerUrlReceiver \
  -a com.chriscartland.batterybutler.SET_SERVER_URL \
  --esn url

# 2. Set Network Mode to GRPC_LOCAL (Real Network)
adb shell am broadcast \
  -n com.chriscartland.batterybutler/com.chriscartland.batterybutler.composeapp.debug.DebugNetworkReceiver \
  -a com.chriscartland.batterybutler.SET_NETWORK_MODE \
  --es mode GRPC_LOCAL

echo "App configured for Cloud Connectivity."
