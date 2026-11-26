#!/bin/bash
# Build Bidhata-Q5 inside Docker (Ubuntu 14.04 + OpenJDK 7, see tools/Dockerfile).
# Run from the top of the source tree.
#
#   device/timmkoo/q5/tools/build.sh                 boot.img + system.img
#   device/timmkoo/q5/tools/build.sh BidhataLauncher just one module (fast, for app work)
set -e
TOP=$(pwd)
[ -f build/envsetup.sh ] || { echo "run this from the top of the source tree" >&2; exit 1; }
docker build -q -t bidhata-q5-build device/timmkoo/q5/tools >/dev/null
TARGETS=${*:-bootimage systemimage}
docker run --rm -e BIDHATA_VERSION -v "$TOP":/src bidhata-q5-build bash -c \
    "source build/envsetup.sh >/dev/null && lunch cm_q5-userdebug >/dev/null && make -j\$(nproc) $TARGETS"
echo
echo "Images: out/target/product/q5/boot.img and system.img"
