//
//  AudioRecorder.m
//  AudioRecorder Plugin
//
//  Created by acr on 02/03/2014.
//  Copyright (c) 2014 University of Southampton. All rights reserved.
//

#import "AudioRecorder.h"

#import <AVFoundation/AVFoundation.h>
#import <AVFoundation/AVAudioSession.h>

#import "Settings.h"
#import "Sonogram.h"
#import "DeviceType.h"
#import "LowPassFilter.h"
#import "GoertzelFilter.h"
#import "RecordingBuffer.h"
#import "HeterodyneDetector.h"

typedef enum {
    SILENT, WHITE_NOISE, HETERODYNE
} output_t;

typedef struct {
    AudioUnit rioUnit;
    AudioStreamBasicDescription asbd;
    output_t output;
    LowPassFilter lowPassFilter;
    HeterodyneDetector heterodyneDetector;
    Sonogram songram;
    RecordingBuffer recordingBuffer;
} AudioRecorderState;

@interface AudioRecorder () {

    AudioRecorderState _audioRecorderState;

    int _numberOfGoetzelFilters;
    int _goertzelStartFrequency;
    int _goertzelStepFrequency;

    float _amplitudeSigmoidFactor;
    float _frequenciesSigmoidFactor;

    float _sonogramMinValue;
    float _sonogramMaxValue;

}

- (NSString*)frequencyColorWithRed:(UInt8)red green:(UInt8)green blue:(UInt8)blue;

@end

static AudioRecorder *_audioRecorder;

@implementation AudioRecorder

+ (AudioRecorder *)getInstance {

    if (_audioRecorder == nil ) {

        _audioRecorder = [[AudioRecorder alloc] init];

    }

    return _audioRecorder;

}

- (NSString*)frequencyColorWithRed:(UInt8)red green:(UInt8)green blue:(UInt8)blue {

    return [NSString stringWithFormat:@"#%02x%02x%02x", red, green, blue];

}

- (id)init {

    self = [super init];

    if (self) {

        _numberOfGoetzelFilters = 43;
        _goertzelStepFrequency = 500;
        _goertzelStartFrequency = 500;

        _amplitudeSigmoidFactor = 100.0f;
        _frequenciesSigmoidFactor = 0.002f;

        _sonogramMinValue = 80.0f;
        _sonogramMaxValue = 5000.0f;

        int deviceType = [DeviceType getDeviceType];

        if (deviceType == DEVICE_TYPE_IPHONE || deviceType == DEVICE_TYPE_IPHONE_3G || deviceType == DEVICE_TYPE_IPHONE_3GS) {

            _amplitudeSigmoidFactor = 40.0f;
            _frequenciesSigmoidFactor = 0.01f;

            _sonogramMinValue = 40.0f;
            _sonogramMaxValue = 1000.0f;

        }

        if (deviceType == DEVICE_TYPE_IPHONE || deviceType == DEVICE_TYPE_IPHONE_3G || deviceType == DEVICE_TYPE_IPHONE_3GS || deviceType == DEVICE_TYPE_IPHONE_4 ) {

            _numberOfGoetzelFilters = 21;
            _goertzelStepFrequency = 1000;
            _goertzelStartFrequency = 1000;

        }

    }

    return self;

}

- (BOOL)initialiseAudioRecorder {
    
    return YES;

}

