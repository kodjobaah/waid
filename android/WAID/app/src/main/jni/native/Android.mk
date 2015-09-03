LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
# OpenCV
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
#WITH_TBB=ON
#OPENCV_LIB_TYPE:=STATIC
include /Users/kodjobaah/software/opencv/OpenCV-2.4.9-android-sdk/sdk/native/jni/OpenCV.mk

#CPP_STATIC := /Users/kodjobaah/software/android/crystax-ndk-10.2.1/sources/cxx-stl/gnu-libstdc++/4.9/libs/$(TARGET_ARCH_ABI)/libgnustl_static.a

OPENGLES_DEF  += -DUSE_OPENGL_ES_2_0
OPENGLES_LIB  := -lGLESv1_CM
OPENGLES_DEF  := -DUSE_OPENGL_ES_1_1
LOCAL_CPPFLAGS   += -Werror -Wno-reorder
LOCAL_CPPFLAGS   += $(OPENGLES_DEF)
LOCAL_CPPFLAGS   += -fno-rtti
LOCAL_CPPFLAGS   += -fno-exceptions
LOCAL_MODULE     := gl2jni

LOCAL_SHARED_LIBRARIES :=  libzmq libjpeg2 -lopencv_core -lopencv_highgui
LOCAL_STATIC_LIBRARIES += boost_serialization_static boost_thread_static libopenssl-static libcrypto libssl-static
LOCAL_C_INCLUDES       += /Users/kodjobaah/projects/waid/android/WAID/app/src/main/jni/zeromq/include \
						  /Users/kodjobaah/projects/waid/android/WAID/app/src/main/jni/libjpeg-turbo \
						  /Users/kodjobaah/projects/waid/android/WAID/app/src/main/jni/cppzmq \
						  /Users/kodjobaah/projects/waid/android/WAID/app/src/main/jni/android-external-openssl-ndk-static/include \
						   $(LOCAL_PATH)/include
LOCAL_SRC_FILES :=  src/gl_code.cpp \
                    src/SoundCapture.cpp \
                    src/VideoRendererVbo.cpp \
                    src/NativeCommunicator.cpp \
                    src/WaidCamera.cpp \
                    src/ZeroMq.cpp \
                    src/ZeroMqTransport.cpp

LOCAL_LDLIBS +=  $(OPENGLES_LIB) -llog -ldl -lEGL  -lGLESv2 -lOpenSLES -ljnigraphics \
			$(CPP_STATIC)
include $(BUILD_SHARED_LIBRARY)
$(call import-module,boost/1.58.0)