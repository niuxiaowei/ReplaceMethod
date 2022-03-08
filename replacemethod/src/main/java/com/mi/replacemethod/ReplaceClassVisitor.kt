package com.mi.replacemethod


import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*


/**
 * Create by niuxiaowei on 2021/12/11
 * Class Visitor
 */
class ReplaceClassVisitor(api: Int, cv: ClassVisitor?, var config: Config) :
        ClassVisitor(api, cv) {

    private var className: String? = null
    private var isInterface = false
    private var curClassVisit = true


    private var insertReplaceMethodInfos: MutableList<InsertReplaceMethodInfo>? = null
    private var visitedMethodRecords: MutableList<VisitedMethodRecord>? = null


    override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)

        this.className = name
        //抽象方法或者接口
//        if (access and Opcodes.ACC_ABSTRACT > 0 || access and Opcodes.ACC_INTERFACE > 0) {
//            this.isInterface = true
//        }
        curClassVisit = config.checkClassVisit(className)
    }

    /**
     * 存储访问过的方法
     */
    private data class VisitedMethodRecord(var name: String?, var desc: String?)

    override fun visitMethod(
            access: Int,
            name: String?,
            desc: String?,
            signature: String?,
            exceptions: Array<out String>?
    ): MethodVisitor {
        //当前的类不用扫描
        if (!curClassVisit) {
            return super.visitMethod(access, name, desc, signature, exceptions)
        }
        try {
            val isConstructor = MethodTool.isConstructor(name)
            var isStaticMethod = false
            if (access and ACC_STATIC > 0) {
                isStaticMethod = true
            }
            var isAbsMethod = access and ACC_ABSTRACT > 0

            return if (isAbsMethod) {
                super.visitMethod(access, name, desc, signature, exceptions)
            } else {
                if (visitedMethodRecords == null) {
                    visitedMethodRecords = mutableListOf()
                }
                if (!isConstructor) {
                    visitedMethodRecords?.add(VisitedMethodRecord(name, desc))
                }
                val mv = cv.visitMethod(access, name, desc, signature, exceptions)
                ReplaceMethodVisitor(api, mv, access, name, desc, className, config, isStaticMethod, this, isConstructor)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("${Config.PRINT_TAG}  className:${className}  visitMethod   err: ${e.toString()}")
            return super.visitMethod(access, name, desc, signature, exceptions)
        }

    }


    /**
     * 插入替换方法的信息
     * 替换方法的模版如下：
     *    返回type  name( oriClass, param1, ...){
     *         if (oriClass instanceOf replaceClass){
     *            return    byClass.byName(param...)
     *         }else{
     *            return oriClass.oriName(param...)
     *         }
     *    }
     */
    class InsertReplaceMethodInfo(var insertMethodName: String,
                                  var insertMethodDesc: String?,
                                  var replaceInfos: MutableList<ReplaceInfo>,
                                  var byInfos: MutableList<ByInfo>,
                                  var accessCode: Int,
                                  var hasExtraParams: Boolean,
                                  var hasThisParam: Boolean) {

        class ReplaceInfo(var replaceClass: String?,
                          var opcode: Int)

        class ByInfo(var byClass: String?,
                     var byMethodName: String?,
                     var byMethodDesc: String?,
                     var addExtraParams: Boolean,
                     var opcode: Int = INVOKESTATIC)

        var params: MutableList<String>? = mutableListOf<String>()
        var returnType: String? = null

        fun addOriInfo(oriClass: String?, oriMethodName: String?, oriMethodDesc: String?, oriOpcode: Int) {
            oriClass?.let {
                // 若replaceInfos已经存在相同的replaceClass，则不需要添加
                replaceInfos.forEach {
                    if (it.replaceClass == oriClass) {
                        return@let
                    }
                }
                replaceInfos.add(ReplaceInfo(oriClass, oriOpcode))
                byInfos.add(ByInfo(oriClass, oriMethodName, oriMethodDesc, false, oriOpcode))
            }
        }

        fun parseParmasAndReturnType() {
            //解析参数和返回值
            var i = 0
            val max = insertMethodDesc?.length ?: 0
            fun parse(strs: String?): String {
                var result = ""
                if (i < max) {
                    when (val str = strs?.subSequence(i, ++i)) {
                        "(" -> {
                        }
                        ")" -> result = "eof"
                        "I", "J", "Z", "F", "D", "C", "S", "B" -> result = str as String
                        "[" -> result = "${str}${parse(strs)}"
                        "L" -> {
                            val index = strs.subSequence(i, max).indexOf(";") + 1
                            result = "${str}${strs.subSequence(i, index + i)}"
                            i += index
                        }
                    }
                }
                return result
            }

            while (i < max) {
                val param = parse(insertMethodDesc)
//                println("param     $param")
                if (param == "eof") {
                    returnType = insertMethodDesc?.length?.let { insertMethodDesc?.substring(i, it) }
                    break
                }
                if (param != "") {
                    params?.add(param)
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as InsertReplaceMethodInfo

            if (insertMethodName != other.insertMethodName) return false
            if (insertMethodDesc != other.insertMethodDesc) return false
            if (accessCode != other.accessCode) return false

            return true
        }

        override fun hashCode(): Int {
            var result = insertMethodName.hashCode()
            result = 31 * result + (insertMethodDesc?.hashCode() ?: 0)
            result = 31 * result + accessCode
            return result
        }


    }


    fun addInsertReplaceMethodInfo(name: String, desc: String?, oriName: String?, oriMethodName: String?, oriDesc: String?, oriOpcode: Int, replaceInfos: MutableList<InsertReplaceMethodInfo.ReplaceInfo>, byInfos: MutableList<InsertReplaceMethodInfo.ByInfo>, accessCode: Int, addExtraParams: Boolean, hasThisParam: Boolean = false) {
        //若当前class已经有了method则，则尽心判断
        if (insertReplaceMethodInfos == null) {
            insertReplaceMethodInfos = mutableListOf()
        }
        val relaceMethodInfo = InsertReplaceMethodInfo(name, desc, replaceInfos, byInfos, accessCode, addExtraParams, hasThisParam)

        //不包含则添加
        if (!insertReplaceMethodInfos?.contains(relaceMethodInfo)!!) {
            relaceMethodInfo.parseParmasAndReturnType()
            relaceMethodInfo.addOriInfo(oriName, oriMethodName, oriDesc, oriOpcode)
            insertReplaceMethodInfos?.add(relaceMethodInfo)
        } else {
            //包含则试着找到对应信息，尝试把ori信息加入replace，by中
            insertReplaceMethodInfos?.forEach {
                if (it == relaceMethodInfo) {
                    it.addOriInfo(oriName, oriMethodName, oriDesc, oriOpcode)
                }
            }
        }
    }

    fun addInsertReplaceMethodInfo(name: String, desc: String?, oriName: String?, oriMethodName: String?, oriDesc: String?, oriOpcode: Int, replaceInfo: InsertReplaceMethodInfo.ReplaceInfo, byInfo: InsertReplaceMethodInfo.ByInfo, accessCode: Int, addExtraParams: Boolean, hasThisParam: Boolean = false) {
        addInsertReplaceMethodInfo(name, desc, oriName, oriMethodName, oriDesc, oriOpcode, mutableListOf(replaceInfo), mutableListOf(byInfo), accessCode, addExtraParams, hasThisParam)
    }


    private fun checkInsertReplaceMethodExist() {
        insertReplaceMethodInfos?.let { replaceMethods ->
            if (replaceMethods.size > 0 && visitedMethodRecords?.size!! > 0) {
                var indexs: MutableList<Int>? = null
                var index = 0
                replaceMethods.forEach {
                    var exist = false
                    for (method in visitedMethodRecords!!) {
                        if (method.desc == it.insertMethodDesc && method.name == it.insertMethodName) {
                            exist = true
                            break
                        }
                    }
                    //把存在的索引放入 indexs中
                    if (exist) {
                        if (indexs == null) {
                            indexs = mutableListOf()
                        }
                        indexs?.add(index)
                    }
                    index++
                }
                //把存在的方法移除
                indexs?.forEach {
                    replaceMethods.removeAt(it)
                }
            }
        }
    }

    override fun visitEnd() {
        checkInsertReplaceMethodExist()
        insertReplaceMethod()
        super.visitEnd()
        config.log("visitEnd  className:${className} ")
    }

    /**
     * 插入aload指令
     * @param addCastInsnInFirstParam第一个参数是否增加cast指令
     */
    private fun insertAloadInsn(params: MutableList<String>?, mv: MethodVisitor, firstParamAddCastInsn: Boolean = false, castClass: String? = "") {
        var index = 0
        //J==long D == double 它们的wide是2，因此需要+2
        params?.forEach {
            when (it) {
                "I" -> mv.visitVarInsn(ILOAD, index++)
                "J" -> {
                    mv.visitVarInsn(LLOAD, index)
                    index += 2
                }
                "Z" -> mv.visitVarInsn(ILOAD, index++)
                "F" -> mv.visitVarInsn(FLOAD, index++)
                "D" -> {
                    mv.visitVarInsn(DLOAD, index)
                    index += 2
                }
                "C" -> mv.visitVarInsn(ILOAD, index++)
                "S" -> mv.visitVarInsn(ILOAD, index++)
                "B" -> mv.visitVarInsn(ILOAD, index++)
                else -> {
                    mv.visitVarInsn(ALOAD, index++)
                    if (index == 1 && firstParamAddCastInsn && castClass != "") {
                        mv.visitTypeInsn(CHECKCAST, castClass);
                    }
                }
            }
        }
    }

    private fun insertReturnInsn(returnType: String?, mv: MethodVisitor) {
        returnType?.let {
            when (it) {
                "I" -> mv.visitInsn(IRETURN)
                "J" -> mv.visitInsn(LRETURN)
                "Z" -> mv.visitInsn(IRETURN)
                "F" -> mv.visitInsn(FRETURN)
                "D" -> mv.visitInsn(DRETURN)
                "C" -> mv.visitInsn(IRETURN)
                "S" -> mv.visitInsn(IRETURN)
                "B" -> mv.visitInsn(IRETURN)
                "V" -> {
                    mv.visitInsn(RETURN)
                }
                else -> mv.visitInsn(ARETURN)
            }
        }
    }

    private fun insertLocalVariable(params: MutableList<String>?, mv: MethodVisitor, label0: Label, label4: Label) {
        var index = 0
        params?.forEach {
            mv.visitLocalVariable("param${index}", it, null, label0, label4, index)
            when(it){
                "J"-> index += 2
                "D"-> index += 2
                else -> index++
            }

        }

    }

    private fun insertReplaceMethod() {
        insertReplaceMethodInfos?.forEach { replaceMethodInfo ->
            try {
                val byInfos = replaceMethodInfo.byInfos
                val replaceInfos = replaceMethodInfo.replaceInfos
                var index = 0
                //插入方法名和方法描述
                val mv = cv.visitMethod(ACC_PRIVATE or replaceMethodInfo.accessCode, replaceMethodInfo.insertMethodName, replaceMethodInfo.insertMethodDesc, null, null)
                mv.visitCode()
                val label0 = Label()
                mv.visitLabel(label0)

                var label1 = Label()

                /**
                 * 下面的代码是插入如下代码：
                 * if( o instanceof A){
                 *    invoke xxmethod
                 * }else if( o instanceof B){
                 *    invoke xxmethod
                 * }else if( o instanceof C){
                 *    invoke xxmethod
                 * }else{
                 *    invoke xxmethod
                 * }
                 */
                //处理非静态方法
                if (replaceInfos[index].opcode != INVOKESTATIC) {
                    while (index < byInfos.size) {
                        mv.visitLabel(label1)
//                  mv.visitLineNumber(52, label1)
                        //if不需要frame指令
                        if (index != 0) {
                            mv.visitFrame(F_SAME, 0, null, 0, null)
                        }
                        //else，因此不需要下面的判断
                        if (index != byInfos.size - 1) {
                            mv.visitVarInsn(ALOAD, 0)
                            mv.visitTypeInsn(INSTANCEOF, replaceInfos[index].replaceClass)
                            label1 = Label()
                            //上面的判断不相等，则跳入label1的位置，label1即下个else if或者else的开始部分
                            mv.visitJumpInsn(IFEQ, label1)
                        }
                        val label5 = Label()
                        mv.visitLabel(label5)
                        if (replaceMethodInfo.hasExtraParams) {
                            if (byInfos[index].addExtraParams) {
                                insertAloadInsn(replaceMethodInfo.params, mv, true, replaceInfos[index].replaceClass)
                            } else {
                                insertAloadInsn(replaceMethodInfo.params?.subList(0, replaceMethodInfo.params!!.size - 1), mv, true, replaceInfos[index].replaceClass)
                            }
                        } else {
                            insertAloadInsn(replaceMethodInfo.params, mv, true, replaceInfos[index].replaceClass)
                        }
                        //插入替换的方法
                        mv.visitMethodInsn(byInfos[index].opcode, byInfos[index].byClass, byInfos[index].byMethodName, byInfos[index].byMethodDesc, false)
                        insertReturnInsn(replaceMethodInfo.returnType, mv)
                        index++
                    }
                } else {
                    while (index < byInfos.size) {
                        mv.visitLabel(label1)
//                  mv.visitLineNumber(52, label1)
                        //if不需要frame指令
                        if (index != 0) {
                            mv.visitFrame(F_SAME, 0, null, 0, null)
                        }
                        //else，因此不需要下面的判断
                        if (index != byInfos.size - 1) {
                            if (replaceMethodInfo.hasThisParam) {
                                replaceMethodInfo.params?.size?.minus(1)?.let { mv.visitVarInsn(ALOAD, it) }
                                mv.visitTypeInsn(INSTANCEOF, replaceInfos[index].replaceClass)
                                label1 = Label()
                                //上面的判断不相等，则跳入label1的位置，label1即下个else if或者else的开始部分
                                mv.visitJumpInsn(IFEQ, label1)
                            }
                        }
                        val label5 = Label()
                        mv.visitLabel(label5)
                        var params = replaceMethodInfo.params
                        if (replaceMethodInfo.hasExtraParams) {
                            if (byInfos[index].addExtraParams) {
                                if (replaceMethodInfo.hasThisParam) {
                                    params = params?.subList(0, params!!.size - 1)
                                }
                            } else {
                                params = if (replaceMethodInfo.hasThisParam) {
                                    params?.subList(0, params!!.size - 2)
                                } else {
                                    params?.subList(0, params!!.size - 1)
                                }
                            }
                        } else {
                            if (replaceMethodInfo.hasThisParam) {
                                params = params?.subList(0, params!!.size - 1)
                            }
                        }
                        insertAloadInsn(params, mv, false, null)

                        //插入替换的方法
                        mv.visitMethodInsn(byInfos[index].opcode, byInfos[index].byClass, byInfos[index].byMethodName, byInfos[index].byMethodDesc, false)
                        insertReturnInsn(replaceMethodInfo.returnType, mv)
                        index++
                    }
                }

                //插入参数
                val label4 = Label()
                insertLocalVariable(replaceMethodInfo.params, mv, label0, label4)
                val length = replaceMethodInfo.params?.size ?: 0
                mv.visitMaxs(length, length)
                mv.visitEnd()


            } catch (e: Exception) {
                println("${Config.PRINT_TAG}   className:${className} insertReplaceMethod err:${e}")
                e.printStackTrace()
            }
        }

    }
}