- (BOOL)startAudioRecorder {

    BOOL success = NO;
    NSError *error = nil;

    AVAudioSession *session = [AVAudioSession sharedInstance];
    
    // Initialise session and set rate and category
    
    success = [session setPreferredSampleRate: 44100 error: &error];
    
    success |= [session setCategory: AVAudioSessionCategoryPlayAndRecord error: &error];
    
    if (!success) {
        
        NSLog(@"[CordovaAudioRecorder] Error: %@", [error localizedDescription]);
        
        return NO;
        
    }

    // Check if input is available
    
    success = [session isInputAvailable];
    
    if (!success) {
        
        NSLog(@"[CordovaAudioRecorder] No input available.");
        
        return NO;
        
    }

    // Set up audio component
    
    AudioComponentDescription audioCompDesc;
    
    audioCompDesc.componentType = kAudioUnitType_Output;
    audioCompDesc.componentSubType = kAudioUnitSubType_RemoteIO;
    audioCompDesc.componentManufacturer = kAudioUnitManufacturer_Apple;
    audioCompDesc.componentFlags = 0;
    audioCompDesc.componentFlagsMask = 0;

    AudioComponent rioComponent = AudioComponentFindNext(NULL, &audioCompDesc);

    OSStatus status = AudioComponentInstanceNew(rioComponent, &_audioRecorderState.rioUnit);
    
    if (status != noErr) {
        
        NSLog(@"[CordovaAudioRecorder] Could not get audio IO unit.");

        return NO;
    
    }
    
    // Enable RIO output

    UInt32 oneFlag = 1;

    AudioUnitElement bus0 = 0;

    status = AudioUnitSetProperty(_audioRecorderState.rioUnit, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Output, bus0, &oneFlag, sizeof(oneFlag));

    if (status != noErr) {
    
        NSLog(@"[CordovaAudioRecorder] Could not enable RIO output.");
        
        return NO;
    
    }
    
    // Enable RIO input

    AudioUnitElement bus1 = 1;

    status = AudioUnitSetProperty(_audioRecorderState.rioUnit, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Input, bus1, &oneFlag, sizeof(oneFlag));

    if (status != noErr) {
        
        NSLog(@"[CordovaAudioRecorder] Could not enable RIO input.");
        
        return NO;
        
    }
    
    // Create format

    AudioStreamBasicDescription myASBD;

    memset(&myASBD, 0, sizeof(myASBD));

    myASBD.mSampleRate = 44100;
    myASBD.mFormatID = kAudioFormatLinearPCM;
    myASBD.mFormatFlags = kAudioFormatFlagIsSignedInteger | kAudioFormatFlagsNativeEndian | kAudioFormatFlagIsPacked;
    myASBD.mBytesPerPacket = 2;
    myASBD.mFramesPerPacket = 1;
    myASBD.mBytesPerFrame = 2;
    myASBD.mChannelsPerFrame = 1;
    myASBD.mBitsPerChannel = 16;

    status = AudioUnitSetProperty(_audioRecorderState.rioUnit, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Input, bus0, &myASBD, sizeof(myASBD));
    
    if (status != noErr) {
        
        NSLog(@"[CordovaAudioRecorder] Could not set the ABSD for RIO on input scope.");

        return NO;
        
    }

    status = AudioUnitSetProperty(_audioRecorderState.rioUnit, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Output, bus1, &myASBD, sizeof(myASBD));
    
    if (status != noErr) {
        
        NSLog(@"[CordovaAudioRecorder] Could not set the ABSD for RIO on output scope.");

        return NO;
        
    }

    // Set up the sonogoram and heterodyne settings
    
    _audioRecorderState.asbd = myASBD;
    _audioRecorderState.output = SILENT;

    Sonogram_initialise(&_audioRecorderState.songram, _numberOfGoetzelFilters, _goertzelStartFrequency, _goertzelStepFrequency);

    RecordingBuffer_initialise(&_audioRecorderState.recordingBuffer);

    _audioRecorderState.heterodyneDetector = HeterodyneDetector_initialise(14000.0f, SAMPLES_PER_SECOND);

    _audioRecorderState.lowPassFilter = LowPassFilter_initialise(1.404746361e+03, 0.9985762554);

    // Set up the render callback
    
    AURenderCallbackStruct callbackStruct;
    callbackStruct.inputProc = InputModulatingRenderCallback;
    callbackStruct.inputProcRefCon = &_audioRecorderState;

    status = AudioUnitSetProperty(_audioRecorderState.rioUnit, kAudioUnitProperty_SetRenderCallback, kAudioUnitScope_Global, bus0, &callbackStruct, sizeof(callbackStruct));
    
    if (status != noErr) {
        
        NSLog(@"[CordovaAudioRecorder] Could not set RIO render callback.");

        return NO;
        
    }
    
    // Initialise and start RIO
    
    status = AudioUnitInitialize(_audioRecorderState.rioUnit);
    
    if (status != noErr) {
        
        NSLog(@"[CordovaAudioRecorder] Could not initialise the RIO unit.");

        return NO;

    }

    status = AudioOutputUnitStart(_audioRecorderState.rioUnit);
    
    if (status != noErr) {
        
        NSLog(@"[CordovaAudioRecorder] Could not start the RIO unit.");

        return NO;

    }
    
    // Activate session
    
    success = [session setActive: YES error: &error];
    
    if (!success) {
        
        NSLog(@"[CordovaAudioRecorder] Error: %@", [error localizedDescription]);
        
        return NO;
        
    }
    
    // All done

    return YES;

}

