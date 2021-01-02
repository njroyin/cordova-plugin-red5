cordova-plugin-red5pro
------------------------

This is a cordova plugin interface to the Red5 Pro Mobile SDK for Android and IOS.

# Install

Install like a typical Cordova Plugin using

```markdown
cordova plugin add git+ssh://git@gitlab.com/redsky_public/cordova-plugin-red5
*You can also use # at the end to target a specific tag such as version 1.2.3*
cordova plugin add git+ssh://git@gitlab.com/redsky_public/cordova-plugin-red5#1.2.3
```

# Uninstall

Remove the cordova plugin and the files it created

```markdown
cordova plugin remove cordova-plugin-red5 && rm -rf platforms/android/app/src/main/jniLibs/ && rm -rf platforms/ios/Piphany/Plugins/cordova-plugin-red5pro/red5pro.h  && rm -rf platforms/ios/Piphany/Plugins/cordova-plugin-red5pro/red5pro.m && rm -rf platforms/ios/Piphany/Plugins/cordova-plugin-red5pro/R5Streaming.framework/

```

# Usage

Once added you can access the Red 5 Pro SDK through the use of the **window.red5promobile** variable. There are two main 
objects that you can create off of the SDK, namely:

- Publisher
- Subscriber

Creation of these objects are done with the following code:

```javascript
var publisher = new window.red5promobile.Publisher;
var subscriber = new window.red5promobile.Subscriber;
```

Once created you can call functions to perform certain features such as: init(), publish(), unpublish, subscribe, unsubscribe, etc.

In designing this plugin we wanted the ability to preview video without publishing. Thus we have an `init()` function 
that sets up all that is needed to know to connect to the red5 server, initialize the camera, and microphone but it does 
not begin publishing until the `publish()` function is called. However `subscribe()` begins subscribing immediately and 
displaying video and audio as soon as a connection is made.

## Callbacks

Each function that publisher and subscriber implements follows the format of:

```javascript
[publisher|subscriber].name(...args, success, fail)
```

where the args argument may or may not exist, and we have success and fail callbacks. Since subscribing and publishing 
video to red5 pro server is an asynchronous process with the real possibility that a connection could be severed at any
time we have implement an events mechanism for monitoring the true status and state of the publisher and subscriber.

For example when you call `publisher.init()` with invalid arguments the fail function will be called **immediately** 
with an error string indicating what is wrong. However, if all options are correct the cordova plugin will **attempt** 
a connection and return success immediately before the attempt has determined if it was successful or not. Thus, it is 
important to register and listen to events.


The prototypes of these callback functions are given below:

```typescript
type SuccessCallback = (message?: string) => void;
type FailCallback = (errorMessage: string) => void;
```

# Subscriber or Publisher ONLY

At this time this plugin only supports being a subscriber or a publisher but **NOT** both.

# Common Methods (Publisher / Subscriber)

A number of methods are similar between publisher and subscriber. **However**, you still need to call the appropriate method on
either the publisher or subscriber object depending on whether your app is

## Check Permissions

In order for a publisher to initialize the correct permissions **MUST** be enabled by a user. These include Camera and Microphone
access. You can check for the appropriate permissions for your application by calling the `checkPermissions()` method.
If a permission has not been granted a popup will be displayed to the user of the app requesting permissions. If a user
presses DENY the popup will not show again until the applicaiton has been re-installed. 

```typescript
checkPermissions(success: SuccessCallback, fail: FailCallback)
```

## Resize

While the initial size of the video renderer is set in either the `init()` method for the publisher and the `subscribe()`
method for the subscriber, the application can dynamically resize the video window by calling the resize method given below.

```typescript
resize(renderX : number,  // New X position given in Web coordinates (dp)
       renderY : number,  // New Y position given in Web coordinates  (dp)
       width : string | number, // Width 
       height : string | number, 
       actualPixels : boolean
       success : SuccessCallback, 
       fail : FailCallback);
```

## Update Scale Mode

The video renderer has the ability to adjust the scale of the rendered video. It has 3 modes of operation described below:

