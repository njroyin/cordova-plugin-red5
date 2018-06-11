var exec = require('cordova/exec');

var PLUGIN_NAME = 'Red5Pro';

var Red5Pro = new function() {
    this.init = function() {
        exec(null, null, PLUGIN_NAME, 'init', []);
    };

    this.resize = function(xPos, yPos, width, height, actualPixels) {
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
    };
};

module.exports = Red5Pro;
