package com.mi.replacemethod

/**
 * create by niuxiaowei
 * date : 21-10-18
 **/
class AbsReplaceByMethods {
    private val methods = mutableListOf<AbsReplaceByMethod>()

    fun add(replace: Replace, by: By) {
        var index = 0
        var isMergeItems = false
        var isAdd = true
        for (absMethod in methods) {
            if (absMethod is SimpleReplaceByMethod) {
                //存放的是一样的，则不用在存放
                if (absMethod.replace == replace) {
                    isAdd = false
                    return
                }else if (absMethod.replace.desc == replace.desc && absMethod.replace.methodName == replace.methodName && absMethod.replace.asmOpcode == replace.asmOpcode) {
                    //发现了与将要添加的replace具有相同的方法名称和方法描述符，因此需要把它们合并到一起
                    isMergeItems = true
                    isAdd = false
                    break
                }
            }else if (absMethod is SameDescReplaceByMethod) {
                if (absMethod.replaceMethodDesc == replace.desc && absMethod.replaceMethodName == replace.methodName && absMethod.asmOpcode == replace.asmOpcode) {
                    absMethod.methods.forEach{
                        //一样的直接退出
                        if (it.replace == replace) {
                            isAdd = false
                            return
                        }
                    }
                    absMethod.add(replace, by)
                    isAdd = false
                }
            }
            index++
        }

        if (isMergeItems) {
            val removedMethod = methods.removeAt(index)
            if (removedMethod is SimpleReplaceByMethod) {
                //进行合并
                val sameDescReplaceByMethod = SameDescReplaceByMethod(replace.methodName,replace.desc,replace.asmOpcode)
                sameDescReplaceByMethod.add(removedMethod.replace, removedMethod.by)
                sameDescReplaceByMethod.add(replace, by)
                methods.add(index,sameDescReplaceByMethod)
            }
        }
        if (isAdd) {
            methods.add(SimpleReplaceByMethod(replace, by))
        }
    }

    override fun toString(): String {
        return "\n    AbsReplaceByMethods(methods=$methods)"
    }


}