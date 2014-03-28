//
//  AudioRecorder.m
//  AudioRecorder Plugin
//
//  Created by acr on 02/03/2014.
//  Copyright (c) 2014 University of Southampton. All rights reserved.
//

#import <AudioToolbox/AudioToolbox.h>

#import "AudioRecorder.h"

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

+ (AudioRecorder*)getInstance {

    if (_audioRecorder == nil ) {

        _audioRecorder = [[AudioRecorder alloc] init];

    }

    return _audioRecorder;

}

- (NSString*)frequencyColorWithRed:(UInt8)red green:(UInt8)green blue:(UInt8)blue {
    
    return [NSString stringWithFormat:@"#%02x%02x%02x", red, green, blue];

}

static BOOL CheckError(OSStatus error, const char *operation) {

    if (error == noErr) {
        return NO;
    }

    char errorString[20];
    *(UInt32 *) (errorString + 1) = CFSwapInt32HostToBig(error);

    if (isprint(errorString[1]) && isprint(errorString[2]) && isprint(errorString[3]) && isprint(errorString[4])) {
        errorString[0] = errorString[5] = '\'';
        errorString[6] = '\0';
    } else {
        sprintf(errorString, "%d", (int) error);
    }

    NSLog(@"Error: %s (%s)", operation, errorString);

    return YES;

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
            
            _numberOfGoetzelFilters = 21;
            _goertzelStepFrequency = 1000;
            _goertzelStartFrequency = 1000;
 
            _amplitudeSigmoidFactor = 40.0f;
            _frequenciesSigmoidFactor = 0.01f;
            
            _sonogramMinValue = 40.0f;
            _sonogramMaxValue = 1000.0f;
            
        }
        
    }
    
    return self;
    
}

- (BOOL)initialiseAudioRecorder {

    BOOL error;

    error = CheckError(AudioSessionInitialize(NULL, kCFRunLoopDefaultMode, MyInterruptionListener, &_audioRecorderState.rioUnit), "Couldn't initialise the audio session");

    if (error) {
        return NO;
    }

    return YES;

}

- (BOOL)startAudioRecorder {

    BOOL error;

    UInt32 category = kAudioSessionCategory_PlayAndRecord;

    error = CheckError(AudioSessionSetProperty(kAudioSessionProperty_AudioCategory, sizeof(category), &category),
            "Couldn't set the category on the audio session");

    if (error) {
        return NO;
    }

    UInt32 inputAvailable;
    UInt32 ui32PropertySize = sizeof(inputAvailable);

    error = CheckError(AudioSessionGetProperty(kAudioSessionProperty_AudioInputAvailable, &ui32PropertySize, &inputAvailable), "Couldn't get current audio input available property");

    if (error || !inputAvailable) {
        return NO;
    }

    AudioComponentDescription audioCompDesc;
    audioCompDesc.componentType = kAudioUnitType_Output;
    audioCompDesc.componentSubType = kAudioUnitSubType_RemoteIO;
    audioCompDesc.componentManufacturer = kAudioUnitManufacturer_Apple;
    audioCompDesc.componentFlags = 0;
    audioCompDesc.componentFlagsMask = 0;

    AudioComponent rioComponent = AudioComponentFindNext(NULL, &audioCompDesc);

    error = CheckError(AudioComponentInstanceNew(rioComponent, &_audioRecorderState.rioUnit), "Couldn't get RIO unit instance");

    if (error) {
        return NO;
    }

    UInt32 oneFlag = 1;

    AudioUnitElement bus0 = 0;

    error = CheckError(AudioUnitSetProperty(_audioRecorderState.rioUnit, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Output, bus0, &oneFlag, sizeof(oneFlag)), "Couldn't enable RIO output");

    if (error) {
        return NO;
    }

    AudioUnitElement bus1 = 1;

    error = CheckError(AudioUnitSetProperty(_audioRecorderState.rioUnit, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Input, bus1, &oneFlag, sizeof(oneFlag)), "Couldn't enable RIO input");

    if (error) {
        return NO;
    }

    AudioStreamBasicDescription myASBD;

    memset(&myASBD, 0, sizeof(myASBD));

    myASBD.mSampleRate = 44100;
    myASBD.mFormatID = kAudioFormatLinearPCM;
    myASBD.mFormatFlags = kAudioFormatFlagsCanonical;
    myASBD.mBytesPerPacket = 2;
    myASBD.mFramesPerPacket = 1;
    myASBD.mBytesPerFrame = 2;
    myASBD.mChannelsPerFrame = 1;
    myASBD.mBitsPerChannel = 16;

    error = CheckError(AudioUnitSetProperty(_audioRecorderState.rioUnit, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Input, bus0, &myASBD, sizeof(myASBD)), "Couldn't set the ABSD for RIO on input scope/bus 0");

    if (error) {
        return NO;
    }

    error = CheckError(AudioUnitSetProperty(_audioRecorderState.rioUnit, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Output, bus1, &myASBD, sizeof(myASBD)), "Couldn't set the ABSD for RIO on output scope/bus 1");

    if (error) {
        return NO;
    }

    _audioRecorderState.asbd = myASBD;
    _audioRecorderState.output = SILENT;
    
    Sonogram_initialise(&_audioRecorderState.songram, _numberOfGoetzelFilters, _goertzelStartFrequency, _goertzelStepFrequency);

    RecordingBuffer_initialise(&_audioRecorderState.recordingBuffer);
    
    _audioRecorderState.heterodyneDetector = HeterodyneDetector_initialise(14000.0f, SAMPLES_PER_SECOND);

    _audioRecorderState.lowPassFilter = LowPassFilter_initialise(1.404746361e+03, 0.9985762554);
    
    AURenderCallbackStruct callbackStruct;
    callbackStruct.inputProc = InputModulatingRenderCallback;
    callbackStruct.inputProcRefCon = &_audioRecorderState;

    error = CheckError(AudioUnitSetProperty(_audioRecorderState.rioUnit, kAudioUnitProperty_SetRenderCallback, kAudioUnitScope_Global, bus0, &callbackStruct, sizeof(callbackStruct)), "Couldn't set RIO's render callback on bus 0");

    if (error) {
        return NO;
    }

    error = CheckError(AudioSessionSetActive(true), "Couldn't set audio session active");

    if (error) {
        return NO;
    }

    error = CheckError(AudioUnitInitialize(_audioRecorderState.rioUnit), "Couldn't initialise the RIO unit");

    if (error) {
        return NO;
    }

    error = CheckError(AudioOutputUnitStart(_audioRecorderState.rioUnit), "Couldn't start the RIO unit");

    if (error) {
        return NO;
    }

    return YES;

}

