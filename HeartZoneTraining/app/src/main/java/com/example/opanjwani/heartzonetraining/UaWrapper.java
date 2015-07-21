package com.example.opanjwani.heartzonetraining;

import android.content.Context;

import com.ua.sdk.Ua;
import com.ua.sdk.internal.UaProviderImpl;
import com.ua.sdk.internal.net.v7.UrlBuilderImpl;

public class UaWrapper {

    private Ua ua;

    private static UaWrapper instance;

    private UaWrapper() {
    }

    public static UaWrapper getInstance() {
        if (instance == null) {
            instance = new UaWrapper();
        }

        return instance;
    }

    public void init(Context context) {

        ua = Ua.getBuilder()
                .setClientId(BuildConfig.CLIENT_KEY)
                .setClientSecret(BuildConfig.CLIENT_SECRET)
                .setContext(context)
                .setProvider(
                        new UaProviderImpl(BuildConfig.CLIENT_KEY, BuildConfig.CLIENT_SECRET, context, false) {
                            @Override
                            public UrlBuilderImpl getUrlBuilder() {
                                return new UrlBuilderImpl();
                            }
                        }
                )
                .setDebug(true)
                .build();
    }

    public Ua getUa() {
        return ua;
    }
}
