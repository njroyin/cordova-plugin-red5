#include <sys/types.h>
#include <sys/sysctl.h>
#include "TargetConditionals.h"

#import <Cordova/CDV.h>
#import "red5pro.h"


@interface Red5Pro () {
    
    int _scaleMode;
    int _logLevel;
    int _audioMode;
    BOOL _showDebugInfo;
    NSString *_streamName;  // required.
    
    BOOL _isStreaming;
    BOOL _isPublisher;      // determined.
    BOOL _useVideo;
    BOOL _useAudio;
    BOOL _playbackVideo;
    int _cameraWidth;
    int _cameraHeight;
    int _bitrate;
    int _framerate;
    int _audioBitrate;
    int _audioSampleRate;
    BOOL _useAdaptiveBitrateController;
    BOOL _useBackfacingCamera;
    
    int _currentRotation;
    bool _playBehindWebview;
    
    NSString *eventCallbackId;
}
@end

@implementation Red5Pro


- (void)pluginInitialize
{
    _scaleMode = 0;
    _logLevel = 3;
    _showDebugInfo = NO;
    _useVideo = YES;
    _useAudio = YES;
    _playbackVideo = YES;
    _bitrate = 750;
    _framerate = 15;
    _audioBitrate = 32;
    _cameraWidth = 640;
    _cameraHeight = 360;
    _audioSampleRate = 16000;
    _useAdaptiveBitrateController = NO;
    _audioMode = R5AudioControllerModeStandardIO;
    _useBackfacingCamera = NO;
    r5_set_log_level(_logLevel);
    
    _playBehindWebview = false;
}

#pragma mark Helper Functions
- (AVCaptureDevice *)getCameraDevice:(BOOL)backfacing {
    
    NSArray *list = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    AVCaptureDevice *frontCamera;
    AVCaptureDevice *backCamera;
    for (AVCaptureDevice *device in list) {
        if (device.position == AVCaptureDevicePositionFront) {
            frontCamera = device;
        }
        else if (device.position == AVCaptureDevicePositionBack) {
            backCamera = device;
        }
    }
    
    if (backfacing && backCamera != NULL) {
        return backCamera;
    }
    return frontCamera;
    
}

- (R5Microphone *)setUpMicrophone {
    AVCaptureDevice *audio = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeAudio];
    R5Microphone *microphone = [[R5Microphone alloc] initWithDevice:audio];
    microphone.bitrate = _audioBitrate;
    microphone.sampleRate = _audioSampleRate;
    return microphone;
}

- (R5Camera *)setUpCamera {
    AVCaptureDevice *video = [self getCameraDevice:_useBackfacingCamera];
    R5Camera *camera = [[R5Camera alloc] initWithDevice:video andBitRate:_bitrate];
    [camera setWidth:_cameraWidth];
    [camera setHeight:_cameraHeight];
    [camera setOrientation:90];
    [camera setFps:_framerate];
    return camera;
}

- (void)onDeviceOrientation:(NSNotification *)notification {
    
    dispatch_async(dispatch_get_main_queue(), ^{
        if (_isPublisher) {
            R5Camera *camera = (R5Camera *)[self.stream getVideoSource];
            UIDeviceOrientation orientation = [UIDevice currentDevice].orientation;
            
            if (orientation == UIDeviceOrientationPortraitUpsideDown) {
                [camera setOrientation: 270];
            }
            else if (orientation == UIDeviceOrientationLandscapeLeft) {
                if (_useBackfacingCamera) {
                    [camera setOrientation: 0];
                }
                else {
                    [camera setOrientation: 180];
                }
            }
            else if (orientation == UIDeviceOrientationLandscapeRight) {
                if (_useBackfacingCamera) {
                    [camera setOrientation: 180];
                }
                else {
                    [camera setOrientation: 0];
                }
            }
            else {
                [camera setOrientation: 90];
            }
            [self.controller showPreview:YES];
            [self.stream updateStreamMeta];
            
        }
    });
    
}


- (void)initiateConnection:(R5Configuration *)configuration
{
    //  dispatch_async(dispatch_get_main_queue(), ^{
    R5Connection *connection = [[R5Connection alloc] initWithConfig:configuration];
    R5Stream *stream = [[R5Stream alloc] initWithConnection:connection];
    [stream setDelegate:self];
    [stream setClient:self];
    
    self.stream = stream;
    self.connection = connection;
    
    //        if (self.onConfigured) {
    //            self.onConfigured(@{@"key": key});
    //        }
    //    });
}

#pragma mark Publisher

