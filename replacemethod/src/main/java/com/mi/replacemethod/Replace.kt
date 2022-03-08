package com.mi.replacemethod

import org.objectweb.asm.Opcodes

/**
 * create by niuxiaowei
 * date : 21-10-18
 **/
class Replace {
    /**
     *
     */
    var invokeType: String? = null

    var asmOpcode: Int? = null

    /**
     * 类的完整名字
     */
    var className: String? = null

    /**
     * 方法名字
     */
    var methodName: String? = null

    /**
     * 方法参数，返回值描述
     */
    var desc: String? = null

    // TODO: 21-11-1 是否需要吧asmopcode和下面的变量加入equal中
    var ignoreOverideStaticMethod: Boolean = false

    /**
     * release build type 时候是否启用替换功能
     */
    var releaseEnable = false

    /**
     * 需要替换的包，不配置则认为是替换当前工程所有的包，
     */
    val replacePackages = mutableListOf<String>()


    fun invokeType(type: String) {
        this.invokeType = type
        asmOpcode = when (type) {
            InvokeType.INVOKE_STATIC -> Opcodes.INVOKESTATIC
            InvokeType.INVOKE_VIRTUAL -> Opcodes.INVOKEVIRTUAL
            InvokeType.INVOKE_NEW -> Opcodes.INVOKESPECIAL
            else -> 0
        }
    }

    fun ignoreOverideStaticMethod(ignoreOverideStaticMethod: Boolean) {
        this.ignoreOverideStaticMethod = ignoreOverideStaticMethod
    }

    /**
     * 是否是静态方法
     */
    fun isStaticMethod(): Boolean {
        return asmOpcode == Opcodes.INVOKESTATIC
    }

    fun className(className: String?) {
        this.className = className
    }

    fun methodName(methodName: String?) {
        this.methodName = methodName
    }

    fun desc(desc: String?) {
        this.desc = desc
    }

    fun releaseEnable(enable: Boolean) {
        this.releaseEnable = enable
    }

    fun replacePackages(vararg packages:String) {
        if (packages.isNotEmpty()) {
            this.replacePackages.clear()
            packages.forEach { replacePackages.add(it) }
        }
    }

    /**
     * 是否是new 对象的方法
     */
    fun isNewClassMethod(): Boolean {
        return methodName == "<init>"
    }

    fun classInReplacePackages(className: String?): Boolean {
        val replacedClassName = className?.replace("/", ".")
        //该情况认为是扫描所有的包下的类
        if (replacePackages.isNullOrEmpty()) {
            return true
        }
        replacePackages.forEach {
            if (replacedClassName?.startsWith(it)!!) {
                return true
            }
        }
        return false
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Replace

        if (invokeType != other.invokeType) return false
        if (className != other.className) return false
        if (methodName != other.methodName) return false
        if (desc != other.desc) return false

        return true
    }

    override fun hashCode(): Int {
        var result = invokeType.hashCode()
        result = 31 * result + (className?.hashCode() ?: 0)
        result = 31 * result + (methodName?.hashCode() ?: 0)
        result = 31 * result + (desc?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Replace(invokeType=$invokeType, asmOpcode=$asmOpcode, className=$className, methodName=$methodName, desc=$desc, ignoreOverideStaticMethod=$ignoreOverideStaticMethod, releaseEnable=$releaseEnable, replacePackages=$replacePackages)"
    }


}