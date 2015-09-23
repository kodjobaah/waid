//
// Created by kodjo baah on 18/08/2015.
//


#define LOG_TAG    "NATIVE_COMMUNICATOR"
#define LOG(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#include <string>
#include <android/log.h>
#include <NativeCommunicator.h>

namespace  waid {

    JavaVM *NativeCommunicator::gJavaVM;
    jobject NativeCommunicator::gNativeObject;

    NativeCommunicator::NativeCommunicator(JavaVM *vm, jobject no) {
        gJavaVM = vm;
        gNativeObject = no;

    }

    void NativeCommunicator::setJavaVm(JavaVM * javaVm) {
        gJavaVM = javaVm;
    }

    void NativeCommunicator::storedMessenger(jobject gNObject) {
        gNativeObject = gNObject;
    }

    void NativeCommunicator::unableToInitalizeCamera() {

        JNIEnv *env = getJniEnv();
        LOG("ZMQ_EVENT_NATIVE_UABLE_TO_INITIALIZE_CAMERA");
        jobject obj;
        if (env) {
            obj = getCallbackInterface(env);
        }

        bool send = true;
        if (obj) {
            /* Find the callBack method ID */
            jclass interfaceClass = env->GetObjectClass(obj);
            jmethodID method = env->GetMethodID(interfaceClass, "unableToInitalizeCamera", "()V");
            if (!method) {
                LOG("callback_handler: failed to get method ID (unableToInitalizeCamera");
                gJavaVM->DetachCurrentThread();
                send = false;
            }

            if (send) {
                env->CallVoidMethod(obj, method);
            }

            gJavaVM->DetachCurrentThread();

        }
        LOG("------------ Messages Sent (unableToInitalizeCamera)----------------");
    }


    void NativeCommunicator::connectionLost() {

        JNIEnv *env = getJniEnv();
        LOG("ZMQ_EVENT_NATIVE_COMMUNICATOR_CONNECTION_LOST");
        jobject obj;
        if (env) {
            LOG("-UMSC2");
            obj = getCallbackInterface(env);
        }

        bool send = true;
        if (obj) {
            /* Find the callBack method ID */
            jclass interfaceClass = env->GetObjectClass(obj);
            jmethodID method = env->GetMethodID(interfaceClass, "connectionLost", "()V");
            if (!method) {
                LOG("callback_handler: failed to get method ID (connectionLost)");
                gJavaVM->DetachCurrentThread();
                send = false;
            }

            if (send) {
                env->CallVoidMethod(obj, method);
            }

            gJavaVM->DetachCurrentThread();

        }
        LOG("------------ Messages Sent----------------");
    }

    void NativeCommunicator::invalidToken() {

        JNIEnv *env = getJniEnv();
        LOG("ZMQ_EVENT_NATIVE_COMMUNICATOR_INVALID_TOKEN");
        jobject obj;
        if (env) {
            LOG("-UMSC2");
            obj = getCallbackInterface(env);
        }

        bool send = true;
        if (obj) {
            /* Find the callBack method ID */
            jclass interfaceClass = env->GetObjectClass(obj);
            jmethodID method = env->GetMethodID(interfaceClass, "invalidToken", "()V");
            if (!method) {
                LOG("callback_handler: failed to get method ID (invalidToken)");
                gJavaVM->DetachCurrentThread();
                send = false;
            }

            if (send) {
                env->CallVoidMethod(obj, method);
            }

            gJavaVM->DetachCurrentThread();

        }
        LOG("------------ Messages Sent----------------");
    }


    void NativeCommunicator::updateStreamToken(std::string streamToken) {

        JNIEnv *env = getJniEnv();
        LOG("ZMQ_EVENT_NATIVE_COMMUNICATOR_UPDATE_STREAM_TOKEN");
        jobject obj;
        if (env) {
            LOG("-UMSC2");
            obj = getCallbackInterface(env);
        }

        bool send = true;
        if (obj) {
            /* Find the callBack method ID */
            jstring jstr = env->NewStringUTF(streamToken.c_str());
            jclass interfaceClass = env->GetObjectClass(obj);
            jmethodID method = env->GetMethodID(interfaceClass, "updateStreamToken", "(Ljava/lang/String;)V");
            if (!method) {
                LOG("callback_handler: failed to get method ID (updateStreamToken)");
                gJavaVM->DetachCurrentThread();
                send = false;
            }

            if (send) {
                env->CallVoidMethod(obj, method,jstr);
            }

            gJavaVM->DetachCurrentThread();

        }
        LOG("------------ Messages Sent----------------");
    }