```typescript
enum ScaleMode {
    SCALE_TO_FILL = 0, // Scales video to fill entire render area, cropping could occur
    SCALE_TO_FIT = 1, // Scales video to fit inside entire render area, letterboxing or pillarboxing could occur
    STRETCH = 2, // Video will stretch to fill entire area. Aspect ratio is NOT preserved 
}
```

**NOTE**: video is not centered, it is only placed at the top left of the video container.

```typescript
updateScaleMode(scaleMode : ScaleMode, success : SuccessCallback, fail : FailCallback)
```

## Get Stream Stats

A method is provided that will return the current stream statistics for subscriber or publisher.

```typescript
interface StatsData {
    buffered_time; // 
    subscribe_queue_size; // 
    nb_audio_frames; // 
    nb_video_frames; // 
    pkts_received; // 
    pkts_sent; // 
    pkts_video_dropped; // 
    pkts_audio_dropped; // 
    publish_pkts_dropped; // 
    total_bytes_received; // 
    total_bytes_sent; // 
    subscribe_bitrate; // 
    publish_bitrate; // 
    socket_queue_size; // 
    bitrate_sent_smoothed; // 
    bitrate_received_smoothed; // 
    subscribe_latency; // 
};

type StatsCallback = (statsData: StatsData) => void;
```

```typescript
getStreamStats(statsCallback: StatsCallback, fail: FailCallback)
```



## Events

Since callbacks return as soon as possible it is important to register and listen to events to know the true status.
To register for events call the appropriate registerEvents function on either the publisher or subscriber.
Likewise, you can call unregisterEvents. You can only have one callback associated with the registerEvents function.
It will be continuously called with any new events.

Each event is returned as a JSON object with the following structure:

```typescript
interface EventData {
    "type" : string;
    "data" : string;
    "original_type" : string // Not set for native, HTML SDK set to the original type for additional details
}
```

```typescript
type EventCallback = (event: EventData) => void;

registerEvents(eventCallback: EventCallback, fail: FailCallback) // < eventCallback will be called on all events until unregister is called
unregisterEvents(success: SuccessCallback, fail: FailCallback)
```

To account for differences between HTML and Native SDKs and for future events that may be added, we have added the "OTHER" event type as well as the **original_type** key. In this case the event JSON key of original_type will be set with the event name received and data key populated with whatever is returned.

Below is the list of events that get generated.

|  Event Type | Event Data  |
| ------------ | ------------ |
| CONNECTED  | N/A  |
| DISCONNECTED | N/A  |
| ERROR  | extra error info  |
| TIMEOUT  |   |
| CLOSE  |   |
| START_STREAMING  | N/A  |
| STOP_STREAMING  |  N/A  |
| NET_STATUS  |   |
| AUDIO_MUTE  |   |
| AUDIO_UNMUTE  |   |
| VIDEO_MUTE  |   |
| VIDEO_UNMUTE  |   |
| LICENSE_ERROR  |   |
| LICENSE_VALID  |   |
| BUFFER_FLUSH_START  | N/A  |
| BUFFER_FLUSH_END  |  N/A |
|  VIDEO_RENDER_START | N/A  |
|  OTHER | dependent  |


# Available Methods (Publisher)

## Init Publisher

When publishing you must first call `init(...)` that will initialize the connection to the red5 server along with 
initializing the camera and rendering out a preview of the camera that will be published. The init function has signature defined below 

```typescript
interface PublisherInitParams {
    renderX : number;               // Left coordinate of the video container that the camera will be rendered into
    renderY : number;               // Top coordinate of the video container
    renderWidth : number;           // Width of the video container
    renderHeight : number;          // Height of the video container
    host : string;                  // Host ip address of the origin server in which to publish video to
    port : number;                  // Port number of the origin server to stream video to (e.x. 8554)
    app : 'live';                   // Context in which the video is streaming. For RTSP video always use 'live'
    audioBandwidthKbps : number;    // Streaming audio bandwidth in Kilo bit per second
    videoBandwidthKbps : number;    // Streaming video bandwidth in Kilo bit per second
    frameRate : number;             // Video Framerate
    licenseKey : string;            // SDK license key
    debugView : boolean;            // Set to true to enable a text overlay that shows debug stats
    renderBelow : boolean;          // If true will render the video below the webview container
    cameraCaptureWidth : number;    // Width of the camera to capture. NOTE cell phone video is typically rotated
    cameraCaptureHeight : number;   // Height of the camera to capture.
    scaleMode: ScaleMode;           // Initial scale mode 0, 1, 2, see ScaleMode enum
    audioSampleRateHz: number;      // Sample rate to use for microphone in Hz (e.x. 44100)
    streamMode: 'live' | 'append' | 'record'; // Live = live video no record, Record = start new recording, Append = ?? append to previous recording?
}
```

