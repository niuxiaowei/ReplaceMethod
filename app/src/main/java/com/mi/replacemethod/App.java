package com.mi.replacemethod;

import android.app.Application;


/**
 * create by niuxiaowei
 * date : 21-8-24
 **/
public class App extends Application {

    private static App instance;

    public static App getInstance(){
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
