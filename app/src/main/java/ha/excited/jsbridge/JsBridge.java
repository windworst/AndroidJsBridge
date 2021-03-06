package ha.excited.jsbridge;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class JsBridge {
    public interface Callback {
        void callback(String paramString);
    }

    public interface Function {
        String call(String paramString, Callback callback);
    }

    public static abstract class Adapter {
        private JsBridge jsBridge = null;
        private final String name;

        public Adapter(String name) {
            this.name = name;
        }

        public String jsCallNative(String message) {
            return jsBridge.jsCallNative(message);
        }

        protected final String exposeName() {
            return EXPOSE_PREFIX + name;
        }

        private void nativeCallJs(String message) {
            evalJs(String.format("javascript:window.%s.nativeCallJs('%s')", name, message));
        }

        protected abstract void evalJs(String jsCode);
    }


    private final static String FUNC_NAME = "funcName";
    private final static String SYNC = "sync";
    private final static String RESULT = "result";
    private final static String PARAM_STRING = "paramString";
    private final static String JS_CALLBACK = "jsCallback";
    private final static String NATIVE_CALLBACK = "nativeCallback";
    private final static String EXPOSE_PREFIX = "ANDROID_NATIVE_";
    private final static String CALLBACK_PREFIX = "NATIVE_CB_";
    private final static int SYNC_TIME_WAIT = 500;

    private final Adapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Map<String, Function> functions = new HashMap<>();
    private Map<String, Callback> callbacks = new HashMap<>();
    private int callbackCount = 0;

    public JsBridge(Adapter adapter) {
        adapter.jsBridge = this;
        this.adapter = adapter;
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

    private String jsCallNative(String message) {
        try {
            final JSONObject params = new JSONObject(message);
            if (params.has(RESULT)) {
                handleSyncResult(params.getString(RESULT));
            } else if (params.has(SYNC)) {
                return syncCall(new SyncCall<String>() {
                    @Override
                    public String call() {
                        return handleMessage(params);
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleMessage(params);
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String handleMessage(JSONObject params) {
        try {
            if (params.has(FUNC_NAME)) {
                final String funcName = params.getString(FUNC_NAME);
                final String paramString = params.getString(PARAM_STRING);
                final Function function = functions.get(funcName);
                if (null != function) {
                    String jsCallback = null;
                    if (params.has(JS_CALLBACK)) {
                        jsCallback = params.getString(JS_CALLBACK);
                    }
                    final String finalJsCallback = jsCallback;
                    try {
                        return function.call(paramString, makeJsCallback(finalJsCallback));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            } else if (params.has(NATIVE_CALLBACK)) {
                String nativeCallback = params.getString(NATIVE_CALLBACK);
                final String paramString = params.getString(PARAM_STRING);
                final Callback callback = callbacks.get(nativeCallback);
                if (null != callback) {
                    callbacks.remove(nativeCallback);
                    callback.callback(paramString);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void callJs(String funcName, String paramString) {
        callJs(funcName, paramString, null);
    }

    public void callJs(String funcName, String paramString, Callback callback) {
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
    }

    public String callJsSync(String funcName, String paramString) {
        try {
            JSONObject data = new JSONObject();
            data.put(FUNC_NAME, funcName).put(PARAM_STRING, paramString).put(SYNC, true);
            return sendToJsSync(data.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendToJs(final String data) {
        adapter.nativeCallJs(data);
    }

    private String syncResult = null;
    private boolean syncWait = false;
    private final Object syncLock = new Object();

    private void handleSyncResult(String result) {
        synchronized (syncLock) {
            syncResult = result;
            syncWait = false;
            syncLock.notifyAll();
        }
    }

    private synchronized String sendToJsSync(String data) {
        sendToJs(data);
        synchronized (syncLock) {
            syncWait = true;
            syncResult = null;
            try {
                if (syncWait) syncLock.wait(SYNC_TIME_WAIT, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String result = syncResult;
        syncResult = null;
        return result;
    }

    private boolean runOnUiThread(Runnable runnable) {
        if (handler.getLooper().getThread() == Thread.currentThread()) {
            runnable.run();
            return false;
        } else {
            handler.post(runnable);
            return true;
        }
    }

    private interface SyncCall<T> {
        T call();
    }

    private <T> T syncCall(final SyncCall<T> syncCall) {
        if (handler.getLooper().getThread() == Thread.currentThread()) {
            return syncCall.call();
        }
        final Object lock = new Object();
        final Object[] result = new Object[1];
        final boolean[] finished = new boolean[]{false};
        handler.post(new Runnable() {
            @Override
            public void run() {
                result[0] = syncCall.call();
                synchronized (lock) {
                    finished[0] = true;
                    lock.notifyAll();
                }
            }
        });
        synchronized (lock) {
            try {
                if (!finished[0]) lock.wait(SYNC_TIME_WAIT, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return (T) result[0];
    }
}
