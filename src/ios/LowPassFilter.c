//
//  LowPassFilter.c
//  AudioRecorder Plugin
//
//  Created by acr on 23/12/2012.
//  Copyright (c) 2012 University of Southampton. All rights reserved.
//

#include "LowPassFilter.h"

LowPassFilter LowPassFilter_initialise(float GAIN, float RATIO) {
	
	LowPassFilter temp;
	
	temp.xv0 = 0;
	temp.xv1 = 0;
	temp.yv0 = 0;
	temp.yv1 = 0;
	temp.GAIN = GAIN;
	temp.RATIO = RATIO;
	
	return temp;
	
}

void LowPassFilter_update(AudioSampleType sample, LowPassFilter* lowPassFilter) {
	
	lowPassFilter->xv0 = lowPassFilter->xv1;
	lowPassFilter->xv1 = (float)abs(sample) / (float)INT16_MAX / lowPassFilter->GAIN;
	lowPassFilter->yv0 = lowPassFilter->yv1;
	lowPassFilter->yv1 = ( lowPassFilter->xv0 + lowPassFilter->xv1 ) + ( lowPassFilter->RATIO  * lowPassFilter->yv0 );
	
}

float LowPassFilter_output(LowPassFilter* lowPassFilter) {
    
    return lowPassFilter->yv1;
    
}