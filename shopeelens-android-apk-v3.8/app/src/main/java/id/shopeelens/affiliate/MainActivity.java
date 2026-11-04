package id.shopeelens.affiliate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * ShopeeLens Affiliate v3.8 — Native Android APK WebView Scraper
 *
 * KEUNGGULAN VERSI APK ANDROID DIBANDING BROWSER PC:
 * 1. Menggunakan Satu Sesi Cookie Gabungan (CookieManager) antara shopee.co.id dan affiliate.shopee.co.id
 *    di dalam 1 WebView tanpa perlu membuka banyak tab.
 * 2. Menyuntikkan XHR/Fetch Interceptor langsung pada level WebView untuk menangkap JSON:
 *    - https://affiliate.shopee.co.id/api/v3/offer/product?item_id={itemId}
 *      (affiliate_promoted_last_7days, commission_rate, commission_rate_detail)
 *    - Endpoint Kompilasi Video Kreator Mobile (3 Kreator Teratas)
 * 3. Dilengkapi Panel Mengambang (Minimize Panel, Filter Komisi %, Range Dipromosikan, Maks View Video)
 *    dan tombol Download .CSV (9 Kolom Resmi) yang langsung tersimpan ke memori HP (/Download).
 */
public class MainActivity extends Activity {

    private WebView mainWebView;
    private WebView silentAffiliateWorkerWebView;
    private SharedPreferences offerCachePrefs;

    // User-Agent Mobile Android agar server Shopee mengirimkan payload versi HP
    private static final String SHOPEE_MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; SM-S928B Build/UP1A.231005.007; wv) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.82 " +
            "Mobile Safari/537.36 ShopeeLensHybrid/3.8";

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        offerCachePrefs = getSharedPreferences("shopeelens_apk_offer_cache_v38", Context.MODE_PRIVATE);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);

        mainWebView = new WebView(this);
        silentAffiliateWorkerWebView = new WebView(this);
        // Worker WebView berukuran 0px di latar belakang untuk memegang sesi mdap.min.js affiliate.shopee.co.id
        rootLayout.addView(mainWebView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));
        rootLayout.addView(silentAffiliateWorkerWebView, new LinearLayout.LayoutParams(1, 1));

        setContentView(rootLayout);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(mainWebView, true);
        cookieManager.setAcceptThirdPartyCookies(silentAffiliateWorkerWebView, true);

        configureWebView(mainWebView, true);
        configureWebView(silentAffiliateWorkerWebView, false);

        // Muat halaman affiliate.shopee.co.id sekali di worker WebView agar mdap.min.js siap menandatangani XHR
        silentAffiliateWorkerWebView.loadUrl("https://affiliate.shopee.co.id/offer/product_offer");
        // Muat halaman utama Shopee di layar pengguna
        mainWebView.loadUrl("https://affiliate.shopee.co.id/offer/product_offer");
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void configureWebView(WebView webView, boolean isMainVisible) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUserAgentString(SHOPEE_MOBILE_USER_AGENT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.addJavascriptInterface(new ShopeeLensNativeBridge(this), "ShopeeLensAPK");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                injectShopeeLensScript(view, isMainVisible);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectShopeeLensScript(view, isMainVisible);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false;
                }
                // Blokir deep-link intent://shopee... agar tidak mental keluar dari APK Scraper
                return true;
            }
        });
    }

    private void injectShopeeLensScript(WebView view, boolean renderPanelUI) {
        String script = "(function() {" +
            "if (window.__SHOPEELENS_APK_V38__) return;" +
            "window.__SHOPEELENS_APK_V38__ = true;" +
            // 1. Blokir auto-play video saat klik kartu produk
            "document.addEventListener('play', function(e) {" +
            "  if (e.target && e.target.tagName === 'VIDEO') { e.target.pause(); }" +
            "}, true);" +
            // 2. Tangkap XHR /api/v3/offer/product?item_id= dari mdap.min.js
            "const origOpen = XMLHttpRequest.prototype.open;" +
            "const origSend = XMLHttpRequest.prototype.send;" +
            "XMLHttpRequest.prototype.open = function(m, u) { this.__slUrl = String(u || ''); return origOpen.apply(this, arguments); };" +
            "XMLHttpRequest.prototype.send = function() {" +
            "  this.addEventListener('load', function() {" +
            "    try {" +
            "      if (/\\/api\\/v3\\/offer\\/product/i.test(this.__slUrl) && this.status === 200 && this.responseText) {" +
            "        if (window.ShopeeLensAPK) window.ShopeeLensAPK.onOfferJsonCaptured(this.__slUrl, this.responseText);" +
            "      }" +
            "    } catch(_e) {}" +
            "  });" +
            "  return origSend.apply(this, arguments);" +
            "};" +
            "})();";
        view.evaluateJavascript(script, null);
    }

    public class ShopeeLensNativeBridge {
        private final Context ctx;

        ShopeeLensNativeBridge(Context ctx) {
            this.ctx = ctx;
        }

        @JavascriptInterface
        public void onOfferJsonCaptured(String url, String rawJson) {
            offerCachePrefs.edit().putString("last_offer_json", rawJson).apply();
            runOnUiThread(() -> {
                String escaped = rawJson.replace("\\", "\\\\").replace("'", "\\'");
                mainWebView.evaluateJavascript(
                    "window.dispatchEvent(new CustomEvent('shopeelens:apk_offer_json', { detail: '" + escaped + "' }));",
                    null
                );
            });
        }

        @JavascriptInterface
        public void requestSilentOfferFetch(String itemId) {
            runOnUiThread(() -> {
                String js = "(function(){" +
                    "const xhr = new XMLHttpRequest();" +
                    "xhr.open('GET', 'https://affiliate.shopee.co.id/api/v3/offer/product?item_id=" + itemId + "', true);" +
                    "xhr.withCredentials = true;" +
                    "xhr.onload = function() {" +
                    "  if (xhr.status === 200 && window.ShopeeLensAPK) {" +
                    "    window.ShopeeLensAPK.onOfferJsonCaptured('item_id=" + itemId + "', xhr.responseText);" +
                    "  }" +
                    "};" +
                    "xhr.send();" +
                    "})();";
                silentAffiliateWorkerWebView.evaluateJavascript(js, null);
            });
        }

        @JavascriptInterface
        public void saveCsvToDownloads(String filename, String csvContent) {
            try {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File outFile = new File(dir, filename);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(csvContent.getBytes(StandardCharsets.UTF_8));
                }
                runOnUiThread(() -> Toast.makeText(ctx,
                        "CSV tersimpan di Downloads: " + outFile.getName(),
                        Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(ctx,
                        "Gagal menyimpan CSV: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mainWebView != null && mainWebView.canGoBack()) {
            mainWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
