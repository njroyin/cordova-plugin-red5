#include <sys/types.h>
#include <sys/sysctl.h>
#include "TargetConditionals.h"
#include <AVFoundation/AVFoundation.h>

#import <Cordova/CDV.h>
#import "Red5pro.h"


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
    _bitrate = 3000;
    _framerate = 15;
    _audioBitrate = 32;
    _cameraWidth = 1920;
    _cameraHeight = 1080;
    _audioSampleRate = 44100;
    _useAdaptiveBitrateController = NO;
    _audioMode = R5AudioControllerModeStandardIO;
    _useBackfacingCamera = NO;
    r5_set_log_level(_logLevel);

    _playBehindWebview = false;

    if ([self verifyMediaAuthorization:AVMediaTypeVideo] == FALSE) {
        NSLog(@"Error getting video permissions");
    }

    if ([self verifyMediaAuthorization:AVMediaTypeAudio] == FALSE) {
        NSLog(@"Error getting audio mic permissions.");
    }
}

#pragma mark Helper Functions
- (AVCaptureDevice *)getCameraDevice:(BOOL)backfacing
{
    NSArray *list = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    AVCaptureDevice *frontCamera;
    AVCaptureDevice *backCamera;
    for (AVCaptureDevice *device in list) {
        // Auto focus
        if ([device isFocusModeSupported:AVCaptureFocusModeContinuousAutoFocus]) {
            CGPoint autofocusPoint = CGPointMake(0.5f, 0.5f);
            [device setFocusPointOfInterest:autofocusPoint];
            [device setFocusMode:AVCaptureFocusModeContinuousAutoFocus];
        }
        
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

- (R5Microphone *)setUpMicrophone
{
    AVCaptureDevice *audio = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeAudio];
    R5Microphone *microphone = [[R5Microphone alloc] initWithDevice:audio];
    microphone.bitrate = _audioBitrate;
    microphone.sampleRate = _audioSampleRate;
    return microphone;
}

- (R5Camera *)setUpCamera
{
    AVCaptureDevice *video = [self getCameraDevice:_useBackfacingCamera];
    R5Camera *camera = [[R5Camera alloc] initWithDevice:video andBitRate:_bitrate];
    [camera setWidth:_cameraWidth];
    [camera setHeight:_cameraHeight];
    [camera setOrientation:90];
    [camera setFps:_framerate];
    return camera;
}

- (void)showAlert:(NSString *)title withMessage:(NSString *)message
{
    UIAlertController * alert = [UIAlertController
                                 alertControllerWithTitle:title
                                 message:message
                                 preferredStyle:UIAlertControllerStyleAlert];



    UIAlertAction* okButton = [UIAlertAction
                               actionWithTitle:@"Ok"
                               style:UIAlertActionStyleDefault
                               handler:^(UIAlertAction * action) {
                                   //Handle your yes please button action here
                               }];

    [alert addAction:okButton];

    id rootViewController = [UIApplication sharedApplication].delegate.window.rootViewController;
    if([rootViewController isKindOfClass:[UINavigationController class]])
        rootViewController = ((UINavigationController *)rootViewController).viewControllers.firstObject;

    if([rootViewController isKindOfClass:[UITabBarController class]])
        rootViewController = ((UITabBarController *)rootViewController).selectedViewController;

    [rootViewController presentViewController:alert animated:YES completion:nil];
}

- (void)deviceDeniedError
{
    NSLog(@"Denied access to device");

    NSString *alertText;

    alertText = @"It looks like your privacy settings are preventing us from accessing your camera or microphone for video presenting. You can fix this by doing the following:\n\n1. Close this app.\n\n2. Open the Settings app.\n\n3. Scroll to the privacy section and select Camera or Microphone.\n\n4. Turn the Camera and Microphone on for this app.\n\n5. Open this app and try again.";

    [self showAlert:@"Permission Error" withMessage:alertText];

//    UIAlertView *alert = [[UIAlertView alloc]
//                          initWithTitle:@"Error"
//                          message:alertText
//                          delegate:self
//                          cancelButtonTitle:alertButton
//                          otherButtonTitles:nil];
//    alert.tag = 3491832;
//    [alert show];



}

- (BOOL)verifyMediaAuthorization:(NSString *)mediaType
{
    AVAuthorizationStatus authStatus = [AVCaptureDevice authorizationStatusForMediaType:mediaType];
    if(authStatus == AVAuthorizationStatusAuthorized) {
        return TRUE;
    } else if(authStatus == AVAuthorizationStatusDenied){
        [self deviceDeniedError];
        return FALSE;
    } else if(authStatus == AVAuthorizationStatusRestricted){
        // It means their ADMIN locked them out. Not likely to happen
        [self deviceDeniedError];
        return FALSE;
    } else if(authStatus == AVAuthorizationStatusNotDetermined){
        // not determined?!
        [AVCaptureDevice requestAccessForMediaType:mediaType completionHandler:^(BOOL granted) {
            if(granted){
                NSLog(@"Granted access to %@", mediaType);
            } else {
                NSLog(@"Not granted access to %@", mediaType);
                [self deviceDeniedError];
            }
        }];
    } else {
        // impossible, unknown authorization status
        return FALSE;
    }

    return FALSE;
}

- (void)onDeviceOrientation:(NSNotification *)notification {

    dispatch_async(dispatch_get_main_queue(), ^{
        if (self->_isPublisher) {
            R5Camera *camera = (R5Camera *)[self.stream getVideoSource];
            UIDeviceOrientation orientation = [UIDevice currentDevice].orientation;

            if (orientation == UIDeviceOrientationPortraitUpsideDown) {
                [camera setOrientation: 270];
            }
            else if (orientation == UIDeviceOrientationLandscapeLeft) {
                if (self->_useBackfacingCamera) {
                    [camera setOrientation: 0];
                }
                else {
                    [camera setOrientation: 180];
                }
            }
            else if (orientation == UIDeviceOrientationLandscapeRight) {
                if (self->_useBackfacingCamera) {
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
    R5Connection *connection = [[R5Connection alloc] initWithConfig:configuration];
    R5Stream *stream = [[R5Stream alloc] initWithConnection:connection];
    [stream setDelegate:self];
    [stream setClient:self];

    self.stream = stream;
    self.connection = connection;
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

    // Force bit rate higher
    _bitrate = 3000;

    NSString *licenseKey = [command argumentAtIndex:10];
    _showDebugInfo = ((NSNumber*)[command.arguments objectAtIndex:11]).boolValue;
    bool playBehindWebview = ((NSNumber*)[command.arguments objectAtIndex:12]).boolValue;

    if ([self verifyMediaAuthorization:AVMediaTypeVideo] == FALSE || [self verifyMediaAuthorization:AVMediaTypeAudio] == FALSE) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Missing Authorization."];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }

    R5Configuration *configuration = [[R5Configuration alloc] init];
    configuration.protocol = 1;
    configuration.host = host;
    configuration.port = port;
    configuration.contextName = appName;
    configuration.streamName = @"stream_no_name";
    configuration.bundleID = @"";
    configuration.licenseKey = licenseKey;
    configuration.parameters = @"";

    dispatch_async(dispatch_get_main_queue(), ^{
        self->_isPublisher = YES;

        [self initiateConnection:configuration];

        if (self->_useAdaptiveBitrateController) {
            R5AdaptiveBitrateController *abrController = [[R5AdaptiveBitrateController alloc] init];
            [abrController attachToStream:self.stream];
            [abrController setRequiresVideo:self->_useVideo];
        }

        if (self->_useAudio) {
            R5Microphone *microphone = [self setUpMicrophone];
            [self.stream attachAudio:microphone];
        }

        if (self->_useVideo) {
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

            [self.stream attachVideo:camera];
            [self.controller attachStream:self.stream];

            [self.controller showPreview:YES];
            [self.controller showDebugInfo:self->_showDebugInfo];
            [self.controller setScaleMode:self->_scaleMode];

        }
    });

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)publish:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called publish");

    NSString *streamName = [command argumentAtIndex:0];
    bool isRecording = ((NSNumber*)[command.arguments objectAtIndex:1]).boolValue;

    _streamName = streamName;

    if (isRecording)
        [self.stream publish:streamName type:R5RecordTypeRecord];
    else
        [self.stream publish:streamName type:R5RecordTypeLive];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)unpublish:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called unpublish");

    dispatch_async(dispatch_get_main_queue(), ^{
        if (self->_isStreaming) {
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

    float bufferTime = ((NSNumber*)[command.arguments objectAtIndex:14]).floatValue;
    float serverBufferTime = ((NSNumber*)[command.arguments objectAtIndex:15]).floatValue;

    R5Configuration *configuration = [[R5Configuration alloc] init];
    configuration.protocol = 1;
    configuration.host = host;
    configuration.port = port;
    configuration.contextName = appName;
    configuration.streamName = streamName;
    configuration.bundleID = @"";
    configuration.licenseKey = licenseKey;
    configuration.buffer_time = bufferTime > 1.0f ? bufferTime : 1.0f;
    configuration.stream_buffer_time = serverBufferTime > 2.0f ? serverBufferTime : 2.0f;
    configuration.parameters = @"";

    dispatch_async(dispatch_get_main_queue(), ^{
        self->_isPublisher = NO;
        self->_streamName = streamName;

        [self initiateConnection:configuration];

        if (self->_playbackVideo) {
            self.controller = [[R5VideoViewController alloc] init];
            UIView *view = [[UIView alloc] initWithFrame:CGRectMake(xPos, yPos + AACStatusBarHeight(), width, height)];
            [view setBackgroundColor:UIColor.blackColor];
            view.clipsToBounds = YES;
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

            [self.controller showDebugInfo:self->_showDebugInfo];
            [self.controller setScaleMode:self->_scaleMode];
        }

        [self.stream setAudioController:[[R5AudioController alloc] initWithMode:self->_audioMode]];

        [self.stream play:streamName];
    });

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)unsubscribe:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called unsubscribe");

    dispatch_async(dispatch_get_main_queue(), ^{
        if (self->_isStreaming) {
            [self.stream stop];
        } else {
            [self tearDown];
        }
    });

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)pauseVideo:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called pause video");

    dispatch_async(dispatch_get_main_queue(), ^{
        if (self->_isStreaming)
            self.stream.pauseVideo = true;
    });

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)unpauseVideo:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called unpause video");

    dispatch_async(dispatch_get_main_queue(), ^{
        if (self->_isStreaming)
            self.stream.pauseVideo = false;
    });

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)pauseAudio:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called pause audio");

    dispatch_async(dispatch_get_main_queue(), ^{
        if (self->_isStreaming)
            self.stream.pauseAudio = true;
    });

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)unpauseAudio:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called unpause audio");

    dispatch_async(dispatch_get_main_queue(), ^{
        if (self->_isStreaming)
            self.stream.pauseAudio = false;
    });

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)resize:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called resize");

    int xPos = ((NSNumber*)[command.arguments objectAtIndex:0]).intValue;
    int yPos = ((NSNumber*)[command.arguments objectAtIndex:1]).intValue;
    int width = ((NSNumber*)[command.arguments objectAtIndex:2]).intValue;
    int height = ((NSNumber*)[command.arguments objectAtIndex:3]).intValue;
    CGRect frame = CGRectMake(xPos, yPos, width, height);

    dispatch_async(dispatch_get_main_queue(), ^{
        [UIView animateWithDuration: 0.3
        animations:^{
            self.controller.view.frame = frame;
        }
        completion: ^(BOOL finished) {
            [self.controller showPreview:YES];
            NSLog(@"Video Resized");
        }];
    });

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)sendVideoToBack:(CDVInvokedUrlCommand*)command
{
    NSLog(@"sendVideoToBack");
    dispatch_async(dispatch_get_main_queue(), ^{
        [self.controller.view.superview sendSubviewToBack: self.controller.view];
    });

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)bringVideoToFront:(CDVInvokedUrlCommand*)command
{
    NSLog(@"bringVideoToFront");
    dispatch_async(dispatch_get_main_queue(), ^{
        [self.controller.view.superview bringSubviewToFront: self.controller.view];
    });

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"All Good."];
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
        if (self->_isPublisher) {
            self->_useBackfacingCamera = !self->_useBackfacingCamera;
            AVCaptureDevice *device = [self getCameraDevice:self->_useBackfacingCamera];
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

- (void)getStreamStats:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Called getStreamStats");

    if (self.stream == nil) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No streaming connection."];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }

    r5_stats *stats = [self.stream getDebugStats];
    if (stats == nil) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Error getting stats."];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }


    NSString *statsJson = [NSString stringWithFormat:@"{ \"buffered_time\" : \"%f\", \"subscribe_queue_size\" : \"%d\", \"nb_audio_frames\" : \"%d\", \"nb_video_frames\" : \"%d\", \
    \"pkts_received\" : \"%ld\", \"pkts_sent\" : \"%ld\", \"pkts_video_dropped\" : \"%ld\", \"pkts_audio_dropped\" : \"%ld\", \"publish_pkts_dropped\" : \"%ld\", \
    \"total_bytes_received\" : \"%ld\", \"total_bytes_sent\" : \"%ld\", \"subscribe_bitrate\" : \"%f\", \"publish_bitrate\" : \"%f\", \"socket_queue_size\" : \"%ld\",  \
     \"bitrate_sent_smoothed\" : \"%f\", \"bitrate_received_smoothed\" : \"%lf\", \"subscribe_latency\" : \"%lf\"}",
    stats->buffered_time,
    stats->subscribe_queue_size,
    stats->nb_audio_frames,
    stats->nb_video_frames,
    stats->pkts_received,
    stats->pkts_sent,
    stats->pkts_video_dropped,
    stats->pkts_audio_dropped,
    stats->publish_pkts_dropped,
    stats->total_bytes_received,
    stats->total_bytes_sent,
    stats->subscribe_bitrate,
    stats->publish_bitrate,
    stats->socket_queue_size,
    stats->bitrate_sent_smoothed,
    stats->bitrate_received_smoothed,
    stats->subscribe_latency];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:statsJson];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


- (void)tearDown
{
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.stream != nil) {
            [self.stream setDelegate:nil];
            [self.stream setClient:nil];

            self.stream = nil;

            // Give the stream time to set the delegate and client to nil in their thread, stops from crashing
            [NSThread sleepForTimeInterval: 0.25f];

            [self.controller willMoveToParentViewController:nil];
            [self.controller.view removeFromSuperview];
            [self.controller removeFromParentViewController];
            self.controller = nil;
            self.connection = nil;
        }

        self->_streamName = nil;
        self->_isStreaming = NO;
    });
}

-(void)onR5StreamStatus:(R5Stream *)stream withStatus:(int) statusCode withMessage:(NSString*)msg
{
    NSLog(@"Received event %@ with status message %@", @(r5_string_for_status(statusCode)), msg);

    // Now we need to convert Status Code to an all upper case string with _ for spaces. This matches how the
    // android event status code text is and we want them to be uniform.
    NSString *eventName = GetStatusStringFromCode(statusCode);
    [self sendEventMessage:[NSString stringWithFormat:@"{ \"type\" : \"%@\", \"data\" : \"%@\"}",eventName, msg]];

    if (statusCode == r5_status_start_streaming) {
        _isStreaming = YES;
    }

    dispatch_async(dispatch_get_main_queue(), ^{

        if (statusCode == r5_status_disconnected && self->_isStreaming) {
            [self tearDown];
            self->_isStreaming = NO;
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
