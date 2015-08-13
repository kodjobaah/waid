//
// Created by kodjo baah on 10/08/2015.
//

#include "WaidCamera.h"
#include <boost/thread.hpp>
#include <boost/thread/thread.hpp>

#include <VideoRendererVbo.h>
#include <ZeroMqTransport.h>

#define  LOG_TAG    "WAIDCAMERA"
#define LOG(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
namespace waid {

    void WaidCamera::renderFrame() {

        cv::Mat fr;
        cv::Mat outframe;
        cv::Mat rgbframe;
        cv::Mat rgbOne;

        if ((bufferIndex > 0) && (processVideo)) {
            pthread_mutex_lock(&FGmutex);
            buffer[(bufferIndex - 1) % 30].copyTo(fr);
            pthread_mutex_unlock(&FGmutex);

            if (fr.empty() == false) {
                cv::cvtColor(fr, outframe, CV_BGR2BGR565);
                //cv::cvtColor(fr, rgbframe, CV_BGR2BGR565);
                //cv::cvtColor(fr, outframe, CV_BGR2RGBA);

                std::stringstream s;
                s << "Display performance: " << outframe.cols << "x" << outframe.rows << "@" <<
                fps;
                cv::putText(outframe, s.str(), cv::Point(outframe.rows / 4, outframe.cols / 4),
                            cv::FONT_HERSHEY_PLAIN, 2.5, cv::Scalar(0, 255, 0, 255), 2, CV_AA);

                // cv::transpose(outframe,outframe);
                if (orientation == 0) { //landscape
                    cv::flip(outframe, rgbframe, -1);
                } else {
                    cv::flip(outframe, rgbframe, 1);
                }




                cv::Mat frameToSend;
                pthread_mutex_lock(&FGmutex);
                rgbframe.copyTo(frameToSend);
                videoRendererVbo->renderFrame(screenWidth, screenHeight, rgbframe);

                pthread_mutex_unlock(&FGmutex);
                //videoRendererNoVbo.renderFrame(screenWidth,screenHeight,outframe);
                //videoRenderer.draw(outframe);
            } else {
                LOG("DAMN---VIDOEO IS EMPTY");
            }

        }

    }

    void WaidCamera::frameRetriever() {

        LOG("READING-FRAME");

        videoStopped = 1;
        cv::Mat drawing_frame;
        std::queue <int64> time_queue;

        boost::posix_time::ptime current_date_microseconds = boost::posix_time::microsec_clock::local_time();
        long previousFrameTime = current_date_microseconds.time_of_day().total_milliseconds();

        int count = 0;
        while (true) {

            try {
                boost::this_thread::interruption_point();
            }
            catch (const boost::thread_interrupted &) {
                break;
            }

            int64 then;
            int64 now = cv::getTickCount();
            time_queue.push(now);

            LOG("READ %d", capture.empty());
            // Capture frame from camera and draw it
            if (!capture.empty()) {
                // if (capture->grab())
                //  capture->retrieve(drawing_frame, CV_CAP_ANDROID_COLOR_FRAME_RGBA);

                capture->read(drawing_frame);
                // capture->retrieve(drawing_frame, CV_CAP_ANDROID_COLOR_FRAME_BGRA);

                pthread_mutex_lock(&FGmutex);
                int loc = bufferIndex++ % 30;
                drawing_frame.copyTo(buffer[loc]);

                pthread_mutex_unlock(&FGmutex);

                boost::posix_time::ptime current_date_microseconds = boost::posix_time::microsec_clock::local_time();
                long currentFrameTime = current_date_microseconds.time_of_day().total_milliseconds();
                long diffbetweenFrame = currentFrameTime - previousFrameTime;

                cv::Mat anotherCopy;
                drawing_frame.copyTo(anotherCopy);
                if (shouldSendToZmq) {
                    LOG("---SHOULD-SEND-TO-ZMQ [%d]",count);
                    count = count + 1;
                    cv::Mat o;
                    cv::cvtColor(anotherCopy, o, CV_BGR2BGR565);
                    zeroMqTransport->addDataToQueue(o,diffbetweenFrame);
                } else {
                    LOG("--NOT-SENDKNG-TO-ZMQ");
                }
            }

            if (time_queue.size() >= 2)
                then = time_queue.front();
            else
                then = 0;

            if (time_queue.size() >= 25)
                time_queue.pop();

            fps = time_queue.size() * (float) cv::getTickFrequency() / (now - then);
        }
        LOG("Camera Closed");
        capture.release();
        videoStopped = 0;
        LOG("DIG-DIG %d", videoStopped);

    }

    cv::Size WaidCamera::calc_optimal_camera_resolution(const char *supported, int width,
                                                        int height) {
        int frame_width = 0;
        int frame_height = 0;

        size_t prev_idx = 0;
        size_t idx = 0;
        float min_diff = FLT_MAX;

        do {
            int tmp_width;
            int tmp_height;

            prev_idx = idx;
            while ((supported[idx] != '\0') && (supported[idx] != ','))
                idx++;

            sscanf(&supported[prev_idx], "%dx%d", &tmp_width, &tmp_height);

            int w_diff = width - tmp_width;
            int h_diff = height - tmp_height;
            if ((h_diff >= 0) && (w_diff >= 0)) {
                if ((h_diff <= min_diff) && (tmp_height <= 720)) {
                    frame_width = tmp_width;
                    frame_height = tmp_height;
                    min_diff = h_diff;
                }
            }

            idx++; // to skip comma symbol

        } while (supported[idx - 1] != '\0');

        return cv::Size(frame_width, frame_height);
    }


