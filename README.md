cordova-plugin-red5pro
------------------------

This is a cordova plugin interface to the Red5 Pro Mobile SDK for Android and IOS.



# Installation


Install like a typical Cordova Plugin using

```markdown
cordova plugin add git+ssh://git@gitlab.com/plvr/cordova-plugin-red5.git#1.2.0
```

# Usage

Once added you can access the Red 5 Pro SDK through the use of the **window.red5promobile** variable. There are two main objects that you can create off the of SDK, namely:

- Publisher
- Subscriber

Creation of these objects are done with the following code:

```javascript
var publisher = new window.red5promobile.Publisher;
var subscriber = new window.red5promobile.Subscriber;
```

Once create you can call functions to perform certain features such as: init, Publish, Unpublish, Subscribe, Unsubscribe, etc. 

In designing this plugin we wanted the ability to preview video without publishing. Thus we have an `init()` function that sets up all that is needed to know to connect to the red5 server, initialize the camera, and microphone but it does not begin publishing until the `publish()` function is called. However `subscribe()` begins subscribing immediately and displaying video and audio as soon as a connection is made.

## Callbacks

Each function that publisher and subscriber implements follows the format of:

```javascript
[publisher|subscriber].name(options, success, fail)
```

where the options argument may or may not exist and we have success and fail callbacks. Since subscribing and publishing video to red5 pro server is an asynchronous process with the real possibility that a connection could be severed at any time we have implement an events mechanism for see the true status and state of the publisher and subscriber.

For example when you call `publisher.init()` with invalid arguments the fail function will be called **immediately** with an error string indicating what is wrong. However if all options are correct the cordova plugin will **attempt** a connection and return success immediately before the attempt has determined if it was successful or not. Thus it important to register and listen to events.

## Events

Since callbacks return as soon as possible it is important to register and listen to events to know the true status. To register for events call the appropriate registerEvents function on either the publisher or subscriber. Likewise you can call unregisterEvents. You can only have one callback associated with the registerEvents function. It will be continously called with any new events.

Each event is returned as a JSON object with the following structure:

```
{
"type" : "EVENT_TYPE",
"data" : "EVENT_DATA",
"original_type" : "HTML_TYPE" // Not set for native, HTML SDK set to the original type for additional details
}
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


## Rendering Video Above/Below

With this cordova plugin you can choose whether you want the video view to render above or below the webview. This works on both Android and IOS and is controlled by one of the options variables when calling `init()` on publisher or `subscribe` on the subscriber objects respectively. For example:

```javascript
var options = {
    ...
    renderBelow: true, // default or if missing is false
}
```

In order to actually view the rendered video below you will need to set the background-color for the `<body>` element to transparent along with any other items that might be above the video window.

# Gotchas
When removing the plugin from a cordova project it leaves behind libraries in the jniLibs folder. You either need to delete this folder if re-installing this plugin or you need to remove your platform and re-add it.
