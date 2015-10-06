//
// Created by kodjo baah on 01/08/2015.
//


//Encodes Base64
#include <openssl/bio.h>
#include <openssl/evp.h>
#include <openssl/buffer.h>
#include <stdint.h>


#include <boost/thread/mutex.hpp>
#include <boost/tuple/tuple.hpp>
#include <boost/thread/thread.hpp>

#include <android/log.h>
#include <jni.h>
#include <math.h>
#include <sched.h>
#include <string.h>
#include <zhelpers.hpp>
#include <string>
#include <vector>
#include <queue>
#include <exception>
#include <tuple>
#include <thread>
#include <functional>

#include <turbojpeg.h>

#include <time.h>
#include <string.h>

#include <chrono>         // std::chrono::seconds

#include <ZeroMqTransport.h>

#define LOG_TAG    "ZEROMQ_TRANSPORT"
#define LOG(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#define BUFFERSIZE 16777216
namespace  waid {


    const std::string ZeroMqTransport::CONNECT_MESSAGE = "CONNECT";
    const std::string ZeroMqTransport::IS_ALIVE_MESSAGE = "IS_VALID";
    const std::string ZeroMqTransport::BROADCAST_MESSAGE = "BROADCAST";
    const std::string ZeroMqTransport::TYPE_MESSAGE = "VIDEO";
    const std::string ZeroMqTransport::INIT_MESSAGE = "INIT";
    const std::string ZeroMqTransport::END_STREAM_MESSAGE = "END_STREAM";

    ZeroMqTransport::ZeroMqTransport() {

        pthread_mutex_init(&jpegFileMutex, NULL);
    }

    ZeroMqTransport::~ZeroMqTransport() {
        pthread_mutex_destroy(&jpegFileMutex);
    }

    void ZeroMqTransport::addDataToQueue(std::tuple < cv::Mat, std::string, std::string > frameData) {

        // Acquire lock on the queue
        boost::unique_lock <boost::mutex> lock(m_mutex);
        dataToSend.push(frameData);

        // Notify others that data is ready
        m_cond.notify_one();
    }

    bool ZeroMqTransport::getDataFromDequeue(std::tuple < cv::Mat, std::string, std::string > &result) {
        boost::unique_lock <boost::mutex> lock(m_mutex);
        while (dataToSend.empty() && (!stopDequeue)) {
            m_cond.wait(lock);
        }

        if (stopDequeue) {
            return false;
        }
        result = dataToSend.front();
        dataToSend.pop();
        return true;
    }

    void ZeroMqTransport::processImage(const char *urlPath, const char *authToken) {

        processingStarted = false;
        LOG("---------------------------------------------- PROCESSING IMAGE DATA -----------------------");

        int rc;
        int rcAudio;
        bool connected = true;

        zeroMq = new waid::ZeroMq("video");

        zeroMq->setNativeCommunicator(nativeCommunicator);

        rc = zeroMq->create_context();
        if (rc != 0) {
            connected = false;
        }

        if (connected != false) {
            zeroMqAudio = new waid::ZeroMq("audio");
            zeroMqAudio->setNativeCommunicator(nativeCommunicator);
            rcAudio = zeroMqAudio->create_context();

        }

        if (rcAudio != 0) {
            connected = false;
        }

        std::string auth(authToken);
        if (connected != false) {
            LOG("ZMQ_EVENT_1");
            rc = zeroMq->create_socket(auth, urlPath);
        }

        if (rc != 0) {
            connected = false;
        }

        if (connected != false) {
            LOG("ZMQ_EVENT_2");
            rcAudio = zeroMqAudio->create_socket(auth, urlPath);
        }

        if (rcAudio != 0) {
            connected = false;
        }


        if (connected) {
            zeroMq->s_send(strdup(CONNECT_MESSAGE.c_str()));
            std::vector <std::string> messages = zeroMq->s_recv_multi(2);

            std::string connectString;
            if (messages.size() > 0) {
                connectString = messages[0];
                streamId = messages[1];

            }
            if (strcmp(IS_ALIVE_MESSAGE.c_str(), connectString.c_str()) != 0) {
                LOG("IS_ALIVE_MESSAGE[%s] connectString[%s]", IS_ALIVE_MESSAGE.c_str(), connectString.c_str());
                nativeCommunicator->invalidToken();
            } else {

                nativeCommunicator->updateStreamToken(streamId);

                /*
                 * Start recording the sound
                 */
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
                    std::tuple <cv::Mat, std::string, std::string> frameData;

                    bool result = getDataFromDequeue(frameData);
                    if (result) {
                        in = std::get<0>(frameData);
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

                        int compResults = tjCompress2(_jpegCompressor, outdata, _width, 0,
                                                      _height,
                                                      TJPF_RGB,
                                                      &_compressedImage, &_jpegSize, TJSAMP_440,
                                                      JPEG_QUALITY,
                                                      flags);
                        newSize.release();
                        newOut.release();

                        int len = (int) (_jpegSize);
                        char *messageToSend;
                        std::string message = base64Encode(
                                reinterpret_cast<const unsigned char *>(_compressedImage),
                                (int) _jpegSize, &messageToSend);

                        std::vector <std::string> messages;
                        messages.push_back(BROADCAST_MESSAGE);
                        messages.push_back(streamId);
                        std::string fd = std::get<1>(frameData);
                        messages.push_back(fd);
                        messages.push_back(message);
                        std::string frameps = std::get<2>(frameData);
                        messages.push_back(frameps);

                        try {
                            zeroMq->s_sendmultiple(messages);
                        } catch (std::exception &e) {
                            processingStarted = false;
                            nativeCommunicator->connectionLost();
                        }
                        messages.clear();
                        std::vector<std::string>(messages).swap(messages);
                        tjFree(_compressedImage);
                        pthread_mutex_unlock(&jpegFileMutex);
                    } else {
                        LOG("--EMPTY_IMAGE_DATA");
                    }
                }
                tjDestroy(_jpegCompressor);
            }
        }

