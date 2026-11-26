LOCAL_PATH := $(call my-dir)

# JamesDSP audio effect + its control app (GPL-3.0, https://github.com/james34602/JamesDSPManager,
# release 28Nov2023 / jdsp_v6.0). Provides the system-wide parametric/graphic EQ that AutoEQ
# headphone profiles are loaded into.
include $(CLEAR_VARS)
LOCAL_MODULE := JamesDSPManager
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := APPS
LOCAL_SRC_FILES := JamesDSPManager.apk
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE := PRESIGNED
include $(BUILD_PREBUILT)
