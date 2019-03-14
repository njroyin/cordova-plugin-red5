var exec = require('cordova/exec');

var PLUGIN_NAME = 'Red5Pro';

var red5promobile = new function () {

	this.Publisher = function () {

		var initOptions = {};

		// Common functions
		this.resize = Resize;
		this.updateScaleMode = UpdateScaleMode;

		this.init = function (options, success, fail) {

			initOptions = options;

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
			if (initOptions.mediaElementId !== undefined)
				document.getElementById(initOptions.mediaElementId).setAttribute('style', 'display:inherit');
			exec(success, fail, PLUGIN_NAME, 'unpublish', []);
		};
		this.registerEvents = function (success, fail) {
			(function (callback) {
				exec(function (event) {
					console.log(event);
					if (event && event.replace) {
						event = event.replace(/[^\x00-\x7F]/g, "");
						event = event.replace('↵', "");
						event = event.replace('↵', "");
					}
					var eventJson = JSON.parse(event);
					callback(eventJson);
				}, fail, PLUGIN_NAME, 'registerEvents', []);
			})(success);
		}

		this.unregisterEvents = function (success, fail) {
			exec(success, fail, PLUGIN_NAME, 'unregisterEvents', []);
		}
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

		this.getOptions = function () {
			return initOptions;
		};
	};

	this.Subscriber = function () {

		var initOptions = {};

		// Common functions
		this.resize = Resize;
		this.updateScaleMode = UpdateScaleMode;

		this.subscribe = function (options, streamName, success, fail) {

			initOptions = options;

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
			if (initOptions.mediaElementId !== undefined)
				document.getElementById(initOptions.mediaElementId).setAttribute('style', 'display:inhert');
			exec(success, fail, PLUGIN_NAME, 'unsubscribe', []);
		};

		this.registerEvents = function (success, fail) {
			(function (callback) {
				exec(function (event) {
					if (event && event.replace) {
						event = event.replace(/[^\x00-\x7F]/g, "");
						event = event.replace('↵', "");
						event = event.replace('↵', "");
					}
					var eventJson = JSON.parse(event);
					callback(eventJson);
				}, fail, PLUGIN_NAME, 'registerEvents', []);
			})(success);
		};

		this.unregisterEvents = function (success, fail) {
			exec(null, null, PLUGIN_NAME, 'unregisterEvents', []);
		};

		this.getOptions = function () {
			return initOptions;
		};

	};

	function GetVideoElementBounds(mediaElementId) {

		// Get computed positions from media element we are overlaying onto
		var mediaElement = document.getElementById(mediaElementId);
		if (mediaElement == undefined) {
			fail('Missing media element to place video on top of.');
			return;
		}

		return mediaElement.getBoundingClientRect();
	}

	function Resize(xPos, yPos, width, height, actualPixels) {
		if (actualPixels) {
			exec(null, null, PLUGIN_NAME, 'resize', [xPos, yPos, width, height]);
		} else {
			// Check for percent
			if (typeof (width) === 'string' && width.indexOf('%') !== -1) {
				width = parseInt(width) / 100.0;
				width = Math.min(Math.max(width, 0.0), 1.0);
				width *= window.outerWidth;
			}
			if (typeof (height) === 'string' && height.indexOf('%') !== -1) {
				height = parseInt(height) / 100.0;
				height = Math.min(Math.max(height, 0.0), 1.0);
				height *= window.outerHeight;
			}

			exec(null, null, PLUGIN_NAME, 'resize', [xPos, yPos, width, height]);
		}
	}

	function UpdateScaleMode(scaleMode, success, fail) {
		exec(success, fail, PLUGIN_NAME, 'updateScaleMode', [scaleMode]);
	}
};

module.exports = red5promobile;
