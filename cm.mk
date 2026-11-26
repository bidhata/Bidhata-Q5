# Bidhata-Q5 product (lunch cm_q5-userdebug)
# Our audio_effects.conf (adds JamesDSP) must be listed before the inherited AOSP one,
# because the first PRODUCT_COPY_FILES entry for a destination wins.
PRODUCT_COPY_FILES += \
    device/timmkoo/q5/configs/audio/audio_effects.conf:system/etc/audio_effects.conf

# Speaker was 11 dB below the amp's limit with the stock default volume table. Same blob with the
# normal-mode speaker byte raised 128 -> 160 (3 dB below the limit). Headphones/BT untouched.
PRODUCT_COPY_FILES += \
    device/timmkoo/q5/prebuilt/lib/libcustom_nvram.so:system/lib/libcustom_nvram.so

# Bidhata boot animation (sun rising over the mountains); listed first so it beats CM's own.
PRODUCT_COPY_FILES += \
    device/timmkoo/q5/prebuilt/media/bootanimation.zip:system/media/bootanimation.zip

$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base.mk)
$(call inherit-product, vendor/cm/config/common_full_tablet_wifionly.mk)
$(call inherit-product, device/timmkoo/q5/device.mk)

PRODUCT_NAME := cm_q5
PRODUCT_DEVICE := q5
PRODUCT_BRAND := TIMMKOO
PRODUCT_MODEL := Q5
PRODUCT_MANUFACTURER := TIMMKOO
PRODUCT_RELEASE_NAME := Bidhata-Q5

TARGET_SCREEN_WIDTH := 480
TARGET_SCREEN_HEIGHT := 854

# Branding
PRODUCT_BUILD_PROP_OVERRIDES += BUILD_DISPLAY_ID=Bidhata-Q5
# Release version. CI passes the git tag (BIDHATA_VERSION=0.4.8); local builds keep this default.
ifeq ($(BIDHATA_VERSION),)
BIDHATA_VERSION := 0.4.8
endif
PRODUCT_PROPERTY_OVERRIDES += ro.bidhata.version=Bidhata-Q5-$(BIDHATA_VERSION)
