//
//  GoertzelFilter.c
//  AudioRecorder Plugin
//
//  Created by acr on 23/12/2012.
//  Copyright (c) 2012 University of Southampton. All rights reserved.
//

#include <math.h>

#include "GoertzelFilter.h"

GoertzelFilter GoertzelFilter_initialise(int N, double centralFrequency, double samplingFreqency) {

    GoertzelFilter temp;

    temp.N = N;

    double bandpassWidth = 4.0 * samplingFreqency / (double) N;

    temp.k = 4.0 * centralFrequency / bandpassWidth;

    for (int n = 0; n < N; n++) {
        temp.hammingFactor[n] = 0.54 - 0.46 * cos(2.0 * M_PI * (double)n / (double) N);
    }

    temp.realW = 2.0 * cos(2.0 * M_PI * temp.k / (double) temp.N);

    temp.y = 0.0;
    temp.d1 = 0.0;
    temp.d2 = 0.0;

    temp.index = 0;

    temp.kalmanFilter = KalmanFilter_initialise(100.0f, 100.0f, 100.0f, 100.0f);

    return temp;

}

void GoertzelFilter_update(SInt16 sample, GoertzelFilter *goertzelFilter) {
    
    goertzelFilter->y = goertzelFilter->hammingFactor[goertzelFilter->index] * (double)sample + goertzelFilter->realW * goertzelFilter->d1 - goertzelFilter->d2;
    goertzelFilter->d2 = goertzelFilter->d1;
    goertzelFilter->d1 = goertzelFilter->y;

    goertzelFilter->index += 1;
    
    if (goertzelFilter->index >= goertzelFilter->N) {

        goertzelFilter->index = 0;

        goertzelFilter->amplitude = sqrt(goertzelFilter->d1 * goertzelFilter->d1 + goertzelFilter->d2 * goertzelFilter->d2 - goertzelFilter->d1 * goertzelFilter->d2 * goertzelFilter->realW);

        KalmanFilter_update(goertzelFilter->amplitude, &goertzelFilter->kalmanFilter);
        
        goertzelFilter->y = 0;
        goertzelFilter->d1 = 0;
        goertzelFilter->d2 = 0;

    }

}

double GoertzelFilter_estimate(GoertzelFilter *goertzelFilter) {
    
    double value = KalmanFilter_estimate(&goertzelFilter->kalmanFilter);
    
    // Kalman filter lost track. Using current data instead
    
    if (isnan(value) || isinf(value)) {
        
        value = goertzelFilter->amplitude;
        
    }
    
    return value;

}
