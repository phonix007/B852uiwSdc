package com.msbte.modelanswerpaper;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.Task;
import com.ironsource.mediationsdk.IronSource;

import java.io.File;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    String websiteURL;
    private WebView webview;
    private ProgressBar progressBar;

    private long backPressedTime;
    private Toast backToast;
    private int checkad;

    ReviewManager manager;
    ReviewInfo reviewInfo;


    private InterstitialAd mInterstitialAd;
    private AdView mAdView;
    private RewardedAd mRewardedAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        loadreward();
        loadinter();


        mAdView = findViewById(R.id.adView2_main);
        AdRequest adRequest = new AdRequest.Builder().build();
        if (mAdView != null) {
            mAdView.loadAd(adRequest);
        }

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
            }

            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                // Code to be executed when an ad request fails.
                super.onAdFailedToLoad(adError);
                mAdView.loadAd(adRequest);
            }

        });

        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.i(TAG, loadAdError.getMessage());
                        mInterstitialAd = null;
                    }
                });


        websiteURL = getIntent().getStringExtra("url");
        Intent intent = getIntent();
        int check = intent.getIntExtra("ad", 0);
        checkad = check;
        Toast.makeText(this, "Value is" + checkad + check, Toast.LENGTH_SHORT).show();

        progressBar = findViewById(R.id.progress_bar);
        progressBar.setMax(100);
        progressBar.getProgressDrawable().setColorFilter(
                Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);

        netcheck();

        if (!CheckNetwork.isInternetAvailable(this)) //returns true if internet available
        {
            //if there is no internet do this
            setContentView(R.layout.activity_main);
            //Toast.makeText(this,"No Internet Connection, Chris",Toast.LENGTH_LONG).show();
            new AlertDialog.Builder(this) //alert the person knowing they are about to close
                    .setTitle("No internet connection available")
                    .setMessage("Please Check you're Mobile data or Wifi network.")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    //.setNegativeButton("No", null)
                    .show();

        } else {
            //Webview stuff
            webview = findViewById(R.id.webView);
            webview.getSettings().setJavaScriptEnabled(true);
            webview.getSettings().setDomStorageEnabled(true);
            webview.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
            webview.loadUrl(websiteURL);
            netcheck();
            progressBar.setProgress(0);
            webview.setWebViewClient(new WebViewClientDemo());
            netcheck();

            webview.setWebChromeClient(new WebChromeClient() {

                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    progressBar.setProgress(newProgress);
                    if (newProgress == 100)
                        progressBar.setVisibility(View.GONE);
                    else
                        progressBar.setVisibility(View.VISIBLE);
                    netcheck();
                    super.onProgressChanged(view, newProgress);
                }
            });
            netcheck();


        }

        //Swipe to refresh functionality


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {

                Log.d("permission", "permission denied to WRITE_EXTERNAL_STORAGE - requesting it");
                String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permissions, 1);
            }
        }

        netcheck();

//handle downloading
        try {
            webview.setDownloadListener(new DownloadListener() {
                @Override
                public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {

                    Toast.makeText(getApplicationContext(), "File will download after watching video Ad", Toast.LENGTH_LONG).show();
//                    showvideoad();
//                    if (checkad == 10) {
                    if (mRewardedAd != null) {
                        mRewardedAd.show(MainActivity.this, new OnUserEarnedRewardListener() {
                            @Override
                            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                                checkad = rewardItem.getAmount();
                                String rewardType = rewardItem.getType();
//                                downloding();
//                                checkad = 10;
//                    Toast.makeText(getApplicationContext(), "Press Download Again to Download file", Toast.LENGTH_LONG).show();
                                String currentTime = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
                                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                                request.setMimeType(mimeType);
                                String cookies = CookieManager.getInstance().getCookie(url);
                                String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                                request.addRequestHeader("cookie", cookies);
                                request.addRequestHeader("User-Agent", userAgent);
                                request.setDescription("Saved On Storage/Downloads/");
                                request.setTitle("Maha_360_App_" + currentTime + URLUtil.guessFileName(url, contentDisposition, mimeType));
                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MSBTE Solution" + currentTime + URLUtil.guessFileName(url, contentDisposition, mimeType));
                                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                dm.enqueue(request);
                                Toast.makeText(getApplicationContext(), "Downloading File...", Toast.LENGTH_LONG).show();

                            }
                        });

                        mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent();


                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when ad fails to show.
                                Log.d(TAG, "Ad failed to show.");

                                if (mInterstitialAd != null) {
                                    mInterstitialAd.show(MainActivity.this);
                                } else {
                                    Log.d("TAG", "The interstitial ad wasn't ready yet.");
                                }
                                loadinter();
                                loadreward();

                                String currentTime = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
                                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                                request.setMimeType(mimeType);
                                String cookies = CookieManager.getInstance().getCookie(url);
                                String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                                request.addRequestHeader("cookie", cookies);
                                request.addRequestHeader("User-Agent", userAgent);
                                request.setDescription("Saved On Storage/Downloads/");
                                request.setTitle("Maha_360_App_" + currentTime + URLUtil.guessFileName(url, contentDisposition, mimeType));
                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MSBTE Solution" + currentTime + URLUtil.guessFileName(url, contentDisposition, mimeType));
                                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                dm.enqueue(request);
                                Toast.makeText(getApplicationContext(), "Downloading File...", Toast.LENGTH_LONG).show();
                            }

                        });

                    }

