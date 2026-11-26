LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := BidhataLauncher
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
# Platform-signed and privileged so it may read the active media sessions (HiBy's now playing).
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
# Bidhata-Q5 is a music player, not a phone: everything below is left out of /system.
# (Overrides only work from an app module, so the list lives here with the launcher.)
LOCAL_OVERRIDES_PACKAGES := Launcher2 Launcher3 Browser Calendar CalendarProvider Camera2 Contacts Development Email Exchange2 Galaxy4 NoiseField PhaseBeam HoloSpiralWallpaper LiveWallpapers LiveWallpapersPicker PhotoTable Terminal SoundRecorder ExactCalculator CMFileManager LockClock Trebuchet CMWallpapers HexoLibre PhotoPhase WeatherProvider WeatherManagerService Screencast Profiles CMUpdater PicoTts WAPPushManager AudioFX CyanogenSetupWizard BluetoothMidiService PrintSpooler ThemeChooser Eleven WallpaperPicker WallpaperCropper

include $(BUILD_PACKAGE)
