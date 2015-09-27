LOCAL_PATH := $(call my-dir)
#OPENCV_SDK :=  /Users/kodjobaah/software/opencv/3.0.0/OpenCV-android-sdk
#OPENCV_SDK :=   /Users/kodjobaah/software/opencv/3.0.0.RC2/OpenCV-android-sdk
OPENCV_SDK :=  /Users/kodjobaah/software/opencv/OpenCV-2.4.9-android-sdk
include $(CLEAR_VARS)

# OpenCV
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
WITH_TBB=ON
#OPENCV_LIB_TYPE:=STATIC
OPENCV_LIB_TYPE:=SHARED
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

#LOCAL_WHOLE_STATIC_LIBRARIES := shape stitching objdetect superres videostab calib3d features2d highgui videoio imgcodecs video photo ml imgproc flann core hal