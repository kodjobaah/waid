//
// Created by kodjo baah on 20/08/2015.
//

#include <SoundCapture.h>

#include <exception>
#include <android/log.h>
#include <boost/date_time/gregorian/gregorian.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/chrono.hpp>


#define  LOG_TAG    "SOUND_CAPTURE"
#define LOG(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

namespace waid {

    const std::string SoundCapture::BROADCAST_MESSAGE = "BROADCAST";
    const std::string SoundCapture::TYPE_MESSAGE = "AUDIO";

    SoundCapture::SoundCapture(std::string sId) {
        LOG("AUDIO_CREATE");
        streamId = sId;
        SLresult result;

        hasRecordingStop = false;
        processSoundMessage = true;
        /*
         * *****************************************************************************************
         * ******************* CREATING ENGINE *****************************************************
         * *****************************************************************************************
        */
        // create engine
        result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
        assert(SL_RESULT_SUCCESS == result);

        // realize the engine
        result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
        assert(SL_RESULT_SUCCESS == result);

        // get the engine interface, which is needed in order to create other objects
        result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
        assert(SL_RESULT_SUCCESS == result);

        /*
         * *****************************************************************************************
         * ******************* CREATING AUDIO RECORDER  ********************************************
         * *****************************************************************************************
        */
        SLDataLocator_IODevice loc_dev = {SL_DATALOCATOR_IODEVICE, SL_IODEVICE_AUDIOINPUT,
                                          SL_DEFAULTDEVICEID_AUDIOINPUT, NULL};
        SLDataSource audioSrc = {&loc_dev, NULL};
        //audio sink: set to PCM format
        SLDataLocator_AndroidSimpleBufferQueue loc_bq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
                                                         1};
        SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, 1, SL_SAMPLINGRATE_16,
                                       SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
                                       SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN};
        SLDataSink audioSnk = {&loc_bq, &format_pcm};
        // create audio recorder
        // (requires the RECORD_AUDIO permission)
        const SLInterfaceID id[1] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
        const SLboolean req[1] = {SL_BOOLEAN_TRUE};
        result = (*engineEngine)->CreateAudioRecorder(engineEngine, &recorderObject, &audioSrc,
                                                      &audioSnk, 1, id, req);
        if (SL_RESULT_SUCCESS != result) {
            LOG("cannot create audio recorder");
        }
        assert(SL_RESULT_SUCCESS == result);
        // realize the audio recorder
        result = (*recorderObject)->Realize(recorderObject, SL_BOOLEAN_FALSE);
        if (SL_RESULT_SUCCESS != result) {
            LOG("cannot realize audio recorder");
        }
        assert(SL_RESULT_SUCCESS == result);
        // get the record interface
        result = (*recorderObject)->GetInterface(recorderObject, SL_IID_RECORD, &recorderRecord);
        assert(SL_RESULT_SUCCESS == result);
        // get the android specific buffer queue interface
        result = (*recorderObject)->GetInterface(recorderObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                                                 &recorderBufferQueue);
        assert(SL_RESULT_SUCCESS == result);
        // register callback on the buffer queue
        result = (*recorderBufferQueue)->RegisterCallback(recorderBufferQueue,
                                                          bqRecorderStaticCallback, this);
        assert(SL_RESULT_SUCCESS == result);
        recordCnt = 0;

        // recordF = fopen("/sdcard/waidsound.pcm", "wb");
        if (recordF == NULL) {
            LOG("UNABLE_TO_CREATE_FILE");
        }
        recorderBuffer = new short[RECORDER_FRAMES];

        filledIndices = new std::queue <std::vector<short>>();
    }


    void SoundCapture::bqRecorderStaticCallback(SLAndroidSimpleBufferQueueItf bq, void *context) {
        ((SoundCapture *) context)->bqRecorderCallback(bq, NULL);
    }

    // this callback handler is called every time a buffer finishes recording
    void SoundCapture::bqRecorderCallback(SLAndroidSimpleBufferQueueItf bq, void *context) {


        if (processSoundMessage) {
            LOG("AUDIO_LOCK_GETTING_LOCK_TO_STORE");
            boost::unique_lock <boost::mutex> lock(m_mutex);
            std::vector <short> element;
            element.resize(RECORDER_FRAMES);
            //int numOfRecords = fwrite(recorderBuffer, sizeof(short), RECORDER_FRAMES, recordF);
            std::copy(recorderBuffer, recorderBuffer + RECORDER_FRAMES, element.begin());
            filledIndices->push(element);
            m_cond.notify_one();

            LOG("AUDIO_LOCK_UNLOCKING_LOCK_TO_STORE");
            recordCnt++;
            SLresult result;
            //if (recordCnt * 5 < RECORD_TIME) {
            //enqueue the buffer again
            LOG("AUDIO_SOUNDWRITE4");
            if (processSoundMessage) {
                result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, recorderBuffer,
                                                         RECORDER_FRAMES * sizeof(short));
            }
            LOG("AUDIO_SOUNDWRITE5");
        }
        //} else {
        //    LOG("AUDIO_SOUNDWRITE6");
        //    result = (*recorderRecord)->SetRecordState(recorderRecord, SL_RECORDSTATE_STOPPED);
        //    if (SL_RESULT_SUCCESS == result) {
        //        LOG("recorder stopped");
        //        fclose(recordF);
        //    }
        //}

    }


    void SoundCapture::storeRecording() {

        LOG("AUDIO_RECORDING_ABOUT_TO_START");
        std::vector <short> front;

        boost::posix_time::ptime current_date_microseconds = current_date_microseconds = boost::posix_time::microsec_clock::local_time();
        previousSampleTime = current_date_microseconds.time_of_day().total_milliseconds();

        bool processSound = true;
        while (processSound) {


            try {
                boost::this_thread::interruption_point();
            }
            catch (const boost::thread_interrupted &) {
                LOG("STOPPING-STORING-RECORDING");
                processSound = false;
                break;
            }

            boost::unique_lock <boost::mutex> lock(m_mutex);
            while (filledIndices->empty() && (processSoundMessage)) {
                LOG("AUDIO_LOCK_WAITING_FOR_NOTIFY");
                m_cond.wait(lock);
                LOG("AUDIO_LOCK_WAITING_NOTIFIED");
            }

            if (processSoundMessage) {

                front = filledIndices->front();
                filledIndices->pop();

                //LOG("AUDIO_RECORDING_NUM_RECORDS %d", numOfRecords);
               // LOG("AUDIO_LOCK_PROCESSING_BEFORE[%d]", front.size());

                std::string sample;
                for (std::vector<short>::size_type i = 0; i != front.size(); i++) {
                    std::stringstream stream;
                    stream << front[i];
                    std::string s = stream.str();
                    if (i == 0) {
                        sample = s;
                    } else {
                        sample = sample + "," + s;
                    }
                }
                //LOG("AUDIO_LOCK_PROCESSING_AFTER[%d]", sample.length());


                /*
                 * Transmit the samples
                 */
                std::vector <std::string> messages;
                messages.push_back(BROADCAST_MESSAGE);
                messages.push_back(streamId);
                boost::posix_time::ptime current_date_microseconds = boost::posix_time::microsec_clock::local_time();
                long currentSampleTime = current_date_microseconds.time_of_day().total_milliseconds();
                long diffbetweenSample = currentSampleTime - previousSampleTime;
                previousSampleTime = currentSampleTime;

                std::string sampleTime;
                std::stringstream strstream;
                strstream << diffbetweenSample;
                strstream >> sampleTime;

                messages.push_back(sampleTime);
                messages.push_back(sample);
                messages.push_back(TYPE_MESSAGE);

                try {
                    LOG("AUDIO_SENT_BEFORE");
                    zeroMq->s_sendmultiple(messages);
                    LOG("AUDIO_SENT_AFTER");
                } catch (std::exception &e) {
                    processSound = false;
                }
                LOG("AUDIO_SENT_AFTER_AFTER");

                //fflush(recordF);

            }

        }
        hasRecordingStop = true;
        LOG("AUDIO_THREAD_ENDED");
        /*
        int len = sizeof(recorderBuffer)/sizeof(recorderBuffer[0]);

        LOG("SOUNDWRITE1[%d]",len);
        int numOfRecords = fwrite(recorderBuffer, sizeof(short), RECORDER_FRAMES, recordF);
        LOG("SOUNDWRITE2");
        LOG("write %d", numOfRecords);
        fflush(recordF);
        LOG("SOUNDWRITE3");
        */
    }

    void SoundCapture::processRecording() {

        boost::shared_ptr <boost::thread> pt(
                new boost::thread(&SoundCapture::storeRecording, this));
        recordingThread.swap(pt);

    }