```typescript
init(options: PublisherInitParams, success : SuccessCallback, fail : FailCallback);
```

## Publishing

After the publisher has been started with the `init(...)` method, it may now being publishing the stream with a call to `publish(...)`.

```typescript
publish(streamName : string, success : SuccessCallback, fail : FailCallback)
```

`streamName` is a unique stream name that is not currently being streamed from another client.

`record` is set to true if video should be saved on the origin server

`success` and `fail` are standard callbacks.

## Unpublish

When a publisher is currently streaming to the origin it can be stopped with a call to `unpublish()`

```typescript
unpublish(success : SuccessCallback, fail : FailCallback)
```


## Swap Camera

You can swap the camera between the front and back camera with a call to `swapCamera()`

```typescript
swapCamera(success: SuccessCallback, fail: FailCallback)
```

## Pause / Unpause Video

```typescript
pauseVideo(success: SuccessCallback, fail: FailCallback)
unpauseVideo(success: SuccessCallback, fail: FailCallback)
```

## Pause / Unpause Audio

```typescript
pauseAudio(success: SuccessCallback, fail: FailCallback)
unpauseAudio(success: SuccessCallback, fail: FailCallback)
```

## Send Video to Front or Back

If you want to dynamically change the ordering of the video so that it is infront or behind the webview you can use the following methods

```typescript
sendVideoToBack(success: SuccessCallback, fail: FailCallback)
bringVideoToFront(success: SuccessCallback, fail: FailCallback)
```

# Available Methods (Subscriber)

## Subscribe to stream

```typescript
interface SubscribeParams {
    renderX : number;               // Left coordinate of the video container that the camera will be rendered into
    renderY : number;               // Top coordinate of the video container
    renderWidth : number;           // Width of the video container
    renderHeight : number;          // Height of the video container
    host : string;                  // Host ip address of the origin server in which to publish video to
    port : number;                  // Port number of the origin server to stream video to (e.x. 8554)
    app : 'live';                   // Context in which the video is streaming. Use 'live' always
    licenseKey : string;            // SDK license key
    debugView : boolean;            // Set to true to enable a text overlay that shows debug stats
    renderBelow : boolean;          // If true will render the video below the webview container
    scaleMode: ScaleMode;           // Initial scale mode 0, 1, 2, see ScaleMode enum
    bufferTimeSecs: number;         // (1 default) Amount of time in seconds to buffer stream before it starts playing. Useful for unstable connections
    serverBufferTimeSecs: number;   // (2 default) Amount of data server will buffer up to when it has not received acknowledgements
}
```

```typescript
subscribe(options: SubscribeParams, streamName: string, success : SuccessCallback, fail : FailCallback);
```

## Unsubscribe from stream

Once the subscriber is receiving data it can be unsubscribed with the following function:

```typescript
unsubscribe(success : SuccessCallback, fail : FailCallback)
```


# Other Items to Consider

## Rendering Video Above/Below The Webview

With this cordova plugin you can choose whether you want the video view to render above or below the webview. This works on both Android and IOS and is controlled by one of the options variables when calling `init()` on publisher or `subscribe` on the subscriber objects respectively. For example:

```javascript
var options = {
    ...
    renderBelow: true, // default or if missing is false
}
```

In order to actually view the rendered video below you will need to set the background-color for the `<body>` element to transparent along with any other items that might be above the video window.

## Gotchas

When removing the plugin from a cordova project it leaves behind libraries in the jniLibs folder. You either need to delete this folder if re-installing this plugin or you need to remove your platform and re-add it.
