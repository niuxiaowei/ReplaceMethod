package com.mi.replacemethod

/**
 * create by niuxiaowei
 * date : 21-10-18
 **/
class SimpleReplaceByMethod(var replace: Replace, var by: By) : AbsReplaceByMethod(){
    override fun toString(): String {
        return "\n SimpleReplaceByMethod(replace=$replace, by=$by) \n"
    }
}