# Installing Bidhata-Q5

This guide takes you from a stock TIMMKOO Q5 to Bidhata-Q5, one step at a time. No Android
experience needed. If something goes differently from what's written here, stop and open an
Issue on GitHub. Don't improvise; most problems are easy to fix if you stop early.

## Which part do I need?

| Your player runs | What to do | Needs a PC? |
|---|---|---|
| The stock TIMMKOO firmware | **Part A**: first install, once | Yes, Windows |
| Bidhata-Q5 already | **Part B**: update with TWRP | No |
| Bidhata-Q5, and you want stock back | **Part C** | Yes, Windows |
| TIMMKOO's T-ROM 5.0, or anything else | Not supported yet: open an Issue first | |

`backup.bat` checks which bootloader your player has, and `install.bat` stops if it's one
Bidhata hasn't been tested with. That's on purpose.

Why a PC the first time? Bidhata needs a bigger system partition than stock has, so the
first install also rewrites the player's partition table. That can only be done over USB from
a PC. After that, every update happens on the player itself.

---

## Part A: First install (about 1 hour)

### What you need

- A **Windows 10 or 11** PC with about 5 GB of free space.
- A **USB data cable**. Some cheap cables only charge; if the PC never notices the player,
  try another cable first.
- The player charged **above 50%**.
- A **paperclip or SIM pin** for the small reset hole on the player.
- About an hour, most of it waiting.

> **This erases the player's internal storage**: music copied to the player itself, apps and
> settings. **The microSD card is not touched.** Connect the player to the PC now and copy
> anything you want to keep from its internal storage.

### A1. Download and unpack

1. From the project's **Releases** page, download `Bidhata-Q5-<version>-pc-install.zip`.
2. Unpack it to a short path **without spaces**, like `C:\Q5`. You should end up with
   `C:\Q5\Bidhata-Q5-<version>\install.bat` and friends. (On the Desktop or in Downloads the
   path may contain your user name with a space in it, which the tools can't handle.)

### A2. Install Python

1. Download Python from <https://www.python.org/downloads/> and run the installer.
2. On the first screen, **tick "Add python.exe to PATH"**, then click *Install Now*.

### A3. Set up mtkclient

mtkclient is the open-source tool that talks to the player's chip over USB.

1. In the unpacked folder, double-click **`setup-mtkclient.bat`**.
2. It downloads mtkclient and its parts. Wait for *Done*.

### A4. Install the USB driver (Zadig)

Windows has no driver for the player's flashing mode, so we give it one.

1. Download **Zadig** from <https://zadig.akeo.ie/> and run it (no install needed).
2. Menu **Device → Load Preset Device**, and pick **`q5-usb-driver.cfg`** from the unpacked
   folder.
3. In the box next to the green arrow, choose **WinUSB**.
4. Click **Install Driver** and wait for the success message. Close Zadig.

You only do this once per PC.

### A5. Back up the player

The backup is your way back to stock, and it keeps the parts of memory that are unique to your
player (Wi-Fi and Bluetooth calibration). **Don't skip it.**

1. Plug the player into the PC.
2. Double-click **`backup.bat`** and press a key when it asks.
3. When it says so, **press the reset hole** once with the paperclip. The player restarts,
   and in its first seconds the script catches it. The screen may stay black; that's normal.
4. Reading takes 10–15 minutes. Don't touch the cable.
5. When it says *Backup done*, unplug. The player starts normally (or press reset).
6. **Copy the `backup` folder somewhere else as well**: a USB stick or a cloud drive.

Why the reset hole? The player only listens for the PC during the first few seconds after it
starts. The reset hole restarts it on cue, whether it was on, off, or stuck.

### A6. Install

1. Double-click **`install.bat`**. Read the warning, type **YES**, press Enter.
2. Same as the backup: cable plugged in, press a key, then press the reset hole.
3. Writing and checking takes about 25 minutes. Don't touch the cable.
4. When it says ***Installed and verified***, unplug and press the reset hole to start.

If it says something didn't check out: don't worry. The part of the player that starts it
(the bootloader) is never touched, so you can run `install.bat` again, even if the player
no longer starts. The reset hole always gets the PC in.

### A7. First start

- The first start takes **about 5 minutes** (Android prepares every app once). Later starts
  are much faster.
- Bundled apps (HiBy Music, VLC, Cool Reader and others) install themselves in the first
  minute after the home screen appears.
- Pair your Bluetooth headphones again; the player's Bluetooth identity changed.
- TWRP (the recovery tool for updates) is now installed too.

---

## Part B: Updating (15 minutes, no PC)

1. Download `Bidhata-Q5-<version>-update.zip` from **Releases**.
2. Copy it to the player: internal storage or the microSD card, any folder.
3. On the player: **More → Power & updates → Restart into TWRP**.
   (On 0.4.7 and earlier that button doesn't exist yet: connect to a PC and run
   `adb reboot recovery`.)
4. In TWRP tap **Install**, find the zip, tap it, and **swipe to confirm**.
5. Wait for *Done* (5–10 minutes: it checks, writes, then checks again), then tap **Reboot System**.

Your music, apps and settings stay. (The update zip is new with this guide; if anything
looks off, report it in the Issues.) The installer checks the zip before it writes anything,
reads everything back afterwards, and refuses to run on a player that still has the stock
layout. If it reports an error, read what it says. If it says *Do NOT reboot*, flash the zip
again first.

---

## Part C: Back to stock

You need the `backup` folder from Part A5, made while the player ran stock.

> This part is new and hasn't been run on a real player yet. If you try it, please report
> back in the Issues either way.

**No backup, or the restore didn't work?** The community tool **Innioasis Updater CE**
installs and restores official firmware on Innioasis and TIMMKOO players, on Windows, macOS
and Linux. Its getting-started guide is here: <https://innioasis.app/guide.html#platform>. It puts stock firmware back without
needing your backup (your player's own calibration is not restored that way, which is why
`backup.bat` is still worth running first).

1. Put the `backup` folder back into the unpacked package folder (next to `install.bat`).
2. Plug the player in, double-click **`restore-stock.bat`**, type **YES**, and press the
   reset hole when it asks.
3. When it says *Restored*, unplug and press the reset hole to start.
4. Once it has started: **Settings → Backup & reset → Factory data reset**, so the internal
   storage gets its full size back.

---

## Troubleshooting

**Pressing reset does nothing, or the script never finishes**
- Press the reset hole once more; sometimes the first press is too early.
- Try another cable, and a USB port directly on the PC (not a hub or a front-panel port).
- Did Zadig say the driver installed? Run A4 again if unsure.
- Close the window, unplug, plug in again and rerun the script.

**The player shows the charging screen instead**
Press the reset hole again while the script is waiting.

**The player looks dead or sits on the logo after a failed attempt**
Nothing is broken: the bootloader was never touched. Run `install.bat` again and use the
reset hole. It works no matter what state Android is in.

**"This folder's path contains a space"**
Move the whole folder to `C:\Q5` and run the script from there.

**First start is stuck on the boot animation for more than 15 minutes**
Hold Power 10 seconds to restart it. If it happens again, run `install.bat` again; it
clears the old data that sometimes confuses a first start.

**TWRP says "this player still has the stock partition layout"**
The update zip only updates. Do Part A first.

**Nothing here helps and you just want the stock firmware back**
Use **Innioasis Updater CE** to reinstall the official TIMMKOO firmware:
<https://innioasis.app/guide.html#platform>.

Still stuck? Open an Issue on GitHub with what you did and the `.log` file from the folder.
