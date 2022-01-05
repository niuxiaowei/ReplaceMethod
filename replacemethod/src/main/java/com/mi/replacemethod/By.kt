package com.mi.replacemethod

/**
 * create by niuxiaowei
 * date : 21-10-15
 **/
class By() {
    var className:String? = null
    var methodName:String? = null
    var addExtraParams: Boolean = false

    fun className(className: String) {
        this.className = className
    }

    fun methodName(methodName: String) {
        this.methodName = methodName
    }

    fun addExtraParams(addExtraParams: Boolean) {
        this.addExtraParams = addExtraParams
    }

    override fun toString(): String {
        return "By(className=$className, methodName=$methodName, addExtraParams=$addExtraParams)"
    }
}