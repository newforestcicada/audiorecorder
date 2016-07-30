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

    memset(recordingBuffer, 0, sizeof(RecordingBuffer));

}

void RecordingBuffer_update(SInt16 sample, RecordingBuffer *recordingBuffer) {

    recordingBuffer->mainBuffer[recordingBuffer->index++] = sample;

    if (recordingBuffer->index == RECORDING_BUFFER_LENGTH ) {

        recordingBuffer->index = 0;
        recordingBuffer->wrapped = true;

    }

}

void RecordingBuffer_clearBuffer(RecordingBuffer *recordingBuffer) {

    memset(recordingBuffer, 0, sizeof(RecordingBuffer));

}

void RecordingBuffer_copyBuffer(RecordingBuffer *recordingBuffer) {

    int copyIndex = recordingBuffer->index;

    if (copyIndex >= RECORDING_BUFFER_LENGTH ) {

        copyIndex = 0;

    }

    if (recordingBuffer->wrapped) {

        recordingBuffer->copyIsWrapped = true;

        int firstSectionLength = copyIndex;

        int secondSectionLength = RECORDING_BUFFER_LENGTH - firstSectionLength;

        memcpy(recordingBuffer->copyBuffer, &recordingBuffer->mainBuffer[firstSectionLength], secondSectionLength * sizeof(SInt16));

        memcpy(&recordingBuffer->copyBuffer[secondSectionLength], recordingBuffer->mainBuffer, firstSectionLength * sizeof(SInt16));

    } else {

        memcpy(recordingBuffer->copyBuffer, recordingBuffer->mainBuffer, RECORDING_BUFFER_LENGTH * sizeof(SInt16));

    }

}

bool RecordingBuffer_getSample(SInt16 *sample, RecordingBuffer *recordingBuffer, int index, int duration) {

    UInt32 numberOfSamples = SAMPLES_PER_SECOND * duration;

    if (numberOfSamples > RECORDING_BUFFER_LENGTH ) {

        numberOfSamples = RECORDING_BUFFER_LENGTH;

    }

    if (recordingBuffer->copyIsWrapped) {

        index += RECORDING_BUFFER_LENGTH - numberOfSamples;

    }

    if (index < numberOfSamples) {

        *sample = recordingBuffer->copyBuffer[index];

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

    UInt32 bytesToWrite = samplesToWrite * sizeof(SInt16);

    if (recordingBuffer->copyIsWrapped) {

        return AudioFileWriteBytes(*audioFile, false, 0, &bytesToWrite, &recordingBuffer->copyBuffer[RECORDING_BUFFER_LENGTH - samplesToWrite]);

    } else {

        return AudioFileWriteBytes(*audioFile, false, 0, &bytesToWrite, recordingBuffer->copyBuffer);

    }

}
