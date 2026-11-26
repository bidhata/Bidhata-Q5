# Bidhata-Q5 device makefile
LOCAL_PATH := device/timmkoo/q5

# Dalvik heap: the phone-hdpi-512 profile, but 64m per app instead of 48m (HiBy with large covers)
PRODUCT_PROPERTY_OVERRIDES += \
    dalvik.vm.heapstartsize=5m \
    dalvik.vm.heapgrowthlimit=64m \
    dalvik.vm.heapsize=128m \
    dalvik.vm.heaptargetutilization=0.75 \
    dalvik.vm.heapminfree=512k \
    dalvik.vm.heapmaxfree=2m

DEVICE_PACKAGE_OVERLAYS += $(LOCAL_PATH)/overlay

PRODUCT_AAPT_CONFIG := normal hdpi
PRODUCT_AAPT_PREF_CONFIG := hdpi
PRODUCT_CHARACTERISTICS := nosdcard

# Ramdisk
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/rootdir/fstab.mt6582:root/fstab.mt6582 \
    $(LOCAL_PATH)/rootdir/init.mt6582.rc:root/init.mt6582.rc \
    $(LOCAL_PATH)/rootdir/init.mt6582.usb.rc:root/init.mt6582.usb.rc \
    $(LOCAL_PATH)/rootdir/ueventd.mt6582.rc:root/ueventd.mt6582.rc

# Permissions (no telephony, no camera, no sensors)
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/handheld_core_hardware.xml:system/etc/permissions/handheld_core_hardware.xml \
    frameworks/native/data/etc/android.hardware.bluetooth.xml:system/etc/permissions/android.hardware.bluetooth.xml \
    frameworks/native/data/etc/android.hardware.bluetooth_le.xml:system/etc/permissions/android.hardware.bluetooth_le.xml \
    frameworks/native/data/etc/android.hardware.wifi.xml:system/etc/permissions/android.hardware.wifi.xml \
    frameworks/native/data/etc/android.hardware.wifi.direct.xml:system/etc/permissions/android.hardware.wifi.direct.xml \
    frameworks/native/data/etc/android.hardware.touchscreen.multitouch.distinct.xml:system/etc/permissions/android.hardware.touchscreen.multitouch.distinct.xml \
    frameworks/native/data/etc/android.hardware.usb.accessory.xml:system/etc/permissions/android.hardware.usb.accessory.xml \
    frameworks/native/data/etc/android.hardware.usb.host.xml:system/etc/permissions/android.hardware.usb.host.xml

# Graphics
PRODUCT_COPY_FILES += $(LOCAL_PATH)/configs/egl.cfg:system/lib/egl/egl.cfg

# Audio
PRODUCT_PACKAGES += \
    audio.a2dp.default \
    audio.usb.default \
    audio.r_submix.default \
    libaudio-resampler \
    tinymix

# Media codecs (software set first; MediaTek OMX added once the base boots)
PRODUCT_COPY_FILES += \
    frameworks/av/media/libstagefright/data/media_codecs_google_audio.xml:system/etc/media_codecs_google_audio.xml \
    frameworks/av/media/libstagefright/data/media_codecs_google_video_le.xml:system/etc/media_codecs_google_video_le.xml \
    $(LOCAL_PATH)/configs/media_codecs.xml:system/etc/media_codecs.xml

# Wi-Fi (source-built wpa_supplicant, nl80211)
PRODUCT_PACKAGES += \
    libwpa_client \
    wpa_supplicant \
    dhcpcd.conf

# wpa_supplicant base config (j5052 version; CM's wpa_supplicant.conf module does not install one here)
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/wpa_supplicant.conf:system/etc/wifi/wpa_supplicant.conf

# Charger, filesystem tools
PRODUCT_PACKAGES += \
    charger \
    charger_res_images \
    e2fsck \
    setup_fs \
    make_ext4fs \
    libq5_shim

# Bring-up: adb on, root adb, no auth
PRODUCT_DEFAULT_PROPERTY_OVERRIDES += \
    persist.sys.usb.config=mtp \
    persist.sys.root_access=3 \
    cm.service.adb.root=1 \
    ro.enable_boot_charger_mode=1 \
    af.resampler.quality=7 \
    ro.debuggable=1

$(call inherit-product, vendor/timmkoo/q5/q5-vendor.mk)

# Bidhata-Q5 0.2: A2DP LDAC (with ABR) + AAC encoders (bluedroid patch 0001-a2dp-multi-sep-aac-ldac)
PRODUCT_PACKAGES += \
    libldacBT_enc \
    libldacBT_abr

# cm-13.0 dropped STLport, but the j5052 Mali/gralloc/hwcomposer/dpframework blobs still need it.
# This copy was built from the cm-12.1 tree (same bionic ABI).
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/prebuilt/lib/libstlport.so:system/lib/libstlport.so