- (BOOL)stopAudioRecorder {

    // Stop the IO unit
    
    OSStatus status = AudioOutputUnitStop(_audioRecorderState.rioUnit);
    
    if (noErr != status) {
        
        NSLog(@"[CordovaAudioRecorder] Could not start the RIO unit.");
        
    }
    
    // Stop the session

    BOOL success;
    NSError *error;

    AVAudioSession *session = [AVAudioSession sharedInstance];

    success = [session setActive: NO error: &error];
    
    if (!success) {
        
        NSLog(@"[CordovaAudioRecorder] Error: %@", [error localizedDescription]);
        
        return NO;
        
    }
    
    // All done
    
    return YES;

}

- (void)startWhiteNose {

    _audioRecorderState.output = WHITE_NOISE;

}

- (void)stopWhiteNoise {

    _audioRecorderState.output = SILENT;

}

- (void)startHeterodyne {

    _audioRecorderState.output = HETERODYNE;

}

- (void)stopHeterodyne {

    _audioRecorderState.output = SILENT;

}

- (void)setHeterodyneFrequency:(int)frequency {

    HeterodyneDetector_setFrequency((float) frequency, &_audioRecorderState.heterodyneDetector);

}

- (NSNumber*)getAmplitude:(outputScaling_t)outputScaling {

    float value = LowPassFilter_output(&_audioRecorderState.lowPassFilter);

    if (outputScaling == AUDIORECORDER_RAW) {

        return [NSNumber numberWithFloat:value];

    } else {

        float scaledValue = 2.0f / (1.0f + (float)exp(-_amplitudeSigmoidFactor * value)) - 1.0f;

        return [NSNumber numberWithFloat:scaledValue];

    }

}

- (NSArray *)getFrequencies:(outputScaling_t)outputScaling {

    NSMutableArray *array = [[NSMutableArray alloc] initWithCapacity:_numberOfGoetzelFilters];

    for (int i = 0; i < _numberOfGoetzelFilters; i += 1) {

        float value = Sonogram_getCurrentValue(&_audioRecorderState.songram, i);

        if (outputScaling == AUDIORECORDER_RAW) {

            [array addObject:[NSNumber numberWithFloat:value]];

        } else {

            if (outputScaling == AUDIORECORDER_SCALED) {

                float scaledValue = 2.0f / (1.0f + (float)exp(-_frequenciesSigmoidFactor * value)) - 1.0f;

                [array addObject:[NSNumber numberWithFloat:scaledValue]];

            } else {

                UInt8 red;
                UInt8 green;
                UInt8 blue;

                Sonogram_colour(value, _sonogramMinValue, _sonogramMaxValue, &red, &green, &blue);

                [array addObject:[self frequencyColorWithRed:red green:green blue:blue]];

            }

        }

    }

    return array;

}

- (void)clearBuffers {

    Sonogram_clearSonogram(&_audioRecorderState.songram);
    RecordingBuffer_clearBuffer(&_audioRecorderState.recordingBuffer);

}

- (void)captureRecording {

    Sonogram_copySonogram(&_audioRecorderState.songram);
    RecordingBuffer_copyBuffer(&_audioRecorderState.recordingBuffer);
    
}

