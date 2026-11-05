#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

# These hosts compile the same UI-independent browse models used by the apps.
swiftc "$root/ios/Damoves/MoveLogic.swift" "$root/ios/Damoves/BrowseCore.swift" \
    "$root/ios/BehaviorHost/main.swift" -o "$tmp/ios-host"
(cd "$root/android" && ./gradlew --quiet :behaviorHost:installDist)
export DAMOVES_IOS_HOST="$tmp/ios-host"
export DAMOVES_ANDROID_HOST="$root/android/behaviorHost/build/install/behaviorHost/bin/behaviorHost"
node --test "$root/tests/behavior/browse.test.mjs"
