//
//  Sonogram.h
//  AudioRecorder Plugin
//
//  Created by acr on 01/03/2014.
//  Copyright (c) 2014 New Forest Cicada Project. All rights reserved.
//

#import <AudioToolbox/AudioToolbox.h>

#import "Settings.h"
#import "GoertzelFilter.h"

#define SONOGRAM_LENGTH     BUFFER_SECONDS * SAMPLES_PER_SECOND / GOERTZEL_WINDOW_SIZE

typedef struct {
    int index;
    int count;
    bool wrapped;
    int frequencyStep;
    int startFrequency;
    int numberOfFilters;
    GoertzelFilter goerztelFilters[MAX_NUMBER_OF_GOERTZEL_FILTERS];
    double mainSonogram[MAX_NUMBER_OF_GOERTZEL_FILTERS][SONOGRAM_LENGTH];
    double copySonogram[MAX_NUMBER_OF_GOERTZEL_FILTERS][SONOGRAM_LENGTH];
} Sonogram;

void Sonogram_initialise(Sonogram *sonogram, int numberOfFilters, int startFrequency, int frequencyStep);

void Sonogram_update(SInt16 sample, Sonogram *sonogram);

void Sonogram_copySonogram(Sonogram *sonogram);

void Sonogram_clearSonogram(Sonogram *sonogram);

double Sonogram_getCurrentValue(Sonogram *sonogram, int filter);

double Sonogram_getValue(Sonogram *sonogram, int i, int j, int width, int height, int duration);

void Sonogram_colour(double value,  double min, double max, UInt8 *red, UInt8 *green, UInt8 *blue);
