//
//  HighPassFilter.c
//  AudioRecorder Plugin
//
//  Created by acr on 01/03/2014.
//  Copyright (c) 2014 New Forest Cicada Project. All rights reserved.
//

#include "HighPassFilter.h"

HighPassFilter HighPassFilter_initialise(float GAIN, float RATIO) {

    HighPassFilter temp;

    temp.xv0 = 0;
    temp.xv1 = 0;
    temp.yv0 = 0;
    temp.yv1 = 0;
    temp.GAIN = GAIN;
    temp.RATIO = RATIO;

    return temp;

}

void HighPassFilter_update(AudioSampleType sample, HighPassFilter *highPassFilter) {

    highPassFilter->xv0 = highPassFilter->xv1;
    highPassFilter->xv1 = (float) abs(sample) / (float) INT16_MAX / highPassFilter->GAIN;
    highPassFilter->yv0 = highPassFilter->yv1;
    highPassFilter->yv1 = (highPassFilter->xv1 - highPassFilter->xv0) + (highPassFilter->RATIO * highPassFilter->yv0);

}

float HighPassFilter_output(HighPassFilter *highPassFilter) {

    return highPassFilter->yv1;

}