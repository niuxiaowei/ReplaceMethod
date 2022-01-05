package com.mi.replacemethod

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast

/**
 * create by niuxiaowei
 * date : 21-12-17
 **/
class MyLinearLayout(context: Context) : LinearLayout(context) {
    init {
        orientation = LinearLayout.VERTICAL
        var myTextView = MyTextView(getContext())
        myTextView.setOnClickListener {
            Toast.makeText(getContext(), "我是 mytextview", Toast.LENGTH_LONG).show()
        }
        addView(myTextView)

        myTextView = MyTextView(getContext())
        myTextView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                Toast.makeText(getContext(), "我是 mytextview2", Toast.LENGTH_LONG).show()
            }

        })
        addView(myTextView)
    }

    constructor(context: Context, att: AttributeSet) : this(context){

    }

}