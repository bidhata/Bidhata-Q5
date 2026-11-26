LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libq5_shim
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := q5_shim.c
LOCAL_SHARED_LIBRARIES := liblog libdl
include $(BUILD_SHARED_LIBRARY)
