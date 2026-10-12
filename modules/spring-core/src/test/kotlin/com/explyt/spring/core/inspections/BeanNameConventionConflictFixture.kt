/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.util.SpringBootUtil
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.module.Module

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
object BeanNameConventionConflictFixture {
    const val CONFIGURATION = "demo.AppConfiguration"

    fun addUnknownSpringVersionStubs(fixture: CodeInsightTestFixture) {
        fixture.addFileToProject(
            "org/springframework/core/env/Environment.java",
            "package org.springframework.core.env; public interface Environment {}"
        )
        fixture.addFileToProject(
            "org/springframework/context/annotation/Bean.java",
            """
            package org.springframework.context.annotation;
            import java.lang.annotation.*;
            @Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Bean { String[] value() default {}; String[] name() default {}; }
            """.trimIndent()
        )
        fixture.addFileToProject(
            "org/springframework/core/annotation/AliasFor.java",
            """
            package org.springframework.core.annotation;
            import java.lang.annotation.*;
            @Target(ElementType.METHOD)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface AliasFor {
                String value() default "";
                String attribute() default "";
                Class<? extends Annotation> annotation() default Annotation.class;
            }
            """.trimIndent()
        )
    }

    fun problems(fixture: CodeInsightTestFixture): List<HighlightInfo> {
        val toolId = SpringBeanNameConventionConflictInspection().shortName
        return fixture.doHighlighting().filter { it.inspectionToolId == toolId }
    }

fun assertPreconditions(fixture: CodeInsightTestFixture, module: Module, usedAnnotation: String, springMajor: Int?) {
        assertEquals("spring-core major version of the module", springMajor, SpringBootUtil.getSpringCoreMajorVersion(module))
        if (springMajor != null) {
            assertTrue(
                "the inspection is available for the file",
                SpringBeanNameConventionConflictInspection().isAvailableForFile(fixture.file)
            )
        } else {
            assertNotNull(
                "the source stub makes the Spring environment type resolvable",
                findClassOrNull(fixture, "org.springframework.core.env.Environment")
            )
        }
        val factory = findClass(fixture, CONFIGURATION).findMethodsByName("foo", false).single()
        val usage = factory.getAnnotation(usedAnnotation)
        assertNotNull("@$usedAnnotation is on the factory method", usage)
        val composed = resolve(fixture, usage!!)
        assertEquals("the composed annotation resolves", usedAnnotation, composed?.qualifiedName)
        assertTrue("the composed declaration is an annotation type", composed!!.isAnnotationType)
        val bean = metaBean(fixture, composed, emptySet())
        assertNotNull("$usedAnnotation is meta-annotated with Spring @Bean", bean)
        assertTrue(
            "the meta @Bean resolves to the expected source or library",
            if (springMajor == null) {
                bean!!.qualifiedName == SpringCoreClasses.BEAN
            } else {
                bean!!.containingFile.virtualFile.path.substringBefore("!/").substringAfterLast('/').startsWith("spring-context-")
            }
        )
    }

    fun assertSingleConflict(problems: List<HighlightInfo>, useSite: String, annotationName: String, metaName: String) {
        assertEquals("one problem: $problems", 1, problems.size)
        val problem = problems.single()
        assertEquals(HighlightSeverity.ERROR, problem.severity)
        assertEquals(useSite, problem.text)
        assertEquals(
            SpringCoreBundle.message("explyt.spring.inspection.bean.name.convention.conflict", annotationName, metaName),
            problem.description
        )
    }

    fun assertNoConflict(problems: List<HighlightInfo>) {
        assertEquals("no problem: $problems", 0, problems.size)
    }



    private fun metaBean(fixture: CodeInsightTestFixture, type: PsiClass, visited: Set<String>): PsiClass? {
        val name = type.qualifiedName ?: return null
        if (name == SpringCoreClasses.BEAN) return type
        if (name in visited || name.startsWith("java.") || name.startsWith("kotlin.")) return null
        return type.annotations.firstNotNullOfOrNull { meta ->
            resolve(fixture, meta)?.let { metaBean(fixture, it, visited + name) }
        }
    }

    private fun resolve(fixture: CodeInsightTestFixture, annotation: PsiAnnotation): PsiClass? =
        annotation.resolveAnnotationType() ?: annotation.qualifiedName?.let { findClassOrNull(fixture, it) }

    private fun findClass(fixture: CodeInsightTestFixture, fqn: String): PsiClass =
        findClassOrNull(fixture, fqn) ?: error("$fqn is not found")

    private fun findClassOrNull(fixture: CodeInsightTestFixture, fqn: String): PsiClass? =
        JavaPsiFacade.getInstance(fixture.project).findClass(fqn, GlobalSearchScope.allScope(fixture.project))
}
