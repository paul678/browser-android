package com.linkbubble.webrender;


import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.WebView;
import com.linkbubble.util.YouTubeEmbedHelper;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class WebRenderer {

    public interface GetGeolocationCallback {
        public void onAllow();
    }

    public interface Controller {
        public boolean shouldOverrideUrlLoading(String urlAsString, boolean viaUserInput);
        public void onReceivedError();
        public void onPageStarted(String urlAsString, Bitmap favIcon);
        public void onPageFinished(String urlAsString);
        public void onDownloadStart(String urlAsString);
        public void onReceivedTitle(String url, String title);
        public void onReceivedIcon(Bitmap bitmap);
        public void onProgressChanged(int progress, String urlAsString);
        public boolean onBackPressed();
        public void onUrlLongClick(String url);
        public void onShowBrowserPrompt();
        public void onCloseWindow();
        public void onGeolocationPermissionsShowPrompt(String origin, GetGeolocationCallback callback);
        public int getPageInspectFlags();
        public void onPageInspectorYouTubeEmbedFound();
        public void onPageInspectorTouchIconLoaded(Bitmap bitmap, String pageUrl);
        public void onPageInspectorDropDownWarningClick();
    }

    public enum Type {
        Stub,
        WebView,
    };

    public static WebRenderer create(Type type, Context context, Controller controller, View webRendererPlaceholder, String TAG) {
        switch (type) {
            case Stub:
                return new StubRenderer(context, controller, webRendererPlaceholder, TAG);

            case WebView:
                return new WebViewRenderer(context, controller, webRendererPlaceholder, TAG);
        }

        throw new IllegalArgumentException("Invalid type");
    }

    protected URL mUrl;

    WebRenderer(Context context, Controller controller, View webRendererPlaceholder) {
        super();
    }

    public abstract void destroy();
    
    public abstract View getView();

    public abstract void updateIncognitoMode(boolean incognito);

    public abstract void loadUrl(String urlAsString);

    public abstract void reload();

    public abstract void stopLoading();

    public abstract void hidePopups();

    public abstract void resetPageInspector();

    public abstract void runPageInspector();

    public abstract YouTubeEmbedHelper getPageInspectorYouTubeEmbedHelper();

    public void onPageLoadComplete() {}

    public URL getUrl() {
        return mUrl;
    }

    public void setUrl(String urlAsString) throws MalformedURLException {
        mUrl = new URL(urlAsString);
    }

    public void setUrl(URL url) {
        mUrl = url;
    }
}