package com.mi.replacemethod;

import android.app.Fragment;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

/**
 * create by niuxiaowei
 * date : 21-12-31
 **/
public abstract class AbsFragment extends Fragment {

    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void setClickForView() {
        getView().findViewById(R.id.click).setOnClickListener(v->{
            Toast.makeText(getContext(),"点击了我 click",Toast.LENGTH_LONG).show();

        });
    }

    abstract void absMethod();
}
