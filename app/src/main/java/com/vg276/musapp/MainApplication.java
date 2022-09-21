package com.vg276.musapp;

import android.support.multidex.MultiDexApplication;

public class MainApplication extends MultiDexApplication
{
    private static MainApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static MainApplication getInstance() {
        return instance;
    }
}