- (void)initPublisher:(CDVInvokedUrlCommand*)command
{
    int xPos = ((NSNumber*)[command.arguments objectAtIndex:0]).intValue;
    int yPos = ((NSNumber*)[command.arguments objectAtIndex:1]).intValue;
    int width = ((NSNumber*)[command.arguments objectAtIndex:2]).intValue;
    int height = ((NSNumber*)[command.arguments objectAtIndex:3]).intValue;
    NSString *host = [command argumentAtIndex:4];
    int port = ((NSNumber*)[command.arguments objectAtIndex:5]).intValue;
    NSString *appName = [command argumentAtIndex:6];
    _audioBitrate = ((NSNumber*)[command.arguments objectAtIndex:7]).intValue;
    _bitrate = ((NSNumber*)[command.arguments objectAtIndex:8]).intValue;
    _framerate = ((NSNumber*)[command.arguments objectAtIndex:9]).intValue;
    
    NSString *licenseKey = [command argumentAtIndex:10];
    _showDebugInfo = ((NSNumber*)[command.arguments objectAtIndex:11]).boolValue;
    bool playBehindWebview = ((NSNumber*)[command.arguments objectAtIndex:12]).boolValue;
    
    R5Configuration *configuration = [[R5Configuration alloc] init];
    configuration.protocol = 1;
    configuration.host = host;
    configuration.port = port;
    configuration.contextName = appName;
    configuration.streamName = @"josh";
    configuration.bundleID = @"";
    configuration.licenseKey = licenseKey;
    configuration.buffer_time = 1.0f;
    configuration.stream_buffer_time = 1.0f;
    configuration.parameters = @"";
    
    dispatch_async(dispatch_get_main_queue(), ^{
        _isPublisher = YES;
        
        [self initiateConnection:configuration];
        
        if (_useAdaptiveBitrateController) {
            R5AdaptiveBitrateController *abrController = [[R5AdaptiveBitrateController alloc] init];
            [abrController attachToStream:self.stream];
            [abrController setRequiresVideo:_useVideo];
        }
        
        if (_useAudio) {
            R5Microphone *microphone = [self setUpMicrophone];
            [self.stream attachAudio:microphone];
        }
        
        if (_useVideo) {
            R5Camera *camera = [self setUpCamera];
            
            self.controller = [[R5VideoViewController alloc] init];
            UIView *view = [[UIView alloc] initWithFrame:CGRectMake(xPos, yPos + AACStatusBarHeight(), width, height)];
            [view setBackgroundColor:UIColor.blackColor];
            [self.controller setView:view];
            
            if (playBehindWebview) {
                [self.webView.superview insertSubview:view belowSubview:self.webView];
                [self.webView setOpaque:NO];
                [self.webView setBackgroundColor:UIColor.clearColor];
            }
            else {
                [self.webView addSubview:view];
            }
            
            [self.controller showPreview:YES];
            [self.controller showDebugInfo:_showDebugInfo];
            [self.controller setScaleMode:_scaleMode];
            
            [self.controller attachStream:self.stream];
            [self.stream attachVideo:camera];
        }
    });
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)publish:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called publish");
    
    NSString *streamName = [command argumentAtIndex:0];
    
    _streamName = streamName;
    
    [self.stream publish:streamName type:R5RecordTypeLive];
    //[self onDeviceOrientation:NULL];
    //[self.stream updateStreamMeta];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)unpublish:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called unpublish");
    
    dispatch_async(dispatch_get_main_queue(), ^{
        if (_isStreaming) {
            [self.stream stop];
        }
        else {
            //self.onUnpublishNotification(@{});
            [self tearDown];
        }
    });
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)subscribe:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called subscribe");
    
    int xPos = ((NSNumber*)[command.arguments objectAtIndex:0]).intValue;
    int yPos = ((NSNumber*)[command.arguments objectAtIndex:1]).intValue;
    int width = ((NSNumber*)[command.arguments objectAtIndex:2]).intValue;
    int height = ((NSNumber*)[command.arguments objectAtIndex:3]).intValue;
    NSString *host = [command argumentAtIndex:4];
    int port = ((NSNumber*)[command.arguments objectAtIndex:5]).intValue;
    NSString *appName = [command argumentAtIndex:6];
    _audioBitrate = ((NSNumber*)[command.arguments objectAtIndex:7]).intValue;
    _bitrate = ((NSNumber*)[command.arguments objectAtIndex:8]).intValue;
    _framerate = ((NSNumber*)[command.arguments objectAtIndex:9]).intValue;
    
    NSString *licenseKey = [command argumentAtIndex:10];
    _showDebugInfo = ((NSNumber*)[command.arguments objectAtIndex:11]).boolValue;
    NSString *streamName = [command argumentAtIndex:12];
    bool playBehindWebview = ((NSNumber*)[command.arguments objectAtIndex:13]).boolValue;
    
    R5Configuration *configuration = [[R5Configuration alloc] init];
    configuration.protocol = 1;
    configuration.host = host;
    configuration.port = port;
    configuration.contextName = appName;
    configuration.streamName = streamName;
    configuration.bundleID = @"";
    configuration.licenseKey = licenseKey;
    configuration.buffer_time = 1.0f;
    configuration.stream_buffer_time = 1.0f;
    configuration.parameters = @"";
    
    dispatch_async(dispatch_get_main_queue(), ^{
        _isPublisher = NO;
        _streamName = streamName;
        
        [self initiateConnection:configuration];
        
        if (_playbackVideo) {
            self.controller = [[R5VideoViewController alloc] init];
            UIView *view = [[UIView alloc] initWithFrame:CGRectMake(xPos, yPos + AACStatusBarHeight(), width, height)];
            [view setBackgroundColor:UIColor.blackColor];
            [self.controller setView:view];
                   
            if (playBehindWebview) {
                [self.webView.superview insertSubview:view belowSubview:self.webView];
                [self.webView setOpaque:NO];
                [self.webView setBackgroundColor:UIColor.clearColor];
            }
            else {
                [self.webView addSubview:view];
            }

            [self.controller showPreview:YES];
            [self.controller attachStream:self.stream];

            [self.controller showDebugInfo:_showDebugInfo];
            [self.controller setScaleMode:_scaleMode];
        }
        
        [self.stream setAudioController:[[R5AudioController alloc] initWithMode:_audioMode]];

        [self.stream play:streamName];
    });
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)unsubscribe:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called unsubscribe");
    
    dispatch_async(dispatch_get_main_queue(), ^{
        if (_isStreaming) {
            [self.stream stop];
        } else {
            [self tearDown];
        }
    });
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)resize:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called resize");
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Not implemented yet."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)updateScaleMode:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called updateScaleMode");
    
    _scaleMode = ((NSNumber*)[command.arguments objectAtIndex:0]).intValue;;
    if (_playbackVideo) {
        [self.controller setScaleMode:_scaleMode];
    }
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)swapCamera:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called swapCamera");
    
    dispatch_async(dispatch_get_main_queue(), ^{
        if (_isPublisher) {
            _useBackfacingCamera = !_useBackfacingCamera;
            AVCaptureDevice *device = [self getCameraDevice:_useBackfacingCamera];
            R5Camera *camera = (R5Camera *)[self.stream getVideoSource];
            [camera setDevice:device];
        }
    });
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)registerEvents:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called registerEvents");
    
    eventCallbackId = command.callbackId;
}

