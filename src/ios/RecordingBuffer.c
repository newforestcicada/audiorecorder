//
//  RecordingBuffer.c
//  AudioRecorder Plugin
//
//  Created by acr on 24/12/2012.
//  Copyright (c) 2012 University of Southampton. All rights reserved.
//

#include <stdio.h>
#include <string.h>
#include <math.h>

#include "RecordingBuffer.h"

void RecordingBuffer_initialise(RecordingBuffer* recordingBuffer) {
	
	memset(recordingBuffer,0,sizeof(&recordingBuffer));
	
}

void RecordingBuffer_update(AudioSampleType sample, RecordingBuffer* recordingBuffer) {
	
	recordingBuffer->mainBuffer[recordingBuffer->index++] = sample;
	
	if ( recordingBuffer->index == RECORDING_BUFFER_LENGTH ) {
		
		recordingBuffer->index = 0;
		
	}
	
}

void RecordingBuffer_copyMainBuffer(RecordingBuffer* recordingBuffer) {
	
	recordingBuffer->copyIndex = recordingBuffer->index;

	memcpy(recordingBuffer->copyBuffer, recordingBuffer->mainBuffer, RECORDING_BUFFER_LENGTH*sizeof(AudioSampleType));
	
}

OSStatus RecordingBuffer_writeRecording(AudioFileID* audioFile, RecordingBuffer* recordingBuffer,int duration) {
	
	UInt32 samplesToWrite = RECORDING_BUFFER_SAMPLES_PER_SECOND*duration;
	
	if ( samplesToWrite > RECORDING_BUFFER_LENGTH ) {
	
		samplesToWrite = RECORDING_BUFFER_LENGTH;
		
	}
	
	if ( recordingBuffer->copyIndex >= samplesToWrite ) {
		
		UInt32 bytesToWrite = samplesToWrite*sizeof(AudioSampleType);
		
		return AudioFileWriteBytes(*audioFile, false, 0, &bytesToWrite, &recordingBuffer->copyBuffer[recordingBuffer->copyIndex-samplesToWrite]);
		
	} else {
		
		samplesToWrite -= recordingBuffer->copyIndex;
		
		UInt32 bytesToWrite = samplesToWrite*sizeof(AudioSampleType);

		OSStatus status = AudioFileWriteBytes(*audioFile, false, 0, &bytesToWrite, &recordingBuffer->copyBuffer[RECORDING_BUFFER_LENGTH-samplesToWrite]);
		
		if ( status == noErr ) {
			
			UInt32 bytesSoFar = bytesToWrite;
			
			bytesToWrite = recordingBuffer->copyIndex*sizeof(AudioSampleType);
			
			return AudioFileWriteBytes(*audioFile, false, bytesSoFar, &bytesToWrite, &recordingBuffer->copyBuffer);
			
		} else {
				
			return status;
				
		}
		
	}
	
}
