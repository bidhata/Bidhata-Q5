#!/bin/bash
# Pack boot.img + system.img into a TWRP-flashable update zip.
#   tools/make-update-zip.sh <boot.img> <system.img> <version> <out.zip>
set -e
BOOT=$1; SYS=$2; VER=$3; OUT=$4
[ -f "$BOOT" ] && [ -f "$SYS" ] && [ -n "$VER" ] && [ -n "$OUT" ] || {
    echo "usage: $0 <boot.img> <system.img> <version> <out.zip>" >&2; exit 1; }
HERE=$(cd "$(dirname "$0")" && pwd)
W=$(mktemp -d); trap 'rm -rf "$W"' EXIT
mkdir -p "$W/META-INF/com/google/android"
cp "$HERE/update-zip/update-binary" "$W/META-INF/com/google/android/update-binary"
echo "# Bidhata-Q5: the real installer is update-binary" > "$W/META-INF/com/google/android/updater-script"
ln -s "$(realpath "$BOOT")" "$W/boot.img"; ln -s "$(realpath "$SYS")" "$W/system.img"
echo "$VER" > "$W/version"
(cd "$W" && md5sum boot.img system.img | sed 's/  / /' > md5sums \
         && for f in boot.img system.img; do echo "$f $(stat -Lc %s $f)"; done > sizes)
rm -f "$OUT"
# compressed: smaller download, and TWRP maps the whole zip into memory. The installer
# streams each image out of it, so nothing is unpacked to storage or RAM.
(cd "$W" && zip -q -X -9 "$(realpath -m "$OUT")" META-INF/com/google/android/update-binary \
     META-INF/com/google/android/updater-script version md5sums sizes boot.img system.img)
echo "$OUT: $(du -h "$OUT" | cut -f1)"
