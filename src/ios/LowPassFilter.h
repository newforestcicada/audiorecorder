//
//  LowPassFilter.h
//  AudioRecorder Plugin
//
//  Created by acr on 23/12/2012.
//  Copyright (c) 2012 University of Southampton. All rights reserved.
//

#import <AudioToolbox/AudioToolbox.h>

typedef struct {
    double xv0;
    double xv1;
    double yv0;
    double yv1;
    double GAIN;
    double RATIO;
} LowPassFilter;

LowPassFilter LowPassFilter_initialise(double GAIN, double RATIO);

void LowPassFilter_update(SInt16 sample, LowPassFilter *lowPassFilter);

double LowPassFilter_output(LowPassFilter *lowPassFilter);