package com.mi.replacemethod.gradle

import com.mi.replacemethod.By
import com.mi.replacemethod.InvokeType
import com.mi.replacemethod.Replace
import org.gradle.api.GradleException
import org.gradle.util.ConfigureUtil

/**
 * 本类主要包含了 需要替换方法（replace）和 用哪个方法替换（by）的一一对应关系
 */
class ReplaceByMethodParser {
    private final Replace replace = new Replace()
    private final By by = new By()

    //java与字节码的基础类型的对应表
    private final static Map<String, String> JAVA_CLASS_BASE_TYPE_TABLE = new HashMap<>()
    static {
        JAVA_CLASS_BASE_TYPE_TABLE.put("int", "I")
        JAVA_CLASS_BASE_TYPE_TABLE.put("long", "J")
        JAVA_CLASS_BASE_TYPE_TABLE.put("boolean", "Z")
        JAVA_CLASS_BASE_TYPE_TABLE.put("float", "F")
        JAVA_CLASS_BASE_TYPE_TABLE.put("double", "D")
        JAVA_CLASS_BASE_TYPE_TABLE.put("char", "C")
        JAVA_CLASS_BASE_TYPE_TABLE.put("short", "S")
        JAVA_CLASS_BASE_TYPE_TABLE.put("void", "V")
        JAVA_CLASS_BASE_TYPE_TABLE.put("byte", "B")

        //数组
        JAVA_CLASS_BASE_TYPE_TABLE.put("int[]", "[I")
        JAVA_CLASS_BASE_TYPE_TABLE.put("long[]", "[J")
        JAVA_CLASS_BASE_TYPE_TABLE.put("boolean[]", "[Z")
        JAVA_CLASS_BASE_TYPE_TABLE.put("float[]", "[F")
        JAVA_CLASS_BASE_TYPE_TABLE.put("double[]", "[D")
        JAVA_CLASS_BASE_TYPE_TABLE.put("char[]", "[C")
        JAVA_CLASS_BASE_TYPE_TABLE.put("short[]", "[S")
        JAVA_CLASS_BASE_TYPE_TABLE.put("byte[]", "[B")

    }

    /**
     * 配置需要替换的方法
     * @param c
     * @return
     */
    ReplaceByMethodParser replace(Closure c) {
        ConfigureUtil.configure(c, replace)
        println("[ReplaceMethod]:  ReplaceByMethodParser  start  replace:${replace}")
        checkClassName(replace.className, "replace")
        checkInvokeType(replace)
        //若是对new 实例  进行替换，则直接把methodName设置为<init>
        if (replace.invokeType == InvokeType.INVOKE_NEW) {
            replace.methodName = "<init>"
        } else {
            checkMethodName(replace.methodName, "replace")
        }
        replace.className = replace.className.replace(".", "/")
        replace.desc = convertJavaDesc2ClassDesc(replace.desc, "replace")
        println("[ReplaceMethod]:  ReplaceByMethodParser  end  replace:${replace}")
        return this
    }

    private static boolean checkInvokeType(Replace replace) {
        if (replace.invokeType != InvokeType.INVOKE_NEW && replace.invokeType != InvokeType.INVOKE_VIRTUAL && replace.invokeType != InvokeType.INVOKE_STATIC) {
            throw new GradleException("must set invokeType for Replace. \n  替换静态方法 use:  invokeType \"${InvokeType.INVOKE_STATIC}\"\n" +
                    "替换实例方法 use: invokeType \"${InvokeType.INVOKE_VIRTUAL}\" \n"+
                    "替换构造函数 use: invokeType \"${InvokeType.INVOKE_NEW}\" ")
        }
    }

    /**
     * 配置需要哟弄哪个方法把replace给替换掉
     * @param c
     */
    void by(Closure c) {
        ConfigureUtil.configure(c, by)
        println("[ReplaceMethod]:  ReplaceByMethodParser  start  by:${by}")
        checkClassName(by.className, "by")
        if (strIsEmptyOrNull(by.methodName)) {
            by.methodName = replace.methodName
        }
        checkMethodName(by.methodName, "by")
        by.className = by.className.replace(".", "/")
        println("[ReplaceMethod]:  ReplaceByMethodParser  end  by:${by}")
    }


    /**
     * 用于检测配置的classname是否符合规则
     * @param className
     * @param errMsg
     * @return
     */
    private boolean checkClassName(String className, String byOrReplace) {
        if (className != null && !isValidJavaFullClassName(className)) {
            throw new GradleException("className  of ${byOrReplace}  must valid java full name。（eg :  className java.lang.String  className  java.lang.Object）")
        }
    }

    private static boolean strIsEmptyOrNull(String str) {
        return (str == null || str.replaceAll("\\s*", "") == "")
    }

    private boolean checkMethodName(String methodName, String byOrReplace) {
        if (strIsEmptyOrNull(methodName)) {
            throw new GradleException("methodName  of ${byOrReplace}  must set。（eg :  methodName test）")
        }
    }


