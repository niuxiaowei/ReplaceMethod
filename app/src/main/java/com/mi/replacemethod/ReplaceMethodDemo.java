package com.mi.replacemethod;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.net.NetworkInterface;
import java.util.List;

/**
 * create by niuxiaowei
 * date : 21-12-14
 **/
public class ReplaceMethodDemo {

    private static final String TAG = "ReplaceMethodDemo";

    public static View inflate(LayoutInflater inflater, int layoutId, ViewGroup parent,
            Object[] objects) {
        View result = inflater.inflate(layoutId, parent);
        Log.i(TAG,
                "inflate invoke layoutId:" + layoutId + "  parent:" + parent + "  result:" + result);
        return result;
    }

    public static View inflate(MainActivity mainActivity, int layoutId, ViewGroup parent,
            Object[] objects) {
        View result = mainActivity.inflate(layoutId, parent);
        Log.i(TAG,
                "inflate invoke layoutId:" + layoutId + "  parent:" + parent + "  result:" + result);
        return result;
    }

    public static View inflate(LayoutInflater inflater, int layoutId, ViewGroup parent,
            boolean attachToRoot, Object[] objects) {
        View result = inflater.inflate(layoutId, parent, attachToRoot);
        Log.i(TAG, "inflate invoke layoutId:" + layoutId + "  parent:" + parent + "  attachToRoot" +
                ":" + attachToRoot + "  result:" + result);
        return result;
    }

    public static View inflate(Context context, int layoutId, ViewGroup parent, Object[] objects) {
        View view = View.inflate(context, layoutId, parent);
        Log.i(TAG,
                "inflate invoke context:" + context + "layoutId:" + layoutId + "  parent:" + parent + "  result:" + view);
        return view;
    }

    public static View inflateMyInflater(Context context, int layoutId, ViewGroup parent,
            Object[] objects) {
        View view = MyInflater.inflate(context, layoutId, parent);
        Log.i("ReplaceMethodDemo",
                "inflate invoke context:" + context + "layoutId:" + layoutId + "  parent:" + parent + "  result:" + view);
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
            Toast.makeText(App.getInstance(), "点击事件被拦截了", Toast.LENGTH_LONG).show();
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
        System.out.println(TAG + "    tag is:" + tag + "  msg is:" + msg);
        return 0;
    }

    public static List<ActivityManager.RunningTaskInfo> getRunningTasks(ActivityManager activityManager, int max) {
        Log.i("ReplaceMethodDemo", "getRunningTasks max:" + max);
        return activityManager.getRunningTasks(max);

    }

    public static void requestLocationUpdates(LocationManager locationManager, String s, long l,
            float f, LocationListener locationListener,Object[] params) {

    }

    public static byte[] getHardwareAddress(NetworkInterface info, Object[] params) {

        Log.e(TAG, String.format("getHardwareAddress CTA false: %s#%s {%s} atLine=%s", params[0], params[1], params[2], params[3]));

        return "".getBytes();
    }

    public static void testBaseType(byte b, byte[] bs, short s, short[] ss, long l, long[] ls, boolean bo, boolean[] bss, float f, float[] floats, int i, int[] ints, double d, double[] doubles) {
        Log.i("ReplaceMethodDemo", "testByte b"+b+" bytes:"+bs + "s:"+s);

    }

}
