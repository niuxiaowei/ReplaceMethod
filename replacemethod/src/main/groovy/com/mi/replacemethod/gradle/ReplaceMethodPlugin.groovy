package com.mi.replacemethod.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle

/**
 * 插件类
 */
class ReplaceMethodPlugin implements Plugin<Project> {
    void apply(Project project) {

        //获得启动任务字符串
        Gradle gradle = project.getGradle()
        String tskReqStr = gradle.getStartParameter().getTaskRequests().toString()
        project.extensions.create("replaceMethod", Config)
        boolean isRelease = tskReqStr.contains("Release")
        println '*****************replacemethod Plugin apply*********************'
        if (project.plugins.hasPlugin(AppPlugin)) {
            def android = project.extensions.findByType(AppExtension)
            android.registerTransform(new ReplaceMethodTransform(project, isRelease))
        } else if (project.plugins.hasPlugin(LibraryPlugin)) {
            def android = project.extensions.findByType(LibraryExtension)
            android.registerTransform(new ReplaceMethodTransform(project, isRelease))
        }
    }
}