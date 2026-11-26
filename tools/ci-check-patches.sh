#!/bin/bash
# Check that every patch in patches/ still applies to clean LineageOS cm-13.0.
# Only the touched projects are cloned (shallow, files fetched on demand), so this is quick.
set -e
HERE=$(cd "$(dirname "$0")/.." && pwd)
WORK=${WORK:-$(mktemp -d)}
rc=0
for p in $(cd "$HERE/patches" && find . -name '*.patch' | sort); do
    proj=$(dirname "$p" | sed 's#^\./##')                 # e.g. system/bt
    repo=android_$(echo "$proj" | tr '/' '_')               # e.g. android_system_bt
    dir=$WORK/$proj
    if [ ! -d "$dir/.git" ]; then
        git clone -q --depth 1 --filter=blob:none -b cm-13.0 \
            "https://github.com/LineageOS/$repo" "$dir" || { echo "FAILED  clone $repo"; rc=1; continue; }
    fi
    if git -C "$dir" apply --check "$HERE/patches/$p" 2>"$WORK/err"; then
        echo "ok      $p"
    else
        echo "FAILED  $p"; head -5 "$WORK/err"; rc=1
    fi
done
exit $rc
