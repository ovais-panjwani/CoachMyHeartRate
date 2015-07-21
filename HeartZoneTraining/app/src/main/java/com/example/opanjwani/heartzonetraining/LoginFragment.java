package com.example.opanjwani.heartzonetraining;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ua.sdk.UaLog;


public class LoginFragment extends BaseFragment {

    private String authorizationUrl;
    private WebView authorizationWebView;
    private Listener listener;

    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    @Override
    protected int getTitleId() {
        return R.string.title_login;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (Listener) activity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_login, container, false);
        authorizationWebView = (WebView) view.findViewById(R.id.oauth2AuthorizationWebView);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        authorizationWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(getString(R.string.intent_scheme))) {
                    // parse URL & log in with authorization code
                    UaLog.debug("Got url: %s", url);
                    Uri uri = Uri.parse(url);
                    String code = uri.getQueryParameter("code");
                    if (code != null) {
                        listener.onReceivedAuthorizationCode(code);
                    }
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });
        WebSettings settings = authorizationWebView.getSettings();
        settings.setJavaScriptEnabled(true);

        if (authorizationUrl != null) {
            UaLog.debug("Loading WebView with authorizationUrl: %s", authorizationUrl);
            authorizationWebView.loadUrl(authorizationUrl);
        }
    }

    public interface Listener {
        void onReceivedAuthorizationCode(String authorizationCode);
    }
}
