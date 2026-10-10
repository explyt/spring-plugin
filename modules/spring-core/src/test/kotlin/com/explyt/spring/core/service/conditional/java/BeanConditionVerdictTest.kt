/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.java

import com.explyt.spring.core.service.conditional.BeanConditionVerdictTestCase
import com.explyt.spring.core.service.conditional.ConditionFixtures

class BeanConditionVerdictTest : BeanConditionVerdictTestCase() {
    override val fixtures: ConditionFixtures by lazy { JavaConditionFixtures(myFixture) }

    fun testMissingClassHasRuntimeClasspathAssumption() {
        myFixture.addFileToProject(
            "com/app/MissingClassConfig.java",
            """
            package com.app;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
            @Configuration
            @ConditionalOnClass(Missing.class)
            public class MissingClassConfig {}
            """.trimIndent()
        )
        val config = beanClass("com.app.MissingClassConfig")
        val annotation = "org.springframework.boot.autoconfigure.condition.ConditionalOnClass"
        assertAnnotatedBy(config, annotation)
        assertNull(com.intellij.psi.JavaPsiFacade.getInstance(project).findClass("com.app.Missing", config.resolveScope))

        val verdict = com.explyt.spring.core.service.SpringSearchService.getInstance(project).conditionVerdictOf(config, module)
        assertInstanceOf(verdict, com.explyt.spring.core.service.conditional.ConditionVerdict.Inactive::class.java)
        val evidence = (verdict as com.explyt.spring.core.service.conditional.ConditionVerdict.Inactive).condition
        assertEquals(annotation, evidence.annotationFqn)
        assertEquals(com.explyt.spring.core.service.conditional.ConditionReason.NOT_MATCHED, evidence.reason)
        assertEquals(setOf(com.explyt.spring.core.service.conditional.ConditionAssumption.COMPILE_CLASSPATH_IS_RUNTIME), evidence.assumptions)
    }
}
