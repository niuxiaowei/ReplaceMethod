package com.mi.replacemethod


import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.Opcodes


/**
 * Create by niuxiaowei on 2021/12/11
 * Method Visitor
 */
class ReplaceMethodVisitor(
        api: Int, mv: MethodVisitor?, access: Int,
        var curMethodName: String?, var curMethodesc: String?, var className: String?,
        var config: Config,
        var isStaticMethod: Boolean,
        var replaceClassVisitor: ReplaceClassVisitor,
        var isConstructor: Boolean
) : AdviceAdapter(api, mv, access, curMethodName, curMethodesc) {

    private var lineNumber: Int? = 0
    private var loadParam: String? = null
    private var isNewOpcode = false


    override fun onMethodEnter() {
        super.onMethodEnter()
        config.log("onMethodEnter className:${className}  methodName:${curMethodName}")
    }

    override fun onMethodExit(opcode: Int) {
        super.onMethodExit(opcode)
        config.log("onMethodExit className:${className}  methodName:${curMethodName}")
    }

    override fun visitLineNumber(line: Int, start: Label?) {
        super.visitLineNumber(line, start)
        lineNumber = line
    }

    /**
     * 注入额外参数  object[] {className, methodName, desc, linenumber
     */
    private fun insertExtraParams() {
        mv?.visitInsn(Opcodes.ICONST_4);
        mv?.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        mv?.visitInsn(Opcodes.DUP);
        mv?.visitInsn(Opcodes.ICONST_0);
        mv?.visitLdcInsn(className?.replace("/", "."));
        mv?.visitInsn(Opcodes.AASTORE);
        mv?.visitInsn(Opcodes.DUP);
        mv?.visitInsn(Opcodes.ICONST_1);
        mv?.visitLdcInsn(curMethodName);
        mv?.visitInsn(Opcodes.AASTORE);
        mv?.visitInsn(Opcodes.DUP);
        mv?.visitInsn(Opcodes.ICONST_2);
        mv?.visitLdcInsn(methodDesc?.replace("/", "."));
        mv?.visitInsn(Opcodes.AASTORE);
        mv?.visitInsn(Opcodes.DUP);
        mv?.visitInsn(Opcodes.ICONST_3);
        mv?.visitLdcInsn(lineNumber);
        mv?.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        mv?.visitInsn(Opcodes.AASTORE);
    }



    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, desc: String?, b: Boolean) {
        if (findReplaceMethod(opcode, owner, name, desc, b)) {
            return
        }
        super.visitMethodInsn(opcode, owner, name, desc, b)
    }

    /**
     * 查找替换的构造函数
     * @param replace Replace
     * @param owner String?
     * @param by By
     * @param itf Boolean
     * @return Boolean  true:找到了，false：没找到
     */
    private fun findReplaceConstructMethod(replace: Replace, owner: String?, by: By, itf: Boolean, desc: String?): Boolean {
        config.log("findReplaceConstructMethod newclassmethod:${replace.isNewClassMethod()}  owner:$owner  replace.classname:${replace.className}")
        if (replace.isNewClassMethod() && owner == replace.className) {
            //对方法增加 replace.className  和 extra参数object[]
            var newDesc: String? = null
            desc?.let {
                val leftBracketIndex = it.indexOf("(")
                val rightBracketIndex = it.indexOf(")")
                var extraParams = ""
                if (by.addExtraParams) {
                    extraParams = "[Ljava/lang/Object;"
                }
                newDesc = "${it.substring(0, rightBracketIndex)}$extraParams)L${replace.className};"
            }
            if (by.addExtraParams) {
                insertExtraParams()
            }
            mv?.visitMethodInsn(Opcodes.INVOKESTATIC, by.className, by.methodName, newDesc, itf)
            return true
        }
        return false
    }

    /**
     * 对desc进行修改，生成新的方法描述符模板：结果: addExtraParams 为true:  (%s, olddescparam, [Ljava/lang/Object;， %s) oldDescReturn
     *    false:  (%s, olddescparam， %s) oldDescReturn
     * @param desc String?
     * @param addExtraParams Boolean
     * @return String
     */
    private fun buildNewMethodDescTemplate(desc: String?, addExtraParams: Boolean): String? {
        desc?.let {
            val leftBracketIndex = it.indexOf("(")
            val rightBracketIndex = it.indexOf(")")
            val firstParam = "%s"
            var extraParams = ""
            if (addExtraParams) {
                extraParams = "[Ljava/lang/Object;"
            }
            val thisParam = "%s"
            return it.substring(0, leftBracketIndex + 1) + firstParam + it.substring(leftBracketIndex + 1, rightBracketIndex) + extraParams + thisParam + it.substring(rightBracketIndex, it.length)
        }
        return null
    }

    private fun createNewMethodName(name: String?, owner: String?): String {
        return "_${name}_of_${owner?.replace("/", "")}_"
    }

    private data class MethodInfo(val newMethodDesc: String?, val byMethodDesc: String?, val newMethodName: String)

    private fun buildMethodInfo(desc: String?, addExtraParams: Boolean, owner: String?, replace: Replace, name: String?, addThisParam: Boolean): MethodInfo {
        val methodDescTemplate = buildNewMethodDescTemplate(desc, addExtraParams)
        var newMethodDesc: String? = null
        var byMethodDesc: String? = null
        methodDescTemplate?.let {
            newMethodDesc = it.format(if (replace.isStaticMethod()) "" else "L${owner};", if (addThisParam) "L${className};" else "")
            byMethodDesc = it.format(if (replace.isStaticMethod()) "" else "L${replace.className};", "")
        }
        val newMethodName = createNewMethodName(name, owner)
        return MethodInfo(newMethodDesc, byMethodDesc, newMethodName)
    }

    /**
     * 查找替换的静态方法
     * @param replace Replace
     * @param owner String?
     * @param by By
     * @param name String?
     * @param opcode Int
     * @return Boolean
     */
    private fun findReplaceStaticMethod(replace: Replace, owner: String?, by: By, name: String?, opcode: Int, itf: Boolean, desc: String?): Boolean {
        if (!replace.isStaticMethod()) {
            return false
        }
        var addThisParam = false
        //当前方法是静态方法并且当前方法所属的类与替换方法所属的类不一样，则不处理，现在暂时处理不了这种情况
        if (isStaticMethod && owner != replace.className) {
            return false
        } else if (!isStaticMethod && owner != replace.className) {
            //处理非静态方法中调用静态方法的例子比如： View中调用infalte方法
            if (owner == className && replace.ignoreOverideStaticMethod) {
                addThisParam = true
            }
        }

        // 发现疑似的方法，因此把他引入当前classname的 staticReplaceMethodName = "_${by.methodName}_of_${by.className}_"方法中，进行if else的处理判断
        val (newMethodDesc, byMethodDesc, newMethodName) = buildMethodInfo(desc, by.addExtraParams, owner, replace, name, addThisParam)

        //如果当前visit的方法与包裹它的方法相同（name和desc都相同则不用替换）
        if (newMethodDesc == methodDesc && newMethodName == curMethodName) {
            return false
        }
        if (by.addExtraParams) {
            insertExtraParams()
        }
        if (addThisParam) {
            //注入this对象
            mv?.visitVarInsn(Opcodes.ALOAD, 0);
        }
        val accessCode = Opcodes.ACC_STATIC
        mv?.visitMethodInsn(Opcodes.INVOKESTATIC, className, newMethodName, newMethodDesc, itf)
        replaceClassVisitor.addInsertReplaceMethodInfo(newMethodName,
                newMethodDesc,
                owner,
                name,
                desc,
                opcode,
                ReplaceClassVisitor.InsertReplaceMethodInfo.ReplaceInfo(replace.className, replace.asmOpcode!!),
                ReplaceClassVisitor.InsertReplaceMethodInfo.ByInfo(by.className, by.methodName, byMethodDesc, by.addExtraParams),
                accessCode,
                by.addExtraParams,
                addThisParam)
        config.log("findReplaceStaticMethod className:${className}  methodName:${curMethodName}  owner:${owner}  name:${name}  desc:${desc}   loadParam:${loadParam}")

        return true
    }


    /**
     * 查找替换 实例方法
     * @param replace Replace
     * @param owner String?
     * @param by By
     * @param name String?
     * @param opcode Int
     * @return Boolean
     */
    private fun findReplaceInstanceMethod(replace: Replace, owner: String?, by: By, name: String?, opcode: Int, itf: Boolean, desc: String?): Boolean {
        if (replace.isStaticMethod() || replace.isNewClassMethod()) {
            return false
        }
        val (newMethodDesc, byMethodDesc, newMethodName) = buildMethodInfo(desc, by.addExtraParams, owner, replace, name, false)

        //如果当前visit的方法与包裹它的方法相同（name和desc都相同则不用替换）
        if (newMethodDesc == methodDesc && newMethodName == curMethodName) {
            return false
        }
        if (by.addExtraParams) {
            insertExtraParams()
        }
        val accessCode = Opcodes.ACC_STATIC
        mv?.visitMethodInsn(Opcodes.INVOKESTATIC, className, newMethodName, newMethodDesc, itf)
        replaceClassVisitor.addInsertReplaceMethodInfo(newMethodName,
                newMethodDesc,
                owner,
                name,
                desc,
                opcode,
                ReplaceClassVisitor.InsertReplaceMethodInfo.ReplaceInfo(replace.className, replace.asmOpcode!!),
                ReplaceClassVisitor.InsertReplaceMethodInfo.ByInfo(by.className, by.methodName, byMethodDesc, by.addExtraParams),
                accessCode,
                by.addExtraParams,
                false)

        config.log("findReplaceInstanceMethod  className:${className}  methodName:${curMethodName}  owner:${owner}  name:${name}  desc:${desc}   loadParam:${loadParam}")

        return true
    }

    private data class CollectedReplaceMethods(val addExtraParams: Boolean, var methodIndexs: MutableList<Int>?, val addThisParam: Boolean)

    /**
     * 根据规则来从SameDescReplaceByMethod 把可以进行替换的方法手机起来
     * @param sameDescReplaceByMethod SameDescReplaceByMethod
     * @param owner String?
     * @return CollectedReplaceMethods
     */
    private fun collectReplaceMethodsFOrSameDescReplaceByMethod(sameDescReplaceByMethod: SameDescReplaceByMethod, owner: String?): CollectedReplaceMethods {
        var addExtraParams = false
        var addThisParam = false
        var foundMethodIndexs: MutableList<Int>? = null
        var index = 0
        var exactIndex = -1
        sameDescReplaceByMethod.methods.forEach {
            //当前的class与by的class相同，则直接返回
            if (className == it.by.className) {
                foundMethodIndexs = null
                exactIndex = -1
                return@forEach
            }
            if (it.by.addExtraParams) {
                addExtraParams = true
            }
            //替换静态方法
            if (sameDescReplaceByMethod.isStaticMethod()) {
                if (owner == it.replace.className && it.replace.classInReplacePackages(className) && releaseEnable(it.replace)) {
                    //找到了真正的替换方法
                    exactIndex = index
                    return@forEach
                } else if (!isStaticMethod && owner != it.replace.className) {
                    //处理非静态方法中调用静态方法的例子比如： View中调用infalte方法
                    if (owner == className && it.replace.ignoreOverideStaticMethod && it.replace.classInReplacePackages(className) && releaseEnable(it.replace)) {
                        addThisParam = true
                        if (foundMethodIndexs == null) {
                            foundMethodIndexs = mutableListOf()
                        }
                        foundMethodIndexs?.add(index)
                    }
                }
            } else {
                if (it.replace.classInReplacePackages(className) && releaseEnable(it.replace)) {
                    if (owner == it.replace.className) {
                        //找到了真正的替换方法
                        exactIndex = index
                        return@forEach
                    } else {
                        if (foundMethodIndexs == null) {
                            foundMethodIndexs = mutableListOf()
                        }
                        foundMethodIndexs?.add(index)
                    }
                }
            }
            index++
        }

        if (exactIndex != -1) {
            if (foundMethodIndexs == null) {
                foundMethodIndexs = mutableListOf()
            } else {
                foundMethodIndexs?.clear()
            }
            foundMethodIndexs?.add(exactIndex)
        }
        return CollectedReplaceMethods(addExtraParams, foundMethodIndexs, addThisParam)
    }

    private fun releaseEnable(replace: Replace): Boolean {
        if (!config.isRelease) {
            return true
        } else {
            return replace.releaseEnable
        }
    }

    /**
     * 查找替换的方法
     * @param opcode
     * @param owner
     * @param name
     * @param desc
     * @param itf
     * @return true:找到了，false：没找到
     */
    private fun findReplaceMethod(opcode: Int, owner: String?, name: String?, desc: String?, itf: Boolean): Boolean {
        config.log("findReplaceMethod  className:${className}  methodName:${curMethodName}  owner:${owner}  name:${name}  desc:${desc} opcode:${opcode}     loadParam:${loadParam}")
        config.methods?.let { methods ->
            for (absReplaceByMethod in methods) {
                if (absReplaceByMethod is SimpleReplaceByMethod) {
                    val replace = absReplaceByMethod.replace
                    val by = absReplaceByMethod.by
                    if (className == by.className) {
                        return false
                    }
                    config.log("findReplaceMethod  simplereplacebymethod  className:${className}  methodName:${curMethodName}  owner:${owner}  replace.asmopcode:${replace.asmOpcode} opcode:${opcode} replace.methodname:${replace.methodName}  name:$name replace.desc:${replace.desc} desc:$desc  inpackagers:${replace.classInReplacePackages(className)}  release:${releaseEnable(replace)}")
                    //先判断opcode，name，desc，如果击中了则对owner再次进行判断（推迟判断owner的主要原因是，对于多态调用，owner与replace.className有可能会不一样，因此需要特殊处理）
                    if (replace.asmOpcode == opcode && name == replace.methodName && desc == replace.desc && replace.classInReplacePackages(className) && releaseEnable(replace)) {
                        return if (findReplaceConstructMethod(replace, owner, by, itf, desc) || findReplaceInstanceMethod(replace, owner, by, name, opcode, itf, desc)) {
                            true
                        } else {
                            findReplaceStaticMethod(replace, owner, by, name, opcode, itf, desc)
                        }
                    }
                } else if (absReplaceByMethod is SameDescReplaceByMethod) {
                    if (absReplaceByMethod.asmOpcode == opcode && name == absReplaceByMethod.replaceMethodName && desc == absReplaceByMethod.replaceMethodDesc) {

                        var (addExtraParams, methodIndexs, addThisParam) = collectReplaceMethodsFOrSameDescReplaceByMethod(absReplaceByMethod, owner)

                        //SameDescReplaceByMethod的methods中没有真正需要替换的方法，直接返回
                        if (methodIndexs.isNullOrEmpty()) {
                            return false
                        }

                        val methodDescTemplate = buildNewMethodDescTemplate(desc, addExtraParams)
                        var newMethodDesc: String? = null
                        methodDescTemplate?.let {
                            newMethodDesc = it.format(if (absReplaceByMethod.asmOpcode == Opcodes.INVOKESTATIC) "" else "Ljava/lang/Object;", if (addThisParam) "L${className};" else "")
                        }

                        val byInfos = mutableListOf<ReplaceClassVisitor.InsertReplaceMethodInfo.ByInfo>()
                        val replaceInfos = mutableListOf<ReplaceClassVisitor.InsertReplaceMethodInfo.ReplaceInfo>()
                        var newMethodName = "_${name}_of_"
                        var index = 0
                        absReplaceByMethod.methods.forEach { method ->
                            if (methodIndexs.contains(index)) {
                                byInfos.add(ReplaceClassVisitor.InsertReplaceMethodInfo.ByInfo(method.by.className,
                                        method.by.methodName,
                                        methodDescTemplate?.format(if (absReplaceByMethod.asmOpcode == Opcodes.INVOKESTATIC) "" else "L${method.replace.className};", ""),
                                        method.by.addExtraParams))
                                replaceInfos.add(ReplaceClassVisitor.InsertReplaceMethodInfo.ReplaceInfo(method.replace.className,
                                        method.replace.asmOpcode!!))
                                newMethodName += method.replace.className?.let {
                                    it.substring(it.lastIndexOf("/") + 1, it.length)
                                }
                            }
                            index++
                        }

                        //如果当前visit的方法与包裹它的方法相同（name和desc都相同则不用替换）
                        if (newMethodDesc == methodDesc && newMethodName == curMethodName) {
                            return false
                        }
                        if (addExtraParams) {
                            insertExtraParams()
                        }
                        if (addThisParam) {
                            //注入this对象
                            mv?.visitVarInsn(Opcodes.ALOAD, 0);
                        }
                        var accessCode = Opcodes.ACC_STATIC
                        mv?.visitMethodInsn(Opcodes.INVOKESTATIC, className, newMethodName, newMethodDesc, itf)
                        replaceClassVisitor.addInsertReplaceMethodInfo(newMethodName, newMethodDesc, owner, name, desc, opcode, replaceInfos, byInfos, accessCode, addExtraParams, addThisParam)

                        config.log("SameDescReplaceByMethod: className:${className}  methodName:${curMethodName}  owner:${owner}  name:${name}  desc:${desc}     loadParam:${loadParam}")
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun visitLdcInsn(cst: Any?) {
        if (cst is String) {
            loadParam = cst
        }
        super.visitLdcInsn(cst)
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        if (opcode == Opcodes.NEW) {
            isNewOpcode = true
        }
        super.visitTypeInsn(opcode, type)
    }

}