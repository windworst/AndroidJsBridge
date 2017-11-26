package ha.excited.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

import ha.excited.jsbridge.JsBridge;
import ha.excited.jsbridge.R;
import ha.excited.jsbridge.WebViewAdapter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WebView webView = (WebView) findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        final EditText editText = (EditText) findViewById(R.id.editText);
        final Button buttonFromJsSync = (Button) findViewById(R.id.buttonFromJsSync);
        final Button buttonFromJsAsync = (Button) findViewById(R.id.buttonFromJsAsync);
        final Button buttonSendToJs = (Button) findViewById(R.id.buttonSendToJs);
        final Button buttonSyncCallTest = (Button) findViewById(R.id.buttonSyncCallTest);
        final Button buttonAsyncCallTest = (Button) findViewById(R.id.buttonAsyncCallTest);

        final JsBridge jsBridge = new JsBridge(new WebViewAdapter("jsBridge", webView)).register("nativeGetInput", new JsBridge.Function() {
            @Override
            public String call(String paramString, JsBridge.Callback callback) {
                String value = editText.getText().toString();
                if (null != callback) callback.callback(value);
                return value;
            }
        }).register("nativeSetInput", new JsBridge.Function() {
            @Override
            public String call(String paramString, JsBridge.Callback callback) {
                editText.setText(paramString);
                return null;
            }
        }).register("alert", new JsBridge.Function() {
            @Override
            public String call(String paramString, JsBridge.Callback callback) {
                new AlertDialog.Builder(MainActivity.this).setTitle("Info").setMessage(paramString).show();
                return null;
            }
        });
        buttonFromJsSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText.setText(jsBridge.callJsSync("jsGetInput", ""));
            }
        });
        buttonFromJsAsync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jsBridge.callJs("jsGetInput", "", new JsBridge.Callback() {
                    @Override
                    public void callback(final String paramString) {
                        editText.setText(paramString);
                    }
                });
            }
        });
        buttonSendToJs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jsBridge.callJs("jsSetInput", editText.getText().toString());
            }
        });
        final Handler handler = new Handler();
        buttonSyncCallTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final long time = SystemClock.uptimeMillis();
                for (int i = 0; i < 10000; ++i) {
                    final int finalI = i;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            jsBridge.callJsSync("jsSetInput", "" + finalI);
                            if (finalI == 10000 - 1) {
                                new AlertDialog.Builder(MainActivity.this).setTitle("Info").setMessage("Spend time: " + (SystemClock.uptimeMillis() - time) + "ms").show();
                            }
                        }
                    });
                }
            }
        });
        buttonAsyncCallTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final long time = SystemClock.uptimeMillis();
                for (int i = 0; i < 10000; ++i) {
                    final int finalI = i;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            jsBridge.callJs("jsSetInput", "" + finalI);
                            if (finalI == 10000 - 1) {
                                new AlertDialog.Builder(MainActivity.this).setTitle("Info").setMessage("Spend time: " + (SystemClock.uptimeMillis() - time) + "ms").show();
                            }
                        }
                    });
                }
            }
        });

        webView.loadUrl("file:///android_asset/demo.html");
    }
}
