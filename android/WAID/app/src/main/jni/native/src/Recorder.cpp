/*
 * Recorder.cpp
 *
 *  Created on: 2011/12/31
 *      Author: nobnak
 */
#include <linux/stddef.h>
#include <assert.h>
#include <string.h>

#include "Recorder.h"
#include "androidlog.h"

Recorder::Recorder(SLEngineItf engineEngine, int nBufferSamples, int nBuffers) :
        currentFrame(0) {
    SLresult result;

    // configure audio source
    SLDataLocator_IODevice loc_dev = { SL_DATALOCATOR_IODEVICE,
                                       SL_IODEVICE_AUDIOINPUT, SL_DEFAULTDEVICEID_AUDIOINPUT, NULL };
    SLDataSource audioSrc = { &loc_dev, NULL };

    // configure audio sink
    SLDataLocator_AndroidSimpleBufferQueue loc_bq = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2 };
    SLDataFormat_PCM format_pcm = { SL_DATAFORMAT_PCM, 1, SL_SAMPLINGRATE_16,
                                    SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
                                    SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN };
    SLDataSink audioSnk = { &loc_bq, &format_pcm };

    try {
        // create audio recorder
        // (requires the RECORD_AUDIO permission)
        const SLInterfaceID id[1] = { SL_IID_ANDROIDSIMPLEBUFFERQUEUE };
        const SLboolean req[1] = { SL_BOOLEAN_TRUE };
        result = (*engineEngine)->CreateAudioRecorder(engineEngine,
                                                      &recorderObject, &audioSrc, &audioSnk, 1, id, req);
        if (SL_RESULT_SUCCESS != result) {
            throw "Failed to make recorder";
        }

        // realize the audio recorder
        result = (*recorderObject)->Realize(recorderObject, SL_BOOLEAN_FALSE);
        if (SL_RESULT_SUCCESS != result) {
            throw "Failed to realize recorder";
        }

        // get the record interface
        result = (*recorderObject)->GetInterface(recorderObject, SL_IID_RECORD,
                                                 &recorderRecord);
        assert(SL_RESULT_SUCCESS == result);
        if (SL_RESULT_SUCCESS != result) {
            throw "Failed to get recorder interface";
        }

        // get the buffer queue interface
        result = (*recorderObject)->GetInterface(recorderObject,
                                                 SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &recorderBufferQueue);
        assert(SL_RESULT_SUCCESS == result);
        if (SL_RESULT_SUCCESS != result) {
            throw "Failed to get buffer queue";
        }
        recordingState = SL_RECORDSTATE_STOPPED;

        // register callback on the buffer queue
        result = (*recorderBufferQueue)->RegisterCallback(recorderBufferQueue,
                                                          Recorder::bqStaticRecorderCallback, this);
        assert(SL_RESULT_SUCCESS == result);
        if (SL_RESULT_SUCCESS != result) {
            throw "Failed to register callback of the buffer queue";
        }

        filledIndices = new std::queue<int>();
        this->nBuffers = nBuffers;
        this->nBufferSamples = nBufferSamples;
        recorderBuffer = new short[nBufferSamples * nBuffers];
        pthread_mutex_init(&mutex_filledIndices, NULL);
    } catch (const char *e) {
        if (recorderObject != NULL)
            (*recorderObject)->Destroy(recorderObject);
        throw e;
    }
}

Recorder::~Recorder() {
    // destroy audio recorder object, and invalidate all associated interfaces
    if (recorderObject != NULL) {
        (*recorderObject)->Destroy(recorderObject);
        recorderObject = NULL;
    }
    if (filledIndices != NULL) {
        delete filledIndices;
        filledIndices = NULL;
    }

}

