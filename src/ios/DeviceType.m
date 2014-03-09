//
//  DeviceType.m
//  AudioRecorder Plugin
//
//  Created by acr on 23/02/2013.
//  Copyright (c) 2013 New Forest Cicada Project. All rights reserved.
//

#import "DeviceType.h"
#import <sys/utsname.h>

@implementation DeviceType

+(int)getDeviceType {
    
    struct utsname systemInfo;
    
    uname(&systemInfo);
    
    NSString *deviceName = [NSString stringWithCString:systemInfo.machine encoding:NSUTF8StringEncoding];
    
    if ( [deviceName isEqualToString:@"i386"] ) {
        
        return DEVICE_TYPE_SIMULATOR;
        
    } else if ( [deviceName isEqualToString:@"iPod1,1"] ) {
        
        return DEVICE_TYPE_IPOD;
        
    } else if ( [deviceName isEqualToString:@"iPod2,1"] ) {
        
        return DEVICE_TYPE_IPOD2;
        
    } else if ( [deviceName isEqualToString:@"iPod3,1"] ) {
        
        return DEVICE_TYPE_IPOD3;
        
    } else if ( [deviceName isEqualToString:@"iPod4,1"] ) {
        
        return DEVICE_TYPE_IPOD4;
        
    } else if ( [deviceName isEqualToString:@"iPhone1,1"] ) {
        
        return DEVICE_TYPE_IPHONE;
        
    } else if ( [deviceName isEqualToString:@"iPhone1,2"] | [deviceName isEqualToString:@"iPhone2,1"] ) {
        
        return DEVICE_TYPE_IPHONE3;
    
    } else if ( [deviceName isEqualToString:@"iPhone3,1"] | [deviceName isEqualToString:@"iPhone3,2"] | [deviceName isEqualToString:@"iPhone3,3"]) {
        
        return DEVICE_TYPE_IPHONE4;
        
    } else if ( [deviceName isEqualToString:@"iPhone4,1"] ) {
        
         return DEVICE_TYPE_IPHONE4S;
    
    } else if ( [deviceName isEqualToString:@"iPhone5,1"] | [deviceName isEqualToString:@"iPhone5,2"] ) {
        
        return DEVICE_TYPE_IPHONE5;
        
    } else if ( [deviceName isEqualToString:@"iPad1,1"] ) {
        
        return DEVICE_TYPE_IPAD;
        
    } else if ( [deviceName isEqualToString:@"iPad2,1"] ) {
        
        return DEVICE_TYPE_IPAD2;
        
    } else if ( [deviceName isEqualToString:@"iPad3,1"] ) {
        
        return DEVICE_TYPE_IPAD3;
        
    } else {
        
        return DEVICE_TYPE_UNKNOWN;
    
    }
                                                  
}

@end
