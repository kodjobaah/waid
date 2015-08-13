LOCAL_PATH := $(call my-dir)
OPENCV_SDK :=  /Users/kodjobaah/software/opencv/OpenCV-2.4.9-android-sdk
include $(CLEAR_VARS)

# OpenCV
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
include $(OPENCV_SDK)/sdk/native/jni/OpenCV.mk

# ZeroMQ
LOCAL_MODULE := libzmq
LOCAL_SRC_FILES := /Users/kodjobaah/projects/waid/android/WAID/app/src/main/jni/zeromq/lib/libzmq.so
LOCAL_EXPORT_C_INCLUDES :=  /Users/kodjobaah/AndroidStudioProjects/WAID/app/src/main/jni/zeromq/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
include /Users/kodjobaah/projects/waid/android/WAID/app/src/main/jni/libjpeg-turbo/Android.mk

#include $(CLEAR_VARS)
#include /Users/kodjobaah/projects/waid/android/WAID/app/src/main/jni/msgpack-c/Android.mk


include $(CLEAR_VARS)
include /Users/kodjobaah/projects/waid/android/WAID/app/src/main/jni/android-external-openssl-ndk-static/Android.mk

include $(CLEAR_VARS)
include /Users/kodjobaah/projects/waid/android/WAID/app/src/main/jni/native/Android.mk

