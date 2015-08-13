//
// Created by kodjo baah on 10/08/2015.
//

#ifndef WAID_WAIDCAMERA_H
#define WAID_WAIDCAMERA_H


#include <boost/thread.hpp>
#include <boost/thread/thread.hpp>
#include <boost/atomic.hpp>

//#include  <atomic>

#include <cv.h>
#include <highgui.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include <opencv2/highgui/highgui.hpp>
#include <VideoRendererVbo.h>
#include <ZeroMqTransport.h>

namespace waid {
    class WaidCamera {

    private:

        waid::VideoRendererVbo *videoRendererVbo;
        waid::ZeroMqTransport *zeroMqTransport;

        boost::thread *cameraOperatorThread;

        JavaVM *gJavaVM;
        jobject gNativeObject;

        cv::Ptr <cv::VideoCapture> capture;
        cv::Mat buffer[30];

        int bufferIndex;
        int frameWidth;
        int frameHeight;
        int screenWidth;
        int screenHeight;
        int orientation = 0;
        int fps =0;

        boost::atomic <bool> shouldSendToZmq;

        pthread_mutex_t FGmutex;

        int processVideo = 1;
        int videoStopped = 1;

        void frameRetriever();

        cv::Size calc_optimal_camera_resolution(const char *supported, int width, int height);

        JNIEnv *getJniEnv();
        jobject getCallbackInterface(JNIEnv *env);

    public:
        WaidCamera();

        ~WaidCamera();

        void renderFrame();

        void initialize(int width, int height, int camera);

        void startCamera(int width, int height, int camera);

        void stop();

        void start();

        void sendToZeroMq(const char *urlPath, const char *authPath);

        void stopSendToZeroMq();

        void storeJni(JavaVM *gjVM);

        void storeMessenger(jobject gnObject);

        void resize(int width, int height);

        void setOrientation(int orient);

        void restartCamera(int camera);

    };

}


#endif //WAID_WAIDCAMERA_H
