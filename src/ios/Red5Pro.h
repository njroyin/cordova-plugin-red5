#import <Cordova/CDVPlugin.h>
#import <R5Streaming/R5Streaming.h>

@interface Red5Pro : CDVPlugin<R5StreamDelegate>

@property R5Stream *stream;
@property R5Connection *connection;
@property R5VideoViewController *controller;

#pragma mark Javascript Interface
- (void)initPublisher:(CDVInvokedUrlCommand*)command;
- (void)publish:(CDVInvokedUrlCommand*)command;
- (void)unpublish:(CDVInvokedUrlCommand*)command;
- (void)subscribe:(CDVInvokedUrlCommand*)command;
- (void)unsubscribe:(CDVInvokedUrlCommand*)command;
- (void)pauseVideo:(CDVInvokedUrlCommand*)command;
- (void)unpauseVideo:(CDVInvokedUrlCommand*)command;
- (void)pauseAudio:(CDVInvokedUrlCommand*)command;
- (void)unpauseAudio:(CDVInvokedUrlCommand*)command;
- (void)resize:(CDVInvokedUrlCommand*)command;
- (void)updateScaleMode:(CDVInvokedUrlCommand*)command;

#pragma mark Other Methods
- (void)swapCamera:(CDVInvokedUrlCommand*)command;
- (void)registerEvents:(CDVInvokedUrlCommand*)command;
- (void)unregisterEvents:(CDVInvokedUrlCommand*)command;


   
@end
