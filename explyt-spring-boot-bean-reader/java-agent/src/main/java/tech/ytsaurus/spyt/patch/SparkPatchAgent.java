/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Original source code: https://github.com/ytsaurus/ytsaurus-spyt/blob/feature/spark-3.5.x_support/spark-patch/src/main/java/tech/ytsaurus/spyt/patch/SparkPatchAgent.java
 */

package tech.ytsaurus.spyt.patch;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.bytecode.ClassFile;
import tech.ytsaurus.spyt.patch.annotations.*;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static tech.ytsaurus.spyt.patch.SparkPatchClassTransformer.toClasspathName;

/**
 * Strategies for patching Spark classes:
 *
 * <ol>
 * <li>Completely replace class bytecode with provided implementation. This is the default strategy. The patched
 * class implementation must be annotated only with {@link OriginClass} annotation that should be parameterized with
 * full name of the original class.</li>
 *
 * <li>Replace with subclass. In this strategy the original class is preserved but renamed to "original name"Base
 * at runtime. The patched class should be placed into the same package as the original class and annotated with
 * {@link Subclass} and {@link OriginClass} annotations. The latter should be parameterized with full name of the
 * original class. At runtime the patched class is renamed to "original name" and has "original name"Base superclass
 * which is actually the original class before patching</li>
 *
 * <li>Decorate methods. In this strategy the base class body is generally preserved but is enriched with additional
 * decorating methods which are defined in decorator class. The decorator class should be annotated with
 * {@link Decorate} and {@link OriginClass} annotations. The latter should be parameterized with full name of the
 * original class. In the decorating class the methods should also be annotated with {@link DecoratedMethod} annotation.
 * The method should has the same name and signature as the original method. Also there may be a stub method that
 * has the same signature and the same name that is prefixed with __ when it is required to call original method from
 * the decorating method.</li>
 * </ol>
 */
public class SparkPatchAgent {


    public static void premain(String args, Instrumentation inst) throws IOException, ClassNotFoundException {
        Map<String, String> classMappings = new HashMap<>();
        classMappings.put(
                toClasspathName("com.explyt.spring.boot.bean.reader.AbstractApplicationContextDecorator"),
                toClasspathName("org.springframework.context.support.AbstractApplicationContext")
        );
        classMappings.put(
                toClasspathName("com.explyt.spring.boot.bean.reader.AspectJAopUtilsDecorator"),
                toClasspathName("org.springframework.aop.aspectj.AspectJAopUtils")
        );
        inst.addTransformer(new SparkPatchClassTransformer(classMappings));
    }
}

class SparkPatchClassTransformer implements ClassFileTransformer {

    private final Map<String, String> classMappings;
    private final Map<String, String> patchedClasses;

