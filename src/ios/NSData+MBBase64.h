//
//  NSData+MBBase64.h
//  AudioRecorder Plugin
//
//  Created by acr on 13/05/2013.
//
//

#import <Foundation/Foundation.h>

@interface NSData (MBBase64)

+ (id)dataWithBase64EncodedString:(NSString *)string;
- (NSString *)base64Encoding;

@end
