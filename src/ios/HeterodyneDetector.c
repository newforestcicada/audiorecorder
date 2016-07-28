//
//  HeterodyneDetector.c
//  AudioRecorder Plugin
//
//  Created by acr on 01/03/2014.
//  Copyright (c) 2014 New Forest Cicada Project. All rights reserved.
//

#include <math.h>

#include "Settings.h"
#include "HeterodyneDetector.h"

#define MAX(a,b) ((a) > (b) ? a : b)
#define MIN(a,b) ((a) < (b) ? a : b)

HeterodyneDetector HeterodyneDetector_initialise(double frequency, double samplingRate) {

    HeterodyneDetector temp;

    temp.realPart = 0.0;
    temp.imaginaryPart = 1.0;

    temp.samplingRate = samplingRate;

    double theta = 2.0 * M_PI * frequency / samplingRate;

    temp.cosTheta = cos(theta);
    temp.sinTheta = sin(theta);

    /* High pass with 10kHz cut-off frequency */

    temp.preMixingHighPassFilter = HighPassFilter_initialise(1.863674229, 0.0731489272);

    /* Low pass with 1kHz cut-off frequency */

    temp.postMixingLowPassFilter = LowPassFilter_initialise(15.01371197, 0.8667884395);

    /* High pass with 10Hz cut-off frequency */

    temp.dcRemovingHighPassFilter = HighPassFilter_initialise(1.000712379, 0.9985762554);

    return temp;

}

void HeterodyneDetector_setFrequency(double frequency, HeterodyneDetector *heterodyneDetector) {

    double theta = 2.0 * M_PI * frequency / heterodyneDetector->samplingRate;

    heterodyneDetector->cosTheta = cos(theta);
    heterodyneDetector->sinTheta = sin(theta);

}

SInt16 HeterodyneDetector_update(SInt16 sample, HeterodyneDetector *heterodyneDetector) {

    HighPassFilter_update(sample, &heterodyneDetector->preMixingHighPassFilter);

    double real = heterodyneDetector->cosTheta * heterodyneDetector->realPart - heterodyneDetector->sinTheta * heterodyneDetector->imaginaryPart;
    double imaginary = heterodyneDetector->sinTheta * heterodyneDetector->realPart + heterodyneDetector->cosTheta * heterodyneDetector->imaginaryPart;

    heterodyneDetector->realPart = real;
    heterodyneDetector->imaginaryPart = imaginary;

    double output = HighPassFilter_output(&heterodyneDetector->preMixingHighPassFilter) * heterodyneDetector->realPart * 100.0f * (double) INT16_MAX;

    output = MAX(INT16_MIN, MIN(INT16_MAX, output));

    LowPassFilter_update((SInt16) output, &heterodyneDetector->postMixingLowPassFilter);

    output = LowPassFilter_output(&heterodyneDetector->postMixingLowPassFilter) * (double) INT16_MAX;

    output = MAX(INT16_MIN, MIN(INT16_MAX, output));

    HighPassFilter_update((SInt16) output, &heterodyneDetector->dcRemovingHighPassFilter);

    output = HighPassFilter_output(&heterodyneDetector->dcRemovingHighPassFilter) * (double) INT16_MAX;

    output = MAX(INT16_MIN, MIN(INT16_MAX, output));

    return (SInt16) output;

}