# Per-unit MAC addresses derived from the eMMC CID
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/rootdir/bin/q5_nvram_mac.sh:system/bin/q5_nvram_mac.sh
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/nvram/BT_Addr.default:system/etc/q5/BT_Addr.default

# MediaTek thermal policy loader (j5052). thermal_manager needs it to load /etc/.tp/thermal.conf;
# without it the CPU/battery thermal zones are never polled (interval=0) and nothing throttles.
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/prebuilt/lib/libmtcloader.so:system/lib/libmtcloader.so

# FM radio: MediaTek's per-chip FM config (CUST_get_cfg), taken from sprout (MT6582 + MT6627).
# Without it libfmjni refuses to load and the FM app closes on start.
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/prebuilt/lib/libfmcust.so:system/lib/libfmcust.so

# Free apps installed on first boot as normal (updatable) apps, see q5_preinstall.sh
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/rootdir/bin/q5_preinstall.sh:system/bin/q5_preinstall.sh \
    $(LOCAL_PATH)/rootdir/bin/q5_autoeq.sh:system/bin/q5_autoeq.sh \
    $(LOCAL_PATH)/prebuilt/media/bidhata_boot_base.zip:system/media/bidhata_boot_base.zip \
    $(LOCAL_PATH)/prebuilt/preinstall/com.hiby.music_5586.apk:system/preinstall/com.hiby.music_5586.apk \
    $(LOCAL_PATH)/prebuilt/preinstall/helium314.keyboard_4201.apk:system/preinstall/helium314.keyboard_4201.apk \
    $(LOCAL_PATH)/prebuilt/preinstall/me.zhanghai.android.files_40.apk:system/preinstall/me.zhanghai.android.files_40.apk \
    $(LOCAL_PATH)/prebuilt/preinstall/net.gsantner.markor_164.apk:system/preinstall/net.gsantner.markor_164.apk \
    $(LOCAL_PATH)/prebuilt/preinstall/org.coolreader_32582.apk:system/preinstall/org.coolreader_32582.apk \
    $(LOCAL_PATH)/prebuilt/preinstall/org.fdroid.fdroid_1023052.apk:system/preinstall/org.fdroid.fdroid_1023052.apk \
    $(LOCAL_PATH)/prebuilt/preinstall/org.solovyev.android.calculator_161.apk:system/preinstall/org.solovyev.android.calculator_161.apk \
    $(LOCAL_PATH)/prebuilt/preinstall/org.videolan.vlc_13070205.apk:system/preinstall/org.videolan.vlc_13070205.apk