void Recorder::stopRecording() {
    SLresult result;

    // in case already recording, stop recording and clear buffer queue
    SLAndroidSimpleBufferQueueState state;
    (*recorderBufferQueue)->GetState(recorderBufferQueue, &state);
    if (state.count == 0) {
        recordingState = RECORDING_STATE_STOPPED;
    } else {
        recordingState = RECORDING_STATE_STOPPING;
    }
//	result = (*recorderRecord)->SetRecordState(recorderRecord, recordingState);
//	assert(SL_RESULT_SUCCESS == result);
//	if (SL_RESULT_SUCCESS != result) {
//		androidlog("Failed to stop recording");
//		return JNI_FALSE;
//	}
    result = (*recorderBufferQueue)->Clear(recorderBufferQueue);
    assert(SL_RESULT_SUCCESS == result);
    if (SL_RESULT_SUCCESS != result) {
        throw "Failed to clear the buffer queue";
    }
}
void Recorder::startRecording() {
    SLresult result;

    if (recordingState != RECORDING_STATE_STOPPED) {
        throw "Failed to start recording: Recording state is not Stopped";
    }

    // enqueue an empty buffer to be filled by the recorder
    // (for streaming recording, we would enqueue at least 2 empty buffers to start things off)
    int i;
    for (i = 0; i < nBuffers; i++) {
        result = enqueueBuffer(i);
        // the most likely other result is SL_RESULT_BUFFER_INSUFFICIENT,
        // which for this code example would indicate a programming error
        assert(SL_RESULT_SUCCESS == result);
        if (SL_RESULT_SUCCESS != result) {
            if (result == SL_RESULT_BUFFER_INSUFFICIENT) {
                throw "Failed to enqueue buffer: buffer insufficient";
            } else {
                throw "Faild to enqueue buffer...";
            }
        }
    }
    currentFrame = 0;

    // start recording
    recordingState = RECORDING_STATE_RECORDING;
    result = (*recorderRecord)->SetRecordState(recorderRecord,
                                               SL_RECORDSTATE_RECORDING);
    assert(SL_RESULT_SUCCESS == result);
    if (SL_RESULT_SUCCESS != result) {
        throw "Failed to start recording";
    }
}
void Recorder::march() {
    int front;

    while (true) {
        pthread_mutex_lock(&mutex_filledIndices);
        if (filledIndices->empty()) {
            pthread_mutex_unlock(&mutex_filledIndices);
            break;
        }
        front = filledIndices->front();
        filledIndices->pop();
        pthread_mutex_unlock(&mutex_filledIndices);

        //callback(front);
        dst->write(recorderBuffer + (front % nBuffers) * nBufferSamples,
                   nBufferSamples);
    }

    if (recordingState == RECORDING_STATE_STOPPING) {
        SLAndroidSimpleBufferQueueState state;
        (*recorderBufferQueue)->GetState(recorderBufferQueue, &state);
        if (state.count == 0) {
            dst->end();
            recordingState = RECORDING_STATE_STOPPED;
        }
    }
}

void Recorder::bqStaticRecorderCallback(SLAndroidSimpleBufferQueueItf bq,
                                        void *context) {
    ((Recorder*) context)->bqRecorderCallback(bq, NULL);
}
void Recorder::bqRecorderCallback(SLAndroidSimpleBufferQueueItf bq,
                                  void *context) {
    assert(bq == recorderBufferQueue);
    assert(NULL == context);

    // for streaming recording, here we would call Enqueue to give recorder the next buffer to fill
    // but instead, this is a one-time buffer so we stop recording
    SLAndroidSimpleBufferQueueState state;
    SLresult result;
    switch (recordingState) {
        case RECORDING_STATE_STOPPING:
            break;
        case RECORDING_STATE_RECORDING:
            result = enqueueBuffer(currentFrame);
            if (SL_RESULT_SUCCESS != result) {
                Log::d("Failed to enqueue next buffer");
                return;
            }
            break;
        default:
            break;
    }

    //callbackHandler(currentFrame);
    pthread_mutex_lock(&mutex_filledIndices);
    filledIndices->push(currentFrame);
    pthread_mutex_unlock(&mutex_filledIndices);
    currentFrame = (currentFrame + 1) % nBuffers;
}
void Recorder::pipe(IWritable<short> *dst) {
    this->dst = dst;
}

SLresult Recorder::enqueueBuffer(int i) {
    return (*recorderBufferQueue)->Enqueue(recorderBufferQueue,
                                           recorderBuffer + i * nBufferSamples, nBufferSamples * sizeof(short));
}