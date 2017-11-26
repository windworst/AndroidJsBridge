package ha.excited.jsbridge;

import android.webkit.WebView;

public class WebViewAdapter extends JsBridge.Adapter {
    private final WebView webView;

    public WebViewAdapter(String name, WebView webView) {
        super(name);
        this.webView = webView;
        webView.addJavascriptInterface(this, exposeName());
    }

    @android.webkit.JavascriptInterface
    @Override
    public String jsCallNative(String message) {
        return super.jsCallNative(message);
    }

    @Override
    protected void evalJs(String jsCode) {
        webView.loadUrl(jsCode);
    }
}
