package com.example.opanjwani.heartzonetraining;

import android.app.Application;

public class BaseApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();

        UaWrapper uaWrapper = UaWrapper.getInstance();
        uaWrapper.init(getApplicationContext());
        uaWrapper.getUa().getAuthenticationManager();

    }
}
