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
    public void jsCallNative(String message) {
        super.jsCallNative(message);
    }

    @Override
    protected void callJs(String uri) {
        webView.loadUrl(uri);
    }
}
