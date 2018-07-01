
package com.red5pro;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.Runnable;

import android.Manifest;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.telecom.Call;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;

import com.red5pro.streaming.R5Connection;
import com.red5pro.streaming.R5Stream;
import com.red5pro.streaming.R5StreamProtocol;
import com.red5pro.streaming.config.R5Configuration;
import com.red5pro.streaming.event.R5ConnectionEvent;
import com.red5pro.streaming.event.R5ConnectionListener;
import com.red5pro.streaming.source.R5Camera;
import com.red5pro.streaming.source.R5Microphone;
import com.red5pro.streaming.R5Stream.RecordType;
import com.red5pro.streaming.view.R5VideoView;

public class Red5Pro extends CordovaPlugin implements R5ConnectionListener {

    // Standard Android
    public static final String TAG = "Red5Pro";
    private FrameLayout layout;

    // Cordova
    private CallbackContext eventCallbackContext;

    // State management
    protected String streamName;
    private boolean isPublisher = false;
    private boolean isSubscriber = false;
    volatile private boolean isStreaming = false; // Either receiving or sending video
    private int cameraOrientation;
    private boolean isPreviewing = false;

    // Red5 Classes
    private R5VideoView videoView;
    private R5Stream stream;
    private R5Camera camera;
    private R5Connection connection;
    private R5Microphone mic;

    private int currentCamMode = Camera.CameraInfo.CAMERA_FACING_FRONT;

    public static final String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public enum ArgumentTypes {
        STRING,
        INT,
        DOUBLE,
        BOOLEAN
    }

    ;

    public Red5Pro() {
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        layout = (FrameLayout) webView.getView().getParent();

        if (!checkForPermissions()) {
            cordova.requestPermissions(this, 0, permissions);
        }
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action          The action to execute.
     * @param args            JSONArry of arguments for the plugin.
     * @param callbackContext The callback id used when calling back into JavaScript.
     * @return True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        // Make sure we have our needed permissions before allowing any calls
        if (!checkForPermissions()) {
            callbackContext.error("Permission denied for device.");
            return true;
        }

        switch (action) {
            case "initPublisher":
                this.initPublisher(args, callbackContext);
                return true;
            case "publish":
                this.publishStream(args, callbackContext);
                return true;
            case "unpublish":
                this.unpublish(callbackContext);
                return true;
            case "subscribe":
                this.subscribe(args, callbackContext);
                return true;
            case "unsubscribe":
                this.unsubscribe(callbackContext);
                return true;
            case "resize":
                resize(args, callbackContext);
                return true;
            case "updateScaleMode":
                updateScaleMode(args, callbackContext);
                return true;
            case "swapCamera":
                swapCamera(callbackContext);
                return true;
            case "registerEvents":
                eventCallbackContext = callbackContext;
                return true;
            case "unregisterEvents":
                if (eventCallbackContext != null) {
                    eventCallbackContext.success();
                    callbackContext.success();
                } else
                    callbackContext.error("Not previously registered for events");
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onConnectionEvent(R5ConnectionEvent event) {

        Log.d("R5VideoViewLayout", ":onConnectionEvent " + event.name());

        sendEventMessage(event.name() + ':' + event.message);

        if (event == R5ConnectionEvent.START_STREAMING) {
            isStreaming = true;
        } else if (event == R5ConnectionEvent.DISCONNECTED && isStreaming) {
            cleanup();
        }
    }

    private void createVideoView() {

        videoView = new R5VideoView(layout.getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(500, 500);
        params.setMargins(50, 50, 100, 100);
        videoView.setLayoutParams(params);
        videoView.setBackgroundColor(Color.BLACK);
        layout.addView(videoView);
    }

    private void initiateConnection(R5Configuration configuration) {
        connection = new R5Connection(configuration);

        //setup a new stream using the connection
        stream = new R5Stream(connection);
        stream.setListener(this);
        stream.client = this;

    }

    private void initPublisher(JSONArray args, CallbackContext callbackContext) throws JSONException {

        final ArgumentTypes[] types = {ArgumentTypes.INT, ArgumentTypes.INT, ArgumentTypes.INT, ArgumentTypes.INT,
                ArgumentTypes.STRING, ArgumentTypes.INT, ArgumentTypes.STRING, ArgumentTypes.INT,
                ArgumentTypes.INT, ArgumentTypes.INT, ArgumentTypes.STRING, ArgumentTypes.BOOLEAN};
        if (!validateArguments(args, types)) {
            callbackContext.error("Invalid arguments given");
            return;
        }

        // If we are previously previewing we need to clean up and re-launch
        if (isPreviewing) {
            stopPreviewAndStreaming();
            if (isStreaming) {

                // Eventhough we issued a stop streaming we need to wait until the event is fired back
                // to us before everything is fully released. Let's sleep this thread for a bit until
                // we can get clarification on the stop.
                for (int i = 0; i < 10; i++) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (!isStreaming) {
                        break;
                    }
                }
                if (isStreaming) {
                    callbackContext.error("Could not stop streaming");
                    return;
                }
            }
        }

        isPublisher = true;

        // Pull out all the parameters passed in
        int xPos = args.getInt(0);
        int yPos = args.getInt(1);
        int width = args.getInt(2);
        int height = args.getInt(3);

        String host = args.getString(4);
        int portNumber = args.getInt(5);
        String appName = args.getString(6);
        int audioBandwidth = args.getInt(7);
        int videoBandwidth = args.getInt(8);
        int frameRate = args.getInt(9);

        String licenseKey = args.getString(10);
        boolean showDebugView = args.getBoolean(11);

        R5Configuration configuration = new R5Configuration(R5StreamProtocol.RTSP, host, portNumber, appName, 1.0f);
        configuration.setLicenseKey(licenseKey);
        configuration.setBundleID(cordova.getActivity().getPackageName());

        initiateConnection(configuration);

        stream.setScaleMode(2); // Does NOT work....bug in SDK...you must be publishing for it to work

        stream.audioController.sampleRate = 44100;

        //show all logging
        stream.setLogLevel(R5Stream.LOG_LEVEL_DEBUG);

        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {

                createVideoView();

                Camera cam = openFrontFacingCameraGingerbread();
                cam.setDisplayOrientation((cameraOrientation + 180) % 360);

                camera = new R5Camera(cam, 640, 360);
                camera.setBitrate(videoBandwidth);
                camera.setOrientation(cameraOrientation);
                camera.setFramerate(frameRate);

                mic = new R5Microphone();
                stream.attachMic(mic);

                videoView.attachStream(stream);
                stream.attachCamera(camera);

                videoView.showDebugView(showDebugView);

                cam.startPreview();

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
                params.setMargins(xPos, yPos, 0, 0);
                videoView.setLayoutParams(params);
                layout.requestLayout();

                sendEventMessage("PREVIEWING");
                isPreviewing = true;

                callbackContext.success();
            }
        });
    }


