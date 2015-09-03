//
// Created by kodjo baah on 20/08/2015.
//

#ifndef WAID_SOUNDCAPTURE_H
#define WAID_SOUNDCAPTURE_H

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>


#include <stdio.h>
#include <assert.h>
#include <pthread.h>
#include <sstream>
#include <queue>
#include <vector>
#include <boost/shared_array.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include <boost/atomic.hpp>

#include <ZeroMq.h>

// 1 seconds of recorded audio at 16 kHz mono, 16-bit signed little endian
#define RECORDER_FRAMES (16000 * 1)
#define RECORD_TIME 15


namespace  waid {
    class SoundCapture {

    private:


        static const std::string BROADCAST_MESSAGE;
        static const std::string TYPE_MESSAGE;

        std::string streamId;
        //engine interfaces
        SLEngineItf engineEngine;
        SLObjectItf engineObject;

        //recorder interfaces
        SLObjectItf recorderObject;
        SLRecordItf recorderRecord;
        SLAndroidSimpleBufferQueueItf recorderBufferQueue;

        SLDataFormat_PCM format_pcm;
        SLDataLocator_AndroidSimpleBufferQueue loc_bq;

        waid::ZeroMq *zeroMq;

        long previousSampleTime;
        short *recorderBuffer;
        FILE *recordF;
        int recordCnt;

        boost::atomic<bool> hasRecordingStop;
        boost::atomic<bool> processSoundMessage;
        boost::condition_variable m_cond;// The condition to wait for
        boost::mutex m_mutex;   // The mutex to synchronise on
        std::queue <std::vector<short>> *filledIndices;
        boost::shared_ptr <boost::thread> recordingThread;

    public:
        SoundCapture(std::string sId);

        static void bqRecorderStaticCallback(SLAndroidSimpleBufferQueueItf bq, void *context);

        void bqRecorderCallback(SLAndroidSimpleBufferQueueItf bq, void *context);

        void startRecording();

        void stopRecording();

        void processRecording();

        void storeRecording();

        void setZeroMq(waid::ZeroMq *zmw);

        ~SoundCapture();

    };
}


#endif //WAID_SOUNDCAPTURE_H
