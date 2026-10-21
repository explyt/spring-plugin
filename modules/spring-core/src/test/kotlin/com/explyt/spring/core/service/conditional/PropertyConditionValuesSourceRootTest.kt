/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary

class PropertyConditionValuesSourceRootTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testProductionValueWinsOverTestValue() {
        addFileToModule(module, "application.properties", "x.v=main\n")
        addTestSourceFileToModule(module, "application.properties", "x.v=test\n")

        assertBothDefinitionsAreInScope()
        assertEquals(ConditionPropertyValue.Known("main"), valueOf("x.v"))
    }

    fun testProductionValueWinsRegardlessOfCreationOrder() {
        addTestSourceFileToModule(module, "application.properties", "x.v=test\n")
        addFileToModule(module, "application.properties", "x.v=main\n")

        assertBothDefinitionsAreInScope()
        assertEquals(ConditionPropertyValue.Known("main"), valueOf("x.v"))
    }

    private fun assertBothDefinitionsAreInScope() {
        val definedValues = DefinedConfigurationPropertiesSearch.getInstance(project)
            .findProperties(module, "x.v")
            .mapNotNull { it.value }
            .sorted()

        assertEquals(listOf("main", "test"), definedValues)
    }

    private fun valueOf(key: String): ConditionPropertyValue {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return PropertyConditionValues(module).valueOf(key)
    }
}
