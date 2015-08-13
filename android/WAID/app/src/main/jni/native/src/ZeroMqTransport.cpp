//
// Created by kodjo baah on 01/08/2015.
//


//Encodes Base64
#include <openssl/bio.h>
#include <openssl/evp.h>
#include <openssl/buffer.h>
#include <stdint.h>


#include <boost/thread/mutex.hpp>
#include "boost/tuple/tuple.hpp"
#include <boost/date_time/gregorian/gregorian.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/chrono.hpp>
#include <boost/thread/thread.hpp>

#include <android/log.h>
#include <jni.h>
#include <math.h>
#include <sched.h>
#include <string.h>
#include <zmq.h>
#include <zhelpers.hpp>
#include <string>
#include <vector>
#include <queue>
#include <exception>
#include <tuple>


#include <android/native_activity.h>
#include <turbojpeg.h>

#include <time.h>


#include <ZeroMqTransport.h>


#define LOG_TAG    "ZEROMQ_TRANSPORT"
#define LOG(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define BUFFERSIZE 16777216
namespace  waid {


    const std::string ZeroMqTransport::CONNECT_MESSAGE = "CONNECT";
    const std::string ZeroMqTransport::IS_ALIVE_MESSAGE = "IS_VALID";
    const std::string ZeroMqTransport::BROADCAST_MESSAGE = "BROADCAST";

    ZeroMqTransport::ZeroMqTransport() {

    }


    void ZeroMqTransport::setJavaVm(JavaVM * javaVm) {
        gJavaVM = javaVm;
    }

    void ZeroMqTransport::storedMessenger(jobject gNObject) {
        gNativeObject = gNObject;

    }

    void ZeroMqTransport::addDataToQueue(cv::Mat image, long time) {

        std::string arrivalTime;
        std::stringstream strstream;
        strstream << time;
        strstream >> arrivalTime;

        std::tuple <cv::Mat, std::string> frameData(image, arrivalTime);

        // Acquire lock on the queue
        boost::unique_lock <boost::mutex> lock(m_mutex);
        //LOG("SIZE_OF_QUEUE[%d]",dataToSend.size());

        // Add the data to the queue
        dataToSend.push(frameData);

        // Notify others that data is ready
        m_cond.notify_one();
        //mtx_.unlock();
    }

    bool ZeroMqTransport::getDataFromDequeue(std::tuple < cv::Mat, std::string > &result) {
        boost::unique_lock <boost::mutex> lock(m_mutex);
        while (dataToSend.empty() && (!stopDequeue)) {
            m_cond.wait(lock);
        }

        if (stopDequeue) {
            //LOG("STOPPING-ZMQ");
            return false;
        }
        result = dataToSend.front();
        dataToSend.pop();
        return true;
    }


    zmq::socket_t ZeroMqTransport::s_client_socket(zmq::context_t &context, const char *urlPath,
                                                   const char *authToken) {

        LOG("Connecting to zeromq server...urlPath=%s..........authToken=%s",
            urlPath, authToken);

        zmq::socket_t socketExternal(context, ZMQ_DEALER);
        std::string auth(authToken);
        int linger = 0;

        socketExternal.setsockopt(ZMQ_LINGER, &linger, sizeof(linger));
        socketExternal.setsockopt(ZMQ_IDENTITY, auth.c_str(), (long) auth.length());
        socketExternal.connect(urlPath);
        return socketExternal;
    }


    void ZeroMqTransport::processImage(const char *urlPath, const char *authToken) {

        LOG("---------------------------------------------- PROCESSING IMAGE DATA -----------------------");

        try {
            zmq::context_t contextExternal = zmq::context_t(1);
            zmq::socket_t socketExternal = s_client_socket(contextExternal, urlPath, authToken);
            s_send(socketExternal, CONNECT_MESSAGE);
            std::string connectString = s_recv(socketExternal);
            std::string streamId = s_recv(socketExternal);


            if (IS_ALIVE_MESSAGE.compare(connectString) != 0) {
                LOG("---IS NOT ALIVE");
                unableToConnectZmq();
            } else {
                LOG("--IS_ALIVE__");

                processingStarted = true;
                tjhandle _jpegCompressor = tjInitCompress();
                int count = 0;
                boost::posix_time::ptime const time_epoch(boost::gregorian::date(1970, 1, 1));
                while (processingStarted) {

                    try {
                        boost::this_thread::interruption_point();
                    }
                    catch (const boost::thread_interrupted &) {
                        LOG("BREAK-FROM-ZQM");
                        break;
                    }

                    CvMat *out;
                    cv::Mat cl;
                    cv::Mat in;
                    std::tuple <cv::Mat, std::string> frameData;


                    bool result = getDataFromDequeue(frameData);
                    if (result) {
                        in = std::get<0>(frameData);
                        LOG("POPPED: row=%d, col=%d", in.rows, in.cols);
                    }

                    if (result && !in.empty()) {

                        pthread_mutex_lock(&jpegFileMutex);
                        cv::Mat newOut;
                        cv::cvtColor(in, newOut, CV_BGR5652RGB);
                        in.release();
                        cv::Mat newSize;
                        int _width = 352;
                        int _height = 288;

                        cv::Size dsize(_width, _height);
                        cv::resize(newOut, newSize, dsize, 0, 0, cv::INTER_AREA);

                        unsigned char *outdata = (uchar *) newSize.datastart;
                        int flags = TJFLAG_BOTTOMUP;
                        const int JPEG_QUALITY = 75;
                        long unsigned int _jpegSize = 0;
                        unsigned char *_compressedImage = NULL; //!< Memory is allocated by tjCompress2 if _jpegSize == 0

                        int compResults = tjCompress2(_jpegCompressor, outdata, _width, 0, _height,
                                                      TJPF_RGB,
                                                      &_compressedImage, &_jpegSize, TJSAMP_440,
                                                      JPEG_QUALITY,
                                                      flags);
                        newSize.release();

                        /*
                        char numstr[65]; // enough to hold all numbers up to 64-bits
                        sprintf(numstr, "%d", count);
                        std::string loc = "/sdcard/image/image";
                        std::string ext = ".jpg";
                        std::string configFile = loc + numstr + ext;
                        FILE *appConfigFile = fopen(configFile.c_str(), "w+");
                        int res = 0;
                        res = fwrite(_compressedImage, sizeof(char), _jpegSize, appConfigFile);
                        fclose(appConfigFile);
                        LOG("WROTE-FILE %s= size=%d", configFile.c_str(), (int) _jpegSize);
                        count = count + 1;
                         */

                        int len = (int) (_jpegSize);
                        char *messageToSend;
                        std::string message = base64Encode(
                                reinterpret_cast<const unsigned char *>(_compressedImage),
                                (int) _jpegSize, &messageToSend);
                        s_sendmore(socketExternal, BROADCAST_MESSAGE);
                        s_sendmore(socketExternal, streamId);
                        //s_sendmore(socketExternal,ss.str());
                        s_sendmore(socketExternal, std::get<1>(frameData));
                        s_send(socketExternal, message);

                        tjFree(_compressedImage);
                        pthread_mutex_unlock(&jpegFileMutex);
                    } else {
                        //  LOG("--RECEIVED IMAGE DATA");
                    }
                }
                tjDestroy(_jpegCompressor);
                processingStarted = false;
            }
        } catch (zmq::error_t &) {
            LOG("--ERROR-UNABLE-TO-CONNECT");
            unableToConnectZmq();
        }

    }