- (void)unregisterEvents:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called unregisterEvents");
    
    eventCallbackId = nil;
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)tearDown
{
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.stream != nil) {
            [self.stream setDelegate:nil];
            [self.stream setClient:nil];
            
            self.stream = nil;
            
            [self.controller willMoveToParentViewController:nil];
            [self.controller.view removeFromSuperview];
            [self.controller removeFromParentViewController];
            self.controller = nil;
            self.connection = nil;
        }
        
        _streamName = nil;
        _isStreaming = NO;
    });
}

-(void)onR5StreamStatus:(R5Stream *)stream withStatus:(int) statusCode withMessage:(NSString*)msg
{
    NSLog(@"Received event %@ with status message %@", @(r5_string_for_status(statusCode)), msg);
    
    // Now we need to convert Status Code to an all upper case string with _ for spaces. This matches how the
    // android event status code text is and we want them to be uniform.
    NSString *eventName = GetStatusStringFromCode(statusCode);
    [self sendEventMessage:[NSString stringWithFormat:@"{ \"type\" : \"%@\", \"data\" : \"%@\"}",eventName, msg]];
    
    NSString *tmpStreamName = _streamName;
    
    if (statusCode == r5_status_start_streaming) {
        _isStreaming = YES;
    }
    
    dispatch_async(dispatch_get_main_queue(), ^{
       
        if (statusCode == r5_status_disconnected && _isStreaming) {
            [self tearDown];
            _isStreaming = NO;
        }
    });
}

-(void)sendEventMessage:(NSString*)message
{
    if (eventCallbackId != nil) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
        pluginResult.keepCallback = [NSNumber numberWithBool:true];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:eventCallbackId];
    }
}

NSString * GetStatusStringFromCode(int code)
{
    NSArray *statusStringArray = @[ @"CONNECTED", @"DISCONNECTED", @"ERROR", @"TIMEOUT", @"CLOSE", @"START_STREAMING", @"STOP_STREAMING",
                                    @"NET_STATUS", @"AUDIO_MUTE", @"AUDIO_UNMUTE", @"VIDEO_MUTE", @"VIDEO_UNMUTE", @"LICENSE_ERROR",
                                    @"LICENSE_VALID", @"BUFFER_FLUSH_START", @"BUFFER_FLUSH_EMPTY", @"VIDEO_RENDER_START" ];
    
    if (code < 0 || code >= [statusStringArray count] )
        return @"UNKNOWN";
    
    return statusStringArray[code];
}

CGFloat AACStatusBarHeight()
{
    CGSize statusBarSize = [[UIApplication sharedApplication] statusBarFrame].size;
    return MIN(statusBarSize.width, statusBarSize.height);
}


@end
