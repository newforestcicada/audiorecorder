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
    double cosTheta;
    double sinTheta;
    double realPart;
    double imaginaryPart;
    double samplingRate;
    HighPassFilter preMixingHighPassFilter;
    LowPassFilter postMixingLowPassFilter;
    HighPassFilter dcRemovingHighPassFilter;
} HeterodyneDetector;

HeterodyneDetector HeterodyneDetector_initialise(double frequency, double samplingRate);

void HeterodyneDetector_setFrequency(double frequency, HeterodyneDetector *heterodyneDetector);

SInt16 HeterodyneDetector_update(SInt16 sample, HeterodyneDetector *heterodyneDetector);