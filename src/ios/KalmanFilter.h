//
//  Kalman.h
//  AudioRecorder Plugin
//
//  Created by acr on 23/12/2012.
//  Copyright (c) 2012 University of Southampton. All rights reserved.
//

typedef struct {
	float q;
	float r;
	float p;
	float x;
	float k;
} KalmanFilter;

KalmanFilter KalmanFilter_initialise( float processNoise, float  measurementNoise,  float estimateValue, float estimateVariance);
void KalmanFilter_update(float sample, KalmanFilter *kalmanFilter);
float KalmanFilter_estimate(KalmanFilter *kalmanFilter);