    void NativeCommunicator::updateMessageSent(long messageSent) {

        JNIEnv *env = getJniEnv();
        LOG("ZMQ_EVENT_NATIVE_COMMUNICATOR_UPDATE_MESSAGE_SENT");
        jobject obj;
        if (env) {
            LOG("-UMSC2");
            obj = getCallbackInterface(env);
        }

        bool send = true;
        if (obj) {
            /* Find the callBack method ID */
            jclass interfaceClass = env->GetObjectClass(obj);
            jmethodID method = env->GetMethodID(interfaceClass, "updateMessagesSent", "(J)V");
            if (!method) {
                LOG("callback_handler: failed to get method ID (updateMessagesSent)");
                gJavaVM->DetachCurrentThread();
                send = false;
            }

            if (send) {
                env->CallVoidMethod(obj, method);
            }

            gJavaVM->DetachCurrentThread();

        }
        LOG("------------ Messages Sent----------------");
    }

    void NativeCommunicator::zmqConnectionDropped() {

        JNIEnv *env = getJniEnv();
        LOG("ZMQ_EVENT_NATIVE_COMMUNICATOR_CONNECTION_DROPPED");
        jobject obj;
        if (env) {
            LOG("-ZNCD2");
            obj = getCallbackInterface(env);
        }

        bool send = true;
        if (obj) {
            LOG("---ABOUT-TO-CALL-JAVA-METHOD");
            /* Find the callBack method ID */
            jclass interfaceClass = env->GetObjectClass(obj);
            jmethodID method = env->GetMethodID(interfaceClass, "connectionDropped", "()V");
            if (!method) {
                LOG("callback_handler: failed to get method ID (connectionDropped)");
                gJavaVM->DetachCurrentThread();
                send = false;
            }

            if (send) {
                env->CallVoidMethod(obj, method);
            }

            gJavaVM->DetachCurrentThread();

        }
        LOG("------------ CONNECTION DROPPED----------------");
    }

    void NativeCommunicator::unableToConnectZmq() {

        JNIEnv *env = getJniEnv();
        LOG("ZMQ_EVENT_NATIVE_COMMUNICATOR_UNABLE_TO_CONNECT_ZMQ");
        jobject obj;
        if (env) {
            LOG("-ZNC2");
            obj = getCallbackInterface(env);
        }

        bool send = true;
        if (obj) {
            /* Find the callBack method ID */
            jclass interfaceClass = env->GetObjectClass(obj);
            jmethodID method = env->GetMethodID(interfaceClass, "unableToConnect", "()V");
            if (!method) {
                LOG("callback_handler: failed to get method ID (unableToConnect)");
                gJavaVM->DetachCurrentThread();
                send = false;
            }

            if (send) {
                env->CallVoidMethod(obj, method);
            }

            gJavaVM->DetachCurrentThread();

        }
        LOG("------------ SENT UNABLE TO CONNECT MESSAGE----------------");
    }

    void NativeCommunicator::connectedZmq() {

        JNIEnv *env = getJniEnv();
        LOG("ZMQ_EVENT_NATIVE_COMMUNICATOR_CONNECTED_ZEROMQ");
        jobject obj;
        if (env) {
            LOG("-ZC2");
            obj = getCallbackInterface(env);
        }

        bool send = true;
        if (obj) {
            LOG("-ZC3");
            /* Find the callBack method ID */
            jclass interfaceClass = env->GetObjectClass(obj);
            LOG("-ZC4");
            jmethodID method = env->GetMethodID(interfaceClass, "ableToConnect",
                                                "()V");
            LOG("-ZC5");
            if (!method) {
                LOG("callback_handler: failed to get method ID (ableToConnect)");
                gJavaVM->DetachCurrentThread();
                send = false;
            }

            if (send) {
                LOG("-ZC6");
                env->CallVoidMethod(obj, method);
                LOG("-ZC7");
            }

            gJavaVM->DetachCurrentThread();

        }
        LOG("------------ SEND ABLET TO CONNECT MESSAGE-----------------");
    }

    jobject NativeCommunicator::getCallbackInterface(JNIEnv * env) {

        bool send = true;
        /* Construct a Java string */
        LOG("-Z3");
        jobject interfaceClass = env->NewWeakGlobalRef(gNativeObject);
        LOG("-Z4");
        if (!interfaceClass) {
            LOG("-Z5");
            LOG("callback_handler: failed to get class reference");
            gJavaVM->DetachCurrentThread();
            send = false;
        }

        if (send) {
            return interfaceClass;
        } else {
            return NULL;
        }

    }

    JNIEnv *NativeCommunicator::getJniEnv() {

        JNIEnv *env;
        bool isAttached = false;
        bool send = true;
        LOG("JUST-BEFORE");
        if (!gJavaVM) {
            LOG("GJAVAVM_NOT_INIT");
        }
        LOG("JUST-AFTER");
        int status = gJavaVM->GetEnv((void **) &env, JNI_VERSION_1_6);
        if (status < 0) {
            LOG("callback_handler: failed to get JNI environment, "
                        "assuming native thread");
            status = gJavaVM->AttachCurrentThread(&env, NULL);
            if (status < 0) {
                LOG("callback_handler: failed to attach "
                            "current thread");
                send = false;
            }
            isAttached = true;
        }

        if (send) {
            return env;
        } else {
            return NULL;
        }
    }

}