// set the recording state for the audio recorder
    void SoundCapture::startRecording() {
        SLresult result;
        //open the file to store data

        // in case already recording, stop recording and clear buffer queue
        LOG("SOUND1");
        result = (*recorderRecord)->SetRecordState(recorderRecord, SL_RECORDSTATE_STOPPED);
        LOG("SOUND2");
        assert(SL_RESULT_SUCCESS == result);
        result = (*recorderBufferQueue)->Clear(recorderBufferQueue);
        LOG("SOUND3");
        assert(SL_RESULT_SUCCESS == result);
        // the buffer is not valid for playback yet
        recordCnt = 0;
        // enqueue an empty buffer to be filled by the recorder
        result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, recorderBuffer,
                                                 RECORDER_FRAMES * sizeof(short));
        // the most likely other result is SL_RESULT_BUFFER_INSUFFICIENT,
        // which for this code example would indicate a programming error
        LOG("SOUND4");

        assert(SL_RESULT_SUCCESS == result);
        // start recording
        result = (*recorderRecord)->SetRecordState(recorderRecord, SL_RECORDSTATE_RECORDING);
        LOG("SOUND5");

        assert(SL_RESULT_SUCCESS == result);
        LOG("recorder started recording");
    }

    void SoundCapture::stopRecording() {
        SLresult result;
        LOG("AUDIO_STOP_1");
        result = (*recorderRecord)->SetRecordState(recorderRecord, SL_RECORDSTATE_STOPPED);
        LOG("AUDIO_STOP_2");
        assert(SL_RESULT_SUCCESS == result);
        result = (*recorderBufferQueue)->RegisterCallback(recorderBufferQueue,NULL, NULL);
        LOG("AUDIO_STOP_3");
        assert(SL_RESULT_SUCCESS == result);
        result = (*recorderBufferQueue)->Clear(recorderBufferQueue);
        LOG("AUDIO_STOP_3-1");
        assert(SL_RESULT_SUCCESS == result);

        SLAndroidSimpleBufferQueueState state;
        result = (*recorderBufferQueue)->GetState(recorderBufferQueue,&state);

        /*
         * NOTE: Sometime this is one but some time it is zero:
         * https://www.khronos.org/registry/sles/api/1.1/OpenSLES.h -- but it is always meant
         * to be 1 when stopped.
         */
        //LOG("AUDIO_STOP_RECORDING_STATE[%d]",state.count);
        assert(SL_RESULT_SUCCESS == result);
        /*
        while(state.count != 0) {
            (*recorderBufferQueue)->GetState(recorderBufferQueue,&state);
            LOG("AUDIO_STOP_RECORDING_STATE[%d]",state.count);
            result = (*recorderRecord)->SetRecordState(recorderRecord, SL_RECORDSTATE_STOPPED);
        }
         */

        recordingThread->interrupt();
        LOG("AUDIO_STOP_4");
        //fclose(recordF);
        processSoundMessage = false;
        m_cond.notify_one();
        LOG("AUDIO_STOP_5");
        while (!hasRecordingStop) {
            LOG("AUDIO_WAITING_FOR_RECORDING_TO_STOP");
            m_cond.notify_one();
        }

        LOG("AUDIO_STOP_RECORDING_STOPED");

    }

    SoundCapture::~SoundCapture() {

        LOG("AUDIO_CREATE_DESTROY");
        // destroy audio recorder object, and invalidate all associated interfaces

        if (recorderObject != NULL) {
            LOG("AUDIO_CREATE_1");
          /*
           * Some times this call blocks for ever
           */
          //  (*recorderObject)->Destroy(recorderObject);
            LOG("AUDIO_CREATE_2");
            recorderObject = NULL;
            recorderRecord = NULL;
            recorderBufferQueue = NULL;
        }

        // destroy engine object, and invalidate all associated interfaces
        if (engineObject != NULL)
            LOG("AUDIO_CREATE_3");{
            (*engineObject)->Destroy(engineObject);
            LOG("AUDIO_CREATE_4");
            engineObject = NULL;
            engineEngine = NULL;
        }
        LOG("AUDIO_CREATE_5");
        fclose(recordF);
        LOG("AUDIO_CREATE_DESTROY_END");

    }

    void SoundCapture::setZeroMq(waid::ZeroMq * zmw) {
        zeroMq = zmw;
    }
}