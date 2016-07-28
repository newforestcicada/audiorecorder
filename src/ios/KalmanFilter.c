//
//  KalmanFilter.c
//  AudioRecorder Plugin
//
//  Created by acr on 23/12/2012.
//  Copyright (c) 2012 University of Southampton. All rights reserved.
//

#include <math.h>

#include "KalmanFilter.h"

KalmanFilter KalmanFilter_initialise(double processNoise, double measurementNoise, double estimateValue, double estimateVariance) {

    KalmanFilter temp;

    temp.q = processNoise;
    temp.r = measurementNoise;
    temp.p = estimateVariance;
    temp.x = estimateValue;
    temp.k = 0.0;

    return temp;

}

void KalmanFilter_update(double sample, KalmanFilter *kalmanFilter) {

    kalmanFilter->p = kalmanFilter->p + kalmanFilter->q;
    kalmanFilter->k = kalmanFilter->p / (kalmanFilter->p + kalmanFilter->r);
    kalmanFilter->x = kalmanFilter->x + kalmanFilter->k * (sample - kalmanFilter->x);
    kalmanFilter->p = (1.0 - kalmanFilter->k) * kalmanFilter->p;

}

double KalmanFilter_estimate(KalmanFilter *kalmanFilter) {

    return kalmanFilter->x;

}
