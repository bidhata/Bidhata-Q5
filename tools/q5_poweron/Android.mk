LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := q5_poweron
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := q5_poweron.c
include $(BUILD_EXECUTABLE)
