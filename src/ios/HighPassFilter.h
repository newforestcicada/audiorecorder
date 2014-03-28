//
//  HighPassFilter.h
//  AudioRecorder Plugin
//
//  Created by acr on 01/03/2014.
//  Copyright (c) 2014 New Forest Cicada Project. All rights reserved.
//

#import <AudioToolbox/AudioToolbox.h>

typedef struct {
    float xv0;
    float xv1;
    float yv0;
    float yv1;
    float GAIN;
    float RATIO;
} HighPassFilter;

HighPassFilter HighPassFilter_initialise(float GAIN, float RATIO);

void HighPassFilter_update(AudioSampleType sample, HighPassFilter *highPassFilter);

float HighPassFilter_output(HighPassFilter *highPassFilter);