- (BOOL)stopAudioRecorder {

    BOOL error = CheckError(AudioOutputUnitStop(_audioRecorderState.rioUnit), "Couldn't stop the RIO unit");

    if (error) {
        return NO;
    }

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

- (void)captureRecording {

    RecordingBuffer_copyMainBuffer(&_audioRecorderState.recordingBuffer);
    Sonogram_copyMainSonogram(&_audioRecorderState.songram);

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

        NSLog(@"Couldn't create the sonogram bitmap object.");

    } else {

        @try {

            UIImage *uiImage = [UIImage imageWithCGImage:cgImage];
            NSData *pngData = UIImagePNGRepresentation(uiImage);

            [pngData writeToURL:url atomically:YES];

            CFRelease(cgImage);
            CFRelease(bitmapContext);

            serialisedSonogram = [pngData base64Encoding];

        } @catch (NSException *e) {

            NSLog(@"Couldn't write the sonogram to a file.");

        }

    }

    free(rgba);

    return serialisedSonogram;

}

- (BOOL)writeRecordingWithURL:(NSURL *)url forDuration:(int)duration {

    AudioFileID audioFile;

    BOOL error;

    error = CheckError(AudioFileCreateWithURL((__bridge CFURLRef) url, kAudioFileWAVEType, &_audioRecorderState.asbd, kAudioFileFlags_EraseFile, &audioFile), "Couldn't open audio file");

    if (error) {
        return NO;
    }

    error = CheckError(RecordingBuffer_writeRecording(&audioFile, &_audioRecorderState.recordingBuffer, duration), "Couldn't write data to audio file");

    if (error) {
        return NO;
    }

    error = CheckError(AudioFileClose(audioFile), "Couldn't close audio file");

    if (error) {
        return NO;
    }

    return YES;

}

static void MyInterruptionListener(void *inUserData, UInt32 inInterruptionState) {

    switch (inInterruptionState) {
        case kAudioSessionBeginInterruption:
            NSLog(@"Audio interupted. Stopping recorder.");
            [[AudioRecorder getInstance] stopAudioRecorder];
            break;
        case kAudioSessionEndInterruption:
            NSLog(@"Audio interuption ended. Starting recorder.");
            [[AudioRecorder getInstance] startAudioRecorder];
            break;
        default:
            break;
    }

}

static OSStatus InputModulatingRenderCallback(void *inRefCon, AudioUnitRenderActionFlags *ioActionFlags, const AudioTimeStamp *inTimeStamp, UInt32 inBusNumber, UInt32 inNumberFrames, AudioBufferList *ioData) {

    AudioRecorderState *audioRecorderState = (AudioRecorderState *) inRefCon;

    UInt32 bus1 = 1;

    CheckError(AudioUnitRender(audioRecorderState->rioUnit, ioActionFlags, inTimeStamp, bus1, inNumberFrames, ioData), "Couldn't render from RemoteIO unit");

    AudioSampleType sample = 0;
    AudioSampleType silent = 0;

    UInt32 bytesPerChannel = audioRecorderState->asbd.mBytesPerFrame / audioRecorderState->asbd.mChannelsPerFrame;

    for (int bufCount = 0; bufCount < ioData->mNumberBuffers; bufCount++) {

        AudioBuffer buf = ioData->mBuffers[bufCount];

        int currentFrame = 0;

        while (currentFrame < inNumberFrames) {

            for (int currentChannel = 0; currentChannel < buf.mNumberChannels; currentChannel++) {

                memcpy(&sample, buf.mData + (currentFrame * audioRecorderState->asbd.mBytesPerFrame) + (currentChannel * bytesPerChannel), sizeof(AudioSampleType));

                Sonogram_update(sample, &audioRecorderState->songram);
                LowPassFilter_update(sample, &audioRecorderState->lowPassFilter);
                RecordingBuffer_update(sample, &audioRecorderState->recordingBuffer);

                AudioSampleType *outputSample = &silent;

                if (audioRecorderState->output == WHITE_NOISE) {

                    AudioSampleType random = rand();

                    outputSample = &random;

                } else if (audioRecorderState->output == HETERODYNE) {

                    AudioSampleType heterodyne = HeterodyneDetector_update(sample, &audioRecorderState->heterodyneDetector);

                    outputSample = &heterodyne;

                }

                memcpy(buf.mData + (currentFrame * audioRecorderState->asbd.mBytesPerFrame) + (currentChannel * bytesPerChannel), outputSample, sizeof(AudioSampleType));

            }

            currentFrame++;

        }

    }

    return noErr;

}

@end
