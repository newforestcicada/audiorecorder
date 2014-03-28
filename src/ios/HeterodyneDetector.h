//
//  HeterodyneDetector.h
//  AudioRecorder Plugin
//
//  Created by acr on 01/03/2014.
//  Copyright (c) 2014 New Forest Cicada Project. All rights reserved.
//

#import <AudioToolbox/AudioToolbox.h>

#import "LowPassFilter.h"
#import "HighPassFilter.h"

typedef struct {
    int frequency;
    float cosTheta;
    float sinTheta;
    float realPart;
    float imaginaryPart;
    float samplingRate;
    HighPassFilter preMixingHighPassFilter;
    LowPassFilter postMixingLowPassFilter;
    HighPassFilter dcRemovingHighPassFilter;
} HeterodyneDetector;

HeterodyneDetector HeterodyneDetector_initialise(float frequency, float samplingRate);

void HeterodyneDetector_setFrequency(float frequency, HeterodyneDetector *heterodyneDetector);

AudioSampleType HeterodyneDetector_update(AudioSampleType sample, HeterodyneDetector *heterodyneDetector);