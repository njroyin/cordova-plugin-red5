
package com.red5pro;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.Runnable;

import android.graphics.Color;
import android.hardware.Camera;
import android.provider.Settings;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

    //public TextView textView;

    // Red5 Classes
    private R5Configuration configuration;
    private R5VideoView preview;
    protected R5Stream publish;
    protected R5Camera camera;

    // Camera1 Android capture
    protected Camera cam;



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
        layout.addView(preview);



//        textView = new TextView(layout.getContext());
//        textView.setText("Hello");
//        textView.setBackgroundColor(Color.BLUE);
//        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(500, 500);
//        params.setMargins(50, 50, 100, 100);
//        textView.setLayoutParams(params);
//        layout.addView(textView);
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
        if (action.equals("init")) {
            this.init(callbackContext);
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

    private void init(CallbackContext callbackContext) {
        Log.d(TAG, "Init called in Red5 Plugin");

//        cordova.getActivity().runOnUiThread(new Runnable() {
//            public void run() {
                configuration = new R5Configuration(R5StreamProtocol.RTSP, "piphany.ontrac.io", 8554, "live", 1.0f);
                configuration.setLicenseKey("ETGX-35SG-FLAB-BJKI");
                configuration.setBundleID(cordova.getActivity().getPackageName());

                R5Connection connection = new R5Connection(configuration);

                //setup a new stream using the connection
                publish = new R5Stream(connection);

                publish.audioController.sampleRate = 44100;

                //show all logging
                publish.setLogLevel(R5Stream.LOG_LEVEL_DEBUG);

                cam = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                cam.setDisplayOrientation(90);

                camera = new R5Camera(cam, 640, 360);
                camera.setBitrate(750);
                camera.setOrientation(270);
                camera.setFramerate(15);

                R5Microphone mic = new R5Microphone();
                publish.attachMic(mic);

                preview.attachStream(publish);
                publish.attachCamera(camera);
                preview.showDebugView(true);

                publish.publish("stream1", RecordType.Live);
                cam.startPreview();

                callbackContext.success();
//            }
//        });
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
                //textView.setLayoutParams(params);
                layout.requestLayout();
                callbackContext.success();
            }
        });

    }
}
