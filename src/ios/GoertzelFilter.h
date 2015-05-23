//
//  GoertzelFilter.h
//  AudioRecorder Plugin
//
//  Created by acr on 23/12/2012.
//  Copyright (c) 2012 University of Southampton. All rights reserved.
//

#define MAX_GOERTZEL_FILTER_LENGTH 1024

#import <AudioToolbox/AudioToolbox.h>

#import "KalmanFilter.h"

typedef struct {
    int N;
    int index;
    double k;
    double realW;
    double y;
    double d1;
    double d2;
    KalmanFilter kalmanFilter;
    double hammingFactor[MAX_GOERTZEL_FILTER_LENGTH];
} GoertzelFilter;

GoertzelFilter GoertzelFilter_initialise(int N, double centralFrequency, double samplingFreqency);

void GoertzelFilter_update(SInt16 sample, GoertzelFilter *goertzelFilter);

float GoertzelFilter_estimate(GoertzelFilter *goertzelFilter);

