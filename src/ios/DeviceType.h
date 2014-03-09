//
//  DeviceType.h
//  AudioRecorder Plugin
//
//  Created by acr on 23/02/2013.
//  Copyright (c) 2013 New Forest Cicada Project. All rights reserved.
//

#import <Foundation/Foundation.h>

#define DEVICE_TYPE_UNKNOWN     0
#define DEVICE_TYPE_SIMULATOR   1

#define DEVICE_TYPE_IPHONE      2
#define DEVICE_TYPE_IPHONE3     3
#define DEVICE_TYPE_IPHONE4     4
#define DEVICE_TYPE_IPHONE4S    5
#define DEVICE_TYPE_IPHONE5     6

#define DEVICE_TYPE_IPAD        7
#define DEVICE_TYPE_IPAD2       8
#define DEVICE_TYPE_IPAD3       9

#define DEVICE_TYPE_IPOD        10
#define DEVICE_TYPE_IPOD2       11
#define DEVICE_TYPE_IPOD3       12
#define DEVICE_TYPE_IPOD4       13

@interface DeviceType : NSObject

+(int)getDeviceType;

@end
