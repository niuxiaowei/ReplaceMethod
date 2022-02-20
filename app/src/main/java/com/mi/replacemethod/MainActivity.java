package com.mi.replacemethod;

import android.app.Activity;
import android.app.ActivityManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

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
//        Log.i("Main", "trydelete result:" + tryDeleteGenericsClass("(java.util.List<com.u.O>,int,java.util.List<com.u.O>)" +
//                "java.util.List<com.niu" +
//                ".Test>"));
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