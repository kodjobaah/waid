/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


#include <boost/thread/thread.hpp>
#include <boost/atomic.hpp>
#include "boost/date_time/posix_time/posix_time.hpp"


// OpenGL ES 2.0 cod
#include <jni.h>
#include <android/log.h>
#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <errno.h>
#include <sys/time.h>
#include <time.h>
#include <android/log.h>

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <queue>
#include <string>
#include <sstream>
#include <vector>
#include <iostream>


#include <cv.h>
#include <highgui.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>

#include <VideoRendererVbo.h>
#include <ZeroMqTransport.h>
#include <WaidCamera.h>

#define  LOG_TAG    "libgl2jni"
#define LOG(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)


JavaVM *gJavaVM;
jobject gNativeObject;

waid::WaidCamera *waidCamera;

extern "C" {

    JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_init(JNIEnv * env, jobject ojb, jint width, jint height, int camera, int restart);
    JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_step(JNIEnv * env, jobject obj);
    JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_stopZeroMQ(JNIEnv * env, jobject obj);
    JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_startZeroMQ(JNIEnv * env, jobject obj,jstring url, jstring token);

    JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_resize(JNIEnv* env, jobject obj, jint width, jint height);
    JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_orientationChange(JNIEnv* env, jobject obj, jint orientation);

    JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_storeMessenger(JNIEnv * env, jclass c, jobject jc);

    JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_stopCamera(JNIEnv * env, jobject obj);
    JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_startCamera(JNIEnv * env, jobject obj);
    JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_restartCamera(JNIEnv * env, jobject obj, jint camera);

};


JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_stopCamera(JNIEnv * env, jobject obj) {

    waidCamera->stop();
}

JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_startCamera(JNIEnv * env, jobject obj) {
    waidCamera->start();
}


JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_restartCamera(JNIEnv * env, jobject obj, jint camera) {
    waidCamera->restartCamera(camera);
}

JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_init(JNIEnv * env, jobject obj, int width , int height, int camera, int restart)
{

    waidCamera = new waid::WaidCamera();
    waidCamera->startCamera(width,height,camera);
    waidCamera->storeJni(gJavaVM);
    waidCamera->storeMessenger(gNativeObject);
}


JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_orientationChange(JNIEnv* env, jobject obj, jint orient) {
    waidCamera->setOrientation(orient);
}

JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_step(JNIEnv * env, jobject obj)
{
    waidCamera->renderFrame();
}

JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_resize(JNIEnv* env, jobject obj, jint width, jint height) {

    LOG("RESIZING-CAMERA width=%d height=%d",width,height);
    waidCamera->resize(width,height);
}



JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_stopZeroMQ(JNIEnv * env, jobject obj)
{
    waidCamera->stopSendToZeroMq();
}

JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_startZeroMQ(JNIEnv * env, jobject ob, jstring url, jstring token) {


        const char *urlPath = env->GetStringUTFChars(url, 0);
        const char *authToken = env->GetStringUTFChars(token, 0);
        waidCamera->sendToZeroMq(urlPath,authToken);

}

JNIEXPORT void JNICALL Java_com_waid_nativecamera_GL2JNILib_storeMessenger(JNIEnv * env, jclass c, jobject jc) {
        LOG("SETTING MESSENGER");
        gNativeObject  = env->NewGlobalRef(jc);

}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {

    JNIEnv *env;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOG("Failed to get the environment using GetEnv()");
        return -1;
    }

    gJavaVM = vm;
    LOG("JNIOnLoad-SETTING JVM CONTEXT");

    return JNI_VERSION_1_6; /* the required JNI version */
}

