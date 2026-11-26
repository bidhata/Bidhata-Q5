#!/bin/bash
# Compile-check the Bidhata apps against the public Android 6.0 SDK (API 23), no ROM tree needed.
# Catches broken Java, XML and resource references in a minute. Used by the CI workflow.
#   ANDROID_JAR=/path/to/platforms/android-23/android.jar AAPT=/path/to/aapt tools/ci-check-apps.sh
set -e
HERE=$(cd "$(dirname "$0")/.." && pwd)
: "${ANDROID_JAR:?set ANDROID_JAR to platforms/android-23/android.jar}"
: "${AAPT:?set AAPT to build-tools/<ver>/aapt}"
WORK=$(mktemp -d); trap 'rm -rf "$WORK"' EXIT
rc=0
for app in "$HERE"/apps/*/; do
    name=$(basename "$app")
    [ -f "$app/AndroidManifest.xml" ] || continue
    mkdir -p "$WORK/$name/gen" "$WORK/$name/classes"
    if "$AAPT" package -f -m -S "$app/res" -M "$app/AndroidManifest.xml" \
            -I "$ANDROID_JAR" -J "$WORK/$name/gen" >"$WORK/$name/aapt.log" 2>&1 &&
       javac -nowarn -encoding UTF-8 -source 8 -target 8 -bootclasspath "$ANDROID_JAR" \
            -d "$WORK/$name/classes" $(find "$WORK/$name/gen" "$app/src" -name '*.java') \
            >"$WORK/$name/javac.log" 2>&1; then
        echo "ok      $name"
    else
        echo "FAILED  $name"; cat "$WORK/$name/aapt.log" "$WORK/$name/javac.log" | grep -v "^warning" | head -30
        rc=1
    fi
done
exit $rc
