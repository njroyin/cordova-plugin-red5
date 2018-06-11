
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
import android.provider.Settings;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Red5Pro extends CordovaPlugin {
    public static final String TAG = "Red5Pro";

    public TextView textView;
    FrameLayout layout;

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

        //mainView = new RelativeLayout(cordova.getActivity());

        //root = (ViewGroup) webView.getParent();

        layout = (FrameLayout) webView.getView().getParent();

        textView = new TextView(layout.getContext());
        textView.setText("Hello");
        textView.setId(55);
        textView.setBackgroundColor(Color.BLUE);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(500, 500);
        params.setMargins(50, 50, 100, 100);
        textView.setLayoutParams(params);
        layout.addView(textView);
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
        callbackContext.success();
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
                textView.setLayoutParams(params);
                layout.requestLayout();
                callbackContext.success();
            }
        });

    }
}
