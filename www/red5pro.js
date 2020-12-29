let exec = require('cordova/exec');

const PLUGIN_NAME = 'Red5Pro';

let red5promobile = new function () {

    this.Publisher = function () {
        // Common functions
        this.resize = Resize;
        this.updateScaleMode = UpdateScaleMode;
        this.getStreamStats = GetStreamStats;
        this.checkPermissions = CheckPermissions;
        this.registerEvents = RegisterEvents;
        this.unregisterEvents = UnregisterEvents;

        this.init = function (options, success, fail) {
            let initArray = [
                options.renderX,
                options.renderY,
                options.renderWidth,
                options.renderHeight,
                options.host,
                options.port,
                options.app,
                options.audioBandwidthKbps,
                options.videoBandwidthKbps,
                options.frameRate,
                options.licenseKey,
                options.debugView,
                options.renderBelow,
                options.cameraCaptureWidth,
                options.cameraCaptureHeight,
                options.scaleMode,
                options.audioSampleRateHz,
                options.streamMode
            ];
            exec(success, fail, PLUGIN_NAME, 'initPublisher', initArray);
        };

        this.publish = function (streamName, success, fail) {
            exec(success, fail, PLUGIN_NAME, 'publish', [streamName]);
        };

        this.unpublish = function (success, fail) {
            exec(success, fail, PLUGIN_NAME, 'unpublish', []);
        };

        this.swapCamera = function (success, fail) {
            exec(success, fail, PLUGIN_NAME, 'swapCamera', []);
        };

        this.pauseVideo = function (success, fail) {
            exec(success, fail, PLUGIN_NAME, 'pauseVideo', []);
        };

        this.pauseAudio = function (success, fail) {
            exec(success, fail, PLUGIN_NAME, 'pauseAudio', []);
        };

        this.unpauseVideo = function (success, fail) {
            exec(success, fail, PLUGIN_NAME, 'unpauseVideo', []);
        };

        this.unpauseAudio = function (success, fail) {
            exec(success, fail, PLUGIN_NAME, 'unpauseAudio', []);
        };

        this.sendVideoToBack = function(success, fail) {
            exec(success, fail, PLUGIN_NAME, 'sendVideoToBack', []);
        };

        this.bringVideoToFront = function(success, fail) {
            exec(success, fail, PLUGIN_NAME, 'bringVideoToFront', []);
        };
    };

    this.Subscriber = function () {
        // Common functions
        this.resize = Resize;
        this.updateScaleMode = UpdateScaleMode;
        this.getStreamStats = GetStreamStats;
        this.checkPermissions = CheckPermissions;
        this.registerEvents = RegisterEvents;
        this.unregisterEvents = UnregisterEvents;

        this.subscribe = function (options, streamName, success, fail) {
            let initArray = [
                streamName,
                options.renderX,
                options.renderY,
                options.renderWidth,
                options.renderHeight,
                options.host,
                options.port,
                options.app,
                options.licenseKey,
                options.debugView || false,
                options.renderBelow || false,
                options.bufferTime || 0,
                options.serverBufferTime || 0
            ];
            exec(success, fail, PLUGIN_NAME, 'subscribe', initArray);
        };

        this.unsubscribe = function (success, fail) {
            exec(success, fail, PLUGIN_NAME, 'unsubscribe', []);
        };
    };

    function RegisterEvents(success, fail) {
        (function (callback) {
            exec(function (event) {
                // Remove invalid characters from events and carriage returns/line feeds as they will break JSON.parse
                if (event && event.replace) {
                    event = event.replace(/[^\x00-\x7F]/g, "");
                    event = event.replace('\r\n', '').replace('\r\n', '');
                }
                let eventJson = {};
                try {
                    eventJson = JSON.parse(event);
                } catch (e) {
                    console.error('Could not parse event json from red5 plugin:', event);
                }
                callback(eventJson);
            }, fail, PLUGIN_NAME, 'registerEvents', []);
        })(success);
    }

    function UnregisterEvents(success, fail) {
        exec(success, fail, PLUGIN_NAME, 'unregisterEvents', []);
    }

    function CheckPermissions(isPublisher, success, fail) {
        exec(success, fail, PLUGIN_NAME, 'checkPermissions', [isPublisher ? "publisher" : "subscriber"]);
    }

    function GetStreamStats(success, fail) {
        exec(success, fail, PLUGIN_NAME, 'getStreamStats', []);
    }

    function Resize(renderX, renderY, renderWidth, renderHeight, success, failure) {
        exec(success, failure, PLUGIN_NAME, 'resize', [renderX, renderY, renderWidth, renderHeight]);
    }

    function UpdateScaleMode(scaleMode, success, fail) {
        exec(success, fail, PLUGIN_NAME, 'updateScaleMode', [scaleMode]);
    }
};

module.exports = red5promobile;
