<p align="center"><img src="docs/banner.png" alt="Bidhata-Q5: a budget DAP, finally done right"></p>

# Bidhata-Q5

**A cheap little music player, finally done right.**

The TIMMKOO Q5 is one of those budget Android DAPs that costs less than a decent pair of earphones.
The hardware is honestly decent: a quad-core MediaTek chip, a proper 3.5 mm output,
Bluetooth, Wi-Fi, FM radio, a microSD slot. The software it ships with is not. It's
Android 4.4 from 2014, the Bluetooth stack only speaks SBC, and half the apps you'd want
don't install anymore.

Bidhata-Q5 is my own ROM for it. Android 6.0.1, built from source, with **LDAC and AAC
Bluetooth**, a home screen and lockscreen designed for listening rather than for a phone,
system-wide **AutoEQ**, and every rough edge I could find sanded down. It turns a
throwaway gadget into a DAP I actually reach for.

<p align="center">
  <a href="docs/screens/demo.mp4"><img src="docs/screens/demo.gif" width="300" alt="Demo of Bidhata-Q5 on a real Q5"></a><br>
  <sub>Boot, unlock, play, tune. Everything here is recorded on a real Q5. Tap for the full video.</sub>
</p>

---

## What you get

**Sound first**

- **LDAC** (990 / 660 / 330 kbps, or adaptive), **AAC** and **SBC XQ** over Bluetooth,
  added to the stock Bluetooth stack from source. Pick the codec and LDAC quality right on
  the Sound page.
- **AutoEQ built in.** Around 8,800 measured headphones from the AutoEQ database, applied
  system-wide through JamesDSP, with a separate profile for wired, Bluetooth and the speaker.
  How to use it is below.