    std::string ZeroMqTransport::base64Encode(const unsigned char *buffer, size_t length,
                                              char **b64text) { //Encodes a binary safe base 64 string

        BIO *bmem, *b64;
        BUF_MEM *bptr;

        b64 = BIO_new(BIO_f_base64());
        bmem = BIO_new(BIO_s_mem());
        b64 = BIO_push(b64, bmem);
        BIO_set_flags(bmem, BIO_FLAGS_BASE64_NO_NL);
        BIO_write(b64, buffer, length);
        BIO_flush(b64);
        BIO_get_mem_ptr(b64, &bptr);
        BIO_set_close(bmem, BIO_NOCLOSE);

        char *buf = (char *) malloc(bptr->length + 1);
        memcpy(buf, bptr->data, bptr->length);
        buf[bptr->length] = '\0';
        std::string message(buf);

        BIO_free_all(b64);
        return message; //success
    }

    void ZeroMqTransport::updateMessageSent(long messageSent) {

        JNIEnv *env = getJniEnv();
        LOG("-UMSC1");
        jobject obj;
        if (env) {
            LOG("-UMSC2");
            obj = getCallbackInterface(env);
        }

        bool send = true;
        if (obj) {
            /* Find the callBack method ID */
            jclass interfaceClass = env->GetObjectClass(obj);
            jmethodID method = env->GetMethodID(interfaceClass, "updateMessagesSent",
                                                "(J)V");
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

    void ZeroMqTransport::zmqConnectionDropped() {

        JNIEnv *env = getJniEnv();
        LOG("-ZNCD1");
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
            jmethodID method = env->GetMethodID(interfaceClass, "connectionDropped",
                                                "()V");
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


    void ZeroMqTransport::unableToConnectZmq() {

        JNIEnv *env = getJniEnv();
        LOG("-ZNC1");
        jobject obj;
        if (env) {
            LOG("-ZNC2");
            obj = getCallbackInterface(env);
        }

        bool send = true;
        if (obj) {
            /* Find the callBack method ID */
            jclass interfaceClass = env->GetObjectClass(obj);
            jmethodID method = env->GetMethodID(interfaceClass, "unableToConnect",
                                                "()V");
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

    void ZeroMqTransport::connectedZmq() {

        JNIEnv *env = getJniEnv();
        LOG("-ZC1");
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


    jobject ZeroMqTransport::getCallbackInterface(JNIEnv * env) {

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

    JNIEnv *ZeroMqTransport::getJniEnv() {

        JNIEnv *env;
        bool isAttached = false;
        bool send = true;
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


    void ZeroMqTransport::stop() {

        LOG("---ZEROMQ-INTERUPTINT");
        stopDequeue = true;
        //processImageThread->interrupt();
        processingStarted = false;
        m_cond.notify_one();
        /*
        while(processingStarted) {
            LOG("--WAITING_FOR_PROCESSING TO STOPE");
        }
         */
        LOG("---ZEROMQ-INT-COMPLETED");

    }

    void ZeroMqTransport::establishConnection(const char *urlPath, const char *authToken) {
        LOG("---ESTABLISING CONNECTION ------");
        stopDequeue = false;

        boost::shared_ptr <boost::thread> pt(
                new boost::thread(&ZeroMqTransport::processImage, this, urlPath, authToken));
        processImageThread.swap(pt);
        LOG("---ESTABLISHMENT COMPLETE 1 ------");

    }

    void ZeroMqTransport::storeJni(JavaVM * gjVM) {
        gJavaVM = gjVM;
    }

    void ZeroMqTransport::storeMessenger(jobject gnObject) {
        gNativeObject = gnObject;
    }

}