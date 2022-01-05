package com.mi.replacemethod

import android.view.View
import android.widget.Toast

/**
 * create by niuxiaowei
 * date : 21-12-31
 **/
interface IInterfaceTest {
    fun test(){
        val view = View(null)
        view.setOnClickListener(View.OnClickListener { v: View? -> Toast.makeText(App.getInstance(), "点击了我 click", Toast.LENGTH_LONG).show() })
    }
}