#!/bin/bash
# Build the "PC install" download: Windows scripts + images for the first install from stock.
#   tools/make-pc-package.sh <boot.img> <system.img> <twrp-q5.img> <T-ROM zip> <version> <out.zip>
# Needs make_ext4fs (from the ROM build: out/host/linux-x86/bin) on PATH.
set -e
BOOT=$1; SYS=$2; TWRP=$3; TROM=$4; VER=$5; OUT=$6
[ -n "$OUT" ] || { echo "usage: $0 <boot.img> <system.img> <twrp-q5.img> <T-ROM zip> <version> <out.zip>" >&2; exit 1; }
command -v make_ext4fs >/dev/null || { echo "make_ext4fs not on PATH (source build/envsetup.sh, or add out/host/linux-x86/bin)" >&2; exit 1; }
HERE=$(cd "$(dirname "$0")" && pwd)
W=$(mktemp -d); trap 'rm -rf "$W"' EXIT
P=$W/Bidhata-Q5-$VER
mkdir -p "$P"
cp "$HERE"/windows/*.bat "$HERE"/windows/q5-usb-driver.cfg "$P/"
ln -s "$(realpath "$BOOT")" "$P/boot.img"; ln -s "$(realpath "$SYS")" "$P/system.img"
ln -s "$(realpath "$TWRP")" "$P/twrp-q5.img"
# Bidhata's partition table is TIMMKOO's own Android 5 one (bigger system partition)
unzip -p "$TROM" EBR1 > "$P/ebr1-q5.bin"
[ "$(sha256sum < "$P/ebr1-q5.bin" | cut -c1-64)" = d3362e430dd0209376f3978633ff0ea2cc1402885341778f3d4aa8ca0365c8a2 ] \
    || { echo "EBR1 in $TROM is not the expected one" >&2; exit 1; }
head -c 1048576 /dev/zero > "$P/zeros-1m.bin"
# empty cache and data in the stock layout, for restore-stock.bat
make_ext4fs -l $((0x7E00000)) -a cache "$P/stock-cache-empty.img" >/dev/null
make_ext4fs -l $((0x1F300000)) -a data "$P/stock-userdata-empty.img" >/dev/null
cp "$HERE/../docs/INSTALL.md" "$P/README-FIRST.md"
(cd "$P" && sha256sum -- *.img *.bin > SHA256SUMS.txt)
rm -f "$OUT"
(cd "$W" && zip -q -r -9 "$(realpath -m "$OUT")" "Bidhata-Q5-$VER")
echo "$OUT: $(du -h "$OUT" | cut -f1)"
