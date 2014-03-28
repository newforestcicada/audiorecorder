//
//  DeviceType.h
//  AudioRecorder Plugin
//
//  Created by acr on 01/03/2014.
//  Copyright (c) 2014 New Forest Cicada Project. All rights reserved.
//

#import <Foundation/Foundation.h>

#define DEVICE_TYPE_UNKNOWN         0
#define DEVICE_TYPE_SIMULATOR       1

#define DEVICE_TYPE_IPHONE          2
#define DEVICE_TYPE_IPHONE_3G       3
#define DEVICE_TYPE_IPHONE_3GS      4
#define DEVICE_TYPE_IPHONE_4        5
#define DEVICE_TYPE_IPHONE_4S       6
#define DEVICE_TYPE_IPHONE_5        7
#define DEVICE_TYPE_IPHONE_5S       8
#define DEVICE_TYPE_IPHONE_5C       9

#define DEVICE_TYPE_IPAD            10
#define DEVICE_TYPE_IPAD_2          11
#define DEVICE_TYPE_IPAD_MINI       12
#define DEVICE_TYPE_IPAD_3          13
#define DEVICE_TYPE_IPAD_4          14
#define DEVICE_TYPE_IPAD_AIR        15
#define DEVICE_TYPE_IPAD_MINI_2     16

#define DEVICE_TYPE_IPOD            17
#define DEVICE_TYPE_IPOD_2          18
#define DEVICE_TYPE_IPOD_3          19
#define DEVICE_TYPE_IPOD_4          20
#define DEVICE_TYPE_IPOD_5          21

@interface DeviceType : NSObject

+(int)getDeviceType;
+(BOOL)iPhone;
+(BOOL)iPod;
+(BOOL)iPad;
+(BOOL)simulator;

@end
