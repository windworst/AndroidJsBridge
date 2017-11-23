package ha.excited.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

import ha.excited.jsbridge.JsBridge;
import ha.excited.jsbridge.R;
import ha.excited.jsbridge.WebViewJsBridge;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WebView webView = (WebView) findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        final EditText editText = (EditText) findViewById(R.id.editText);
        final Button buttonFromJs = (Button) findViewById(R.id.buttonFromJs);
        final Button buttonSendToJs = (Button) findViewById(R.id.buttonSendToJs);

        final JsBridge webViewJsBridge = new WebViewJsBridge(webView, "jsBridge").register("nativeGetInput", new JsBridge.Function() {
            @Override
            public String call(String paramString, JsBridge.Callback callback) {
                String value = editText.getText().toString();
                if(null != callback) callback.callback(value);
                return value;
            }
        }).register("nativeSetInput", new JsBridge.Function() {
            @Override
            public String call(String paramString, JsBridge.Callback callback) {
                editText.setText(paramString);
                return null;
            }
        });

        buttonFromJs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webViewJsBridge.evalJs("jsGetInput", "", new JsBridge.Callback() {
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
                webViewJsBridge.evalJs("jsSetInput", editText.getText().toString());
            }
        });

        webView.loadUrl("file:///android_asset/demo.html");
    }
}
