//
// Created by kodjo baah on 10/08/2015.
//

#include <WaidCamera.h>
#include <boost/thread.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>

#include <VideoRendererVbo.h>
#include <ZeroMqTransport.h>

#include <boost/date_time/gregorian/gregorian.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/chrono.hpp>


#define  LOG_TAG    "WAIDCAMERA"
#define LOG(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
namespace waid {


    pthread_mutex_t WaidCamera::FGmutex=PTHREAD_MUTEX_INITIALIZER;

    void WaidCamera::addToQueue() {

        boost::unique_lock <boost::mutex> lock(m_mutex);
        while (dataToSend.empty() && (!stopZeroMqDeque)) {
            zmq_send_cond.wait(lock);
        }

        if (!stopZeroMqDeque) {
            std::tuple <cv::Mat, std::string, std::string> result = dataToSend.front();
            dataToSend.pop();

            std::string framePs  = std::get<2>(result);
            std::string time = std::get<1>(result);
            cv::Mat outframe = std::get<0>(result);
            cv::Mat rgbframe;
            if (orientation == 0) { //landscape
                cv::flip(outframe, rgbframe, -1);
            } else {
                cv::flip(outframe, rgbframe, 1);
            }

            std::tuple <cv::Mat, std::string, std::string> frameData(rgbframe, time,framePs);
            zeroMqTransport->addDataToQueue(frameData);
            outframe.release();

        }
    }

    void WaidCamera::sendToZeroMqTransport() {
        sendingToZeroMq = true;
        while (!stopZeroMqDeque) {
            try {
                boost::this_thread::interruption_point();
            }
            catch (const boost::thread_interrupted &) {
                LOG("ZMQ_BREAK");
                break;
            }

            addToQueue();

        }
        sendingToZeroMq = false;
    }

    void WaidCamera::renderFrame() {

        cv::Mat fr;
        cv::Mat outframe;
        cv::Mat rgbframe;
        cv::Mat rgbOne;

        //LOG("RENDER_FRAME_BEGIN[%d]",bufferIndex);
        if (bufferIndex > 0) {
            pthread_mutex_lock(&FGmutex);
            buffer[(bufferIndex - 1) % 30].copyTo(fr);
            pthread_mutex_unlock(&FGmutex);

            if (fr.empty() == false) {
                cv::cvtColor(fr, outframe, CV_BGR2BGR565);

                /*
                std::stringstream s;
                s << "Display performance: " << outframe.cols << "x" << outframe.rows << "@" <<
                fps;
                cv::putText(outframe, s.str(), cv::Point(outframe.rows / 4, outframe.cols / 4),
                            cv::FONT_HERSHEY_PLAIN, 2.5, cv::Scalar(0, 255, 0, 255), 2, CV_AA);

                 */
                if (orientation == 0) { //landscape
                    cv::flip(outframe, rgbframe, -1);
                } else {
                    cv::flip(outframe, rgbframe, 1);
                }


                cv::Mat frameToSend;
                pthread_mutex_lock(&FGmutex);
                frameToSend = rgbframe.clone();
                videoRendererVbo->renderFrame(screenWidth, screenHeight, rgbframe);
                outframe.release();
                pthread_mutex_unlock(&FGmutex);
            } else {
                LOG("DAMN---VIDOEO IS EMPTY");
            }

        }
        //LOG("RENDER_FRAME_END");

    }