- (NSString *)writeSonogramWithURL:(NSURL *)url withX:(int)x andY:(int)y forDuration:(int)duration {

    char *rgba = malloc(4 * x * y);

    UInt8 red;
    UInt8 green;
    UInt8 blue;

    int index = 0;

    for (int j = 0; j < y; j += 1) {

        for (int i = 0; i < x; i += 1) {

            float value = Sonogram_getValue(&_audioRecorderState.songram, i, j, x, y, duration);

            Sonogram_colour(value, _sonogramMinValue, _sonogramMaxValue, &red, &green, &blue);

            rgba[index++] = red;
            rgba[index++] = green;
            rgba[index++] = blue;
            rgba[index++] = 255;

        }

    }

    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGContextRef bitmapContext = CGBitmapContextCreate(rgba, x, y, 8, (size_t)(4 * x), colorSpace, (CGBitmapInfo) kCGImageAlphaPremultipliedLast);

    CFRelease(colorSpace);

    CGImageRef cgImage = CGBitmapContextCreateImage(bitmapContext);

    NSString *serialisedSonogram;

    if (cgImage == NULL ) {

        NSLog(@"Could not create the sonogram bitmap object.");

    } else {

        @try {

            UIImage *uiImage = [UIImage imageWithCGImage:cgImage];
            NSData *pngData = UIImagePNGRepresentation(uiImage);

            [pngData writeToURL:url atomically:YES];

            CFRelease(cgImage);
            CFRelease(bitmapContext);

            serialisedSonogram = [pngData base64EncodedStringWithOptions:NSDataBase64EncodingEndLineWithLineFeed];

        } @catch (NSException *e) {

            NSLog(@"Could not write the sonogram to a file.");

        }

    }

    free(rgba);

    return serialisedSonogram;

}

- (BOOL)writeRecordingWithURL:(NSURL *)url forDuration:(int)duration {

    AudioFileID audioFile;

    OSStatus status = AudioFileCreateWithURL((__bridge CFURLRef) url, kAudioFileWAVEType, &_audioRecorderState.asbd, kAudioFileFlags_EraseFile, &audioFile);
    
    if (status != noErr) {
        
        NSLog(@"[CordovaAudioRecorder] Could not open audio file.");
        
        return NO;

    }

    status = RecordingBuffer_writeRecording(&audioFile, &_audioRecorderState.recordingBuffer, duration);
    
    if (status != noErr) {
        
        NSLog(@"[CordovaAudioRecorder] Could not write data to audio file.");

        return NO;

    }

    status = AudioFileClose(audioFile);
    
    if (status != noErr) {
        
        NSLog(@"[CordovaAudioRecorder] Could not close audio file.");

        return NO;

    }

    return YES;

}

static OSStatus InputModulatingRenderCallback(void *inRefCon, AudioUnitRenderActionFlags *ioActionFlags, const AudioTimeStamp *inTimeStamp, UInt32 inBusNumber, UInt32 inNumberFrames, AudioBufferList *ioData) {

    AudioRecorderState *audioRecorderState = (AudioRecorderState *) inRefCon;

    UInt32 bus1 = 1;

    OSStatus status = AudioUnitRender(audioRecorderState->rioUnit, ioActionFlags, inTimeStamp, bus1, inNumberFrames, ioData);
    
    if (status != noErr) {
        
        NSLog(@"[CordovaAudioRecorder] Could not render from RemoteIO unit.");
        
        return status;
        
    }

    SInt16 sample = 0;
    SInt16 silent = 0;

    UInt32 bytesPerChannel = audioRecorderState->asbd.mBytesPerFrame / audioRecorderState->asbd.mChannelsPerFrame;

    for (int bufCount = 0; bufCount < ioData->mNumberBuffers; bufCount++) {

        AudioBuffer buf = ioData->mBuffers[bufCount];

        int currentFrame = 0;

        while (currentFrame < inNumberFrames) {

            for (int currentChannel = 0; currentChannel < buf.mNumberChannels; currentChannel++) {

                memcpy(&sample, buf.mData + (currentFrame * audioRecorderState->asbd.mBytesPerFrame) + (currentChannel * bytesPerChannel), sizeof(SInt16));

                Sonogram_update(sample, &audioRecorderState->songram);
                LowPassFilter_update(abs(sample), &audioRecorderState->lowPassFilter);
                RecordingBuffer_update(sample, &audioRecorderState->recordingBuffer);

                SInt16 *outputSample = &silent;

                if (audioRecorderState->output == WHITE_NOISE) {

                    SInt16 random = rand();

                    outputSample = &random;

                } else if (audioRecorderState->output == HETERODYNE) {

                    SInt16 heterodyne = HeterodyneDetector_update(sample, &audioRecorderState->heterodyneDetector);

                    outputSample = &heterodyne;

                }

                memcpy(buf.mData + (currentFrame * audioRecorderState->asbd.mBytesPerFrame) + (currentChannel * bytesPerChannel), outputSample, sizeof(SInt16));

            }

            currentFrame++;

        }

    }

    return noErr;

}

@end
