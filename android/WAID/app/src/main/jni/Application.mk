#APP_STL := stlport_shared
APP_CPPFLAGS := -frtti -fexceptions
#APP_ABI := x86
APP_PLATFORM := android-18
APP_STL := gnustl_static

#APP_CPPFLAGS += -std=c++11
APP_OPTIM := release
LOCAL_ARM_MODE := thumb
APP_ABI := armeabi armeabi-v7a