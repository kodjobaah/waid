/*
 * Recorder.h
 *
 *  Created on: 2011/12/31
 *      Author: nobnak
 */

#ifndef RECORDER_H_
#define RECORDER_H_

#include <queue>
#include <pthread.h>

// OpenSLES
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#include "IReadable.h"

// Recording States
#define RECORDING_STATE_STOPPED (0x00000001)
#define RECORDING_STATE_RECORDING (0x00000002)
#define RECORDING_STATE_STOPPING (0x00000003)


class Recorder : public IReadable<short> {
public:
    Recorder(SLEngineItf engineEngine, int nBufferSamples, int nBuffers);
    virtual ~Recorder();
    void startRecording();
    void stopRecording();
    void march();
    // IReadable
    void pipe(IWritable<short> *dst);
private:
    SLObjectItf recorderObject;
    SLRecordItf recorderRecord;
    SLAndroidSimpleBufferQueueItf recorderBufferQueue;
    int recordingState;
    int nBuffers;
    int nBufferSamples;
    short *recorderBuffer;
    int currentFrame;
    // synchronized filledIndices
    pthread_mutex_t mutex_filledIndices;
    std::queue<int> *filledIndices;
    IWritable<short> *dst;

    static void bqStaticRecorderCallback(SLAndroidSimpleBufferQueueItf bq,
                                         void *context);
    void bqRecorderCallback(SLAndroidSimpleBufferQueueItf bq, void *context);
    SLresult enqueueBuffer(int i);
};

#endif /* RECORDER_H_ */