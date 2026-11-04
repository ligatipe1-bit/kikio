# Panduan Menjalankan ShopeeLens Affiliate v3.8 di HP Android (Real-Time Scraper)

Ada **2 metode terbaik** untuk menjalankan seluruh fitur ShopeeLens v3.8 di HP Android Anda:

---

## CARA 1 (PALING CEPAT — LANGSUNG JALAN DI HP DALAM 1 MENIT TANPA CODING)
Gunakan browser Android yang mendukung Chrome Extension Manifest V3 (**Mises Browser** atau **Kiwi Browser** di Play Store):
1. Install **Mises Browser** atau **Kiwi Browser** dari Google Play Store di HP Android Anda.
2. Download file **`shopeelens-affiliate-extension-v3.8.zip`** dari aplikasi ini ke HP Anda.
3. Buka Mises Browser / Kiwi Browser -> ketik `chrome://extensions` -> aktifkan **Developer mode**.
4. Klik tombol **`+ (from .zip / .crx / .user.js)`** lalu pilih langsung file **`shopeelens-affiliate-extension-v3.8.zip`** (tidak perlu diekstrak).
5. Buka 1 tab `https://affiliate.shopee.co.id` dan login ke akun Affiliate Anda, lalu buka halaman pencarian / produk Shopee di tab berikutnya. Semua fitur (Rincian Komisi `product_offer`, Berapa Kali Dipromosikan, View 3 Kreator Teratas, Minimize Panel, dan Export `.CSV`) langsung berjalan di HP!

---

## CARA 2 (KOMPILASI MENJADI FILE NATIVE `.APK` MANDIRI)
Paket ini sudah berisi proyek Android Studio lengkap dengan **Dual-WebView (`MainActivity.java`)**:
- **WebView #1 (Layar Utama)**: Untuk Anda melakukan pencarian produk dan melihat panel ShopeeLens.
- **WebView #2 (Background Worker 1x1px)**: Memegang sesi `affiliate.shopee.co.id` (`mdap.min.js`) secara hening di dalam aplikasi sehingga tidak perlu membuka tab baru sama sekali.

### Langkah Build `.APK` Otomatis via GitHub (Tanpa Install Android Studio):
1. Ekstrak file `shopeelens-android-apk-v3.8.zip` ini lalu upload ke repository GitHub Anda.
2. Buka menu **Actions** di GitHub -> klik workflow **Build ShopeeLens Affiliate Android APK (v3.8)** -> klik **Run workflow**.
3. Tunggu ~2 menit, lalu unduh file **`ShopeeLens-Affiliate-v3.8-Android-APK.zip`** yang berisi file `.apk` siap di-install di HP Android Anda.
