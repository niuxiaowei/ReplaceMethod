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

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends Activity {

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View view = inflate(R.layout.activity_second, null);
        Log.i("Main", "oncreate");
        getRunningTasks();

//        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        locationManager.requestLocationUpdates("gps", 1000l, 2.0f, new LocationListener() {
//            @Override
//            public void onLocationChanged(@NonNull Location location) {
//
//            }
//        });

        try {
            getMacAddressByNetworkInterface1();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        testBaseType((byte) 1, new byte[]{}, (short) 1, null, 100L, null, true, null, 0.1f, null, 1, new int[]{}, 100.0D, null);
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

    private static void testBaseType(byte b, byte[] bs, short s, short[] ss, long l, long[] ls, boolean bo, boolean[] bss, float f, float[] floats, int i, int[] ints, double d, double[] doubles) {

    }

    private static void getMacAddressByNetworkInterface1() throws SocketException {


        NetworkInterface ni = null;
        byte[] macBytes = ni.getHardwareAddress();
    }

    private static String getMacAddressByNetworkInterface() {

        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                if (ni != null) {
                    byte[] macBytes = ni.getHardwareAddress();
                }
            }
        } catch (Exception var8) {
            var8.printStackTrace();
        }
        return "02:00:00:00:00:00";
    }
}