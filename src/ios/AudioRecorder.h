//
//  AudioRecorder.h
//  AudioRecorder Plugin
//
//  Created by acr on 02/03/2014.
//  Copyright (c) 2014 University of Southampton. All rights reserved.
//

#import <Foundation/Foundation.h>

typedef enum {
    AUDIORECORDER_RAW, AUDIORECORDER_SCALED, AUDIORECORDER_COLOURS
} outputScaling_t;

@interface AudioRecorder : NSObject

- (BOOL)initialiseAudioRecorder;

- (BOOL)startAudioRecorder;

- (BOOL)stopAudioRecorder;

- (void)startWhiteNose;

- (void)stopWhiteNoise;

- (void)startHeterodyne;

- (void)stopHeterodyne;

- (void)setHeterodyneFrequency:(int)frequency;

- (NSNumber*)getAmplitude:(outputScaling_t)outputScaling;

- (NSArray*)getFrequencies:(outputScaling_t)outputScaling;

- (void)captureRecording;

- (BOOL)writeRecordingWithURL:(NSURL *)url forDuration:(int)duration;

- (NSString*)writeSonogramWithURL:(NSURL *)url withX:(int)x andY:(int)y forDuration:(int)duration;

+ (AudioRecorder*)getInstance;

@end

