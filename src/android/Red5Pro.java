
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
import android.hardware.Camera;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.red5pro.streaming.R5Connection;
import com.red5pro.streaming.R5Stream;
import com.red5pro.streaming.R5StreamProtocol;
import com.red5pro.streaming.config.R5Configuration;
import com.red5pro.streaming.source.R5Camera;
import com.red5pro.streaming.source.R5Microphone;
import com.red5pro.streaming.R5Stream.RecordType;
import com.red5pro.streaming.view.R5VideoView;

public class Red5Pro extends CordovaPlugin {
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
    protected Camera cam;

    public static final String [] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

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
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
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
        }
        else if (action.equals("publish")) {
            publish.publish(args.getString(0), RecordType.Live);
//            cordova.getActivity().runOnUiThread(new Runnable() {
//                public void run() {
//                    publish.publish("stream1", RecordType.Live);
//                    callbackContext.success();
//                }
//            });
            return true;
        }
        else if (action.equals("unpublish")) {
            publish.stop();
            callbackContext.success();
            return true;
        }
        else if (action.equals("resize")) {
            resize(args, callbackContext);
            return true;
        }
        else {
            return false;
        }
    }

    private boolean checkForPermissions(){



        //final String READ = Manifest.permission.READ_CONTACTS;


        for (String perm:permissions){
            if (cordova.hasPermission(perm) == false) {
                return false;
            }
        }

        return true;
//        cordova.requestPermissions(this, 0, permissions);
//        if (cordova.has  == false) {
//            cordova.requestPermission(this, 0, );
//        }
    }

    private void init(JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "Init called in Red5 Plugin");

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

                cam = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                cam.setDisplayOrientation(90);

                camera = new R5Camera(cam, 640, 360);
                camera.setBitrate(videoBandwidth);
                camera.setOrientation(270);
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
                publish.publish("stream1", RecordType.Live);

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
                params.setMargins(xPos, yPos, 0, 0);
                preview.setLayoutParams(params);
                preview.setVisibility(View.VISIBLE);
                layout.requestLayout();

                callbackContext.success();
            }
        });
    }

    private void resize(JSONArray args, CallbackContext callbackContext) throws JSONException {
        int xPos = args.getInt(0);
        int yPos = args.getInt(1);
        int width = args.getInt(2);
        int height = args.getInt(3);
        Log.d(TAG, String.format("Resize %d, %d, (%d, %d)", xPos, yPos, width, height));

        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run(){
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
                params.setMargins(xPos, yPos, 0, 0);
                preview.setLayoutParams(params);
                layout.requestLayout();
                callbackContext.success();
            }
        });

    }
}