    void WaidCamera::initialize(int width, int height, int camera) {

        LOG("STARTING-CAMERA w=%d, h=%d", width, height);
        LOG("BOOLAH-1");
        capture = new cv::VideoCapture(camera);
        LOG("BOOLAH-3");

        union {
            double prop;
            const char *name;
        } u;
        u.prop = capture->get(CV_CAP_PROP_SUPPORTED_PREVIEW_SIZES_STRING);

        LOG("SUPPORTED-STRING %s", u.name);
        cv::Size camera_resolution;

        //if (u.name)
        camera_resolution = calc_optimal_camera_resolution(u.name, width, height);

        if ((camera_resolution.width != 0) && (camera_resolution.height != 0)) {
            capture->set(CV_CAP_PROP_FRAME_WIDTH, camera_resolution.width);
            capture->set(CV_CAP_PROP_FRAME_HEIGHT, camera_resolution.height);
        }

        float scale = std::min((float) width / camera_resolution.width,
                               (float) height / camera_resolution.height);

        LOG("Camera initialized at resolution %dx%d", camera_resolution.width,
            camera_resolution.height);

        frameWidth = camera_resolution.width;
        frameHeight = camera_resolution.height;

        screenWidth = frameWidth;
        screenHeight = frameHeight;

        LOG("DONG-WIDTH=%d, HIEGHT=%d", frameWidth, frameHeight);


    }

    void WaidCamera::startCamera(int width, int height, int camera) {

        initialize(width,height,camera);

        videoRendererVbo->setupGraphics(width, height);

        cameraOperatorThread = new boost::thread(&WaidCamera::frameRetriever,this);

    }

    void WaidCamera::sendToZeroMq(const char *urlPath, const char *authToken) {
        shouldSendToZmq = true;
        zeroMqTransport->establishConnection(urlPath,authToken);
    }

    void WaidCamera::stopSendToZeroMq() {
        shouldSendToZmq = false;
        zeroMqTransport->stop();
    }

    void WaidCamera::setOrientation(int orient) {
        orientation = orient;
    }

    void WaidCamera::resize(int width, int height) {

        //TODO:
        //union {double prop; const char* name;} u;
        //u.prop = capture->get(CV_CAP_PROP_SUPPORTED_PREVIEW_SIZES_STRING);
        //cv::Size camera_resolution = calc_optimal_camera_resolution(u.name,width,height);
        //LOG("BLOO-RESIZE-2 width=%d height=%d", camera_resolution.width, camera_resolution.height);
        //frameWidth = camera_resolution.width;
        //frameHeight = camera_resolution.height;
    }

    void WaidCamera::storeJni(JavaVM *gjVM){
        gJavaVM = gjVM;
        zeroMqTransport->storeJni(gjVM);
    }

    void WaidCamera::storeMessenger(jobject gnObject){
        gNativeObject = gnObject;
        zeroMqTransport->storeMessenger(gnObject);
    }


    void WaidCamera::stop() {
        LOG("WAITIING_STOP");
        cameraOperatorThread->interrupt();
        while(videoStopped) {
            LOG("WAITING-FOR-VIDEO-STOPED[%d]",videoStopped);
        }
        LOG("CAMERA_STOPPED");

        delete cameraOperatorThread;

        LOG("THREAD_DELETED");
    }

    void WaidCamera::start() {
        LOG("STARTING-CAMERA");
        initialize(frameWidth,frameHeight,0);
        LOG("CAMERA_INIT_COMPLETE");
        cameraOperatorThread = new boost::thread(&WaidCamera::frameRetriever,this);
        LOG("RETRIEVER STARTED");
    }

    void WaidCamera::restartCamera(int camera) {

        LOG("--RESTARTING-CAMERA-");
        cameraOperatorThread->interrupt();
        while(videoStopped) {
            LOG("WAITING-FOR-VIDEO-STOPED[%d]",videoStopped);
        }
        delete cameraOperatorThread;

        LOG("--STARTING-CAMERA-INIT");
        initialize(frameWidth,frameHeight,camera);
        LOG("--CAMERA-INIT-COMPLETE");
        cameraOperatorThread = new boost::thread(&WaidCamera::frameRetriever,this);
        LOG("---CAMREA-THREAD-STARTED");
    }

    WaidCamera::~WaidCamera() {

        delete videoRendererVbo;
        delete zeroMqTransport;

    }

    WaidCamera::WaidCamera():videoRendererVbo(new waid::VideoRendererVbo()),zeroMqTransport(new waid::ZeroMqTransport()) {
        shouldSendToZmq = false;

    }


    jobject WaidCamera::getCallbackInterface(JNIEnv *env) {

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

    JNIEnv * WaidCamera::getJniEnv() {

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


}