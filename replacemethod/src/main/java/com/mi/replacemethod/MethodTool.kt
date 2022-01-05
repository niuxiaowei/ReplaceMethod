package com.mi.replacemethod

/**
 *  * Create by niuxiaowei on 2021/12/11
 *  方法工具类
 */
class MethodTool {

    companion object {

        fun isConstructor(methodName: String?): Boolean {
            return methodName?.contains("<init>") ?: false
        }

        fun isStaticInit(methodName: String?): Boolean {
            return methodName?.contains("<clinit>") ?: false
        }

    }
}