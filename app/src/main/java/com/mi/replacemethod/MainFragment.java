package com.mi.replacemethod;

import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;


import java.util.Set;


/**
 * create by niuxiaowei
 * date : 21-8-26
 **/
public class MainFragment extends Fragment {

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView( LayoutInflater inflater,  ViewGroup container,  Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container);
        MyTextView myTextView = new MyTextView(getContext());
        if (view instanceof LinearLayout) {
            LinearLayout linearLayout = (LinearLayout) view;
            linearLayout.addView(myTextView);
        }
        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.click).setOnClickListener(view1->{
            Toast.makeText(getContext(),"点击了我 click",Toast.LENGTH_LONG).show();

        });
        view.findViewById(R.id.click1).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(),"点击了我 click1",Toast.LENGTH_LONG).show();
            }
        });
    }
}
