package com.mi.replacemethod


import java.io.File
import java.io.FileNotFoundException
import java.util.HashSet


/**
 * Create by cxzheng on 2019/6/4
 */
class Config(var open: Boolean,private val openLog: Boolean, val isRelease: Boolean, private var logFilters: List<String>) {
    private val allReplacePackages = mutableListOf<String>()

    var methods: MutableList<AbsReplaceByMethod>? = null
        set(value) {
            field = value
            value?.forEach {
                if (it is SameDescReplaceByMethod) {
                    it.methods.forEach { simpleMethod ->
                        simpleMethod.replace.apply {
                            if (replacePackages.isNullOrEmpty()) {
                                //代表扫描所有的包
                                allReplacePackages.clear()
                                return
                            } else {
                                replacePackages.forEach { pack->
                                    if (!allReplacePackages.contains(pack)) {
                                        allReplacePackages.add(pack)
                                    }
                                }
                            }
                        }
                    }
                }else if (it is SimpleReplaceByMethod) {
                    it.replace.apply {
                        if (replacePackages.isNullOrEmpty()) {
                            //代表扫描所有的包
                            allReplacePackages.clear()
                            return
                        } else {
                            replacePackages.forEach { pack->
                                if (!allReplacePackages.contains(pack)) {
                                    allReplacePackages.add(pack)
                                }
                            }
                        }
                    }
                }
            }
        }


    companion object{
        val PRINT_TAG = "[ReplaceMethod]"
    }

    /**
     * 打印日志
     * @param msg String?
     */
    fun log(msg: String) {
        //日志开关关闭了
        if (!openLog) {
            return
        }
        //表面没有设置任何过滤，所有日志都打印
        if (logFilters.isNullOrEmpty()) {
            println("$PRINT_TAG  $msg")
        } else {
            var canLog = false
            logFilters.forEach {
                if (msg.contains(it)) {
                    canLog = true
                    return@forEach
                }
            }
            if (canLog) {
                println("$PRINT_TAG  $msg")
            }
        }
    }

    //一些默认无需插桩的类
    private val UNNEED_TRACE_CLASS = arrayOf("R.class", "R$", "Manifest", "BuildConfig")

    fun isNeedTraceClass(fileName: String): Boolean {
        var isNeed = true
        if (fileName.endsWith(".class")) {
            for (unTraceCls in UNNEED_TRACE_CLASS) {
                if (fileName.contains(unTraceCls)) {
                    isNeed = false
                    break
                }
            }
        } else {
            isNeed = false
        }
        return isNeed
    }

    fun checkClassVisit(className: String?): Boolean {
        val replaceClassName = className?.replace("/", ".")
        //这种情况认为扫描所有的包下的类
        if (allReplacePackages.isNullOrEmpty()) {
            return true
        }
        allReplacePackages?.forEach {
            if (replaceClassName?.startsWith(it)!!) {
                return true
            }
        }
        return false
    }

    override fun toString(): String {
        return "Config  (open=$open, openLog=$openLog, isRelease=$isRelease, allReplacePackages=$allReplacePackages, methods=$methods)"
    }


}



