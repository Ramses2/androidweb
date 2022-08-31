package com.example.webview;

import androidx.appcompat.app.AppCompatActivity;

import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            WebView webView = (WebView) findViewById(R.id.webView);
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            if (webView != null) {
                // Get cert from raw resource...
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = getResources().openRawResource(R.raw.rootca); // stored at \app\src\main\res\raw
                final Certificate certificate = cf.generateCertificate(caInput);
                caInput.close();

                String url = "http://192.168.2.24:3000";
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                        // Get cert from SslError
                        SslCertificate sslCertificate = error.getCertificate();
                        Certificate cert = getX509Certificate(sslCertificate);
                        if (cert != null && certificate != null){
                            try {
                                // Reference: https://developer.android.com/reference/java/security/cert/Certificate.html#verify(java.security.PublicKey)
                                cert.verify(certificate.getPublicKey()); // Verify here...
                                handler.proceed();
                            } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
                                super.onReceivedSslError(view, handler, error);
                                e.printStackTrace();
                            }
                        } else {
                            super.onReceivedSslError(view, handler, error);
                        }
                    }
                });

                webView.loadUrl(url);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // credits to @Heath Borders at http://stackoverflow.com/questions/20228800/how-do-i-validate-an-android-net-http-sslcertificate-with-an-x509trustmanager
    private Certificate getX509Certificate(SslCertificate sslCertificate){
        Bundle bundle = SslCertificate.saveState(sslCertificate);
        byte[] bytes = bundle.getByteArray("x509-certificate");
        if (bytes == null) {
            return null;
        } else {
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                return certFactory.generateCertificate(new ByteArrayInputStream(bytes));
            } catch (CertificateException e) {
                return null;
            }
        }
    }
}