//                        String currentTime = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
//
//                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
//                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
//                        request.setMimeType(mimeType);
//                        String cookies = CookieManager.getInstance().getCookie(url);
//                        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
//                        request.addRequestHeader("cookie", cookies);
//                        request.addRequestHeader("User-Agent", userAgent);
//                        request.setDescription("Saved On Storage/Downloads/");
//                        request.setTitle("Maha_360_App_" + currentTime + URLUtil.guessFileName(url, contentDisposition, mimeType));
//                        request.allowScanningByMediaScanner();
//                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MSBTE Solution" + currentTime + URLUtil.guessFileName(url, contentDisposition, mimeType));
//                        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
//                        dm.enqueue(request);
//                        Toast.makeText(getApplicationContext(), "Downloading File...", Toast.LENGTH_LONG).show();

//                        if (mInterstitialAd != null) {
//                            mInterstitialAd.show(MainActivity.this);
//                        } else {
//                            Log.d("TAG", "The interstitial ad wasn't ready yet.");
//                        }
//                        loadinter();

//                    } else {
//
//                        netcheck();
//                        new AlertDialog.Builder(MainActivity.this) //alert the person knowing they are about to close
//                                .setTitle("Unlimited Downloading Over!")
//                                .setMessage("Every Time you need to watch an Ad Because \uD835\uDC18\uD835\uDC28\uD835\uDC2E\uD835\uDC2B \uD835\uDC14\uD835\uDC27\uD835\uDC25\uD835\uDC22\uD835\uDC26\uD835\uDC2D\uD835\uDC1E\uD835\uDC1D \uD835\uDC1D\uD835\uDC28\uD835\uDC30\uD835\uDC27\uD835\uDC25\uD835\uDC28\uD835\uDC1A\uD835\uDC1D\uD835\uDC22\uD835\uDC27\uD835\uDC20 \uD835\uDC22\uD835\uDC2C \uD835\uDC28\uD835\uDC2F\uD835\uDC1E\uD835\uDC2B Please, go to \uD835\uDC07\uD835\uDC28\uD835\uDC26\uD835\uDC1E \uD835\uDC12\uD835\uDC1C\uD835\uDC2B\uD835\uDC1E\uD835\uDC1E\uD835\uDC27 \uD835\uDC1A\uD835\uDC27\uD835\uDC1D \uD835\uDC00\uD835\uDC1C\uD835\uDC2D\uD835\uDC22\uD835\uDC2F\uD835\uDC1A\uD835\uDC2D\uD835\uDC1E \uD835\uDC14\uD835\uDC27\uD835\uDC25\uD835\uDC22\uD835\uDC26\uD835\uDC22\uD835\uDC2D\uD835\uDC1E\uD835\uDC1D \uD835\uDC03\uD835\uDC28\uD835\uDC30\uD835\uDC27\uD835\uDC25\uD835\uDC28\uD835\uDC1A\uD835\uDC1D\uD835\uDC22\uD835\uDC27\uD835\uDC20...")
//                                .setPositiveButton("Watch Ad", new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//
//                                        Toast.makeText(MainActivity.this, "Ad is Loading Please wait a movement... ", Toast.LENGTH_LONG).show();
//                                        showvideoad();
//                                    }
//                                })
//                                //.setNegativeButton("No", null)
//                                .show();
//
//                    }
//
                }
            });

        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Something Went Wrong!", Toast.LENGTH_LONG).show();
            Toast.makeText(MainActivity.this, "Please Check Your Internet Connection...", Toast.LENGTH_LONG).show();
        }
        netcheck();
    }

    private void loadreward() {

        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917",
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error.

                        mRewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        mRewardedAd = rewardedAd;

                    }
                });


    }

    private void loadinter() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Toast.makeText(MainActivity.this, "Error is" + loadAdError, Toast.LENGTH_LONG).show();
                        Toast.makeText(MainActivity.this, "Error is" + loadAdError, Toast.LENGTH_LONG).show();

                        Toast.makeText(MainActivity.this, "Error is" + loadAdError, Toast.LENGTH_LONG).show();

                        // Handle the error
                        Log.i(TAG, loadAdError.getMessage());
                        mInterstitialAd = null;

                    }
                });


    }

    private void showvideoad() {

        if (mRewardedAd != null) {
            mRewardedAd.show(MainActivity.this, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    downloding();
                    checkad = 10;
//                    Toast.makeText(getApplicationContext(), "Press Download Again to Download file", Toast.LENGTH_LONG).show();


                }
            });

            mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();


                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    // Called when ad fails to show.
                    Log.d(TAG, "Ad failed to show.");
                }

            });

        }

    }

    private void downloding() {
        try {
            webview.setDownloadListener(new DownloadListener() {
                @Override
                public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {


                    String currentTime = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());

                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                    request.setMimeType(mimeType);
                    String cookies = CookieManager.getInstance().getCookie(url);
                    String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setDescription("Saved On Storage/Downloads/");
                    request.setTitle("Maha_360_App_" + currentTime + URLUtil.guessFileName(url, contentDisposition, mimeType));
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MSBTE Solution" + currentTime + URLUtil.guessFileName(url, contentDisposition, mimeType));
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                    Toast.makeText(getApplicationContext(), "Downloading File...", Toast.LENGTH_LONG).show();

//                        if (mInterstitialAd != null) {
//                            mInterstitialAd.show(MainActivity.this);
//                        } else {
//                            Log.d("TAG", "The interstitial ad wasn't ready yet.");
//                        }
//                        loadinter();


                }
            });

        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Something Went Wrong!", Toast.LENGTH_LONG).show();
            Toast.makeText(MainActivity.this, "Please Check Your Internet Connection...", Toast.LENGTH_LONG).show();
        }
    }


    private class WebViewClientDemo extends WebViewClient {
        @Override
        //Keep webview in app when clicking links
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }
    }

    //set back button functionality
    @Override
    public void onBackPressed() { //if user presses the back button do this
        if (webview.isFocused() && webview.canGoBack()) { //check if in webview and the user can go back
            webview.goBack(); //go back in webview
        } else { //do this if the webview cannot go back any further

            // review
            manager = ReviewManagerFactory.create(MainActivity.this);
            Task<ReviewInfo> request1 = manager.requestReviewFlow();
            request1.addOnCompleteListener(new OnCompleteListener<ReviewInfo>() {
                @Override
                public void onComplete(@NonNull Task<ReviewInfo> task) {

                    if (task.isSuccessful()) {
                        reviewInfo = task.getResult();
                        Task<Void> flow = manager.launchReviewFlow(MainActivity.this, reviewInfo);

                        flow.addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void result) {

                            }
                        });
                    } else {
                        Toast.makeText(MainActivity.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            // review end


            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                backToast.cancel();
                super.onBackPressed();
                return;
            } else {
                backToast = Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT);
                backToast.show();
            }
            backPressedTime = System.currentTimeMillis();
        }

    }

    private void netcheck() {

        if (!CheckNetwork.isInternetAvailable(this)) //returns true if internet available
        {

            new AlertDialog.Builder(this) //alert the person knowing they are about to close
                    .setTitle("No internet connection available")
                    .setMessage("Please Check you're Mobile data or Wifi network.")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            webview.setVisibility(View.INVISIBLE);
                            finish();
                        }
                    })
                    //.setNegativeButton("No", null)
                    .show();
        }

    }

    protected void onResume() {
        super.onResume();
        IronSource.onResume(this);
    }

    protected void onPause() {
        super.onPause();
        IronSource.onPause(this);
    }
}


class CheckNetwork {

    private static final String TAG = CheckNetwork.class.getSimpleName();

    public static boolean isInternetAvailable(Context context) {
        NetworkInfo info = (NetworkInfo) ((ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (info == null) {
            Log.d(TAG, "no internet connection");
            return false;
        } else {
            if (info.isConnected()) {
                Log.d(TAG, " internet connection available...");
                return true;
            } else {
                Log.d(TAG, " internet connection");
                return true;
            }

        }
    }
}