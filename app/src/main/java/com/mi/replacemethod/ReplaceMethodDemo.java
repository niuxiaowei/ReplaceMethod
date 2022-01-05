package com.mi.replacemethod;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * create by niuxiaowei
 * date : 21-12-14
 **/
public class ReplaceMethodDemo {

    private static final String TAG = "ReplaceMethodDemo";

    public static View inflate(LayoutInflater inflater, int layoutId, ViewGroup parent, Object[] objects) {
        View result = inflater.inflate(layoutId, parent);
        Log.i(TAG, "inflate invoke layoutId:" + layoutId + "  parent:" + parent + "  result:" + result);
        return result;
    }

    public static View inflate(MainActivity mainActivity, int layoutId, ViewGroup parent, Object[] objects) {
        View result = mainActivity.inflate(layoutId, parent);
        Log.i(TAG, "inflate invoke layoutId:" + layoutId + "  parent:" + parent + "  result:" + result);
        return result;
    }

    public static View inflate(LayoutInflater inflater, int layoutId, ViewGroup parent, boolean attachToRoot, Object[] objects) {
        View result = inflater.inflate(layoutId, parent, attachToRoot);
        Log.i(TAG, "inflate invoke layoutId:" + layoutId + "  parent:" + parent + "  attachToRoot:" + attachToRoot + "  result:" + result);
        return result;
    }

    public static View inflate(Context context, int layoutId, ViewGroup parent, Object[] objects) {
        View view = View.inflate(context, layoutId, parent);
        Log.i(TAG, "inflate invoke context:" + context + "layoutId:" + layoutId + "  parent:" + parent + "  result:" + view);
        return view;
    }

    public static View inflateMyInflater(Context context, int layoutId, ViewGroup parent, Object[] objects) {
        View view = MyInflater.inflate(context, layoutId, parent);
        Log.i("ReplaceMethodDemo", "inflate invoke context:" + context + "layoutId:" + layoutId + "  parent:" + parent + "  result:" + view);
        return view;
    }

    public static void setContentView(Activity activity, int layoutId) {
        activity.setContentView(layoutId);
        Log.i(TAG, "setContentView invoke activity:" + activity + "layoutId:" + layoutId);
    }

    public static void setOnClickListener(View view, View.OnClickListener clickListener) {
        Log.i(TAG, "setOnClickListener invoke view:" + view + " listener:" + clickListener);
        view.setOnClickListener(new MyClickListener(clickListener));
    }

    public static class MyClickListener implements View.OnClickListener {
        private View.OnClickListener listener;

        public MyClickListener(View.OnClickListener listener) {
            this.listener = listener;
        }

        @Override
        public void onClick(View v) {
            Toast.makeText(App.getInstance(),"点击事件被拦截了",Toast.LENGTH_LONG).show();
            if (listener != null) {
                listener.onClick(v);
            }
        }
    }


    public static MyTextView newMyTextView(Context context) {
        MyTextView myTextView = new MyTextView(context);
        myTextView.setText("我是被替换方法new出来的view");
        Log.i(TAG, "newMyTextView invoke context:" + context + "  mytextview:" + myTextView);
        return myTextView;
    }

    public static int i(String tag, String msg) {
        System.out.println(TAG+"    tag is:"+tag+"  msg is:"+msg);
        return 0;
    }
}
