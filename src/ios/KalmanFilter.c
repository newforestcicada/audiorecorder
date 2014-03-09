//
//  KalmanFilter.c
//  AudioRecorder Plugin
//
//  Created by acr on 23/12/2012.
//  Copyright (c) 2012 University of Southampton. All rights reserved.
//

#include <stdio.h>

#include "KalmanFilter.h"

KalmanFilter KalmanFilter_initialise( float processNoise, float  measurementNoise, float estimateValue, float estimateVariance) {
	
	KalmanFilter temp;
	
	temp.q = processNoise;
	temp.r = measurementNoise;
	temp.p = estimateVariance;
	temp.x = estimateValue;
	temp.k = 0;
	
	return temp;
	
}

void KalmanFilter_update(float sample, KalmanFilter *kalmanFilter) {
	
	kalmanFilter->p = kalmanFilter->p + kalmanFilter->q;
	kalmanFilter->k = kalmanFilter->p/(kalmanFilter->p+kalmanFilter->r);
	kalmanFilter->x = kalmanFilter->x + kalmanFilter->k*(sample-kalmanFilter->x);
	kalmanFilter->p = (1-kalmanFilter->k)*kalmanFilter->p;
	
}

float KalmanFilter_estimate(KalmanFilter *kalmanFilter) {
	
	return kalmanFilter->x;
	
}