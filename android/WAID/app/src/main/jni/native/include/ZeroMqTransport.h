//
// Created by kodjo baah on 01/08/2015.
//


#ifndef WAID_ZEROMQTRANSPORT_H
#define WAID_ZEROMQTRANSPORT_H


#include <boost/shared_ptr.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>

#include <tuple>
#include <queue>
#include <atomic>
#include <cv.h>
#include <highgui.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <jni.h>
#include <zmq.hpp>

namespace  waid {

    class ZeroMqTransport {

    private:


        static const std::string CONNECT_MESSAGE;
        static const std::string IS_ALIVE_MESSAGE;

        static const std::string BROADCAST_MESSAGE;

        std::atomic<bool> processingStarted;
        bool stopDequeue = false;
        boost::mutex m_mutex;   // The mutex to synchronise on
        boost::condition_variable m_cond;// The condition to wait for

        std::queue<std::tuple<cv::Mat, std::string>> dataToSend;
       // boost::lockfree::spsc_queue <boost::tuple<cv::Mat &, std::string>, boost::lockfree::capacity<1024>> dataToSend;
        boost::mutex mtx_;

        JavaVM *gJavaVM;
        jobject gNativeObject;

        pthread_mutex_t jpegFileMutex;

        boost::shared_ptr<boost::thread> processImageThread;

        JNIEnv *getJniEnv();

        jobject getCallbackInterface(JNIEnv *env);

        std::string base64Encode(const unsigned char *buffer, size_t length, char **b64text);

        void unableToConnectZmq();

        void zmqConnectionDropped();

        void processImage(const char *urlPath, const char *authToken);

        void updateMessageSent(long messageSent);

        zmq::socket_t s_client_socket(zmq::context_t & context,const char *urlPath, const char *authToken);

        bool getDataFromDequeue(std::tuple<cv::Mat, std::string>& result);

    public:

        void storeJni(JavaVM *gjVM);

        void storeMessenger(jobject gnObject);

        void stop();

        void establishConnection(const char *urlPath, const char *authToken);
        void connectedZmq();

        void storedMessenger(jobject gNObject);

        void setJavaVm(JavaVM *javaVm);

        ZeroMqTransport();


        void addDataToQueue(cv::Mat image, long time);
    };

}

#endif //WAID_ZEROMQTRANSPORT_H
