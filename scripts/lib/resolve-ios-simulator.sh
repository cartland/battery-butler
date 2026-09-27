#!/bin/bash
# =============================================================================
# RESOLVE AN AVAILABLE iOS SIMULATOR
# =============================================================================
#
# PURPOSE:
#   Pick a concrete simulator device + iOS runtime to hand to xcodebuild.
#
#   Source this file, then call resolve_ios_simulator. On success it sets
#   IOS_DEVICE_NAME and IOS_RUNTIME_VERSION and returns 0. On failure it
#   prints every available device and returns 1.
#
# WHY A PREFERENCE LIST AND NOT ONE HARDCODED DEVICE:
#   Both callers used to hardcode "iPhone 16". GitHub's macos runner image
#   dropped it (the image ships iOS 26.x with iPhone 17 / 17 Pro / Air / 16e),
#   so the ios-snapshots job in auto-generate.yml failed on every run from
#   ~2026-08 onward with "No 'iPhone 16' simulator found". Nothing reported it,
#   because the automation pipeline filed no issues until PR #1485.
#
#   A single pinned model breaks every time Apple ships a new one. The list is
#   still ordered and explicit rather than "newest available", because snapshot
#   image dimensions depend on the device -- drifting automatically onto a new
#   model would silently rewrite every reference PNG.
#
# OVERRIDE:
#   IOS_SIMULATOR_DEVICE="iPhone Air" ./scripts/test-ios.sh
# =============================================================================

resolve_ios_simulator() {
    local candidates=()
    if [ -n "${IOS_SIMULATOR_DEVICE:-}" ]; then
        candidates+=("$IOS_SIMULATOR_DEVICE")
    fi
    # Newest first. Older entries keep local machines on older Xcode working.
    candidates+=("iPhone 17" "iPhone 16" "iPhone 15")

    local listing
    listing=$(xcrun simctl list devices available)

    local name version current line device
    for name in "${candidates[@]}"; do
        version=""
        current=""
        # `simctl list` groups devices under "-- iOS X.Y --" headers in
        # ascending order, so taking the last match yields the newest runtime
        # that offers this device.
        while IFS= read -r line; do
            if [[ "$line" =~ ^--\ iOS\ ([0-9]+\.[0-9]+)\ -- ]]; then
                current="${BASH_REMATCH[1]}"
            elif [[ "$line" == *"$name"* ]] && [[ "$line" == *"Shutdown"* || "$line" == *"Booted"* ]]; then
                device=$(echo "$line" | sed -E 's/^[[:space:]]+//' | sed -E 's/ \([A-F0-9-]+\).*//')
                # Exact match only: "iPhone 17" must not select "iPhone 17 Pro",
                # which renders at different dimensions.
                if [ "$device" = "$name" ]; then
                    version="$current"
                fi
            fi
        done <<<"$listing"

        if [ -n "$version" ]; then
            # Consumed by the sourcing script, so shellcheck cannot see the use.
            # shellcheck disable=SC2034
            IOS_DEVICE_NAME="$name"
            # shellcheck disable=SC2034
            IOS_RUNTIME_VERSION="$version"
            return 0
        fi
    done

    echo "ERROR: none of these simulators are available: ${candidates[*]}" >&2
    echo "Set IOS_SIMULATOR_DEVICE to choose one explicitly." >&2
    echo "Available devices:" >&2
    echo "$listing" >&2
    return 1
}