    void WaidCamera::frameRetriever() {

        LOG("READING-FRAME");

        videoStopped = 1;
        cv::Mat drawing_frame;
        std::queue <int64> time_queue;

        boost::posix_time::ptime current_date_microseconds = current_date_microseconds = boost::posix_time::microsec_clock::local_time();
        previousFrameTime = current_date_microseconds.time_of_day().total_milliseconds();

        numberOfFramesSent = 0;
        int count = 0;
        if (!capture->grab()) {
            nativeCommunicator->unableToInitalizeCamera();
        } else {
            while (true) {
                cameraStarted = true;
                try {
                    boost::this_thread::interruption_point();
                }
                catch (const boost::thread_interrupted &) {
                    break;
                }

                int64 then;
                int64 now = cv::getTickCount();
                time_queue.push(now);

                LOG("READ %d", capture->isOpened());
                // Capture frame from camera and draw it
                if (capture->isOpened()) {

                    capture->read(drawing_frame);

                    pthread_mutex_lock(&FGmutex);
                    int loc = bufferIndex++ % 30;
                    drawing_frame.copyTo(buffer[loc]);

                    pthread_mutex_unlock(&FGmutex);

                    boost::posix_time::ptime current_date_microseconds = boost::posix_time::microsec_clock::local_time();
                    long currentFrameTime = current_date_microseconds.time_of_day().total_milliseconds();
                    long diffbetweenFrame = currentFrameTime - previousFrameTime;
                    previousFrameTime = currentFrameTime;

                    cv::Mat anotherCopy;
                    drawing_frame.copyTo(anotherCopy);
                    if (shouldSendToZmq) {
                        LOG("---SHOULD-SEND-TO-ZMQ [%d][%d]", count, (int) diffbetweenFrame);
                        count = count + 1;
                        cv::Mat o;
                        cv::cvtColor(anotherCopy, o, CV_BGR2BGR565);
                        std::string arrivalTime;
                        std::stringstream strstream;
                        strstream << diffbetweenFrame;
                        strstream >> arrivalTime;

                        std::string framePs;
                        std::stringstream fpsstream;
                        fpsstream << fps;
                        fpsstream >> framePs;

                        std::tuple <cv::Mat, std::string, std::string> frameData(o, arrivalTime,
                                                                                 framePs);
                        // Acquire lock on the queue
                        boost::lock_guard <boost::mutex> lock(m_mutex);
                        dataToSend.push(frameData);
                        zmq_send_cond.notify_one();
                    } else {
                        LOG("--NOT-SENDKNG-TO-ZMQ[%d]",drawing_frame.empty());
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

        }
        capture.release();
        videoStopped = 0;
        cameraStarted = false;
        LOG("VIDEO-READER-THREAD-ENDED");

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

        capture = new cv::VideoCapture(camera);
        LOG("INITIALIZE-DONG-WIDTH=%d, HIEGHT=%d", width, height);

        if (capture->isOpened()) {
            union {
                double prop;
                const char *name;
            } u;

            u.prop = capture->get(CV_CAP_PROP_SUPPORTED_PREVIEW_SIZES_STRING);

            cv::Size camera_resolution;

            camera_resolution = calc_optimal_camera_resolution(u.name, width, height);

            LOG("INITIALIZE-AFTER_CALC_RESOLUTION");
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

            LOG("INITIALIZE-NEW_FRAME_DIMENSIONS WIDTH[%d] HIEGHT[%d]", frameWidth, frameHeight);
        } else {
            LOG("INITIALIZE-UNABLE_TO_OPEN_CAMERA");
        }

    }

    void WaidCamera::startCamera(int width, int height, int camera) {

        initialize(width, height, camera);

        videoRendererVbo->setupGraphics(width, height);

        cameraOperatorThread = new boost::thread(&WaidCamera::frameRetriever, this);

    }

    void WaidCamera::sendToZeroMq(const char *urlPath, const char *authToken) {
        shouldSendToZmq = true;
        stopZeroMqDeque = false;
        zeroMqTransport->establishConnection(urlPath, authToken);
        zeromqTransportThread = new boost::thread(&WaidCamera::sendToZeroMqTransport, this);
        zeroMqTransport->startSoundCapture();
    }

    void WaidCamera::stopSendToZeroMq() {
        shouldSendToZmq = false;

        LOG("ZEROMQ_STOPPING_TRANSPORT");
        zeroMqTransport->stop();
        LOG("ZEROMQ_STOPPING_SOUNDCAPTURE");
        zeroMqTransport->stopSoundCapture();
        LOG("ZEROMQ_STOPPING_SOUNDCAPTURE_STOPPED");
        //zeroMqTransportThread->interrupt();
        zmq_send_cond.notify_one();
        stopZeroMqDeque = true;

        LOG("ZEROMQ_STOP_[%s]", sendingToZeroMq ? "true" : "false");
        while (sendingToZeroMq) {
            LOG("ZEROMQ_SEND_WAITING_TO FINISH");
        }

        //delete zeroMqTransportThread;

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

    void WaidCamera::stop() {

        if (cameraStarted) {
            cameraOperatorThread->interrupt();
            while (videoStopped) {
                LOG("WAITING-FOR-VIDEO-STOPED[%d]", videoStopped);
            }
            delete cameraOperatorThread;
        }
        cameraStarted = false;
    }

    void WaidCamera::start() {
        initialize(frameWidth, frameHeight, 0);
        cameraOperatorThread = new boost::thread(&WaidCamera::frameRetriever, this);
    }

    void WaidCamera::restartCamera(int camera) {
        cameraOperatorThread->interrupt();
        while (videoStopped) {
            LOG("WAITING-FOR-VIDEO-STOPED[%d]", videoStopped);
        }
        delete cameraOperatorThread;

        initialize(frameWidth, frameHeight, camera);
        cameraOperatorThread = new boost::thread(&WaidCamera::frameRetriever, this);

    }

    void WaidCamera::setNativeCommunicator(waid::NativeCommunicator * nc) {
        nativeCommunicator = nc;
    }

    bool WaidCamera::isOpened() {
        return capture->isOpened();
    }
    void WaidCamera::cleanUp() {
        stopSendToZeroMq();
        stop();
        delete videoRendererVbo;
        delete zeroMqTransport;

    }

    WaidCamera::~WaidCamera() {

        delete videoRendererVbo;
        delete zeroMqTransport;

    }

    WaidCamera::WaidCamera() : videoRendererVbo(new waid::VideoRendererVbo()),
                               zeroMqTransport(new waid::ZeroMqTransport()) {
        shouldSendToZmq = false;
    }

}