//
//  RecordingBuffer.h
//  AudioRecorder Plugin
//
//  Created by acr on 24/12/2012.
//  Copyright (c) 2012 University of Southampton. All rights reserved.
//

#define RECORDING_BUFFER_LENGTH 1323008                 // 30 seconds of recording rounded up to nearest multiple of 1024

#import <AudioToolbox/AudioToolbox.h>

typedef struct {
	int index;
	AudioSampleType mainBuffer[RECORDING_BUFFER_LENGTH];
	AudioSampleType copyBuffer[RECORDING_BUFFER_LENGTH];
} RecordingBuffer;

void RecordingBuffer_initialise(RecordingBuffer* recordingBuffer);
void RecordingBuffer_update(AudioSampleType sample, RecordingBuffer* recordingBuffer);
void RecordingBuffer_copyMainBuffer(RecordingBuffer* recordingBuffer);
bool RecordingBuffer_getSample(AudioSampleType* sample, RecordingBuffer* recordingBuffer, int index, int duration);
OSStatus RecordingBuffer_writeRecording(AudioFileID* audioFile, RecordingBuffer* recordingBuffer, int duration);

