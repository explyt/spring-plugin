/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.java

import com.explyt.spring.core.service.conditional.CompiledPropertyConditionTestCase
import com.explyt.spring.core.service.conditional.ConditionFixtures
import com.explyt.spring.core.service.conditional.ConditionVerdict

class CompiledPropertyConditionTest : CompiledPropertyConditionTestCase() {
    override val fixtures: ConditionFixtures by lazy { JavaConditionFixtures(myFixture) }

    override fun addProjectConfiguration(condition: PropertyCondition) {
        myFixture.addFileToProject(
            "com/app/$PROJECT_CONFIGURATION.java",
            """
            package com.app;

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            ${condition.java}
            public class $PROJECT_CONFIGURATION {
                @Bean
                public StringBuilder projectService() { return new StringBuilder(); }
            }
            """.trimIndent()
        )
    }

    fun testCompiledNameWithMatchingHavingValueIsActive() {
        addProperties("lib.feature.enabled=true")
        val method = libraryBeanMethod(PropertyCondition.NAME_HAVING_VALUE)
        assertPropertyDefined("lib.feature.enabled")

        assertEquals(ConditionVerdict.Active, verdictOf(method))
    }

    fun testCompiledNameWithMismatchingHavingValueIsInactive() {
        addProperties("lib.feature.enabled=false")
        val method = libraryBeanMethod(PropertyCondition.NAME_HAVING_VALUE)
        assertPropertyDefined("lib.feature.enabled")

        assertInactiveNaming(verdictOf(method), "lib.feature.enabled")
    }

    fun testCompiledPrefixJoinedWithNameIsActive() {
        addProperties("lib.feature.enabled=true")
        val method = libraryBeanMethod(PropertyCondition.PREFIX_AND_NAME)
        assertPropertyDefined("lib.feature.enabled")

        assertEquals(ConditionVerdict.Active, verdictOf(method))
    }

    fun testCompiledValueAliasWithMissingPropertyIsInactive() {
        addProperties("lib.other=present")
        val method = libraryBeanMethod(PropertyCondition.VALUE_ALIAS)
        assertPropertyDefined("lib.other")
        assertPropertyUndefined("lib.jndi-name")

        assertInactiveNaming(verdictOf(method), "lib.jndi-name")
    }
}