    // Since the initPublisher method only displays a preview view, calling publishStream will start sending
    // the video
    private void publishStream(JSONArray args, CallbackContext callbackContext) throws JSONException {

        final ArgumentTypes[] types = {ArgumentTypes.STRING};
        if (!validateArguments(args, types)) {
            callbackContext.error("Invalid arguments given");
            return;
        }

        if (!isPreviewing) {
            callbackContext.error("Must be previewing first with call to initPublisher.");
            return;
        }

        String streamName = args.getString(0);
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                // We have to do this trickery for the time being in order to kick-start the publishing process.
                // Basically we need to set a camera on the R5Camera right before calling publish.publish.
                R5Camera publishCam = (R5Camera) stream.getVideoSource();
                publishCam.getCamera().stopPreview();
                publishCam.setCamera(publishCam.getCamera());
                publishCam.getCamera().startPreview();

                stream.publish(streamName, RecordType.Live);
                callbackContext.success();
            }
        });
    }

    private void unpublish(CallbackContext callbackContext) {

        stopPreviewAndStreaming();
        callbackContext.success();
    }

    private void subscribe(JSONArray args, CallbackContext callbackContext) throws JSONException {

        final ArgumentTypes[] types = {ArgumentTypes.INT, ArgumentTypes.INT, ArgumentTypes.INT, ArgumentTypes.INT,
                ArgumentTypes.STRING, ArgumentTypes.INT, ArgumentTypes.STRING, ArgumentTypes.INT,
                ArgumentTypes.INT, ArgumentTypes.INT, ArgumentTypes.STRING, ArgumentTypes.BOOLEAN, ArgumentTypes.STRING};
        if (!validateArguments(args, types)) {
            callbackContext.error("Invalid arguments given");
            return;
        }

        // If we are previously previewing we need to clean up and re-launch
        if (isStreaming) {
            stopPreviewAndStreaming();
            if (isStreaming) {

                // Eventhough we issued a stop streaming we need to wait until the event is fired back
                // to us before everything is fully released. Let's sleep this thread for a bit until
                // we can get clarification on the stop.
                for (int i = 0; i < 10; i++) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (!isStreaming) {
                        break;
                    }
                }
                if (isStreaming) {
                    callbackContext.error("Could not stop streaming");
                    return;
                }
            }
        }

        isSubscriber = true;

        // Pull out all the parameters passed in
        int xPos = args.getInt(0);
        int yPos = args.getInt(1);
        int width = args.getInt(2);
        int height = args.getInt(3);

        String host = args.getString(4);
        int portNumber = args.getInt(5);
        String appName = args.getString(6);
        int audioBandwidth = args.getInt(7);
        int videoBandwidth = args.getInt(8);
        int frameRate = args.getInt(9);

        String licenseKey = args.getString(10);
        boolean showDebugView = args.getBoolean(11);
        String streamName = args.getString(12);

        R5Configuration configuration = new R5Configuration(R5StreamProtocol.RTSP, host, portNumber, appName, 1.0f);
        configuration.setLicenseKey(licenseKey);
        configuration.setStreamName(streamName);
        configuration.setBundleID(cordova.getActivity().getPackageName());

        initiateConnection(configuration);

        stream.setScaleMode(2); // Does NOT work....bug in SDK...you must be publishing for it to work

        stream.audioController.sampleRate = 44100;

        //show all logging
        stream.setLogLevel(R5Stream.LOG_LEVEL_DEBUG);

        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {

                createVideoView();

                videoView.attachStream(stream);
                videoView.showDebugView(showDebugView);

                stream.play(streamName);

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
                params.setMargins(xPos, yPos, 0, 0);
                videoView.setLayoutParams(params);
                layout.requestLayout();

                //sendEventMessage("SUBSCRIBING");
                //isPreviewing = true;

                callbackContext.success();
            }
        });


    }

    private void unsubscribe(CallbackContext callbackContext) {

        if (videoView != null) {
            videoView.attachStream(null);
        }

        if (stream != null && isStreaming) {
            stream.stop();
        }
        else {
            cleanup();
        }

        callbackContext.success();
    }

    private void stopPreviewAndStreaming() {

        if (videoView != null) {
            videoView.attachStream(null);
        }

        if (camera != null) {
            Camera c = camera.getCamera();
            c.stopPreview();
            c.release();
            camera = null;
        }

        if (stream != null && isStreaming) {
            stream.stop(); // Cleanup will be called on the disconnect event
        } else {
            cleanup();
        }

        isPreviewing = false;
    }

    private void cleanup() {

        Log.d("R5VideoViewLayout", ":cleanup (" + "stream" + ")!");
        if (stream != null) {
            stream.client = null;
            stream.setListener(null);
            stream.attachMic(null);
            stream = null;
        }

        if (connection != null) {
            connection.removeListener();
            connection = null;
        }
        if (videoView != null) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    videoView.attachStream(null);
                    layout.removeView(videoView);
                    layout.requestLayout();
                    videoView = null;
                }
            });
        }
        isStreaming = false;

    }

    private void resize(JSONArray args, CallbackContext callbackContext) throws JSONException {

        final ArgumentTypes[] types = {ArgumentTypes.INT, ArgumentTypes.INT, ArgumentTypes.INT, ArgumentTypes.INT};
        if (!validateArguments(args, types)) {
            callbackContext.error("Invalid arguments given");
            return;
        }

        int xPos = args.getInt(0);
        int yPos = args.getInt(1);
        int width = args.getInt(2);
        int height = args.getInt(3);

        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
                params.setMargins(xPos, yPos, 0, 0);
                videoView.setLayoutParams(params);
                layout.requestLayout();
                callbackContext.success();
            }
        });
    }

    private void updateScaleMode(JSONArray args, CallbackContext callbackContext) throws JSONException {

        final ArgumentTypes[] types = {ArgumentTypes.INT};
        if (!validateArguments(args, types)) {
            callbackContext.error("Invalid arguments given");
            return;
        }

        if (!isPreviewing && !isStreaming) {
            callbackContext.error("Not previewing or subscribing to video stream");
            return;
        }

        int scaleMode = args.getInt(0);

        if (scaleMode < 0 || scaleMode >= 3) {
            callbackContext.error("Invalid scale mode given");
            return;
        }

        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                stream.setScaleMode(scaleMode);
                callbackContext.success();
            }
        });
    }


    private void swapCamera(CallbackContext callbackContext) throws JSONException {

        if (!isPreviewing) {
            callbackContext.error("Not previewing");
            return;
        }

        R5Camera publishCam = (R5Camera) stream.getVideoSource();

        Camera newCam = null;

        //NOTE: Some devices will throw errors if you have a camera open when you attempt to open another
        publishCam.getCamera().stopPreview();
        publishCam.getCamera().release();

        //NOTE: The front facing camera needs to be 180 degrees further rotated than the back facing camera
        int rotate = 0;
        if (currentCamMode == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            newCam = openBackFacingCameraGingerbread();
            rotate = 0;
            if (newCam != null)
                currentCamMode = Camera.CameraInfo.CAMERA_FACING_BACK;
        } else {
            newCam = openFrontFacingCameraGingerbread();
            rotate = 180;
            if (newCam != null)
                currentCamMode = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }

        if (newCam != null) {

            newCam.setDisplayOrientation((cameraOrientation + rotate) % 360);

            publishCam.setCamera(newCam);
            publishCam.setOrientation(cameraOrientation);

            newCam.startPreview();
            callbackContext.success();
        } else
            callbackContext.error("Could not swap cameras");
    }

    private boolean validateArguments(JSONArray args, ArgumentTypes[] types) throws JSONException {

        if (args.length() != types.length) return false;

        try {
            int index = 0;
            for (ArgumentTypes argumentType : types) {
                if (args.isNull(index)) return false;
                if (argumentType == ArgumentTypes.STRING) {
                    String testStr = args.getString(index);
                    if (testStr == null)
                        return false;
                } else if (argumentType == ArgumentTypes.INT) {
                    args.getInt(index); // Should throw exceptions
                } else if (argumentType == ArgumentTypes.DOUBLE) {
                    args.getDouble(index);
                } else if (argumentType == ArgumentTypes.BOOLEAN) {
                    args.getBoolean(index);
                }

                index++;
            }
        } catch (JSONException e) {
            return false;
        }

        return true;
    }

    private void sendEventMessage(String message) {
        if (eventCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, message);
            result.setKeepCallback(true); // Required so the context doesn't go away
            eventCallbackContext.sendPluginResult(result);
        }
    }

    private boolean checkForPermissions() {
        for (String perm : permissions) {
            if (cordova.hasPermission(perm) == false) {
                return false;
            }
        }
        return true;
    }

    private Camera openFrontFacingCameraGingerbread() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                    cameraOrientation = cameraInfo.orientation;
                    applyDeviceRotation();
                    break;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }

        return cam;
    }

    private Camera openBackFacingCameraGingerbread() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        System.out.println("Number of cameras: " + cameraCount);
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    cam = Camera.open(camIdx);
                    cameraOrientation = cameraInfo.orientation;
                    applyInverseDeviceRotation();
                    break;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }

        return cam;
    }

    private void applyDeviceRotation() {
        int rotation = cordova.getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 270;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 90;
                break;
        }

        Rect screenSize = new Rect();
        cordova.getActivity().getWindowManager().getDefaultDisplay().getRectSize(screenSize);
        float screenAR = (screenSize.width() * 1.0f) / (screenSize.height() * 1.0f);
        if ((screenAR > 1 && degrees % 180 == 0) || (screenAR < 1 && degrees % 180 > 0))
            degrees += 180;

        System.out.println("Apply Device Rotation: " + rotation + ", degrees: " + degrees);

        cameraOrientation += degrees;

        cameraOrientation = cameraOrientation % 360;
    }

    private void applyInverseDeviceRotation() {
        int rotation = cordova.getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 270;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 90;
                break;
        }

        cameraOrientation += degrees;

        cameraOrientation = cameraOrientation % 360;
    }
}