- **Better resampling** (Android's high-quality resampler) for anything that isn't 44.1 kHz.
- **Louder speaker.** The stock volume table held the amp 11 dB below its limit. Now it's 3 dB.
- **USB DACs** work through an OTG adapter (the kernel has USB audio support).
- **HiBy Music** comes preinstalled, the last version that still runs on this hardware, set
  to its Black theme and with its own lockscreen off so it doesn't fight ours.

**Looks like a DAP, not a phone**

- **Listening Room** home: big tiles, a now-playing strip that only shows up while
  something plays, and a turning vinyl record when a track has no cover art.
- **Swipe down anywhere** on home to open notifications (the status bar on this screen is
  too thin to hit reliably).
- A **lockscreen built around the music**: the cover fades into the page, a waveform
  progress bar, big controls. When nothing plays you get a calm landscape and a big clock.
- A custom boot animation, and **you can put your own name on it** (More → Boot screen name).
- Dark, amber-accented Settings, FM Radio and Recorder to match.

**Everything a player needs, nothing it doesn't**

- **Apps**: Cool Reader (page-curl animation, Bengali and other Indic scripts rendered
  properly), VLC, Material Files, Markor, F-Droid, Calculator++, alarm clock, HeliBoard keyboard.
- **microSD and USB drives** show up in apps and on your PC, and **file transfer is on as
  soon as you plug in**: no "USB for charging" tap first.
- **Wi-Fi Share**: browse and copy music to and from the player from any browser on your network.
- **Scheduled power**: switch off at night, switch itself back on in the morning.
- **Bluetooth switches off** by itself when you plug in wired headphones, and back on when you unplug.
- The screen **stays awake while reading, watching video or recording**.
- **Updates without a PC**: More → Power & updates → Restart into TWRP, then install the zip.
- All the phone stuff (dialer, contacts, calendar, camera, browser, gallery) is gone.

**Quietly sturdy**

- Proper thermal protection: the CPU throttles and the battery is guarded when things get hot.
- 256 MB of compressed RAM so apps stay open on a 1 GB device.
- Doze, calmer Wi-Fi scanning and fewer background services for better battery.
- A unique Bluetooth and Wi-Fi address per unit (the stock firmware shipped every unit with the same one).
- Background features run as separate modules: if one ever fails, it is switched off and
  the home screen carries on.

---

## Make it yours

Everything here is done on the player itself; no PC, no root, no app to install.

### AutoEQ for your headphones

1. Home → **Sound**.
2. Choose where it applies: **Wired**, **Bluetooth** or **Speaker**. Each keeps its own profile,
   so your IEMs and your Bluetooth headphones can both be corrected.
3. Type at least two letters of your headphone model (for example `hd 650` or `moondrop`) and
   tap it in the list.
4. Done. The correction is downloaded once and applied through JamesDSP straight away.

The first search downloads the AutoEQ list (a few seconds on Wi-Fi). After that, the list and
every profile you picked are kept on the player and work offline. **AutoEQ off** switches the
correction off for the selected output; **JamesDSP…** opens the full equaliser for manual
tweaking.

### Bluetooth codec and quality

Home → **Sound** → **Bluetooth codec**: Auto (best your headphones support), LDAC, AAC or SBC XQ.
For LDAC you can also fix the quality: 990 kbps (best), 660, 330, or adaptive (drops quality
instead of dropping out when the signal is weak). The codec in use shows on the home screen
while music plays. Reconnect the headphones after changing it.

### Home screen tiles

Long-press any tile on home → **Edit tiles**. This opens `/sdcard/Bidhata/home.json` in Markor:

```json
{"tiles": [
  {"label": "Music", "icon": "music", "apps": ["com.hiby.music", "org.videolan.vlc"]},
  {"label": "Notes", "icon": "notes", "apps": ["net.gsantner.markor"]},
  {"label": "Calc",  "icon": "app",   "apps": ["org.solovyev.android.calculator"]},
  {"label": "Sound", "icon": "sound", "action": "sound"},
  {"label": "More",  "icon": "more",  "action": "more"}
]}
```

- Up to 8 tiles, in reading order.
- `apps`: package names; the first one that is installed opens. Any app works.
- `icon`: `music radio books video recorder sound files more calculator gallery notes settings
  store web`, or `app` to use the app's own icon.
- `action`: `sound` (the Sound page) or `more` (the app list) instead of `apps`.

Save and go back to home; the tiles change at once. If the file has a mistake, home shows the
default tiles with a short note saying what was wrong. **More** is always kept, so you can't
lock yourself out. Long-press → **Reset to default** brings back the original home.

### Background features

More → Power & updates → **Background features** has a switch for each of these:

- Wired headphones turn Bluetooth off (and back on when you unplug)
- Keep the screen on for books and video (Cool Reader, VLC, Recorder)
- Scheduled power on / off (switching it off keeps your times)

### More ways to make it yours

- **Boot screen name**: More → Boot screen name. Your name, drawn into the boot animation.
- **Scheduled power**: More → Power & updates. Pick the off and on times; it powers on by itself
  even from fully off.
- **File transfer by default**: if you'd rather have the phone-style "charging only" until you
  choose, run `setprop persist.bidhata.usb_files false` from a root shell.
- **Add your own app to More**: an app can put a row on the More page by declaring one intent
  filter. See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## Screenshots

| | | |
|:---:|:---:|:---:|
| <img src="docs/screens/01_home.png" width="200"><br>Home | <img src="docs/screens/02_home_playing.png" width="200"><br>Home, playing | <img src="docs/screens/03_lock_idle.png" width="200"><br>Lockscreen |
| <img src="docs/screens/04_lock_playing.png" width="200"><br>Lockscreen, playing | <img src="docs/screens/05_hiby.png" width="200"><br>HiBy Music | <img src="docs/screens/06_more.png" width="200"><br>More |
| <img src="docs/screens/07_sound.png" width="200"><br>Sound | <img src="docs/screens/08_autoeq.png" width="200"><br>AutoEQ search | <img src="docs/screens/09_power.png" width="200"><br>Scheduled power |
| <img src="docs/screens/10_reader.png" width="200"><br>Cool Reader | <img src="docs/screens/11_recorder.png" width="200"><br>Recorder | <img src="docs/screens/12_fm.png" width="200"><br>FM Radio |
| <img src="docs/screens/13_settings.png" width="200"><br>Settings | <img src="docs/screens/14_about.png" width="200"><br>About | <img src="docs/screens/bootanimation.gif" width="200"><br>Boot |

---

## The device

| | |
|---|---|
| Player | TIMMKOO Q5 (board `j5052`, also sold under other names) |
| Chip | MediaTek MT6582, 4 × Cortex-A7 at 1.3 GHz, Mali-400 GPU |
| Memory | 1 GB RAM, 8 GB storage, microSD |
| Screen | 480 × 800 touchscreen |
| Wireless | Bluetooth 4.0, 2.4 GHz Wi-Fi, FM radio (your wired earphones are the antenna) |
| Stock software | Android 4.4.2 |
| Bidhata-Q5 | Android 6.0.1 (LineageOS cm-13.0 base) |

---

## What works

| | |
|---|---|
| Wired audio, speaker | ✅ |
| Bluetooth LDAC / AAC / SBC XQ / SBC | ✅ |
| AutoEQ, JamesDSP | ✅ |
| Wi-Fi, Wi-Fi Share | ✅ |
| FM radio | ✅ opens and tunes with wired earphones plugged in |
| Recorder | ✅ |
| Charging (also when switched off) | ✅ |
| Scheduled power on/off | ✅ |
| microSD, USB drives (OTG), file transfer to a PC | ✅ |
| USB DAC (OTG) | should work, test welcome |
| SELinux | permissive for now |

Found something that doesn't? Open an issue with what you did and what happened.

---

## Install

**[The step-by-step install guide](docs/INSTALL.md)** walks you through it. It's mostly
double-clicking scripts and waiting. The short version:

- **From stock firmware** (once, needs a Windows PC, about an hour): download
  `Bidhata-Q5-<version>-pc-install.zip` from Releases, then run `setup-mtkclient.bat`,
  `backup.bat` and `install.bat`. Every write is read back and checked. TWRP comes with it.
- **Updates** (no PC): copy `Bidhata-Q5-<version>-update.zip` to the player, tap
  **More → Power & updates → Restart into TWRP**, and install the zip. Music, apps and settings
  stay.
- **Changed your mind?** `restore-stock.bat` puts your backup back.

> The first install **erases the player's internal storage** (the microSD card is safe), so
> copy your music off first. The scripts never touch the bootloader, so a failed attempt can
> always be retried. Things still go wrong sometimes with flashing; I'm not responsible for
> bricked players, but I'll gladly help you unbrick one.

---

## Build it yourself

Two commands, once you have Docker, ~60 GB of disk and TIMMKOO's **T-ROM V2.3** zip
(their Android 5.0 package for the Q5, from timmkoo.de; the MediaTek drivers come from it):

```bash
mkdir bidhata && cd bidhata
curl -fsSLO https://raw.githubusercontent.com/bidhata/Bidhata-Q5/main/tools/setup.sh
bash setup.sh ~/Downloads/T-ROM_V.2.3_without_Flashtool.zip   # sync, patch, drivers, apps
device/timmkoo/q5/tools/build.sh                                # boot.img + system.img
```

The build runs in a small Ubuntu 14.04 container, the host LineageOS cm-13.0 expects, so your
own machine stays untouched. The patches in `patches/` are plain `git diff`s, easy to read:

- `patches/system/bt`: LDAC (with adaptive bitrate), AAC and SBC XQ in bluedroid.
- `patches/frameworks/base`: the power-on alarm behind scheduled power.
- `patches/system/core`: init fixes for this board.

Want to change the home screen or add a feature? [CONTRIBUTING.md](CONTRIBUTING.md) has a
two-minute edit, push, reboot loop that doesn't need a full build. Every push and pull request
is checked automatically, and new releases are built from tags ([docs/RELEASING.md](docs/RELEASING.md)).

---

## Thanks

Bidhata-Q5 stands on a lot of other people's work. Thank you to:

**The system**
- **LineageOS** (cm-13.0) and the **Android Open Source Project**, which all of this sits on.
- **TheMuppets** and the Android One (sprout) maintainers, for the MT6582 pieces.
- **TIMMKOO**, for the player and their T-ROM package the drivers and kernel come from.
- **TWRP** (TeamWin), for the recovery.
- **mtkclient** by Bjoern Kerler, which makes installing possible at all, and **Zadig / libwdi**
  by Pete Batard for the USB driver.

**The sound**
- **JamesDSP** by James Fung, the system-wide audio engine.
- **AutoEQ** by Jaakko Pasanen, for the headphone measurements and corrections.
- **libldac** (Sony, Apache 2.0) for LDAC, and the **Fraunhofer FDK AAC** encoder in Android.

**The look**
- **GNOME** and its app authors for the icons (CC-BY-SA 3.0 / their own licences; details in
  `apps/BidhataLauncher/ICONS.md`).
- **Mozilla**, for the root certificate list that lets a 2015 Android talk to today's web.

**The apps that make it a complete player**
- **HiBy Music** (HiBy), **VLC** (VideoLAN), **Cool Reader** (Vadim Lopatin and contributors),
  **HeliBoard** (Helium314 and contributors), **Material Files** (Hai Zhang),
  **Markor** (Gregor Santner), **F-Droid**, and **Calculator++** (Sergey Solovyev).

And the people who tear these cheap players apart and write it up. You know who you are.

## Licence

My own code in this tree is Apache 2.0 (see [LICENSE](LICENSE)). Everything else keeps its own
licence. JamesDSP (`prebuilt/jamesdsp`) is GPL-3.0; its source is at
<https://github.com/james34602/JamesDSPManager>. Proprietary
MediaTek files and third-party APKs are not in this repo: `extract-files.sh` takes the drivers
from TIMMKOO's T-ROM package and `tools/fetch-apps.sh` downloads the apps from their official
sources.

---

Made with patience and a lot of reboots by **Krish Paul**.
If it brought your Q5 back to life, a ⭐ helps other owners find it.
