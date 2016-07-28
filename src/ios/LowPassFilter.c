//
//  LowPassFilter.c
//  AudioRecorder Plugin
//
//  Created by acr on 23/12/2012.
//  Copyright (c) 2012 University of Southampton. All rights reserved.
//

#include "LowPassFilter.h"

LowPassFilter LowPassFilter_initialise(double GAIN, double RATIO) {

    LowPassFilter temp;

    temp.xv0 = 0;
    temp.xv1 = 0;
    temp.yv0 = 0;
    temp.yv1 = 0;
    temp.GAIN = GAIN;
    temp.RATIO = RATIO;

    return temp;

}

void LowPassFilter_update(SInt16 sample, LowPassFilter *lowPassFilter) {

    lowPassFilter->xv0 = lowPassFilter->xv1;
    lowPassFilter->xv1 = (double) sample / (double) INT16_MAX / lowPassFilter->GAIN;
    lowPassFilter->yv0 = lowPassFilter->yv1;
    lowPassFilter->yv1 = (lowPassFilter->xv0 + lowPassFilter->xv1) + (lowPassFilter->RATIO * lowPassFilter->yv0);

}

double LowPassFilter_output(LowPassFilter *lowPassFilter) {

    return lowPassFilter->yv1;

}