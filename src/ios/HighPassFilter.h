//
//  HighPassFilter.h
//  AudioRecorder Plugin
//
//  Created by acr on 01/03/2014.
//  Copyright (c) 2014 New Forest Cicada Project. All rights reserved.
//

#import <AudioToolbox/AudioToolbox.h>

typedef struct {
    double xv0;
    double xv1;
    double yv0;
    double yv1;
    double GAIN;
    double RATIO;
} HighPassFilter;

HighPassFilter HighPassFilter_initialise(double GAIN, double RATIO);

void HighPassFilter_update(SInt16 sample, HighPassFilter *highPassFilter);

double HighPassFilter_output(HighPassFilter *highPassFilter);