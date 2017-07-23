
package dev.alshikh.speedtwitter;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;
import android.widget.Toast;


import im.delight.android.webview.AdvancedWebView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.NativeExpressAdView;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends Activity implements AdvancedWebView.Listener {
    //the main webView where is shown twitter
    private AdvancedWebView webViewTwitter;

    //object to show full screen videos
    private WebChromeClient myWebChromeClient;
    private FrameLayout mTargetView;
    private FrameLayout mContentView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private View mCustomView;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);


        // setup the webView
        webViewTwitter = (AdvancedWebView) findViewById(R.id.webView);

        webViewTwitter.setListener(this, this);
        webViewTwitter.addPermittedHostname("mobile.twitter.com");
        webViewTwitter.addPermittedHostname("twitter.com");

        //full screen video
        myWebChromeClient = new WebChromeClient(){
            //this custom WebChromeClient allow to show video on full screen
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                mCustomViewCallback = callback;
                mTargetView.addView(view);
                mCustomView = view;
                mContentView.setVisibility(View.GONE);
                mTargetView.setVisibility(View.VISIBLE);
                mTargetView.bringToFront();
            }

            @Override
            public void onHideCustomView() {
                if (mCustomView == null)
                    return;

                mCustomView.setVisibility(View.GONE);
                mTargetView.removeView(mCustomView);
                mCustomView = null;
                mTargetView.setVisibility(View.GONE);
                mCustomViewCallback.onCustomViewHidden();
                mContentView.setVisibility(View.VISIBLE);
            }
        };
        webViewTwitter.setWebChromeClient(myWebChromeClient);
        mContentView = (FrameLayout) findViewById(R.id.main_content);
        mTargetView = (FrameLayout) findViewById(R.id.target_view);


        String urlSharer = ExternalLinkListener();//get the external shared link (if it exists)
        if (urlSharer != null) {//if is a share request
            webViewTwitter.loadUrl(urlSharer);//load the sharer url
        } else {
            webViewTwitter.loadUrl(getString(R.string.urlTwitterMobile));//load homepage
        }


        final NativeExpressAdView adView = (NativeExpressAdView)findViewById(R.id.adView);

        // Set an AdListener for the AdView, so the Activity can take action when an ad has finished
        // loading.
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {

                adView.setVisibility(View.VISIBLE);


            }
        });
        adView.loadAd(new AdRequest.Builder().build());



    }

    // app is already running and gets a new intent (used to share link without open another activity
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        webViewTwitter.loadUrl(ExternalLinkListener());
    }

    private String ExternalLinkListener() {
        // grab an url if opened by clicking a link
        String webViewUrl = getIntent().getDataString();
        /** get a subject and text and check if this is a link trying to be shared */
        String sharedSubject = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        String sharedUrl = getIntent().getStringExtra(Intent.EXTRA_TEXT);

        // if we have a valid URL that was shared by us, open the sharer
        if (sharedUrl != null) {
            if (!sharedUrl.equals("")) {
                Log.e("Info", "sharedUrl != null");
                // check if the URL being shared is a proper web URL
                if (!sharedUrl.startsWith("http://") || !sharedUrl.startsWith("https://")) {
                    // if it's not, let's see if it includes an URL in it (prefixed with a message)
                    int startUrlIndex = sharedUrl.indexOf("http:");
                    if (startUrlIndex > 0) {
                        // seems like it's prefixed with a message, let's trim the start and get the URL only
                        sharedUrl = sharedUrl.substring(startUrlIndex);
                    }
                }
                // final step, set the proper Sharer...
                webViewUrl = String.format("https://twitter.com/intent/tweet?text=%s&url=%s", sharedSubject, sharedUrl);
                // ... and parse it just in case
                webViewUrl = Uri.parse(webViewUrl).toString();
            }
        }
        return webViewUrl;
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        webViewTwitter.onResume();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        webViewTwitter.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.e("Info", "onDestroy()");
        webViewTwitter.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        webViewTwitter.onActivityResult(requestCode, resultCode, intent);
    }

    //*********************** WebView methods ****************************

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
    }

    @Override
    public void onPageFinished(String url) {
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
        String summary = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /></head><body><h1 " +
                "style='text-align:center; padding-top:15%;'>" + getString(R.string.titleNoConnection) + "</h1> <h3 style='text-align:center; padding-top:1%; font-style: italic;'>" + getString(R.string.descriptionNoConnection) + "</h3>  <h5 style='text-align:center; padding-top:80%; opacity: 0.3;'>" + getString(R.string.awards) + "</h5></body></html>";
        webViewTwitter.loadData(summary, "text/html; charset=utf-8", "utf-8");//load a custom html page
    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {

    }

    @Override
    public void onExternalPageRequest(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }


    //*********************** BUTTON ****************************
    // handling the back button
    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (mCustomView != null) {
            myWebChromeClient.onHideCustomView();//hide video player
        } else {
            if (webViewTwitter.canGoBack()) {
                webViewTwitter.goBack();
            } else {
                if (doubleBackToExitPressedOnce) {
                    super.onBackPressed();
                    return;
                }
                this.doubleBackToExitPressedOnce = true;
                Toast.makeText(this,"Please click BACK again to exit", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        doubleBackToExitPressedOnce=false;
                    }
                }, 2000);
            }
        }
    }






}