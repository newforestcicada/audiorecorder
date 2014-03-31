//
//  Sonogram.c
//  AudioRecorder Plugin
//
//  Created by acr on 01/03/2014.
//  Copyright (c) 2014 New Forest Cicada Project. All rights reserved.
//

#include "Settings.h"
#include "Sonogram.h"

#define MAX(a,b) ((a) > (b) ? a : b)
#define MIN(a,b) ((a) < (b) ? a : b)

void Sonogram_initialise(Sonogram *sonogram, int numberOfFilters, int startFrequency, int frequencyStep) {
    
    memset(sonogram, 0, sizeof(&sonogram));
    
    sonogram->numberOfFilters = numberOfFilters;
    sonogram->startFrequency = startFrequency;
    sonogram->frequencyStep = frequencyStep;
    
    for (int i = 0; i < numberOfFilters; i += 1) {
        
        sonogram->goerztelFilters[i] = GoertzelFilter_initialise(GOERTZEL_WINDOW_SIZE, (float)startFrequency + (float) i * (float)frequencyStep, SAMPLES_PER_SECOND);
    
    }
    
}

void Sonogram_clearSonogram(Sonogram *sonogram) {
    
    memset(sonogram->mainSonogram, 0, sizeof(sonogram->mainSonogram));
    
}

void Sonogram_update(AudioSampleType sample, Sonogram *sonogram) {
    
    for (int i = 0; i < sonogram->numberOfFilters; i += 1) {
        
        GoertzelFilter_update(sample, &sonogram->goerztelFilters[i]);
        
    }
    
    sonogram->count += 1;
    
    if (sonogram->count >= GOERTZEL_WINDOW_SIZE) {
        
        sonogram->count = 0;
        
        for (int i = 0; i < sonogram->numberOfFilters; i += 1) {
            
            sonogram->mainSonogram[i][sonogram->index] = GoertzelFilter_estimate(&sonogram->goerztelFilters[i]);
            
        }
    
        sonogram->index += 1;
        
        if (sonogram->index >= SONOGRAM_LENGTH ) {
            
            sonogram->index = 0;
            
        }
        
    }

}

float Sonogram_getCurrentValue(Sonogram *sonogram, int filter) {
    
    return GoertzelFilter_estimate(&sonogram->goerztelFilters[filter]);
    
}

void Sonogram_copySonogram(Sonogram *sonogram) {
    
    int copyIndex = sonogram->index;
    
    if (copyIndex >= SONOGRAM_LENGTH ) {
        
        copyIndex = 0;
        
    }
    
    int firstSectionLength = copyIndex;
    
    int secondSectionLength = SONOGRAM_LENGTH - firstSectionLength;
    
    for (int i = 0; i < sonogram->numberOfFilters; i += 1) {
    
        memcpy(&sonogram->copySonogram[i][0], &sonogram->mainSonogram[i][firstSectionLength], secondSectionLength * sizeof(float));
    
        memcpy(&sonogram->copySonogram[i][secondSectionLength], &sonogram->mainSonogram[i][0], firstSectionLength * sizeof(float));
        
    }
    
}

float Sonogram_getValue(Sonogram *sonogram, int i, int j, int width, int height, int duration) {

    if (i < 0 || i > width-1 || j < 0 || j > height-1) {
        
        return 0;
        
    } else {
        
        int numberOfMeasurementPoints = duration * SAMPLES_PER_SECOND / GOERTZEL_WINDOW_SIZE;
        
        float xRatio = (float)(numberOfMeasurementPoints - 1) / (float)(width - 1);
        
        float yRatio = (float)(sonogram->numberOfFilters - 1) / (float)(height - 1);
        
        int xIndex = SONOGRAM_LENGTH - numberOfMeasurementPoints + (int)(0.5f + (float)i * xRatio);
        
        int yIndex = sonogram->numberOfFilters - 1 - (int)(0.5f + (float)j * yRatio);
        
        return sonogram->copySonogram[yIndex][xIndex];
        
    }
    
}

float interpolate(float value, float x0, float x1, float y0, float y1) {
    
    return (value - x0) * (y1 - y0) / (x1 - x0) + y0;

}

float base(float value) {
    
    if (value <= -0.75) {
        return 0;
    } else if (value <= -0.25) {
        return interpolate(value, -0.75f, -0.25f, 0.0f, 1.0f);
    } else if (value <= 0.25) {
        return 1.0;
    } else if (value <= 0.75) {
        return interpolate(value, 0.25f, 0.75f, 1.0f, 0.0f);
    } else {
        return 0.0;
    }
    
}

void Sonogram_colour(float value, float min, float max, UInt8 *red, UInt8 *green, UInt8 *blue) {
    
    float scaledValue = MIN(1.0f, MAX(0.0f, logf( (value + 1.0f) / min) / logf(max / min)));
    
    *red = (UInt8) (0.5f + 255.0f * base(scaledValue - 0.5f));

    *green = (UInt8) (0.5f + 255.0f * base(scaledValue));

    *blue = (UInt8) (0.5f + 255.0f * base(scaledValue + 0.5f));
    
}


