package ha.excited.jsbridge;

import android.webkit.WebView;

public class WebViewJsBridge extends JsBridge {
    private final WebView webView;

    public WebViewJsBridge(WebView webView, String name) {
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
