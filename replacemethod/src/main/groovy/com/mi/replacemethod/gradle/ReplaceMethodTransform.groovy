package com.mi.replacemethod.gradle

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.mi.replacemethod.Config
import com.mi.replacemethod.ReplaceClassVisitor
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES
/**
 * custom transform: tranform classes before dex
 * inputType、scope、isIncremental主要根据 task:transformClassesWithDex来设定
 */
class ReplaceMethodTransform extends Transform {

    private Project project
    private boolean isRelease
    private static sAMSVersion = getAMSVersion()

    ReplaceMethodTransform(Project project, boolean isRelease) {
        this.project = project
        this.isRelease = isRelease
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        //读取配置
        Config replaceMethodConfig = initConfig()
        if (!replaceMethodConfig.open) {
            println '[ReplaceMethod]: closed'
        }

        Collection<TransformInput> inputs = transformInvocation.inputs
        TransformOutputProvider outputProvider = transformInvocation.outputProvider
        if (outputProvider != null) {
            outputProvider.deleteAll()
        }

        //遍历
        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                traceSrcFiles(directoryInput, outputProvider, replaceMethodConfig)
            }

            input.jarInputs.each { JarInput jarInput ->
                traceJarFiles(jarInput, outputProvider, replaceMethodConfig)
            }
        }
    }

    Config initConfig() {
        def replaceMethodConfig = project.replaceMethod
        Config config = new Config(replaceMethodConfig.open, replaceMethodConfig.openLog, isRelease, replaceMethodConfig.getLogFilters())
        if (replaceMethodConfig.replaceByMethods != null && replaceMethodConfig.replaceByMethods.getAbsReplaceByMethods() != null) {
            config.methods = replaceMethodConfig.replaceByMethods.getAbsReplaceByMethods().methods
        }
        println '[ReplaceMethod]:   config: ' + config
        return config
    }

    @Override
    String getName() {
        return "ReplaceMethod"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return true
    }

    private static int getAMSVersion() {
        Class opClass = Opcodes.class
        //从asm7 asm6 asm5找
        try {
            opClass.getDeclaredField("ASM7")
            println '[ReplaceMethod]:   amsversion: asm7'
            return Opcodes.ASM7
        } catch (Exception e) {
            println '[ReplaceMethod]:   get amsversion7 exception:'+e.getMessage()
        }
        try {
            opClass.getDeclaredField("ASM6")
            println '[ReplaceMethod]:   amsversion: asm6'
            return Opcodes.ASM6
        } catch (Exception e) {
            println '[ReplaceMethod]:   get amsversion6 exception:'+e.getMessage()
        }

        println '[ReplaceMethod]:   amsversion: asm5'
        return Opcodes.ASM5
    }


    static void traceSrcFiles(DirectoryInput directoryInput, TransformOutputProvider outputProvider, Config config) {

        if (directoryInput.file.isDirectory()) {
            if (config.openLog) {
//                println '[ReplaceMethod]:traceSrcFiles   filename:' + directoryInput.file
            }
            directoryInput.file.eachFileRecurse { File file ->
                def name = file.name
                if (config.isNeedTraceClass(name)) {
                    ClassReader classReader = new ClassReader(file.bytes)
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                    ClassVisitor cv = new ReplaceClassVisitor(sAMSVersion, classWriter, config)
                    classReader.accept(cv, EXPAND_FRAMES)
                    byte[] code = classWriter.toByteArray()
                    FileOutputStream fos = new FileOutputStream(
                            file.parentFile.absolutePath + File.separator + name)
                    fos.write(code)
                    fos.close()
                }
            }
        }

        //处理完输出给下一任务作为输入
        def dest = outputProvider.getContentLocation(directoryInput.name,
                directoryInput.contentTypes, directoryInput.scopes,
                Format.DIRECTORY)
        FileUtils.copyDirectory(directoryInput.file, dest)
    }


    static void traceJarFiles(JarInput jarInput, TransformOutputProvider outputProvider, Config config) {
        if (config.openLog) {
//            println '[ReplaceMethod]:traceJarFiles   ---------file:' + jarInput.file.getAbsolutePath()
        }
        if (jarInput.file.getAbsolutePath().endsWith(".jar")) {
            //重命名输出文件,因为可能同名,会覆盖
            def jarName = jarInput.name
            def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length() - 4)
            }
            JarFile jarFile = new JarFile(jarInput.file)
            Enumeration enumeration = jarFile.entries()

            File tmpFile = new File(jarInput.file.getParent() + File.separator + "classes_temp.jar")
            if (tmpFile.exists()) {
                tmpFile.delete()
            }

            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile))

            //循环jar包里的文件
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                String entryName = jarEntry.getName()
                ZipEntry zipEntry = new ZipEntry(entryName)
                InputStream inputStream = jarFile.getInputStream(jarEntry)

                if (config.isNeedTraceClass(entryName)) {
                    jarOutputStream.putNextEntry(zipEntry)
                    ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream))
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                    ClassVisitor cv = new ReplaceClassVisitor(sAMSVersion, classWriter, config)
                    classReader.accept(cv, EXPAND_FRAMES)
                    byte[] code = classWriter.toByteArray()
                    jarOutputStream.write(code)
                } else {
                    jarOutputStream.putNextEntry(zipEntry)
                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
                }
                jarOutputStream.closeEntry()
            }

            jarOutputStream.close()
            jarFile.close()

            //处理完输出给下一任务作为输入
            def dest = outputProvider.getContentLocation(jarName + md5Name,
                    jarInput.contentTypes, jarInput.scopes, Format.JAR)
            FileUtils.copyFile(tmpFile, dest)

            tmpFile.delete()
        }
    }
}