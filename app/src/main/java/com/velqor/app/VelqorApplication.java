package com.velqor.app;

import android.app.Application;

public class VelqorApplication extends Application {
    private static VelqorApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    public static VelqorApplication getInstance() {
        return sInstance;
    }
}
