//
//  GoertzelFilter.h
//  AudioRecorder Plugin
//
//  Created by acr on 23/12/2012.
//  Copyright (c) 2012 University of Southampton. All rights reserved.
//

#define MAX_GOERTZEL_FILTER_LENGTH 1000

#import <AudioToolbox/AudioToolbox.h>

#import "KalmanFilter.h"

typedef struct {
	int N;
	int index;
	float k;
	float realW;
	float y;
	float d1;
	float d2;
	KalmanFilter kalmanFilter;
	float hammingFactor[MAX_GOERTZEL_FILTER_LENGTH];
} GoertzelFilter;

GoertzelFilter GoertzelFilter_initialise(int N, float centralFrequency, float samplingFreqency);
void GoertzelFilter_update(AudioSampleType sample, GoertzelFilter* goertzelFilter);
float GoertzelFilter_estimate(GoertzelFilter *goertzelFilter);

