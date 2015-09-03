/*
 * androidlog.cpp
 *
 *  Created on: 2011/12/31
 *      Author: nobnak
 */
#include <android/log.h>
#include "androidlog.h"

const char *Log::LOG_TAG = "nobnak.study.opensles";

void Log::d(const char *msg) {
    __android_log_write(ANDROID_LOG_DEBUG, LOG_TAG, msg);
}

