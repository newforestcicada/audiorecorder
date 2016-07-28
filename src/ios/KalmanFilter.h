//
//  Kalman.h
//  AudioRecorder Plugin
//
//  Created by acr on 23/12/2012.
//  Copyright (c) 2012 University of Southampton. All rights reserved.
//

typedef struct {
    double q;
    double r;
    double p;
    double x;
    double k;
} KalmanFilter;

KalmanFilter KalmanFilter_initialise(double processNoise, double measurementNoise, double estimateValue, double estimateVariance);

void KalmanFilter_update(double sample, KalmanFilter *kalmanFilter);

double KalmanFilter_estimate(KalmanFilter *kalmanFilter);
