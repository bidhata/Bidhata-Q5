#!/bin/bash
# One-time setup of a Bidhata-Q5 source tree.
#
#   mkdir bidhata && cd bidhata
#   curl -fsSLO https://raw.githubusercontent.com/bidhata/Bidhata-Q5/main/tools/setup.sh
#   bash setup.sh /path/to/T-ROM_V.2.3_without_Flashtool.zip
#
# Needs: git, repo, curl, python3, unzip, debugfs (e2fsprogs) and ~60 GB of free disk.
# The T-ROM zip is TIMMKOO's own Android 5.0 package (timmkoo.de, for Q5 owners); the
# MediaTek driver files are taken from it. Run it again any time: every step is idempotent.
set -e
TROM=${1:?usage: setup.sh <T-ROM_V.2.3 zip>}
TROM=$(cd "$(dirname "$TROM")" && pwd)/$(basename "$TROM")
TOP=$(pwd)

step() { echo; echo "==> $*"; }

step "LineageOS cm-13.0 source (the long part: ~30 GB, use a good connection)"
command -v repo >/dev/null || { echo "install 'repo' first: https://source.android.com/setup/develop#installing-repo" >&2; exit 1; }
[ -d .repo ] || repo init -q -u https://github.com/LineageOS/android.git -b cm-13.0 --depth=1
mkdir -p .repo/local_manifests
curl -fsSL -o .repo/local_manifests/bidhata.xml \
    https://raw.githubusercontent.com/bidhata/Bidhata-Q5/main/tools/bidhata.xml
repo sync -c -j8 --no-tags --no-clone-bundle --optimized-fetch

step "Bidhata patches (Bluetooth codecs, init, sepolicy, alarm, FM)"
bash device/timmkoo/q5/patches/apply.sh

step "MediaTek driver files from the T-ROM zip"
(cd device/timmkoo/q5 && ./extract-files.sh "$TROM")

step "Bundled apps (checked against tools/apps.lock)"
bash device/timmkoo/q5/tools/fetch-apps.sh

step "CyanogenMod's ambient SDK (the build's own download breaks on old TLS)"
A=out/target/common/obj/JAVA_LIBRARIES/ambientsdk_intermediates
mkdir -p "$A"
[ -f "$A/com.cyngn.ambient.ambientsdk-1.6.0-147069776-39fe2d07.aar" ] || curl -fsSL -o \
    "$A/com.cyngn.ambient.ambientsdk-1.6.0-147069776-39fe2d07.aar" \
    https://repo1.maven.org/maven2/com/cyngn/ambient/ambientsdk/1.6.0-147069776-39fe2d07/ambientsdk-1.6.0-147069776-39fe2d07.aar

echo
echo "All set. Build with:  device/timmkoo/q5/tools/build.sh"
