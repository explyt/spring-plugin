/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytInspectionBaseTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry

abstract class EnabledPropertiesSecondBeanTestCase : ExplytInspectionBaseTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
    )

    protected abstract val extension: String

    protected abstract fun applicationSource(): String

    protected abstract fun propertiesSource(scanned: Boolean): String

    protected abstract fun enablerSource(): String

    protected abstract fun consumerSource(qualifier: String?): String

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
        myFixture.enableInspections(SpringBeanIncorrectAutowiringInspection::class.java)
    }

    override fun tearDown() {
        try {
            Registry.get("explyt.spring.root.runConfiguration").resetToDefault()
        } finally {
            super.tearDown()
        }
    }

    fun testScannedComponentAlsoEnabledIsTwoBeans() {
        addProject(scanned = true, enabled = true)

        assertPropertiesBeans(listOf(SCAN_NAME, ENABLED_NAME))
    }

    fun testScannedComponentAlsoEnabledMakesUnqualifiedInjectionAmbiguous() {
        addProject(scanned = true, enabled = true)

        val errors = autowiringErrorsOf(consumerSource(qualifier = null))

        assertEquals(listOf("Autowire failed. There is more than one bean of 'Props'"), errors)
    }

    fun testScannedComponentAlsoEnabledResolvesEnabledNameQualifier() {
        addProject(scanned = true, enabled = true)

        val errors = autowiringErrorsOf(consumerSource(qualifier = ENABLED_NAME))

        assertEquals(emptyList<String>(), errors)
    }

    fun testScannedOnlyComponentIsOneBeanWithoutAmbiguity() {
        addProject(scanned = true, enabled = false)

        assertPropertiesBeans(listOf(SCAN_NAME))
        assertEquals(emptyList<String>(), autowiringErrorsOf(consumerSource(qualifier = null)))
    }

    fun testEnabledOnlyPropertiesIsOneBeanWithoutAmbiguity() {
        addProject(scanned = false, enabled = true)

        assertPropertiesBeans(listOf(ENABLED_NAME))
        assertEquals(emptyList<String>(), autowiringErrorsOf(consumerSource(qualifier = null)))
    }

    private fun addProject(scanned: Boolean, enabled: Boolean) {
        myFixture.addFileToProject("app/App.$extension", applicationSource())
        myFixture.addFileToProject("app/Props.$extension", propertiesSource(scanned))
        if (enabled) myFixture.addFileToProject("app/PropsConfig.$extension", enablerSource())
        ModificationTrackerManager.getInstance(project).invalidateAll()

        val packages = PackageScanService.getInstance(project).getAllPackages()
        assertEquals("precondition: the application scans its own package", setOf("app."), packages.getPackages(module).toSet())
        assertEquals(
            "precondition: @EnableConfigurationProperties is read only when declared",
            enabled, PROPERTIES in packages.importQualified
        )
    }

    private fun assertPropertiesBeans(expectedNames: List<String>) {
        val names = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .filter { it.psiClass.qualifiedName == PROPERTIES }
            .map { it.name }
            .sorted()
        assertEquals("active beans of $PROPERTIES: $names", expectedNames.size, names.size)
        assertEquals("names of the active beans of $PROPERTIES", expectedNames.sorted(), names)
    }

    private fun autowiringErrorsOf(consumer: String): List<String> {
        myFixture.configureByText("Consumer.$extension", consumer)
        return myFixture.doHighlighting().mapNotNull { it.description }
            .filter { it.startsWith("Autowire failed") || it.startsWith("Cannot find bean") }
    }

    protected companion object {
        const val PREFIX = "naming"
        const val PROPERTIES = "app.Props"
        const val SCAN_NAME = "props"
        const val ENABLED_NAME = "$PREFIX-$PROPERTIES"
    }
}
