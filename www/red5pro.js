var exec = require('cordova/exec');

var PLUGIN_NAME = 'Red5Pro';

var red5promobile = new function () {

    this.Publisher = function () {

        var initOptions = {};
        var _this = this;

        // Common functions
        this.resize = Resize;
        this.registerEvents = RegisterEvents;
        this.unregisterEvents = UnregisterEvents;
        this.updateScaleMode = UpdateScaleMode;

        this.init = function (options, success, fail) {

            _this.initOptions = options;

            var positionRect = GetVideoElementBounds(options.mediaElementId);
            document.getElementById(options.mediaElementId).setAttribute('style', 'display:none');

            var initArray = [
                positionRect.x,
                positionRect.y,
                positionRect.width,
                positionRect.height,
                options.host,
                options.port,
                options.app,
                options.bandwidth.audio,
                options.bandwidth.video,
                options.frameRate,
                options.licenseKey,
                options.debugView || false,
                options.renderBelow || false
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

        this.getOptions = function () {
            return _this.initOptions;
        };
    };

    this.Subscriber = function () {

        var initOptions = {};
        var _this = this;

        // Common functions
        this.resize = Resize;
        this.registerEvents = RegisterEvents;
        this.unregisterEvents = UnregisterEvents;
        this.updateScaleMode = UpdateScaleMode;

        this.subscribe = function (options, streamName, success, fail) {

            _this.initOptions = options;

            var positionRect = GetVideoElementBounds(options.mediaElementId);
            document.getElementById(options.mediaElementId).setAttribute('style', 'display:none');

            var initArray = [
                positionRect.x,
                positionRect.y,
                positionRect.width,
                positionRect.height,
                options.host,
                options.port,
                options.app,
                options.bandwidth.audio,
                options.bandwidth.video,
                options.frameRate,
                options.licenseKey,
                options.debugView || false,
                streamName,
                options.renderBelow || false
            ];
            exec(success, fail, PLUGIN_NAME, 'subscribe', initArray);
        };

        this.unsubscribe = function (success, fail) {
            exec(success, fail, PLUGIN_NAME, 'unsubscribe', []);
        };

        this.getOptions = function () {
            return _this.initOptions;
        };

    };

    function GetVideoElementBounds(mediaElementId) {

        // Get computed positions from media element we are overlaying onto
        var mediaElement = document.getElementById(mediaElementId);
        if (mediaElement == undefined) {
            fail('Missing media element to place video on top of.');
            return;
        }

        var positionRect = mediaElement.getBoundingClientRect();
        positionRect.x *= window.devicePixelRatio; // Scale up to device true resolution
        positionRect.y *= window.devicePixelRatio;
        positionRect.width *= window.devicePixelRatio;
        positionRect.height *= window.devicePixelRatio;

        return positionRect;
    }

    function RegisterEvents(success, fail) {
        exec(success, fail, PLUGIN_NAME, 'registerEvents', []);
    }

    function UnregisterEvents() {
        exec(null, null, PLUGIN_NAME, 'unregisterEvents', []);
    }

    function Resize(xPos, yPos, width, height, actualPixels) {
        if (actualPixels) {
            exec(null, null, PLUGIN_NAME, 'resize', [xPos, yPos, width, height]);
        }
        else {
            // Check for percent
            if (typeof(width) === 'string' && width.indexOf('%') !== -1) {
                width = parseInt(width) / 100.0;
                width = Math.min(Math.max(width, 0.0), 1.0);
                width *= window.outerWidth;
            }
            if (typeof(height) === 'string' && height.indexOf('%') !== -1) {
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

    function  UpdateScaleMode (scaleMode, success, fail) {
        exec(success, fail, PLUGIN_NAME, 'updateScaleMode', [scaleMode]);
    }
};

module.exports = red5promobile;