    SparkPatchClassTransformer(Map<String, String> classMappings) {
        this.classMappings = classMappings;
        this.patchedClasses = classMappings
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    static String getOriginClass(String fileName) throws IOException, ClassNotFoundException {
        String patchClassName = fileName.substring(0, fileName.length() - 6);
        Optional<ClassFile> optClassFile = loadClassFile(fileName);
        if (!optClassFile.isPresent()) {
            throw new RuntimeException("Class not found: " + fileName);
        }
        ClassFile classFile = optClassFile.get();
        CtClass ctClass = ClassPool.getDefault().makeClass(classFile);
        return getOriginClass(ctClass);
    }

    static String toClasspathName(String className) {
        return className.replace('.', File.separatorChar);
    }

    static String getOriginClass(CtClass ctClass) throws ClassNotFoundException {
        OriginClass originClassAnnotaion = (OriginClass) ctClass.getAnnotation(OriginClass.class);
        if (originClassAnnotaion != null) {
            String originClass = originClassAnnotaion.value();
            if (originClass.endsWith("$") && !ctClass.getName().endsWith("$")) {
                return null;
            }
            return originClass;
        }
        return null;
    }

    static ClassFile loadClassFile(byte[] classBytes) throws IOException {
        try {
            return new ClassFile(new DataInputStream(new ByteArrayInputStream(classBytes)));
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    static Optional<ClassFile> loadClassFile(String classFile) throws IOException {
        try (InputStream inputStream = SparkPatchClassTransformer.class.getClassLoader().getResourceAsStream(classFile)) {
            if (inputStream == null) return Optional.empty();
            byte[] byteArray = toByteArray(inputStream);
            return Optional.of(loadClassFile(byteArray));
        }
    }

    static byte[] serializeClass(ClassFile cf) throws IOException {
        ByteArrayOutputStream patchedBytesOutputStream = new ByteArrayOutputStream();
        cf.write(new DataOutputStream(patchedBytesOutputStream));
        patchedBytesOutputStream.flush();
        return patchedBytesOutputStream.toByteArray();
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {
        if (!patchedClasses.containsKey(className)) {
            return null;
        }

        try {
            String patchClassName = toPatchClassName(className);
            Optional<ClassFile> classFile = loadClassFile(patchClassName);
            ClassFile cf = classFile.orElseThrow(RuntimeException::new);
            cf = processAnnotations(cf, loader, classfileBuffer);
            cf.renameClass(classMappings);
            return serializeClass(cf);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SparkPatchException(e);
        }
    }

    private String toPatchClassName(String className) {
        return patchedClasses.get(className) + ".class";
    }

    private ClassFile processAnnotations(ClassFile cf, ClassLoader loader, byte[] baseClassBytes) throws Exception {
        CtClass ctClass = ClassPool.getDefault().makeClass(cf);
        String originClass = getOriginClass(ctClass);
        if (originClass == null) {
            return cf;
        }

        ClassFile processedClassFile = cf;
        for (Object annotation : ctClass.getAnnotations()) {
            if (annotation instanceof Subclass) {
                String baseClass = originClass + "Base";

                cf.setSuperclass(baseClass);
                cf.renameClass(originClass, baseClass);

                ClassFile baseCf = loadClassFile(baseClassBytes);
                baseCf.renameClass(originClass, baseClass);
                ClassPool.getDefault().makeClass(baseCf).toClass(loader, null);
            }

            if (annotation instanceof AddInterfaces) {
                ClassFile baseCf = loadClassFile(baseClassBytes);
                for (Class<?> i : ((AddInterfaces) annotation).value()) {
                    baseCf.addInterface(i.getName());
                }
                CtClass baseCtClass = ClassPool.getDefault().makeClass(baseCf);
                processedClassFile = baseCtClass.getClassFile();
            }

            if (annotation instanceof Decorate) {
                ClassFile baseCf = loadClassFile(baseClassBytes);
                CtClass baseCtClass = ClassPool.getDefault().makeClass(baseCf);
                for (CtMethod method : ctClass.getDeclaredMethods()) {
                    if (checkDecoratedMethod(method)) {
                        String methodName = method.getName();
                        CtMethod baseMethod = baseCtClass.getMethod(methodName, method.getSignature());

                        String innerMethodName = "__" + methodName;
                        baseMethod.setName(innerMethodName);

                        CtMethod newMethod = CtNewMethod.copy(method, baseCtClass, null);
                        baseCtClass.addMethod(newMethod);
                    } else if (method.hasAnnotation(AddMethod.class)) {
                        CtMethod newMethod = CtNewMethod.copy(method, baseCtClass, null);
                        baseCtClass.addMethod(newMethod);
                    }
                }

                processedClassFile = baseCtClass.getClassFile();
            }
        }

        return processedClassFile;
    }

    private static boolean checkDecoratedMethod(CtMethod method) {
        return method.hasAnnotation(DecoratedMethod.class);
    }

    private static byte[] toByteArray(InputStream inputStream) throws IOException {
        byte[] bytes = new byte[inputStream.available()];
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        dataInputStream.readFully(bytes);
        return bytes;
    }
}