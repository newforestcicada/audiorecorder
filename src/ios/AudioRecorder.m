//
//  AudioRecorder.m
//  AudioRecorder Plugin
//
//  Created by acr on 02/03/2014.
//  Copyright (c) 2014 University of Southampton. All rights reserved.
//

#import <AudioToolbox/AudioToolbox.h>

#import "AudioRecorder.h"

#import "DeviceType.h"
#import "LowPassFilter.h"
#import "GoertzelFilter.h"
#import "RecordingBuffer.h"
#import "HeterodyneDetector.h"

#import "NSData+MBBase64.h"

#define NUMBER_OF_GOERTZEL_FILTERS  20

#define GOERTZEL_WINDOW_SIZE 128
#define SAMPLING_RATE 44100.0

#define MAX_SURVEY_VALUES  1024

typedef enum {SILENT, WHITE_NOISE, HETERODYNE} output_t;

typedef struct {
	AudioUnit rioUnit;
	AudioStreamBasicDescription asbd;
	output_t output;
	LowPassFilter lowPassFilter;
    HeterodyneDetector heterodyneDetector;
	GoertzelFilter goerztelFilters[NUMBER_OF_GOERTZEL_FILTERS];
	RecordingBuffer recordingBuffer;
} AudioRecorderState;

@interface AudioRecorder () {
    
    AudioRecorderState _audioRecorderState;
        
}

@end

static AudioRecorder *_audioRecorder;

@implementation AudioRecorder

+(AudioRecorder*)getInstance {
    
    if ( _audioRecorder == nil ) {
        
        _audioRecorder = [[AudioRecorder alloc] init];
        
    }
    
    return _audioRecorder;
    
}

static BOOL CheckError(OSStatus error, const char *operation) {
	
	if ( error == noErr ) {
		return NO;
	}
	
	char errorString[20];
	*(UInt32*)(errorString+1) = CFSwapInt32HostToBig(error);
	
	if (isprint(errorString[1]) && isprint(errorString[2]) && isprint(errorString[3]) && isprint(errorString[4]) ) {
		errorString[0] = errorString[5] = '\'';
		errorString[6] = '\0';
	} else {
		sprintf(errorString, "%d", (int)error);
	}
	
	NSLog(@"Error: %s (%s)", operation, errorString);
	
	return YES;
	
}

-(BOOL)initialiseAudioRecorder {
	
	BOOL error;
	
	error = CheckError(AudioSessionInitialize(NULL, kCFRunLoopDefaultMode,MyInterruptionListener,&_audioRecorderState.rioUnit),"Couldn't initialise the audio session");
	
	if ( error ) {
		return NO;
	}
    
    return YES;
	
}

