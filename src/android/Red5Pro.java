
package com.red5pro;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.Runnable;

import android.Manifest;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.provider.Settings;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.red5pro.streaming.R5Connection;
import com.red5pro.streaming.R5Stream;
import com.red5pro.streaming.R5StreamProtocol;
import com.red5pro.streaming.config.R5Configuration;
import com.red5pro.streaming.event.R5ConnectionListener;
import com.red5pro.streaming.source.R5Camera;
import com.red5pro.streaming.source.R5Microphone;
import com.red5pro.streaming.R5Stream.RecordType;
import com.red5pro.streaming.view.R5VideoView;

public class Red5Pro extends CordovaPlugin implements R5ConnectionListener {
    public static final String TAG = "Red5Pro";

    private FrameLayout layout;

    // Red5 Classes
    protected R5Configuration configuration;
    protected R5VideoView preview;
    protected R5Stream publish;
    protected R5Camera camera;
    protected R5Connection connection;
    protected R5Microphone mic;

    // Camera1 Android capture
    //protected Camera cam;
    protected int camOrientation;

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

        preview = new R5VideoView(layout.getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(500, 500);
        params.setMargins(50, 50, 100, 100);
        preview.setLayoutParams(params);
        preview.setBackgroundColor(Color.BLUE);
        preview.setVisibility(View.GONE); // Hide preview until they initialize
        layout.addView(preview);

        if (checkForPermissions() == false) {
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
        if (checkForPermissions() == false) {
            callbackContext.error("Permission denied for device.");
            return true;
        }

        if (action.equals("init")) {
            this.init(args, callbackContext);
            return true;
        } else if (action.equals("publish")) {
            this.publishStream(args, callbackContext);
            return true;
        } else if (action.equals("unpublish")) {
            publish.stop();
            callbackContext.success();
            return true;
        } else if (action.equals("resize")) {
            resize(args, callbackContext);
            return true;
        } else if (action.equals("updateScaleMode")) {
            updateScaleMode(args, callbackContext);
            return true;
        } else if (action.equals("swapCamera")) {
            swapCamera(callbackContext);
            return true;
        } else if (action.equals("hideVideo")) {
            camera.getCamera().stopPreview();
            camera.close();
            preview.setVisibility(View.GONE);
            layout.requestLayout();
            callbackContext.success();
            return true;
        } else if (action.equals("showVideo")) {
            preview.setVisibility(View.VISIBLE);
            layout.requestLayout();
            callbackContext.success();
            return true;
        } else {
            return false;
        }
    }

    private void init(JSONArray args, CallbackContext callbackContext) throws JSONException {

        final ArgumentTypes[] types = {ArgumentTypes.INT, ArgumentTypes.INT, ArgumentTypes.INT, ArgumentTypes.INT,
                ArgumentTypes.STRING, ArgumentTypes.INT, ArgumentTypes.STRING, ArgumentTypes.INT,
                ArgumentTypes.INT, ArgumentTypes.INT, ArgumentTypes.STRING, ArgumentTypes.BOOLEAN};
        if (validateArguments(args, types) == false) {
            callbackContext.error("Invalid arguments given");
            return;
        }

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

        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {

                configuration = new R5Configuration(R5StreamProtocol.RTSP, host, portNumber, appName, 1.0f);
                configuration.setLicenseKey(licenseKey);
                configuration.setBundleID(cordova.getActivity().getPackageName());

                connection = new R5Connection(configuration);

                //setup a new stream using the connection
                publish = new R5Stream(connection);

                publish.audioController.sampleRate = 44100;

                //show all logging
                publish.setLogLevel(R5Stream.LOG_LEVEL_DEBUG);

                Camera cam = openFrontFacingCameraGingerbread();
                cam.setDisplayOrientation((camOrientation + 180) % 360);

                camera = new R5Camera(cam, 640, 360);
                camera.setBitrate(videoBandwidth);
                camera.setOrientation(camOrientation);
                camera.setFramerate(frameRate);

                mic = new R5Microphone();
                publish.attachMic(mic);

                preview.attachStream(publish);
                publish.attachCamera(camera);

                if (showDebugView)
                    preview.showDebugView(true);
                else
                    preview.showDebugView(false);

                cam.startPreview();

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
                params.setMargins(xPos, yPos, 0, 0);
                preview.setLayoutParams(params);
                preview.setVisibility(View.VISIBLE);
                layout.requestLayout();

                callbackContext.success();
            }
        });
    }

    private void publishStream(JSONArray args, CallbackContext callbackContext) throws JSONException {

        final ArgumentTypes[] types = {ArgumentTypes.STRING};
        if (validateArguments(args, types) == false) {
            callbackContext.error("Invalid arguments given");
            return;
        }

        String streamName = args.getString(0);
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                // We have to do this trickery for the time being in order to kick-start the publishing process.
                // Basically we need to set a camera on the R5Camera right before calling publish.publish.
                R5Camera publishCam = (R5Camera) publish.getVideoSource();
                publishCam.getCamera().stopPreview();
                publishCam.setCamera(publishCam.getCamera());
                publishCam.getCamera().startPreview();

                publish.publish(streamName, RecordType.Live);
                callbackContext.success();
            }
        });
    }

    private void resize(JSONArray args, CallbackContext callbackContext) throws JSONException {

        final ArgumentTypes[] types = {ArgumentTypes.INT, ArgumentTypes.INT, ArgumentTypes.INT, ArgumentTypes.INT};
        if (validateArguments(args, types) == false) {
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
                preview.setLayoutParams(params);
                layout.requestLayout();
                callbackContext.success();
            }
        });
    }

    private void updateScaleMode(JSONArray args, CallbackContext callbackContext) throws JSONException {

        final ArgumentTypes[] types = {ArgumentTypes.INT};
        if (validateArguments(args, types) == false) {
            callbackContext.error("Invalid arguments given");
            return;
        }

        int scaleMode = args.getInt(0);

        if (scaleMode < 0 || scaleMode >= 3) {
            callbackContext.error("Invalid scale mode given");
            return;
        }

        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                publish.setScaleMode(scaleMode);
                callbackContext.success();
            }
        });
    }

