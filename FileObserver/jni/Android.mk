LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := fileobserver_jni
LOCAL_SRC_FILES := fileobserver_jni.cpp
LOCAL_LDLIBS := -lc -lm -lstdc++ -ldl -llog
include $(BUILD_SHARED_LIBRARY)
