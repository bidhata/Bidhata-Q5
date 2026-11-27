# TWRP for the Q5

You don't need to install TWRP separately: the first install (`install.bat` in the PC
package, see [docs/INSTALL.md](../docs/INSTALL.md)) puts it in place. From then on
**More → Power & updates → Restart into TWRP** gets you there. There's no known key combo
from power-off.

Want the stock firmware back instead? See *Back to stock* in the install guide, or use the
community [Innioasis Updater CE](https://innioasis.app/guide.html#platform), which restores
official firmware on Innioasis and TIMMKOO players.

For the curious: mtkclient can't address partitions by name on this player. Recovery is
written by address, `wo 0x3580000 0x1000000 twrp-q5.img`.

## How it is built

TWRP for this device comes from the **android-5.1** branch of TWRP on top of a LineageOS
**cm-12.1** tree, using the same prebuilt 3.10 kernel as the ROM. It is a separate tree from
the ROM (cm-13.0) because that is the TWRP branch that boots reliably on this kernel.

The files in this folder are the device side:

- `root/etc/twrp.fstab`: partitions as TWRP sees them (MediaTek `/dev/bootimg`, `/dev/recovery`
  and friends, `/data`, `/system`, the microSD card).
- `root/init.recovery.mt6582.rc`: brings up USB (adb) in recovery.

Add this to the cm-12.1 tree's `BoardConfig.mk` for the device, then `make recoveryimage`:

```make
RECOVERY_VARIANT := twrp
TARGET_RECOVERY_FSTAB := $(LOCAL_PATH)/rootdir/fstab.mt6582
BOARD_HAS_NO_SELECT_BUTTON := true
RECOVERY_GRAPHICS_USE_LINELENGTH := true
RECOVERY_SDCARD_ON_DATA := true
TW_THEME := portrait_hdpi
TW_BRIGHTNESS_PATH := /sys/class/leds/lcd-backlight/brightness
TW_MAX_BRIGHTNESS := 255
TW_DEFAULT_BRIGHTNESS := 160
TW_NO_USB_STORAGE := true
TW_EXCLUDE_SUPERSU := true
TW_INCLUDE_CRYPTO := false
TW_EXTERNAL_STORAGE_PATH := "/external_sd"
TW_EXTERNAL_STORAGE_MOUNT_POINT := "external_sd"
```

TWRP is GPL, so the prebuilt image can be shared freely.
