package com.ludofriends.game;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView gameView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().setNavigationBarColor(Color.rgb(8, 17, 31));

        gameView = new WebView(this);
        gameView.setBackgroundColor(Color.rgb(8, 17, 31));
        gameView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        WebSettings settings = gameView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);

        gameView.setWebViewClient(new WebViewClient());
        gameView.setWebChromeClient(new WebChromeClient());
        gameView.addJavascriptInterface(new VibrationBridge(this), "AndroidVibration");
        setContentView(gameView);

        enterImmersiveMode();
        gameView.loadUrl("file:///android_asset/index.html");
    }

    private void enterImmersiveMode() {
        gameView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && gameView != null) enterImmersiveMode();
    }

    @Override
    protected void onPause() {
        if (gameView != null) gameView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) gameView.onResume();
        enterImmersiveMode();
    }


    private static class VibrationBridge {
        private final Context context;

        VibrationBridge(Context context) {
            this.context = context.getApplicationContext();
        }

        private Vibrator vibrator() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager manager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                return manager != null ? manager.getDefaultVibrator() : null;
            }
            return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        @JavascriptInterface
        public void vibrate(long milliseconds) {
            Vibrator vibrator = vibrator();
            if (vibrator == null || !vibrator.hasVibrator() || milliseconds <= 0) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(milliseconds);
            }
        }

        @JavascriptInterface
        public void vibratePattern(String csv) {
            if (csv == null || csv.trim().isEmpty()) return;
            String[] parts = csv.split(",");
            // JavaScript's vibration pattern starts with vibration duration. Android's
            // waveform starts with an initial delay, so prepend a zero-delay slot.
            long[] pattern = new long[parts.length + 1];
            pattern[0] = 0;
            for (int i = 0; i < parts.length; i++) {
                try { pattern[i + 1] = Math.max(0, Long.parseLong(parts[i].trim())); }
                catch (NumberFormatException e) { pattern[i + 1] = 0; }
            }
            Vibrator vibrator = vibrator();
            if (vibrator == null || !vibrator.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }

        @JavascriptInterface
        public void cancel() {
            Vibrator vibrator = vibrator();
            if (vibrator != null) vibrator.cancel();
        }
    }

    @Override
    public void onBackPressed() {
        if (gameView != null && gameView.canGoBack()) {
            gameView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (gameView != null) {
            gameView.stopLoading();
            gameView.destroy();
        }
        super.onDestroy();
    }
}