        delete zeroMq;
        delete zeroMqAudio;

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


    void ZeroMqTransport::startSoundCapture() {


        if (!processingStarted) {
            //Waiting for half a second before trying to start the sound
            LOG("SLEEPING_FOR_SECOND_BEFORE_STARTING_SOUND");
            std::this_thread::sleep_for(std::chrono::seconds(1));
        }

        if (processingStarted) {
            LOG("STARTING_SOUND");
            soundCapture = new waid::SoundCapture(streamId);
            soundCapture->setZeroMq(zeroMqAudio);
            soundCapture->startRecording();
            soundCapture->processRecording();
        }

    }

    void ZeroMqTransport::endStream() {

        LOG("ENDSTREAM_BEFORE");
        std::vector <std::string> messages;
        messages.push_back(END_STREAM_MESSAGE);
        messages.push_back(streamId);

        try {
            zeroMq->s_sendmultiple(messages);
        } catch (std::exception &e) {
            processingStarted = false;
            nativeCommunicator->connectionLost();
        }
        LOG("ENDSTREAM_AFTER");


    }
    void ZeroMqTransport::stopSoundCapture() {
        LOG("STOPPING");
        soundCapture->stopRecording();
        LOG("STOPPED");
        delete soundCapture;
    }

    void ZeroMqTransport::stop() {


        LOG("---ZEROMQ-INTERUPTINT");
        stopDequeue = true;
        endStream();
        if (processingStarted) {
            processingStarted = false;
            LOG("--ZEROMQ-INT-1");
            if (zeroMq) {
                zeroMq->stopAll();
            }
            LOG("--ZEROMQ-INT-2");
            if (zeroMqAudio) {
                zeroMqAudio->stopAll();
            }
            LOG("--ZEROMQ-INT-3");
            processImageThread->interrupt();
            LOG("--ZEROMQ-INT-4");
            m_cond.notify_one();
            LOG("--ZEROMQ-INT-5");

        }
        LOG("--ZEROMQ-INT-6");
        while (processingStarted) {
            LOG("--WAITING_FOR_PROCESSING TO STOPE");
        }

        LOG("---ZEROMQ-INT-COMPLETED");

    }

    void ZeroMqTransport::establishConnection(const char *urlPath, const char *authToken) {
        stopDequeue = false;
        boost::shared_ptr <boost::thread> pt(
                new boost::thread(&ZeroMqTransport::processImage, this, urlPath, authToken));
        processImageThread.swap(pt);
        LOG("---ESTABLISHMENT COMPLETE 1 ------");

    }

    void ZeroMqTransport::setNativeCommunicator(NativeCommunicator * nc) {
        nativeCommunicator = nc;
    }
}