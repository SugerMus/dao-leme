package cool.dao.leme;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final int REQUEST_CREATE_BACKUP = 100;
    private static final int REQUEST_IMPORT_BACKUP = 101;
    private WebView webView;
    private ValueCallback<Uri[]> fileChooserCallback;
    private String pendingBackup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                fileChooserCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(intent, REQUEST_IMPORT_BACKUP);
                return true;
            }
        });
        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMPORT_BACKUP && fileChooserCallback != null) {
            fileChooserCallback.onReceiveValue(resultCode == RESULT_OK && data != null ? new Uri[]{data.getData()} : null);
            fileChooserCallback = null;
        }
        if (requestCode == REQUEST_CREATE_BACKUP && resultCode == RESULT_OK && data != null && pendingBackup != null) {
            try {
                getContentResolver().openOutputStream(data.getData()).write(pendingBackup.getBytes("UTF-8"));
            } catch (Exception ignored) { }
            pendingBackup = null;
        }
    }

    private class AndroidBridge {
        @JavascriptInterface
        public void updateReminder(boolean enabled, String time) {
            runOnUiThread(() -> ReminderReceiver.updateSchedule(MainActivity.this, enabled, time));
        }

        @JavascriptInterface
        public void updateTodayRecord(String date, boolean recorded) {
            getSharedPreferences("dao_leme", MODE_PRIVATE).edit().putString("recorded_date", date).putBoolean("recorded", recorded).apply();
        }

        @JavascriptInterface
        public void exportBackup(String json) {
            runOnUiThread(() -> {
                pendingBackup = json;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "DAO_Leme_backup.json");
                startActivityForResult(intent, REQUEST_CREATE_BACKUP);
            });
        }
    }
}