//    protected void cleanup() {
//
//        Log.d("R5VideoViewLayout", ":cleanup (" + "stream" + ")!");
//        if (stream != null) {
//            mStream.client = null;
//            mStream.setListener(null);
//            mStream = null;
//        }
//
//        if (mConnection != null) {
//            mConnection.removeListener();
//            mConnection = null;
//        }
//        if (mVideoView != null) {
//            mVideoView.attachStream(null);
////            removeView(mVideoView);
//            mVideoView = null;
//        }
//        mIsStreaming = false;
//
//    }

    private void swapCamera(CallbackContext callbackContext) throws JSONException {

        R5Camera publishCam = (R5Camera) publish.getVideoSource();

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

            newCam.setDisplayOrientation((camOrientation + rotate) % 360);

            publishCam.setCamera(newCam);
            publishCam.setOrientation(camOrientation);

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

    private boolean checkForPermissions() {
        for (String perm : permissions) {
            if (cordova.hasPermission(perm) == false) {
                return false;
            }
        }
        return true;
    }

    protected Camera openFrontFacingCameraGingerbread() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                    camOrientation = cameraInfo.orientation;
                    applyDeviceRotation();
                    break;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }

        return cam;
    }

    protected Camera openBackFacingCameraGingerbread() {
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
                    camOrientation = cameraInfo.orientation;
                    applyInverseDeviceRotation();
                    break;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }

        return cam;
    }

    protected void applyDeviceRotation() {
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

        camOrientation += degrees;

        camOrientation = camOrientation % 360;
    }

    protected void applyInverseDeviceRotation() {
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

        camOrientation += degrees;

        camOrientation = camOrientation % 360;
    }
}
