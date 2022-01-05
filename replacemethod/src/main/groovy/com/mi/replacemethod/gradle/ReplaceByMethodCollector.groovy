package com.mi.replacemethod.gradle

import com.mi.replacemethod.AbsReplaceByMethods
import org.gradle.api.GradleException
import org.gradle.util.ConfigureUtil

/**
 * 本类主要包含了 需要替换方法（replace）和 用哪个方法替换（by） 的一个集合,在编译过程中会对replace使用by进行替换
 */
class ReplaceByMethodCollector {

    private AbsReplaceByMethods absReplaceByMethods

    ReplaceByMethodCollector register(Closure c) {
        ReplaceByMethodParser replaceByMethodParser = new ReplaceByMethodParser()
        ConfigureUtil.configure(c, replaceByMethodParser)
        if (replaceByMethodParser.replace.className == null && replaceByMethodParser.by.className == null) {
            return this
        }
        if (replaceByMethodParser.replace.className == null) {
            throw new GradleException("replace not set. eg \n replace { classname java.lang.String    methodName toString    desc  ()void}")
        }
        if (replaceByMethodParser.by.className == null) {
            throw new GradleException("by not set. eg  \n by { classname java.lang.String    methodName toString }")
        }
        if (absReplaceByMethods == null) {
            absReplaceByMethods = new AbsReplaceByMethods()
        }
        absReplaceByMethods.add(replaceByMethodParser.replace, replaceByMethodParser.by)
        return this
    }

    AbsReplaceByMethods getAbsReplaceByMethods() {
        return absReplaceByMethods
    }


    @Override
    String toString() {
        return "ReplaceByMethodCollector{" +
                ", absReplaceByMethods=" + absReplaceByMethods +
                '}';
    }
}











