#!/bin/bash
# Download the bundled apps listed in tools/apps.lock into prebuilt/preinstall and check them.
# "fdroid" means: try the F-Droid repo, then its archive (old versions move there).
set -e
HERE=$(cd "$(dirname "$0")/.." && pwd)
DEST=$HERE/prebuilt/preinstall
mkdir -p "$DEST"
fail=0
while read -r name sum url; do
    case "$name" in ''|\#*) continue ;; esac
    f=$DEST/$name
    if [ -f "$f" ] && echo "$sum  $f" | sha256sum -c --quiet - 2>/dev/null; then
        echo "ok      $name"; continue
    fi
    if [ "$url" = fdroid ]; then
        urls="https://f-droid.org/repo/$name https://f-droid.org/archive/$name"
    else
        urls=$url
    fi
    got=0
    for u in $urls; do
        if curl -fsSL --retry 2 -o "$f.part" "$u"; then got=1; break; fi
    done
    if [ $got = 1 ] && echo "$sum  $f.part" | sha256sum -c --quiet - 2>/dev/null; then
        mv "$f.part" "$f"; echo "fetched $name"
    else
        rm -f "$f.part"; echo "FAILED  $name (download or checksum)" >&2; fail=1
    fi
done < "$HERE/tools/apps.lock"
exit $fail
