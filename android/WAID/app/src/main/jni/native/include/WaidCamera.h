//
// Created by kodjo baah on 10/08/2015.
//

#ifndef WAID_WAIDCAMERA_H
#define WAID_WAIDCAMERA_H


#include <boost/thread.hpp>
#include <boost/thread/thread.hpp>
#include <boost/atomic.hpp>

//#include  <atomic>

#include <queue>

#include <cv.h>
#include <highgui.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include <opencv2/highgui/highgui.hpp>
#include <VideoRendererVbo.h>
#include <ZeroMqTransport.h>
#include <NativeCommunicator.h>
#include <ZeroMq.h>

namespace waid {
    class WaidCamera {

    private:

        boost::atomic <bool> shouldSendToZmq;
        bool stopZeroMqDeque = false;
        bool sendingToZeroMq = false;
        bool cameraStarted = false;

        boost::condition_variable zmq_send_cond;
        boost::mutex m_mutex;   // The mutex to synchronise on
        std::queue<std::tuple<cv::Mat, std::string, std::string>> dataToSend;

        long previousFrameTime;
        int numberOfFramesSent;

        waid::VideoRendererVbo *videoRendererVbo;
        waid::ZeroMqTransport *zeroMqTransport;
        waid::NativeCommunicator *nativeCommunicator;

        boost::thread *cameraOperatorThread;
        boost::thread *zeromqTransportThread;

        cv::Ptr <cv::VideoCapture> capture;
        cv::Mat buffer[30];

        int bufferIndex;
        int frameWidth;
        int frameHeight;
        int screenWidth;
        int screenHeight;
        int orientation = 0;
        int fps =0;


        static pthread_mutex_t FGmutex;

        int processVideo = 1;
        int videoStopped = 1;

        void frameRetriever();

        cv::Size calc_optimal_camera_resolution(const char *supported, int width, int height);


    public:

        WaidCamera();

        ~WaidCamera();

        bool isOpened();

        void renderFrame();

        void initialize(int width, int height, int camera);

        void startCamera(int width, int height, int camera);

        void stop();

        void start();

        void sendToZeroMq(const char *urlPath, const char *authPath);

        void stopSendToZeroMq();

        void resize(int width, int height);

        void setOrientation(int orient);

        void restartCamera(int camera);

        void setNativeCommunicator(NativeCommunicator *nc);

        void sendToZeroMqTransport();

        void cleanUp();

        void addToQueue();

    };

}


#endif //WAID_WAIDCAMERA_H
