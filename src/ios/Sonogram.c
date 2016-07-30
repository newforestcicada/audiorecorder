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

    memset(sonogram, 0, sizeof(Sonogram));

    sonogram->frequencyStep = frequencyStep;
    sonogram->startFrequency = startFrequency;
    sonogram->numberOfFilters = numberOfFilters;

    for (int i = 0; i < numberOfFilters; i += 1) {

        sonogram->goerztelFilters[i] = GoertzelFilter_initialise(GOERTZEL_WINDOW_SIZE, (double)startFrequency + (double) i * (double)frequencyStep, SAMPLES_PER_SECOND);

    }

}

void Sonogram_clearSonogram(Sonogram *sonogram) {

    sonogram->index = 0;
    sonogram->wrapped = false;
    sonogram->copyIsWrapped = false;
    memset(sonogram->mainSonogram, 0, MAX_NUMBER_OF_GOERTZEL_FILTERS * SONOGRAM_LENGTH * sizeof(double));

}

void Sonogram_update(SInt16 sample, Sonogram *sonogram) {

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
            sonogram->wrapped = true;

        }

    }

}

double Sonogram_getCurrentValue(Sonogram *sonogram, int filter) {

    return GoertzelFilter_estimate(&sonogram->goerztelFilters[filter]);

}

void Sonogram_copySonogram(Sonogram *sonogram) {

    int copyIndex = sonogram->index;

    if (copyIndex >= SONOGRAM_LENGTH ) {

        copyIndex = 0;

    }

    if (sonogram->wrapped) {

        sonogram->copyIsWrapped = true;

        int firstSectionLength = copyIndex;

        int secondSectionLength = SONOGRAM_LENGTH - firstSectionLength;

        for (int i = 0; i < sonogram->numberOfFilters; i += 1) {

            memcpy(&sonogram->copySonogram[i][0], &sonogram->mainSonogram[i][firstSectionLength], secondSectionLength * sizeof(double));

            memcpy(&sonogram->copySonogram[i][secondSectionLength], &sonogram->mainSonogram[i][0], firstSectionLength * sizeof(double));

        }

    } else {

        for (int i = 0; i < sonogram->numberOfFilters; i += 1) {

            memcpy(sonogram->copySonogram[i], sonogram->mainSonogram[i], SONOGRAM_LENGTH * sizeof(double));

        }

    }

}

double Sonogram_getValue(Sonogram *sonogram, int i, int j, int width, int height, int duration) {

    if (i < 0 || i > width-1 || j < 0 || j > height-1) {

        return 0;

    } else {

        int numberOfMeasurementPoints = duration * SAMPLES_PER_SECOND / GOERTZEL_WINDOW_SIZE;

        if (numberOfMeasurementPoints > SONOGRAM_LENGTH) {

            numberOfMeasurementPoints = SONOGRAM_LENGTH;

        }

        double xRatio = (float)(numberOfMeasurementPoints - 1) / (double)(width - 1);

        double yRatio = (float)(sonogram->numberOfFilters - 1) / (double)(height - 1);

        int xIndex = (int)(0.5 + (double)i * xRatio);

        if (sonogram->copyIsWrapped) {

            xIndex += SONOGRAM_LENGTH - numberOfMeasurementPoints;

        }

        int yIndex = sonogram->numberOfFilters - 1 - (int)(0.5 + (double)j * yRatio);

        return sonogram->copySonogram[yIndex][xIndex];

    }

}

float interpolate(double value, double x0, double x1, double y0, double y1) {

    return (value - x0) * (y1 - y0) / (x1 - x0) + y0;

}

double base(double value) {

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

void Sonogram_colour(double value, double min, double max, UInt8 *red, UInt8 *green, UInt8 *blue) {

    double scaledValue = MIN(1.0, MAX(0.0, logf( (value + 1.0) / min) / logf(max / min)));

    *red = (UInt8) (0.5 + 255.0 * base(scaledValue - 0.5));

    *green = (UInt8) (0.5 + 255.0 * base(scaledValue));

    *blue = (UInt8) (0.5 + 255.0 * base(scaledValue + 0.5));

}
