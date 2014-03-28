//
//  DeviceType.m
//  AudioRecorder Plugin
//
//  Created by acr on 01/03/2014.
//  Copyright (c) 2014 New Forest Cicada Project. All rights reserved.
//

#import "DeviceType.h"
#import <sys/utsname.h>

@interface DeviceType () {
    
}

+(NSString*)deviceType;

@end

@implementation DeviceType

+(NSString*)deviceType {
 
    struct utsname systemInfo;
    
    uname(&systemInfo);
    
    return [NSString stringWithCString:systemInfo.machine encoding:NSUTF8StringEncoding];
    
}

+(int)getDeviceType
{
    
    NSString* deviceType = [self deviceType];
    
    if ( [deviceType isEqualToString:@"i386"] || [deviceType isEqualToString:@"x86_64"] ) {
        
        return DEVICE_TYPE_SIMULATOR;
        
    } else if ( [deviceType isEqualToString:@"iPod1,1"] ) {
        
        return DEVICE_TYPE_IPOD;
        
    } else if ( [deviceType isEqualToString:@"iPod2,1"] ) {
        
        return DEVICE_TYPE_IPOD_2;
        
    } else if ( [deviceType isEqualToString:@"iPod3,1"] ) {
        
        return DEVICE_TYPE_IPOD_3;
        
    } else if ( [deviceType isEqualToString:@"iPod4,1"] ) {
        
        return DEVICE_TYPE_IPOD_4;
        
    } else if ( [deviceType isEqualToString:@"iPod5,1"] ) {
        
        return DEVICE_TYPE_IPOD_5;
        
    } else if ( [deviceType isEqualToString:@"iPhone1,1"] ) {
        
        return DEVICE_TYPE_IPHONE;
        
    } else if ( [deviceType isEqualToString:@"iPhone1,2"] ) {
        
        return DEVICE_TYPE_IPHONE_3G;
    
    } else if ( [deviceType isEqualToString:@"iPhone2,1"] ) {
        
        return DEVICE_TYPE_IPHONE_3GS;
        
    } else if ( [deviceType isEqualToString:@"iPhone3,1"] || [deviceType isEqualToString:@"iPhone3,3"] ) {
    
        return DEVICE_TYPE_IPHONE_4;
        
    } else if ( [deviceType isEqualToString:@"iPhone4,1"] ) {
    
        return DEVICE_TYPE_IPHONE_4S;
        
    } else if ( [deviceType isEqualToString:@"iPhone5,1"] || [deviceType isEqualToString:@"iPhone5,2"] ) {
    
        return DEVICE_TYPE_IPHONE_5;
        
    } else if ( [deviceType isEqualToString:@"iPhone5,3"] || [deviceType isEqualToString:@"iPhone5,4"] ) {
    
        return DEVICE_TYPE_IPHONE_5C;

    } else if ( [deviceType isEqualToString:@"iPhone6,1"] || [deviceType isEqualToString:@"iPhone6,2"] ) {

        return DEVICE_TYPE_IPHONE_5S;
    
    } else if ( [deviceType isEqualToString:@"iPad1,1"] ) {
        
        return DEVICE_TYPE_IPAD;
        
    } else if ( [deviceType isEqualToString:@"iPad2,1"] || [deviceType isEqualToString:@"iPad2,2"] || [deviceType isEqualToString:@"iPad2,3"] || [deviceType isEqualToString:@"iPad2,4"] ) {
        
        return DEVICE_TYPE_IPAD_2;
        
    } else if ( [deviceType isEqualToString:@"iPad2,5"] || [deviceType isEqualToString:@"iPad2,6"] || [deviceType isEqualToString:@"iPad2,7"] ) {
      
        return DEVICE_TYPE_IPAD_MINI;
    
    } else if ( [deviceType isEqualToString:@"iPad3,1"] || [deviceType isEqualToString:@"iPad3,2"] || [deviceType isEqualToString:@"iPad3,3"] ) {
    
        return DEVICE_TYPE_IPAD_3;
        
    } else if ( [deviceType isEqualToString:@"iPad3,4"] || [deviceType isEqualToString:@"iPad3,5"] || [deviceType isEqualToString:@"iPad3,6"] ) {
        
        return DEVICE_TYPE_IPAD_4;

    } else if ( [deviceType isEqualToString:@"iPad4,1"] || [deviceType isEqualToString:@"iPad4,2"] ) {
        
        return DEVICE_TYPE_IPAD_AIR;

    } else if ( [deviceType isEqualToString:@"iPad4,4"] || [deviceType isEqualToString:@"iPad4,5"] ) {
        
        return DEVICE_TYPE_IPAD_MINI_2;
        
    }
    
    return DEVICE_TYPE_UNKNOWN;

}

+(BOOL)iPhone {
    
    int deviceType = [DeviceType getDeviceType];
    
    return (deviceType == DEVICE_TYPE_IPHONE) || (deviceType == DEVICE_TYPE_IPHONE_3G) || (deviceType == DEVICE_TYPE_IPHONE_3GS) || (deviceType == DEVICE_TYPE_IPHONE_4) || (deviceType == DEVICE_TYPE_IPHONE_4S) || (deviceType == DEVICE_TYPE_IPHONE_5) || (deviceType == DEVICE_TYPE_IPHONE_5C) || (deviceType == DEVICE_TYPE_IPHONE_5S);
}

+(BOOL)iPod {
    
    int deviceType = [DeviceType getDeviceType];
    
    return (deviceType == DEVICE_TYPE_IPOD) || (deviceType == DEVICE_TYPE_IPOD_2) || (deviceType == DEVICE_TYPE_IPOD_3) || (deviceType == DEVICE_TYPE_IPOD_4) || (deviceType == DEVICE_TYPE_IPOD_5);
    
}

+(BOOL)iPad {
    
    int deviceType = [DeviceType getDeviceType];
    
    return (deviceType == DEVICE_TYPE_IPAD) || (deviceType == DEVICE_TYPE_IPAD_2) || (deviceType == DEVICE_TYPE_IPAD_3) || (deviceType == DEVICE_TYPE_IPAD_4) || (deviceType == DEVICE_TYPE_IPAD_AIR) || (deviceType == DEVICE_TYPE_IPAD_MINI) || (deviceType == DEVICE_TYPE_IPAD_MINI_2);
    
}

+(BOOL)simulator {
    
    int deviceType = [DeviceType getDeviceType];
    
    return (deviceType == DEVICE_TYPE_SIMULATOR);
    
}



@end
