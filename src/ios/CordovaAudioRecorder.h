//
//  AudioRecorderPlugin.h
//  AudioRecorder Plugin
//
//  Created by acr on 02/03/2014.
//  Copyright (c) 2014 University of Southampton. All rights reserved.
//

#import <Cordova/CDV.h>
#import <CoreLocation/CoreLocation.h>

#import "AudioRecorder.h"

@interface CordovaAudioRecorder : CDVPlugin <CLLocationManagerDelegate>

/* Initialise audioRecorder. Must be called once at the start of the application to set up interupts for the low level sound access. */

- (void)initialiseAudioRecorder:(CDVInvokedUrlCommand *)command;

/* Start the audio processing. This should be called when the main sound recording view comes ot the foreground, either due to a change of view, or the app coming to the foreground. */

- (void)startAudioRecorder:(CDVInvokedUrlCommand *)command;

/* Stop the audio processing. This should be called when the main sound recording view leaves ot the foreground, either due to a change of view, or the app going to the background. */

- (void)stopAudioRecorder:(CDVInvokedUrlCommand *)command;

/* Start playing white noise through the headphone output. */

- (void)startWhiteNoise:(CDVInvokedUrlCommand *)command;

/* Stop playing white noise through the headphone output. Actually sets the output to silence. */

- (void)stopWhiteNoise:(CDVInvokedUrlCommand *)command;

/* Start playing heterodyne signal through the headphone output. */

- (void)startHeterodyne:(CDVInvokedUrlCommand *)command;

/* Stop playing heterodyne signal through the headphone output. Actually sets the output to silence. */

- (void)stopHeterodyne:(CDVInvokedUrlCommand *)command;

/* Set the frequency of the pure tone used in the heterodyne receiver. */

- (void)setHeterodyneFrequency:(CDVInvokedUrlCommand *)command;

/* Get the current raw amplitude as a double value between 0 and 1. */

- (void)getAmplitude:(CDVInvokedUrlCommand *)command;

/* Get the current raw amplitude as a double value between 0 and 1 scaled with a sigmoid function to provide a more responsive amplitude indicator. */

- (void)getScaledAmplitude:(CDVInvokedUrlCommand *)command;

/* Get the current raw output of the Goertzel filters as a array of double values. */

- (void)getFrequencies:(CDVInvokedUrlCommand *)command;

/* Get the current raw output of the Goertzel filters as a array of double values scaled between 0 and 1 for use on a spectral plot. */

- (void)getScaledFrequencies:(CDVInvokedUrlCommand *)command;

/* Get the current raw output of the Goertzel filters as a array of double values scaled between 0 and 1 for use on a spectral plot. */

- (void)getFrequencyColours:(CDVInvokedUrlCommand *)command;

/* Clear the sonogram and recording buffers. */

- (void)clearBuffers:(CDVInvokedUrlCommand *)command;

/* Copy the main sonogram and recording buffers in preparation for writing to the file system. Returns the filename as a string. */

- (void)captureRecording:(CDVInvokedUrlCommand *)command;

/* Takes the width, height and duration, and writes the sonogram to the filesystem as a PNG file, returning a string containing a Base64 encoded version. */

- (void)writeSonogram:(CDVInvokedUrlCommand *)command;

/* Takes the duration, and writes the recording to the filesystem as a WAV file. */

- (void)writeRecording:(CDVInvokedUrlCommand *)command;

@end
