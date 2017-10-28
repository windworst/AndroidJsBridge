package ha.excited.jsbridge;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

abstract public class JsBridge {
    public interface Callback {
        void callback(String paramString);
    }

    public interface Function {
        void call(String paramString, Callback callback);
    }

    private final static String FUNC_NAME = "funcName";
    private final static String PARAM_STRING = "paramString";
    private final static String JS_CALLBACK = "jsCallback";
    private final static String NATIVE_CALLBACK = "nativeCallback";
    private final static String CALLBACK_PREFIX = "NATIVE_CB_";

    private final String name;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Map<String, Function> functions = new HashMap<>();
    private Map<String, Callback> callbacks = new HashMap<>();
    private int callbackCount = 0;

    public JsBridge(String name) {
        this.name = name;
    }

    private String makeNativeCallbackString(Callback callback) {
        if (null == callback) return null;
        ++callbackCount;
        String callbackString = CALLBACK_PREFIX + callbackCount;
        callbacks.put(callbackString, callback);
        return callbackString;
    }

    private Callback makeJsCallback(final String jsCallback) {
        if (null == jsCallback) return null;
        return new Callback() {
            @Override
            public void callback(String paramString) {
                try {
                    JSONObject data = new JSONObject();
                    data.put(JS_CALLBACK, jsCallback).put(PARAM_STRING, paramString);
                    sendToJs(data.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public JsBridge register(String name, Function function) {
        functions.put(name, function);
        return this;
    }

    public JsBridge unregister(String name) {
        functions.remove(name);
        return this;
    }

    public JsBridge unregisterAll() {
        functions.clear();
        return this;
    }

    public void jsCallNative(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                handleMessage(message);
            }
        });
    }

    private void handleMessage(String message) {
        try {
            JSONObject params = new JSONObject(message);
            if (params.has(FUNC_NAME)) {
                String funcName = params.getString(FUNC_NAME);
                String paramString = params.getString(PARAM_STRING);
                Function function = functions.get(funcName);
                if (null != function) {
                    String jsCallback = null;
                    if (params.has(JS_CALLBACK)) {
                        jsCallback = params.getString(JS_CALLBACK);
                    }
                    function.call(paramString, makeJsCallback(jsCallback));
                }
            } else if (params.has(NATIVE_CALLBACK)) {
                String nativeCallback = params.getString(NATIVE_CALLBACK);
                String paramString = params.getString(PARAM_STRING);
                Callback callback = callbacks.get(nativeCallback);
                if (null != callback) {
                    callbacks.remove(nativeCallback);
                    callback.callback(paramString);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JsBridge callJs(String funcName, String paramString) {
        return callJs(funcName, paramString, null);
    }

    public JsBridge callJs(String funcName, String paramString, Callback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put(FUNC_NAME, funcName).put(PARAM_STRING, paramString);
            if (null != callback) {
                data.put(NATIVE_CALLBACK, makeNativeCallbackString(callback));
            }
            sendToJs(data.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    private void sendToJs(String data) {
        callJs(String.format("javascript:window.%s.nativeCallJs('%s')", name, data));
    }
    protected abstract void callJs(String uri);
}
