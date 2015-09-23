//
// Created by kodjo baah on 18/08/2015.
//

#ifndef WAID_NATIVECOMMUNICATOR_H
#define WAID_NATIVECOMMUNICATOR_H

#include <jni.h>

namespace waid {

    class NativeCommunicator {

    private:

        static JavaVM *gJavaVM;

        static jobject gNativeObject;

    public:

        NativeCommunicator(JavaVM *vm, jobject no);

        JNIEnv *getJniEnv();

        jobject getCallbackInterface(JNIEnv *env);

        void invalidToken();

        void connectionLost();

        void unableToInitalizeCamera();

        void unableToConnectZmq();

        void zmqConnectionDropped();

        void storeMessenger(jobject gnObject);

        void connectedZmq();

        void storedMessenger(jobject gNObject);

        void setJavaVm(JavaVM *javaVm);

        void updateMessageSent(long messageSent);

        void updateStreamToken(std::string streamToken);

    };
}


#endif //WAID_NATIVECOMMUNICATOR_H
