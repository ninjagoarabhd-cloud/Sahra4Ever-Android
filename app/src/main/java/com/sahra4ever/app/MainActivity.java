package com.sahra4ever.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    // Your live website. The app only loads it; it never changes it.
    private static final String HOME_URL = "https://sahra4ever.vercel.app/";

    // Hosts that stay inside the app. Add more here if your site uses other
    // domains for its own pages.
    private static final String[] ALLOWED_HOSTS = { "sahra4ever.vercel.app" };

    private FrameLayout root;
    private WebView webView;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = new FrameLayout(this);
        setContentView(root);
        createWebView();
        boolean restored = false;
        if (savedInstanceState != null) {
            restored = webView.restoreState(savedInstanceState) != null;
        }
        if (!restored) {
            webView.loadUrl(HOME_URL);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void createWebView() {
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF0B0B12);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setTextZoom(100); // keep the site's design regardless of system font size
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportMultipleWindows(false);          // blocks popup windows
        s.setJavaScriptCanOpenWindowsAutomatically(false);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true); // needed by many embedded players

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Never interfere with iframes (server selection / players).
                if (!request.isForMainFrame()) return false;

                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("about".equals(scheme) || "data".equals(scheme)) return false;

                if (("https".equals(scheme) || "http".equals(scheme)) && isAllowedHost(uri)) {
                    return false; // stay in the app
                }

                // Anything else: only open outside if the user actually tapped it.
                // Automatic redirects (typical of ad scripts) are silently blocked.
                if (request.hasGesture()
                        && ("https".equals(scheme) || "http".equals(scheme)
                        || "mailto".equals(scheme) || "tel".equals(scheme))) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) { }
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    showOfflinePage(view);
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                root.removeView(webView);
                webView.destroy();
                createWebView();
                webView.loadUrl(HOME_URL);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                webView.setVisibility(View.GONE);
                root.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                enterImmersive();
            }

            @Override
            public void onHideCustomView() {
                exitFullscreen();
            }
        });
    }

    private boolean isAllowedHost(Uri uri) {
        String host = uri.getHost();
        if (host == null) return false;
        for (String h : ALLOWED_HOSTS) {
            if (h.equalsIgnoreCase(host)) return true;
        }
        return false;
    }

    private void showOfflinePage(WebView view) {
        String html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
                + "<body dir='rtl' style='background:#0b0b12;color:#fff;font-family:sans-serif;"
                + "text-align:center;padding:25vh 24px 0'>"
                + "<h2>\u0644\u0627 \u064a\u0648\u062c\u062f \u0627\u062a\u0635\u0627\u0644 \u0628\u0627\u0644\u0625\u0646\u062a\u0631\u0646\u062a</h2>"
                + "<p>\u062a\u062d\u0642\u0642 \u0645\u0646 \u0627\u0644\u0627\u062a\u0635\u0627\u0644 \u062b\u0645 \u062d\u0627\u0648\u0644 \u0645\u0631\u0629 \u0623\u062e\u0631\u0649.</p>"
                + "<p><a href='" + HOME_URL + "' style='display:inline-block;padding:12px 28px;"
                + "background:#e50914;color:#fff;border-radius:8px;text-decoration:none'>"
                + "\u0625\u0639\u0627\u062f\u0629 \u0627\u0644\u0645\u062d\u0627\u0648\u0644\u0629</a></p>"
                + "</body></html>";
        view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    @SuppressWarnings("deprecation")
    private void enterImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @SuppressWarnings("deprecation")
    private void exitFullscreen() {
        if (customView == null) return;
        root.removeView(customView);
        customView = null;
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        webView.setVisibility(View.VISIBLE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (customView != null) {
            exitFullscreen();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        root.removeAllViews();
        webView.destroy();
        super.onDestroy();
    }
}
