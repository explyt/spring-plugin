/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.kotlin

import com.explyt.spring.core.service.conditional.BeanConditionVerdictTestCase
import com.explyt.spring.core.service.conditional.ConditionFixtures

class BeanConditionVerdictTest : BeanConditionVerdictTestCase() {
    override val fixtures: ConditionFixtures by lazy { KotlinConditionFixtures(myFixture) }

    fun testMissingClassHasRuntimeClasspathAssumption() {
        myFixture.addFileToProject(
            "com/app/MissingClassConfig.kt",
            """
            package com.app
            import org.springframework.context.annotation.Configuration
            import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
            @Configuration
            @ConditionalOnClass(Missing::class)
            class MissingClassConfig
            """.trimIndent()
        )
        val config = beanClass("com.app.MissingClassConfig")
        val annotation = "org.springframework.boot.autoconfigure.condition.ConditionalOnClass"
        assertAnnotatedBy(config, annotation)
        assertNull(com.intellij.psi.JavaPsiFacade.getInstance(project).findClass("com.app.Missing", config.resolveScope))

        myFixture.addFileToProject(
            "com/app/JavaMissingClassConfig.java",
            """
            package com.app;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
            @Configuration
            @ConditionalOnClass(Missing.class)
            public class JavaMissingClassConfig {}
            """.trimIndent()
        )
        val javaConfig = beanClass("com.app.JavaMissingClassConfig")
        assertAnnotatedBy(javaConfig, annotation)
        com.explyt.spring.core.tracker.ModificationTrackerManager.getInstance(project).invalidateAll()
        val service = com.explyt.spring.core.service.SpringSearchService.getInstance(project)
        val javaVerdict = service.conditionVerdictOf(javaConfig, module)
        assertInstanceOf(javaVerdict, com.explyt.spring.core.service.conditional.ConditionVerdict.Inactive::class.java)
        assertEquals(
            setOf(com.explyt.spring.core.service.conditional.ConditionAssumption.COMPILE_CLASSPATH_IS_RUNTIME),
            (javaVerdict as com.explyt.spring.core.service.conditional.ConditionVerdict.Inactive).condition.assumptions
        )

        val verdict = service.conditionVerdictOf(config, module)
        assertInstanceOf(verdict, com.explyt.spring.core.service.conditional.ConditionVerdict.Inactive::class.java)
        val evidence = (verdict as com.explyt.spring.core.service.conditional.ConditionVerdict.Inactive).condition
        assertEquals(annotation, evidence.annotationFqn)
        assertEquals(com.explyt.spring.core.service.conditional.ConditionReason.NOT_MATCHED, evidence.reason)
        assertEquals(setOf(com.explyt.spring.core.service.conditional.ConditionAssumption.COMPILE_CLASSPATH_IS_RUNTIME), evidence.assumptions)
    }
}
