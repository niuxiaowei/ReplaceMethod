package com.mi.replacemethod;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class MainActivity extends Activity {

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View view = inflate(R.layout.activity_second, null);
        Log.i("Main", "oncreate");
        getRunningTasks();

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates("gps", 1000l, 2.0f, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {

            }
        });
    }

    View inflate(int id, ViewGroup parentView) {
        Log.i("Main", "inflate");
        return getLayoutInflater().inflate(id, parentView);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void getRunningTasks() {
        ActivityManager activityManager = getSystemService(ActivityManager.class);
        activityManager.getRunningTasks(10);
    }

//    private String deleteGenericsClass(String oriDesc) {
//        int startIndex = oriDesc.indexOf("<");
//        int endIndex = oriDesc.indexOf(">");
//        return oriDesc.substring(0, startIndex) + oriDesc.substring(endIndex + 1);
//    }
//
//    private String tryDeleteGenericsClass(String oriDesc) {
//        while (oriDesc.contains("<") && oriDesc.contains(">")) {
//            oriDesc = deleteGenericsClass(oriDesc);
//        }
//        return oriDesc;
//    }
}