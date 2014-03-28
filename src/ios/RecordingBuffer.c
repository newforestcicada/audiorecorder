//
//  RecordingBuffer.c
//  AudioRecorder Plugin
//
//  Created by acr on 24/12/2012.
//  Copyright (c) 2012 University of Southampton. All rights reserved.
//

#include "Settings.h"
#include "RecordingBuffer.h"

void RecordingBuffer_initialise(RecordingBuffer *recordingBuffer) {

    memset(recordingBuffer, 0, sizeof(&recordingBuffer));

}

void RecordingBuffer_update(AudioSampleType sample, RecordingBuffer *recordingBuffer) {

    recordingBuffer->mainBuffer[recordingBuffer->index++] = sample;

    if (recordingBuffer->index == RECORDING_BUFFER_LENGTH ) {

        recordingBuffer->index = 0;

    }

}

void RecordingBuffer_copyMainBuffer(RecordingBuffer *recordingBuffer) {

    int copyIndex = recordingBuffer->index;

    if (copyIndex >= RECORDING_BUFFER_LENGTH ) {

        copyIndex = 0;

    }

    int firstSectionLength = copyIndex;

    int secondSectionLength = RECORDING_BUFFER_LENGTH - firstSectionLength;

    memcpy(recordingBuffer->copyBuffer, &recordingBuffer->mainBuffer[firstSectionLength], secondSectionLength * sizeof(AudioSampleType));

    memcpy(&recordingBuffer->copyBuffer[secondSectionLength], recordingBuffer->mainBuffer, firstSectionLength * sizeof(AudioSampleType));

}

bool RecordingBuffer_getSample(AudioSampleType *sample, RecordingBuffer *recordingBuffer, int index, int duration) {

    UInt32 numberOfSamples = SAMPLES_PER_SECOND * duration;

    if (numberOfSamples > RECORDING_BUFFER_LENGTH ) {

        numberOfSamples = RECORDING_BUFFER_LENGTH;

    }

    int indexOfBuffer = RECORDING_BUFFER_LENGTH - numberOfSamples + index;

    if (index < numberOfSamples) {

        *sample = recordingBuffer->copyBuffer[indexOfBuffer];

        return true;

    } else {

        *sample = 0;

        return false;

    }

}

OSStatus RecordingBuffer_writeRecording(AudioFileID *audioFile, RecordingBuffer *recordingBuffer, int duration) {

    UInt32 samplesToWrite = SAMPLES_PER_SECOND * duration;

    if (samplesToWrite > RECORDING_BUFFER_LENGTH ) {

        samplesToWrite = RECORDING_BUFFER_LENGTH;

    }

    UInt32 bytesToWrite = samplesToWrite * sizeof(AudioSampleType);

    return AudioFileWriteBytes(*audioFile, false, 0, &bytesToWrite, &recordingBuffer->copyBuffer[RECORDING_BUFFER_LENGTH - samplesToWrite]);

}
