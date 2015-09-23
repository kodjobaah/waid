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

#include <NativeCommunicator.h>
#include <SoundCapture.h>
#include <ZeroMq.h>

#define LOG_TAG_MONITOR    "ZEROMQ_MONITOR"
#define LOGM(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG_MONITOR, __VA_ARGS__)

namespace  waid {

    class ZeroMqTransport {

    private:

        static const std::string IS_ALIVE_MESSAGE;
        static const std::string BROADCAST_MESSAGE;
        static const std::string TYPE_MESSAGE;
        static const std::string INIT_MESSAGE;
        static const std::string END_STREAM_MESSAGE;

        std::string streamId;

        std::atomic <bool> processingStarted;
        bool stopDequeue = false;

        boost::mutex m_mutex;   // The mutex to synchronise on

        boost::shared_ptr <boost::thread> monitorThread;
        std::queue <std::tuple<cv::Mat, std::string, std::string>> dataToSend;

        pthread_mutex_t jpegFileMutex;

        waid::ZeroMq *zeroMq;
        waid::ZeroMq *zeroMqAudio;
        waid::SoundCapture *soundCapture;
        waid::NativeCommunicator *nativeCommunicator;

        boost::shared_ptr <boost::thread> processImageThread;

        std::string base64Encode(const unsigned char *buffer, size_t length, char **b64text);

        void processImage(const char *urlPath, const char *authToken);

        bool getDataFromDequeue(std::tuple <cv::Mat, std::string, std::string> &result);

    public:

        static const std::string CONNECT_MESSAGE;

        ZeroMqTransport();

        ~ZeroMqTransport();

        void stop();

        void startSoundCapture();

        boost::condition_variable m_cond;// The condition to wait for

        void stopSoundCapture();

        void establishConnection(const char *urlPath, const char *authToken);

        void addDataToQueue(std::tuple <cv::Mat, std::string, std::string> frameData);

        void setNativeCommunicator(NativeCommunicator *nc);

        void endStream();

    };

}

#endif //WAID_ZEROMQTRANSPORT_H
