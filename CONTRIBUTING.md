# Contributing to Bidhata-Q5

Thanks for wanting to help. You don't need to build the whole ROM to contribute, and you
don't need to be an Android expert. Pick the path that fits what you want to do.

## 1. Just report something

Open an issue and tell me what you did, what you expected and what happened. If you can,
add a log:

```bash
adb logcat -d > log.txt          # everything since boot
adb shell dmesg > kernel.txt     # if the player rebooted or froze
```

Bluetooth problems: say which headphones and which codec the home screen showed
(LDAC / AAC / SBC XQ).

## 2. Work on the apps (home screen, lockscreen, Sound page, Recorder)

They live in `apps/` and are plain Java with Android XML layouts. The fast loop, once you
have a source tree (step 3):

```bash
device/timmkoo/q5/tools/build.sh BidhataLauncher       # a minute or two
adb root; adb remount
adb push out/target/product/q5/system/priv-app/BidhataLauncher/BidhataLauncher.apk \
         /system/priv-app/BidhataLauncher/
adb reboot
```

Things to keep in mind:

- The screen is **480 × 800 at hdpi, so 320 × 533 dp**. Size things in dp and check that
  nothing falls off the bottom.
- It's a 2014 chip with 1 GB of RAM. Animations should run only on interaction and never loop
  in the background; long work goes off the main thread.
- Vector icons: Android 6 has no `fillType` and chokes on compact SVG arcs. Convert icons with
  `tools/svg2vd.py`, it handles both.

### Extending the launcher without touching it

- **Home tiles** come from `/sdcard/Bidhata/home.json` (long-press a tile on the player to edit
  it). Any installed app can be a tile; no code needed.
- **A row on the More page**: give one of your app's activities this intent filter. Its label is
  the row text, and a tap opens it. Nothing of yours runs inside the launcher.

  ```xml
  <intent-filter>
      <action android:name="org.bidhata.launcher.MORE_ROW" />
      <category android:name="android.intent.category.DEFAULT" />
  </intent-filter>
  ```
- **A background feature inside the launcher** (like the wired-headphone Bluetooth switch) is a
  `Module`: implement the five methods, add it to `Modules.ALL`, and it gets its own switch under
  Background features. A module that throws on start is left off; the home screen carries on.

## 3. Build the whole ROM

You need Linux (or WSL2), ~60 GB of disk, 16 GB of RAM is comfortable, Docker, and TIMMKOO's
**T-ROM V2.3** zip (their Android 5.0 package, from timmkoo.de) for the MediaTek drivers.

```bash
mkdir bidhata && cd bidhata
curl -fsSLO https://raw.githubusercontent.com/bidhata/Bidhata-Q5/main/tools/setup.sh
bash setup.sh ~/Downloads/T-ROM_V.2.3_without_Flashtool.zip    # once; takes a while
device/timmkoo/q5/tools/build.sh                                 # boot.img + system.img
```

`setup.sh` syncs LineageOS cm-13.0 plus this tree, applies the patches in `patches/`,
pulls the MediaTek files out of the T-ROM zip (no root needed), downloads the bundled apps
and checks their hashes. `build.sh` builds inside a small Ubuntu 14.04 container, the host
cm-13.0 expects, so your own machine stays as it is.

## 4. Send changes

- Fork, branch, pull request. Small, focused PRs are much easier to review.
- Every PR runs the **Checks** workflow: scripts, app links, the apps compiling against
  Android 6.0, and the patches applying to LineageOS. You can run the same locally with
  `tools/ci-check-apps.sh` and `tools/ci-check-patches.sh`.
- Patches against LineageOS projects go in `patches/<project path>/NNNN-what-it-does.patch`
  as plain `git diff` output; `patches/apply.sh` applies them.
- Write comments and commit messages for the next person: what and why, in plain words.

## What never goes into the repo

- Proprietary MediaTek files (`extract-files.sh` recreates them from T-ROM).
- Third-party APKs (`tools/fetch-apps.sh` downloads them; add new ones to `tools/apps.lock`
  with their sha256).
- Images, logs, personal data from your player.

`.gitignore` already covers the generated files.