    private String deleteGenericsClass(String oriDesc) {
        int startIndex = oriDesc.indexOf("<");
        int endIndex = oriDesc.indexOf(">");
        return oriDesc.substring(0, startIndex) + oriDesc.substring(endIndex + 1);
    }

    /**
     * 从oriDesc把泛型的class信息删除掉，因为泛型在class中的信息都会被擦除，因此若在desc中
     * 配置了泛型的class信息则直接删除,比如:java.util.List<com.test.Demo> --> java.util.List
     * @param oriDesc
     * @return
     */
    private String tryDeleteGenericsClass(String oriDesc) {
        while (oriDesc.contains("<") && oriDesc.contains(">")) {
            oriDesc = deleteGenericsClass(oriDesc);
        }
        return oriDesc;
    }

    /**
     * 把java源码的方法描述符转化为字节码的方法秒速符
     * @param desc
     * @param byOrReplace
     * @return
     */
    private String convertJavaDesc2ClassDesc(String desc, String byOrReplace) {
        if (desc != null && desc.replaceAll("\\s*", "") != "") {

            desc = tryDeleteGenericsClass(desc)

            int leftBracketIndex = desc.indexOf("(")
            int rightBracketIndex = desc.indexOf(")")
            if (leftBracketIndex < 0 || rightBracketIndex < 0 || rightBracketIndex < leftBracketIndex) {
                throw new GradleException("desc  of ${byOrReplace}  invalid。eg :  desc (int,java.lang.String,boolean,float)void  \n desc ()void   \n desc (int)java.lang.String \n desc (java.lang.String)int ")
            }

            String result = "("
            String[] params = desc.substring(leftBracketIndex + 1, rightBracketIndex).split(",")

            //检测参数是不是合法的
            for (int i = 0; i < params.length; i++) {
                String param = params[i].replaceAll("\\s*", "")
                if (param == "") {
                    continue
                }
                String baseTypeClassValue = JAVA_CLASS_BASE_TYPE_TABLE.get(param)
                if (baseTypeClassValue != null) {
                    result += baseTypeClassValue
                } else if (param.endsWith("[]") && isValidJavaFullClassName(param.substring(0, param.indexOf("[")))) {
                    //param是数组
                    result += "[L" + param.replace(".", "/") + ";"
                } else if (isValidJavaFullClassName(param)) {
                    result += "L" + param.replace(".", "/") + ";"
                } else {
                    throw new GradleException("${param} of desc is   invalid。eg :  desc (int,java.lang.String,boolean,float)void  \n desc ()void   \n desc (int)java.lang.String \n desc (java.lang.String)int ")
                }
            }
            result += ")"
            String returnType = desc.substring(rightBracketIndex + 1)
            //没有设置返回type则默认为void
            if (strIsEmptyOrNull(returnType)) {
                result += "V"
            } else {
                String baseTypeClassValue = JAVA_CLASS_BASE_TYPE_TABLE.get(returnType)
                if (baseTypeClassValue != null) {
                    result += baseTypeClassValue
                } else if (returnType.endsWith("[]") && isValidJavaFullClassName(returnType.substring(0, returnType.indexOf("[")))) {
                    result += "[L" + returnType.replace(".", "/") + ";"
                } else if (isValidJavaFullClassName(returnType)) {
                    result += "L" + returnType.replace(".", "/") + ";"
                } else {
                    throw new GradleException("${returnType}  is   invalid。eg :  desc (int,java.lang.String,boolean,float)void  \n desc ()void   \n desc (int)java.lang.String \n desc (java.lang.String)int ")
                }
            }


            return result

        } else {
            //没有设置则认为方法的参数为空，返回类型为void
            return "()V"
        }
    }

    private boolean isValidJavaIdentifier(String className) {
        //确定是否允许将指定字符作为 Java 标识符中的首字符。
        if (className.length() == 0
                || !Character.isJavaIdentifierStart(className.charAt(0)))
            return false;

        String name = className.substring(1);
        for (int i = 0; i < name.length(); i++)
            if (!Character.isJavaIdentifierPart(name.charAt(i)))
                return false;
        return true;
    }


    /**
     *  对 package name 和 class name 进行校验
     * @param fullName
     * @return
     */
    private boolean isValidJavaFullClassName(String fullName) {
        if (fullName.equals("")) {
            return false;
        }
        boolean result = true;
        try {
            int index = fullName.indexOf(".");
            if (index != -1) {
                String[] str = fullName.split("\\.");
                for (String name : str) {
                    if (name == "") {
                        result = false;
                        break;
                    } else if (!isValidJavaIdentifier(name)) {
                        result = false;
                        break;
                    }
                }
            } else if (!isValidJavaIdentifier(fullName)) {
                result = false;
            }
        } catch (Exception ex) {
            result = false;
            ex.printStackTrace();
        }
        return result;
    }

    Replace getReplace() {
        return replace
    }

    By getBy() {
        return by
    }

    @Override
    public String toString() {
        return "ReplaceByMethodInfo{" +
                "replace=" + replace +
                ", by=" + by +
                '}';
    }
}