package com.mi.replacemethod

import org.objectweb.asm.Opcodes

/**
 * create by niuxiaowei
 * date : 21-10-18
 * 具有相同的 方法描述符（方法名字，方法参数，方法返回类型都相同，但是属于不同的类） 的 集合
 **/
class SameDescReplaceByMethod(var replaceMethodName:String?, var replaceMethodDesc:String?, var asmOpcode:Int?) : AbsReplaceByMethod(){
     val methods = mutableListOf<SimpleReplaceByMethod>()

    fun add(replace: Replace, by: By) {
        methods.add(SimpleReplaceByMethod(replace, by))
    }

    override fun toString(): String {
        return "\n SameDescReplaceByMethod(replaceMethodName=$replaceMethodName, replaceMethodDesc=$replaceMethodDesc, methods=$methods)   end \n\n"
    }

    fun isStaticMethod(): Boolean {
        return asmOpcode == Opcodes.INVOKESTATIC
    }

}