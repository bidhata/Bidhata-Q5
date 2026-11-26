# Bidhata-Q5 — TIMMKOO Q5 (MT6582, 1 GB, 480x854) — LineageOS cm-13.0
LOCAL_PATH := device/timmkoo/q5

# Platform
TARGET_BOARD_PLATFORM := mt6582
TARGET_BOOTLOADER_BOARD_NAME := mt6582
TARGET_NO_BOOTLOADER := true
TARGET_ARCH := arm
TARGET_ARCH_VARIANT := armv7-a-neon
TARGET_CPU_ABI := armeabi-v7a
TARGET_CPU_ABI2 := armeabi
TARGET_CPU_VARIANT := cortex-a7
TARGET_CPU_SMP := true
ARCH_ARM_HAVE_VFP := true
ARCH_ARM_HAVE_NEON := true
TARGET_CPU_MEMCPY_OPT_DISABLE := true
TARGET_GLOBAL_CFLAGS   += -mfpu=neon -mfloat-abi=softfp
TARGET_GLOBAL_CPPFLAGS += -mfpu=neon -mfloat-abi=softfp

# Kernel: prebuilt j5052 3.10.54 (has the gc9503v_hsd panel and GSL touch), MediaTek KERNEL header kept
TARGET_PREBUILT_KERNEL := $(LOCAL_PATH)/prebuilt/kernel
BOARD_KERNEL_BASE := 0x80000000
BOARD_KERNEL_PAGESIZE := 2048
BOARD_KERNEL_CMDLINE := androidboot.selinux=permissive
BOARD_MKBOOTIMG_ARGS := --kernel_offset 0x00008000 --ramdisk_offset 0x04000000 --tags_offset 0x00000100 --board mx2_ata_v0.3
BOARD_CUSTOM_BOOTIMG_MK := $(LOCAL_PATH)/mkmtkbootimg.mk

# Partitions (j5052 scatter + EBR1). Raw ext4 images: mtkclient writes them as-is.
BOARD_BOOTIMAGE_PARTITION_SIZE := 16777216
BOARD_RECOVERYIMAGE_PARTITION_SIZE := 16777216
BOARD_SYSTEMIMAGE_PARTITION_SIZE := 1415577600
BOARD_CACHEIMAGE_PARTITION_SIZE := 132120576
BOARD_CACHEIMAGE_FILE_SYSTEM_TYPE := ext4
BOARD_USERDATAIMAGE_PARTITION_SIZE := 1073741824
BOARD_FLASH_BLOCK_SIZE := 131072
TARGET_USERIMAGES_USE_EXT4 := true
TARGET_USERIMAGES_SPARSE_EXT_DISABLED := true

# Display
USE_OPENGL_RENDERER := true
BOARD_EGL_CFG := $(LOCAL_PATH)/configs/egl.cfg
TARGET_SCREEN_WIDTH := 480
TARGET_SCREEN_HEIGHT := 854
DEVICE_RESOLUTION := 480x854

# Memory
# MALLOC_IMPL := dlmalloc   (5.1 setting; cm-13 uses jemalloc by default)

# Wi-Fi (MT6582 CONSYS, driver built into the kernel, powered via /dev/wmtWifi)
WPA_SUPPLICANT_VERSION := VER_0_8_X
BOARD_WPA_SUPPLICANT_DRIVER := NL80211
BOARD_HOSTAPD_DRIVER := NL80211
BOARD_WLAN_DEVICE := mediatek
BOARD_WPA_SUPPLICANT_PRIVATE_LIB := lib_driver_cmd_mt66xx
BOARD_HOSTAPD_PRIVATE_LIB := lib_driver_cmd_mt66xx
WIFI_DRIVER_FW_PATH_PARAM := "/dev/wmtWifi"
WIFI_DRIVER_FW_PATH_STA := STA
WIFI_DRIVER_FW_PATH_AP := AP
WIFI_DRIVER_FW_PATH_P2P := P2P

# Bluetooth (bluedroid + MediaTek vendor lib)
BOARD_HAVE_BLUETOOTH := true
BOARD_BLUETOOTH_BDROID_BUILDCFG_INCLUDE_DIR := $(LOCAL_PATH)/bluetooth

# Recovery: TWRP stays on the device from the 5.1 tree; this tree builds the stock CM recovery
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

# Misc
USE_CAMERA_STUB := true
TARGET_SYSTEM_PROP := $(LOCAL_PATH)/system.prop
EXTENDED_FONT_FOOTPRINT := true

# SELinux: the j5052 kernel only loads policy version <= 28 (M defaults to 30)
POLICYVERS := 28

# SELinux: permissive during bring-up (cmdline + patches/system/core/0001); device policy comes after first boot
BIDHATA_BRINGUP_PERMISSIVE := true

# Bring-up: adb without RSA authorization (display may not work on first boot).
# vendor/cm/config/common.mk adds ro.adb.secure=1 for non-eng builds; product makefiles are read before this file.
ADDITIONAL_DEFAULT_PROPERTIES := $(filter-out ro.adb.secure=1,$(ADDITIONAL_DEFAULT_PROPERTIES)) ro.adb.secure=0

# Shim for MediaTek framework symbols used by the j5052 blobs (shims/q5_shim.c)
TARGET_LDPRELOAD += libq5_shim.so

# Prebuilt kernel: cm-13.0 modules still depend on the header dir a source-built kernel would create.
$(shell mkdir -p $(OUT)/obj/KERNEL_OBJ/usr)
