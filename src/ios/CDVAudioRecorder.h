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

@interface CDVAudioRecorder : CDVPlugin <CLLocationManagerDelegate>

- (void)initialiseAudioRecorder:(CDVInvokedUrlCommand *)command;

- (void)startAudioRecorder:(CDVInvokedUrlCommand *)command;

- (void)stopAudioRecorder:(CDVInvokedUrlCommand *)command;

- (void)startWhiteNoise:(CDVInvokedUrlCommand *)command;

- (void)stopWhiteNoise:(CDVInvokedUrlCommand *)command;

- (void)startHeterodyne:(CDVInvokedUrlCommand *)command;

- (void)stopHeterodyne:(CDVInvokedUrlCommand *)command;

- (void)setHeterodyneFrequency:(CDVInvokedUrlCommand *)command;

- (void)getAmplitude:(CDVInvokedUrlCommand *)command;

- (void)getScaledAmplitude:(CDVInvokedUrlCommand *)command;

- (void)getFrequencies:(CDVInvokedUrlCommand *)command;

- (void)getScaledFrequencies:(CDVInvokedUrlCommand *)command;

- (void)getFrequencyColours:(CDVInvokedUrlCommand *)command;

- (void)captureRecording:(CDVInvokedUrlCommand *)command;

- (void)writeSonogram:(CDVInvokedUrlCommand *)command;

- (void)writeRecording:(CDVInvokedUrlCommand *)command;

@end
