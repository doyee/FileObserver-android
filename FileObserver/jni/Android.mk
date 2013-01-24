LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := FileObserver
LOCAL_SRC_FILES := FileObserver.cpp

include $(BUILD_SHARED_LIBRARY)
