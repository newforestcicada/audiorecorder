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

HeterodyneDetector HeterodyneDetector_initialise(float frequency) {
    
    HeterodyneDetector temp;
	
	temp.realPart = 0.0f;
    temp.imaginaryPart = 1.0f;
    
    float theta = 2.0f * M_PI * frequency / (float)SAMPLES_PER_SECOND;
    
    temp.cosTheta = cos(theta);
    temp.sinTheta = sin(theta);
    
    /* High pass with 10kHz cut-off frequency */
    
    temp.preMixingHighPassFilter = HighPassFilter_initialise(1.863674229, 0.0731489272);
    
    /* Low pass with 1kHz cut-off frequency */
    
    temp.postMixingLowPassFilter  = LowPassFilter_initialise(15.01371197, 0.8667884395);
    
    /* High pass with 10Hz cut-off frequency */
    
    temp.dcRemovingHighPassFilter  = HighPassFilter_initialise(1.000712379, 0.9985762554);
	
	return temp;
    
}

void HeterodyneDetector_setFrequency(float frequency, HeterodyneDetector *heterodyneDetector) {
    
    float theta = 2.0f * M_PI * frequency / (float) SAMPLES_PER_SECOND;
    
    heterodyneDetector->cosTheta = cos(theta);
    heterodyneDetector->sinTheta = sin(theta);
    
}

AudioSampleType HeterodyneDetector_update(AudioSampleType sample, HeterodyneDetector *heterodyneDetector) {
    
    HighPassFilter_update(sample, &heterodyneDetector->preMixingHighPassFilter);
    
    float real = heterodyneDetector->cosTheta * heterodyneDetector->realPart - heterodyneDetector->sinTheta * heterodyneDetector->imaginaryPart;
    float imaginary = heterodyneDetector->sinTheta * heterodyneDetector->realPart + heterodyneDetector->cosTheta * heterodyneDetector->imaginaryPart;
    
    heterodyneDetector->realPart = real;
    heterodyneDetector->imaginaryPart = imaginary;
    
    float output = HighPassFilter_output(&heterodyneDetector->preMixingHighPassFilter) * heterodyneDetector->realPart * 100.0f * (float)INT16_MAX;
    
    //float output = (float)sample * heterodyneDetector->realPart * 100.0f;
    
    LowPassFilter_update((AudioSampleType)output, &heterodyneDetector->postMixingLowPassFilter);
    
    output = LowPassFilter_output(&heterodyneDetector->postMixingLowPassFilter) * (float)INT16_MAX;
    
    HighPassFilter_update((AudioSampleType)output, &heterodyneDetector->dcRemovingHighPassFilter);
    
    output = HighPassFilter_output(&heterodyneDetector->dcRemovingHighPassFilter) * (float)INT16_MAX;
    
    return (AudioSampleType)output;

}


