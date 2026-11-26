# Releasing

Push a tag, get a release:

```bash
git tag -a v0.4.8 -m "Bidhata-Q5 0.4.8: what changed, in a line or two"
git push origin v0.4.8
```

The **Release** workflow then builds `boot.img` and `system.img` with that version stamped in
(`ro.bidhata.version=Bidhata-Q5-0.4.8`), and publishes a GitHub Release with:

- `Bidhata-Q5-0.4.8-update.zip`: flash in TWRP, for players already on Bidhata;
- `Bidhata-Q5-0.4.8-pc-install.zip`: Windows scripts + images for the first install from stock;
- `twrp-q5.img` and `SHA256SUMS`. Release notes are generated from the commits since the last tag.

Every push and pull request also runs **Checks** on GitHub's own machines (a few minutes):
scripts, app links, the apps compiling against Android 6.0, and every patch applying to
LineageOS. Fix those before tagging.

## Why a self-hosted runner

A full Android 6 build needs ~55 GB of disk and several hours. GitHub's free machines give
~14 GB and a 6-hour limit, and the MediaTek files can't live in a public repo. So releases
build on your own PC, which already has the source tree, the T-ROM zip and a warm ccache.
With ccache a rebuild after small changes takes minutes, not hours.

Only tags trigger it. Pull requests from forks never run on your machine.

## One-time setup

1. **A source tree** set up with `tools/setup.sh` (see CONTRIBUTING.md). The device folder
   `device/timmkoo/q5` must be a git clone of this repo (setup.sh does that).

2. **Register the runner.** On GitHub: *Settings → Actions → Runners → New self-hosted runner →
   Linux x64*, and run the commands it shows on the build PC (WSL2 works). When it asks for
   labels, add `bidhata`. Install it as a service so it survives reboots:
   `sudo ./svc.sh install && sudo ./svc.sh start`.

3. **Tell the workflow where things are.** *Settings → Secrets and variables → Actions →
   Variables*:

   | Variable | Example | |
   |---|---|---|
   | `BIDHATA_SRC` | `/home/me/bidhata` | top of the source tree |
   | `BIDHATA_TROM_ZIP` | `/home/me/T-ROM_V.2.3_without_Flashtool.zip` | MediaTek files, used when missing |
   | `BIDHATA_TWRP_IMG` | `/home/me/twrp-q5.img` | TWRP image, goes into the PC package |
   | `BIDHATA_BUILD_CMD` | | optional; default is `device/timmkoo/q5/tools/build.sh` (Docker). Set it if you build another way, e.g. in a chroot. It must pass `BIDHATA_VERSION` through. |

4. Push a test tag (`v0.0.1-test`), watch it in the *Actions* tab, then delete that release and tag.

## Before you tag

- Flash the build on a real Q5 and use it for a bit. The checks can't hear audio.
- Flash it with the **update zip** in TWRP, the way users will.
- Note anything users must do differently (a clean flash, a re-pair) in the tag message.
