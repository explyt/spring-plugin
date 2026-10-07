/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary

/**
 * Expected verdicts follow Spring Boot's `OnPropertyCondition` (identical in 3.5.16 and 4.1.0): every repeated
 * annotation must match, every name must match, `havingValue` is compared ignoring case, an empty `havingValue`
 * means "not `false`", and `matchIfMissing` applies only to a missing key.
 */
abstract class ConditionalOnPropertyBootSemanticsTestCase : ExplytBaseLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_4_1_0)

    protected fun addProperties(fileName: String, vararg lines: String) {
        myFixture.addFileToProject(fileName, lines.joinToString("\n"))
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    protected fun assertActive(beanClass: String, vararg requiredKeys: String) {
        assertPreconditions(beanClass, requiredKeys)
        assertTrue(
            "Spring Boot activates $beanClass, but the plugin excluded it",
            beanClass in activeBeanClasses()
        )
    }

    protected fun assertInactive(beanClass: String, vararg requiredKeys: String) {
        assertPreconditions(beanClass, requiredKeys)
        assertFalse(
            "Spring Boot does not activate $beanClass, but the plugin reports it active",
            beanClass in activeBeanClasses()
        )
    }

    private fun assertPreconditions(beanClass: String, requiredKeys: Array<out String>) {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        val foundBeanClasses = activeBeanClasses() + excludedBeanClasses()
        assertTrue("Bean class $beanClass must be found, got $foundBeanClasses", beanClass in foundBeanClasses)

        val definedKeys = DefinedConfigurationPropertiesSearch.getInstance(project).getPropertiesCommonKeyMap(module)
        for (key in requiredKeys) {
            assertTrue(
                "Property '$key' must be loaded from the configuration files, got ${definedKeys.keys}",
                PropertyUtil.toCommonPropertyForm(key) in definedKeys
            )
        }
    }

    private fun activeBeanClasses(): Set<String> =
        SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }

    private fun excludedBeanClasses(): Set<String> =
        SpringSearchServiceFacade.getInstance(project).getExcludedBeansClasses(module)
            .mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }
}
