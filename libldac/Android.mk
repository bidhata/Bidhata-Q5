# Bidhata-Q5: AOSP libldac (Apache 2.0) built for cm-12.1 (Android.mk port of Android.bp).
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libldacBT_enc
LOCAL_MODULE_TAGS := optional
LOCAL_ARM_MODE := arm
LOCAL_SRC_FILES := src/ldaclib.c src/ldacBT.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/inc
LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)/inc
LOCAL_CFLAGS := -O2 -Wall
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libldacBT_abr
LOCAL_MODULE_TAGS := optional
LOCAL_ARM_MODE := arm
LOCAL_SRC_FILES := abr/src/ldacBT_abr.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/abr/inc $(LOCAL_PATH)/inc
LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)/abr/inc
LOCAL_SHARED_LIBRARIES := libldacBT_enc
LOCAL_CFLAGS := -O2 -Wall
include $(BUILD_SHARED_LIBRARY)
