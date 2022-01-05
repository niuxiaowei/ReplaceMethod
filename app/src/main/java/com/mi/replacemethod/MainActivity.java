package com.mi.replacemethod;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View view = inflate(R.layout.activity_second, null);
        Log.i("Main", "oncreate");

    }

     View inflate(int id, ViewGroup parentView) {
         Log.i("Main", "inflate");
         return getLayoutInflater().inflate(id, parentView);
    }
}