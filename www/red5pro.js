var exec = require('cordova/exec');

var PLUGIN_NAME = 'Red5Pro';

var red5promobile = new function() {

    this.Publisher = function () {

        var initOptions = {};
        var _this = this;

        this.init = function(options, success, fail) {

            _this.initOptions = options;

            // Get computed positions from media element we are overlaying onto
            var mediaElement =  document.getElementById(options.mediaElementId);
            if (mediaElement == undefined) {
                fail('Missing media element to place video on top of.');
                return;
            }

            var positionRect = mediaElement.getBoundingClientRect();
            positionRect.xPos *= window.devicePixelRatio; // Scale up to device true resolution
            positionRect.yPos *= window.devicePixelRatio;
            positionRect.width *= window.devicePixelRatio;
            positionRect.height *= window.devicePixelRatio;

            // Init array - layout
            // X position, Y position, Width, Height
            // Host, Port, app name, stream name
            // Audio Bandwidth, Video Bandwidth, Frame Rate
            // License Key, Show Debug View
            var initArray = [
                positionRect.left,
                positionRect.top,
                positionRect.width,
                positionRect.height,
                options.host,
                options.port,
                options.app,
                options.bandwidth.audio,
                options.bandwidth.video,
                options.frameRate,
                options.licenseKey,
                options.debugView || false
            ];
            exec(success, fail, PLUGIN_NAME, 'init', initArray);
        };

        this.publish = function (streamName, success, fail) {
            exec(success, fail, PLUGIN_NAME, 'publish', [streamName]);
        };

        this.unpublish = function (success, fail) {
            exec(success, fail, PLUGIN_NAME, 'unpublish', []);
        };

        this.updateScaleMode = function (scaleMode, success, fail) {
            exec(success, fail, PLUGIN_NAME, 'updateScaleMode', [scaleMode]);
        };

        this.swapCamera = function (success, fail) {
            exec(success, fail, PLUGIN_NAME, 'swapCamera', []);
        };


        this.getOptions = function () {
            return _this.initOptions;
        };


        // Common functions
        this.resize = Resize;
        this.hideVideo = HideVideo;
        this.showVideo = ShowVideo;

    };

    this.Subscriber = function () {

        // Common functions
        this.resize = Resize;
        this.hideVideo = HideVideo;
        this.showVideo = ShowVideo;
    };


    function Resize(xPos, yPos, width, height, actualPixels) {
        if (actualPixels) {
            exec(null, null, PLUGIN_NAME, 'resize', [xPos, yPos, width, height]);
        }
        else {
            // Check for percent
            if(typeof(width) == 'string' && width.indexOf('%') != -1) {
                width = parseInt(width) / 100.0;
                width = Math.min(Math.max(width, 0.0), 1.0);
                width *= window.outerWidth;
            }
            if(typeof(height) == 'string' && height.indexOf('%') != -1) {
                height = parseInt(height) / 100.0;
                height = Math.min(Math.max(height, 0.0), 1.0);
                height *= window.outerHeight;
            }

            xPos *= window.devicePixelRatio;
            yPos *= window.devicePixelRatio;
            width *= window.devicePixelRatio;
            height *= window.devicePixelRatio;

            exec(null, null, PLUGIN_NAME, 'resize', [xPos, yPos, width, height]);
        }
    }

    function HideVideo() {
        exec(null, null, PLUGIN_NAME, 'hideVideo', []);
    }

    function ShowVideo() {
        exec(null, null, PLUGIN_NAME, 'showVideo', []);
    }

};

module.exports = red5promobile;