# Root certificates added since Android 6 (Let's Encrypt ISRG, etc.), from the current Mozilla
# list, so HTTPS works with today's sites (GitHub/AutoEQ, F-Droid mirrors, KOReader downloads).
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/prebuilt/cacerts/01419da9.0:system/etc/security/cacerts/01419da9.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/073bfcc5.0:system/etc/security/cacerts/073bfcc5.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/0d69c7e1.1:system/etc/security/cacerts/0d69c7e1.1 \
    $(LOCAL_PATH)/prebuilt/cacerts/128f4b91.0:system/etc/security/cacerts/128f4b91.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/17882578.0:system/etc/security/cacerts/17882578.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/1b0f7e5c.0:system/etc/security/cacerts/1b0f7e5c.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/1e44ef15.0:system/etc/security/cacerts/1e44ef15.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/252252d2.0:system/etc/security/cacerts/252252d2.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/2c63f966.0:system/etc/security/cacerts/2c63f966.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/2cc2056d.0:system/etc/security/cacerts/2cc2056d.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/2d21b73c.0:system/etc/security/cacerts/2d21b73c.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/302904dd.0:system/etc/security/cacerts/302904dd.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/304d27c3.0:system/etc/security/cacerts/304d27c3.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/31188b5e.0:system/etc/security/cacerts/31188b5e.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/33ee480d.0:system/etc/security/cacerts/33ee480d.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/3428dc64.0:system/etc/security/cacerts/3428dc64.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/3afde786.0:system/etc/security/cacerts/3afde786.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/3c899c73.0:system/etc/security/cacerts/3c899c73.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/41a3f684.0:system/etc/security/cacerts/41a3f684.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/47b283f6.0:system/etc/security/cacerts/47b283f6.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/4c3982f2.0:system/etc/security/cacerts/4c3982f2.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/53a1b57a.0:system/etc/security/cacerts/53a1b57a.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/583ac115.0:system/etc/security/cacerts/583ac115.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/583d0756.0:system/etc/security/cacerts/583d0756.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/5acf816d.0:system/etc/security/cacerts/5acf816d.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/5f9a69fa.0:system/etc/security/cacerts/5f9a69fa.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/5fdd185d.0:system/etc/security/cacerts/5fdd185d.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/6187b673.0:system/etc/security/cacerts/6187b673.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/64316a39.0:system/etc/security/cacerts/64316a39.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/6b03dec0.0:system/etc/security/cacerts/6b03dec0.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/6b483515.0:system/etc/security/cacerts/6b483515.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/6c85d883.0:system/etc/security/cacerts/6c85d883.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/7892ad52.0:system/etc/security/cacerts/7892ad52.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/7a7c655d.0:system/etc/security/cacerts/7a7c655d.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/7e067d03.0:system/etc/security/cacerts/7e067d03.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/83e9984f.0:system/etc/security/cacerts/83e9984f.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/869fbf79.0:system/etc/security/cacerts/869fbf79.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/8761519c.0:system/etc/security/cacerts/8761519c.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/8794b4e3.0:system/etc/security/cacerts/8794b4e3.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/88950faa.0:system/etc/security/cacerts/88950faa.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/8b4600ae.0:system/etc/security/cacerts/8b4600ae.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/8d81b251.0:system/etc/security/cacerts/8d81b251.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/8f6cd7bb.0:system/etc/security/cacerts/8f6cd7bb.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/93851c9e.0:system/etc/security/cacerts/93851c9e.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/9479c8c3.0:system/etc/security/cacerts/9479c8c3.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/9591a472.0:system/etc/security/cacerts/9591a472.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/985c1f52.0:system/etc/security/cacerts/985c1f52.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/98aaf404.0:system/etc/security/cacerts/98aaf404.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/99e1b953.0:system/etc/security/cacerts/99e1b953.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/a716d4ed.0:system/etc/security/cacerts/a716d4ed.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/a81e292b.0:system/etc/security/cacerts/a81e292b.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/a9d40e02.0:system/etc/security/cacerts/a9d40e02.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/ab59055e.0:system/etc/security/cacerts/ab59055e.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/b30d5fda.0:system/etc/security/cacerts/b30d5fda.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/b7b40cb7.0:system/etc/security/cacerts/b7b40cb7.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/b92fd57f.0:system/etc/security/cacerts/b92fd57f.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/b936d1c6.0:system/etc/security/cacerts/b936d1c6.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/bb055c90.0:system/etc/security/cacerts/bb055c90.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/bd43e1dd.0:system/etc/security/cacerts/bd43e1dd.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/c44cc0c0.0:system/etc/security/cacerts/c44cc0c0.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/c7f1359b.0:system/etc/security/cacerts/c7f1359b.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/cb1c3204.0:system/etc/security/cacerts/cb1c3204.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/cbb3f32b.0:system/etc/security/cacerts/cbb3f32b.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/d16a5865.1:system/etc/security/cacerts/d16a5865.1 \
    $(LOCAL_PATH)/prebuilt/cacerts/d39b0a2c.0:system/etc/security/cacerts/d39b0a2c.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/d41b5e2a.0:system/etc/security/cacerts/d41b5e2a.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/d96b65e2.0:system/etc/security/cacerts/d96b65e2.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/da7377f6.0:system/etc/security/cacerts/da7377f6.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/dbff3a01.0:system/etc/security/cacerts/dbff3a01.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/dc99f41e.0:system/etc/security/cacerts/dc99f41e.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/dfc0fe80.0:system/etc/security/cacerts/dfc0fe80.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/e071171e.0:system/etc/security/cacerts/e071171e.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/e13665f9.0:system/etc/security/cacerts/e13665f9.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/e7c037b4.0:system/etc/security/cacerts/e7c037b4.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/ee035825.0:system/etc/security/cacerts/ee035825.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/ee37c333.0:system/etc/security/cacerts/ee37c333.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/ee532fd5.0:system/etc/security/cacerts/ee532fd5.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/f013ecaf.0:system/etc/security/cacerts/f013ecaf.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/f058632f.0:system/etc/security/cacerts/f058632f.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/f459871d.0:system/etc/security/cacerts/f459871d.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/f8fc53da.0:system/etc/security/cacerts/f8fc53da.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/fb5fa911.0:system/etc/security/cacerts/fb5fa911.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/fd08c599.0:system/etc/security/cacerts/fd08c599.0 \
    $(LOCAL_PATH)/prebuilt/cacerts/ffa7f1eb.0:system/etc/security/cacerts/ffa7f1eb.0

# power-on alarm test tool (arms MTK's RTC power-on alarm when /dev/alarm is free)
PRODUCT_PACKAGES += q5_poweron

# Bidhata home screen (replaces Trebuchet as the default launcher)
PRODUCT_PACKAGES += \
    BidhataLauncher \
    BidhataRecorder \
    FMRadio

# System-wide EQ (JamesDSP) - AutoEQ profiles are loaded into its EQ
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/prebuilt/jamesdsp/libjamesdsp.so:system/lib/soundfx/libjamesdsp.so
PRODUCT_PACKAGES += \
    JamesDSPManager