-(BOOL)startAudioRecorder {
	
	BOOL error;
    
    UInt32 category = kAudioSessionCategory_PlayAndRecord;
	
	error = CheckError(AudioSessionSetProperty(kAudioSessionProperty_AudioCategory, sizeof(category), &category),
	                   "Couldn't set the category on the audio session");
	
	if ( error ) {
		return NO;
	}
	
	UInt32 inputAvailable;
	UInt32 ui32PropertySize = sizeof(inputAvailable);
	
	error = CheckError(AudioSessionGetProperty(kAudioSessionProperty_AudioInputAvailable, &ui32PropertySize, &inputAvailable), "Couldn't get current audio input available property");
	
	if ( error || !inputAvailable ) {
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
	
	if ( error ) {
		return NO;
	}
    
	UInt32 oneFlag = 1;
	
	AudioUnitElement bus0 = 0;
	
	error = CheckError(AudioUnitSetProperty(_audioRecorderState.rioUnit, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Output, bus0, &oneFlag, sizeof(oneFlag)), "Couldn't enable RIO output");
	
	if ( error ) {
		return NO;
	}
	
	AudioUnitElement bus1 = 1;
	
	error = CheckError(AudioUnitSetProperty(_audioRecorderState.rioUnit, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Input, bus1, &oneFlag, sizeof(oneFlag)), "Couldn't enable RIO input");
	
	if ( error ) {
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
    
	if ( error ) {
		return NO;
	}
	
	error = CheckError(AudioUnitSetProperty(_audioRecorderState.rioUnit, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Output, bus1, &myASBD, sizeof(myASBD)), "Couldn't set the ABSD for RIO on output scope/bus 1");
	
	if ( error ) {
		return NO;
	}
    
	_audioRecorderState.asbd = myASBD;
	_audioRecorderState.output = SILENT;
    
    RecordingBuffer_initialise(&_audioRecorderState.recordingBuffer);
    
	_audioRecorderState.lowPassFilter = LowPassFilter_initialise(1.404746361e+03, 0.9985762554);
    
    for ( int i=0; i<NUMBER_OF_GOERTZEL_FILTERS; i++) {
        _audioRecorderState.goerztelFilters[i] = GoertzelFilter_initialise(GOERTZEL_WINDOW_SIZE, 1000.0+(float)i*1000.0, SAMPLING_RATE);
    }
    
    _audioRecorderState.heterodyneDetector = HeterodyneDetector_initialise(14000.0f);
    
	AURenderCallbackStruct callbackStruct;
	callbackStruct.inputProc = InputModulatingRenderCallback;
	callbackStruct.inputProcRefCon = &_audioRecorderState;
	
	error = CheckError(AudioUnitSetProperty(_audioRecorderState.rioUnit, kAudioUnitProperty_SetRenderCallback, kAudioUnitScope_Global, bus0, &callbackStruct, sizeof(callbackStruct)), "Couldn't set RIO's render callback on bus 0");
    
	if ( error ) {
		return NO;
	}
	
	error = CheckError(AudioSessionSetActive(true), "Couldn't set audio session active");
	
	if ( error ) {
		return NO;
	}
	
	error = CheckError(AudioUnitInitialize(_audioRecorderState.rioUnit), "Couldn't initialise the RIO unit");
	
	if ( error ) {
		return NO;
	}
	
	error = CheckError(AudioOutputUnitStart(_audioRecorderState.rioUnit), "Couldn't start the RIO unit");

	if ( error ) {
		return NO;
	}
    
	return YES;
	
}

-(BOOL)stopAudioRecorder {
	
    BOOL error = CheckError(AudioOutputUnitStop(_audioRecorderState.rioUnit), "Couldn't stop the RIO unit");
    
    if ( error ) {
        return NO;
    }
    
    return YES;
	
}

-(void)startWhiteNose {
    
    _audioRecorderState.output = WHITE_NOISE;

}

-(void)stopWhiteNoise {
    
	_audioRecorderState.output = SILENT;

}

-(void)startHeterodyne {
    
    _audioRecorderState.output = HETERODYNE;

}

-(void)stopHeterodyne {
    
    _audioRecorderState.output = SILENT;

}

-(void)setHeterodyneFrequency:(int)frequency {
    
    HeterodyneDetector_setFrequency((float)frequency, &_audioRecorderState.heterodyneDetector);

}

-(NSNumber*)getAmplitude {
    
	return [NSNumber numberWithFloat:LowPassFilter_output(&_audioRecorderState.lowPassFilter)];

}

-(NSArray*)getFrequencies {
    
    NSMutableArray *array = [[NSMutableArray alloc] initWithCapacity:NUMBER_OF_GOERTZEL_FILTERS];
    
    for ( int i=0; i<NUMBER_OF_GOERTZEL_FILTERS; i++ ) {
        
        float value = GoertzelFilter_estimate(&_audioRecorderState.goerztelFilters[i]);
        
        [array addObject:[NSNumber numberWithFloat:value]];
        
    }
    
    return array;
    
}

-(void)captureRecording
{
    
    RecordingBuffer_copyMainBuffer(&_audioRecorderState.recordingBuffer);

}

-(NSString*)writeSonogramWithURL:(NSURL*)url withX:(int)x andY:(int)y forDuration:(int)duration {

    char* rgba = (char*)malloc(4*x*y);
    
    /*
    int index = 0;
    
    float xRatio = (float)(_surveyIndex-1)/(float)(x-1);
    float yRatio = (float)(NUMBER_OF_GOERTZEL_FILTERS-1)/(float)(y-1);
    
    for (int j=0; j<y; j++) {
        
        int yIndex = (int)(0.5+yRatio*(float)j);
        
        for (int i=0; i<x; i++) {
            
            int xIndex = (int)(0.5+xRatio*(float)i);
            
            char value = (char)(20.0+200.0*_sonogram[xIndex][NUMBER_OF_GOERTZEL_FILTERS-yIndex-1]);
            
            for (int i=0; i<4; i++) {
                rgba[index++] = value;
            }
            
        }
        
    }
    */
    
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGContextRef bitmapContext = CGBitmapContextCreate(rgba,x,y,8,4*x,colorSpace,kCGImageAlphaPremultipliedLast);
    
    CFRelease(colorSpace);
    
    CGImageRef cgImage = CGBitmapContextCreateImage(bitmapContext);
    
    NSString* serialisedSonogram;
    
    if ( cgImage == NULL ) {
        
        NSLog(@"Couldn't create the sonogram bitmap object.");
        
    } else {
    
        @try {
                        
            UIImage *uiImage = [UIImage imageWithCGImage:cgImage];
            NSData *pngData = UIImagePNGRepresentation(uiImage);
            
            [pngData writeToURL:url atomically:NO];
            
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

-(BOOL)writeRecordingWithURL:(NSURL*)url forDuration:(int)duration {
		
	AudioFileID audioFile;
	
	BOOL error;
	
	error = CheckError(AudioFileCreateWithURL((__bridge CFURLRef)url,kAudioFileWAVEType,&_audioRecorderState.asbd,kAudioFileFlags_EraseFile,&audioFile), "Couldn't open audio file");
	
	if ( error ) {
		return NO;
	}
	
	error = CheckError(RecordingBuffer_writeRecording(&audioFile, &_audioRecorderState.recordingBuffer, duration), "Couldn't write data to audio file");
	
	if ( error ) {
		return NO;
	}
	
	error = CheckError(AudioFileClose(audioFile), "Couldn't close audio file");
	
	if ( error ) {
		return NO;
	}
	
	return YES;
	
}

static void MyInterruptionListener(void *inUserData, UInt32 inInterruptionState)
{
	
	switch (inInterruptionState)
	{
		case kAudioSessionBeginInterruption:
            NSLog(@"Audio interupted. Stopping detector.");
            [[AudioRecorder getInstance] stopAudioRecorder];
			break;
		case kAudioSessionEndInterruption:
            NSLog(@"Audio interuption ended. Starting detector.");
            [[AudioRecorder getInstance] startAudioRecorder];
			break;
		default:
			break;
	}
	
}

static OSStatus InputModulatingRenderCallback(void *inRefCon, AudioUnitRenderActionFlags *ioActionFlags, const AudioTimeStamp *inTimeStamp, UInt32 inBusNumber, UInt32 inNumberFrames, AudioBufferList *ioData)
{
	
	AudioRecorderState *audioRecorderState = (AudioRecorderState*)inRefCon;
	
	UInt32 bus1 = 1;
	
	CheckError(AudioUnitRender(audioRecorderState->rioUnit, ioActionFlags, inTimeStamp, bus1, inNumberFrames, ioData), "Couldn't render from RemoteIO unit");
	
	AudioSampleType sample = 0;
	AudioSampleType silent = 0;
	
	UInt32 bytesPerChannel = audioRecorderState->asbd.mBytesPerFrame / audioRecorderState->asbd.mChannelsPerFrame;
	
	for ( int bufCount = 0; bufCount<ioData->mNumberBuffers; bufCount++) {
		
		AudioBuffer buf = ioData->mBuffers[bufCount];
		
		int currentFrame = 0;
		
		while ( currentFrame < inNumberFrames ) {
			
            for (int currentChannel = 0; currentChannel<buf.mNumberChannels; currentChannel++) {
				
                memcpy(&sample, buf.mData+(currentFrame*audioRecorderState->asbd.mBytesPerFrame)+(currentChannel*bytesPerChannel), sizeof(AudioSampleType));
                
				LowPassFilter_update(sample, &audioRecorderState->lowPassFilter);
				RecordingBuffer_update(sample, &audioRecorderState->recordingBuffer);
                
                for (int i=0; i<NUMBER_OF_GOERTZEL_FILTERS;i++ ) {
                    GoertzelFilter_update(sample, &audioRecorderState->goerztelFilters[i]);
                }
                
                AudioSampleType *outputSample = &silent;

                if ( audioRecorderState->output == WHITE_NOISE ) {
                        
                    AudioSampleType random = rand();
                    
                    outputSample = &random;
                    
                } else if ( audioRecorderState->output == HETERODYNE ) {
                    
                    AudioSampleType heterodyne = HeterodyneDetector_update(sample, &audioRecorderState->heterodyneDetector);

                    outputSample = &heterodyne;
                    
                }
                
                memcpy(buf.mData+(currentFrame*audioRecorderState->asbd.mBytesPerFrame)+(currentChannel*bytesPerChannel), outputSample, sizeof(AudioSampleType));
				
			}
			
			currentFrame++;
			
		}
		
	}
	
	return noErr;
	
}

@end
