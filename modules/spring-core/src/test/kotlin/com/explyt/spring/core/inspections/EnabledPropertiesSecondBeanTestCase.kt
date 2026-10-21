/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytInspectionBaseTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope

abstract class EnabledPropertiesSecondBeanTestCase : ExplytInspectionBaseTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
    )

    protected abstract val extension: String

    protected abstract fun applicationSource(): String

    protected abstract fun propertiesSource(scanned: Boolean, packageName: String): String

    protected abstract fun enablerSource(packageName: String): String

    protected abstract fun consumerSource(qualifier: String?, packageName: String, fieldName: String = SCAN_NAME): String

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

        val errors = autowiringErrorsOf(consumerSource(qualifier = null, SCANNED_PACKAGE, fieldName = NEUTRAL_FIELD))

        assertEquals("autowiring errors: $errors", 1, errors.size)
        assertTrue(
            "autowiring error: ${errors.single()}",
            errors.single().startsWith("Autowire failed. There is more than one bean of 'Props'")
        )
    }

    fun testAmbiguityMessageNamesBothBeans() {
        addProject(scanned = true, enabled = true)
        assertPropertiesBeans(listOf(SCAN_NAME, ENABLED_NAME))

        val errors = autowiringErrorsOf(consumerSource(qualifier = null, SCANNED_PACKAGE, fieldName = NEUTRAL_FIELD))

        assertEquals("autowiring errors: $errors", 1, errors.size)
        val message = errors.single()
        assertEquals("occurrences of [$SCAN_NAME] in: $message", 1, message.windowed("[$SCAN_NAME]".length).count { it == "[$SCAN_NAME]" })
        assertEquals("occurrences of [$ENABLED_NAME] in: $message", 1, message.windowed("[$ENABLED_NAME]".length).count { it == "[$ENABLED_NAME]" })
    }

    fun testScannedComponentAlsoEnabledResolvesByFieldNameMatchingScanBean() {
        addProject(scanned = true, enabled = true)
        assertPropertiesBeans(listOf(SCAN_NAME, ENABLED_NAME))

        val errors = autowiringErrorsOf(consumerSource(qualifier = null, SCANNED_PACKAGE, fieldName = SCAN_NAME))

        assertEquals(emptyList<String>(), errors)
    }

    fun testScannedComponentAlsoEnabledResolvesEnabledNameQualifier() {
        addProject(scanned = true, enabled = true)

        val errors = autowiringErrorsOf(consumerSource(qualifier = ENABLED_NAME, SCANNED_PACKAGE))

        assertEquals(emptyList<String>(), errors)
    }

    fun testScannedOnlyComponentIsOneBeanWithoutAmbiguity() {
        addProject(scanned = true, enabled = false)

        assertPropertiesBeans(listOf(SCAN_NAME))
        assertEquals(emptyList<String>(), autowiringErrorsOf(consumerSource(qualifier = null, SCANNED_PACKAGE)))
    }

    fun testEnabledOnlyPropertiesIsOneBeanWithoutAmbiguity() {
        addProject(scanned = false, enabled = true, packageName = UNSCANNED_PACKAGE)

        assertPropertiesBeans(listOf(UNSCANNED_ENABLED_NAME), UNSCANNED_PROPERTIES)
        assertEquals(emptyList<String>(), autowiringErrorsOf(consumerSource(qualifier = null, UNSCANNED_PACKAGE)))
    }

    fun testPlainPropertiesInScanRootEnabledIsOneBean() {
        addPlainPropertiesInScanRoot()

        val names = activePropertiesBeanNames()
        assertEquals("active beans of $PROPERTIES: $names", 1, names.size)
        assertEquals(emptyList<String>(), autowiringErrorsOf(consumerSource(qualifier = null, SCANNED_PACKAGE, fieldName = NEUTRAL_FIELD)))
    }

    fun testPlainPropertiesInScanRootEnabledIsNamedByTheRegistrar() {
        addPlainPropertiesInScanRoot()

        assertPropertiesBeans(listOf(ENABLED_NAME))
    }

    private fun addPlainPropertiesInScanRoot() {
        addProject(scanned = false, enabled = true, packageName = SCANNED_PACKAGE)
        val propertiesClass = JavaPsiFacade.getInstance(project).findClass(PROPERTIES, GlobalSearchScope.projectScope(project))
        assertNotNull("precondition: $PROPERTIES resolves", propertiesClass)
        assertFalse(
            "precondition: $PROPERTIES is not a @Component",
            AnnotationUtil.isAnnotated(propertiesClass!!, SpringCoreClasses.COMPONENT, 0)
        )
    }

    private fun addProject(scanned: Boolean, enabled: Boolean, packageName: String = SCANNED_PACKAGE) {
        val properties = "$packageName.Props"
        myFixture.addFileToProject("$SCANNED_PACKAGE/App.$extension", applicationSource())
        myFixture.addFileToProject("$packageName/Props.$extension", propertiesSource(scanned, packageName))
        if (enabled) myFixture.addFileToProject("$SCANNED_PACKAGE/PropsConfig.$extension", enablerSource(packageName))
        ModificationTrackerManager.getInstance(project).invalidateAll()

        val packages = PackageScanService.getInstance(project).getAllPackages()
        val scanPackages = packages.getPackages(module)
        assertEquals("precondition: the application scans its own package", setOf("$SCANNED_PACKAGE."), scanPackages.toSet())
        assertEquals(
            "precondition: $properties is under the scan packages $scanPackages only when it lives in $SCANNED_PACKAGE",
            packageName == SCANNED_PACKAGE, scanPackages.any { properties.startsWith(it) }
        )
        assertEquals(
            "precondition: @EnableConfigurationProperties is read only when declared",
            enabled, properties in packages.importQualified
        )
    }

    private fun activePropertiesBeanNames(properties: String = PROPERTIES): List<String> =
        SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .filter { it.psiClass.qualifiedName == properties }
            .map { it.name }
            .sorted()

    private fun assertPropertiesBeans(expectedNames: List<String>, properties: String = PROPERTIES) {
        val names = activePropertiesBeanNames(properties)
        assertEquals("active beans of $properties: $names", expectedNames.size, names.size)
        assertEquals("names of the active beans of $properties", expectedNames.sorted(), names)
    }

    private fun autowiringErrorsOf(consumer: String): List<String> {
        myFixture.configureByText("Consumer.$extension", consumer)
        return myFixture.doHighlighting().mapNotNull { it.description }
            .filter { it.startsWith("Autowire failed") || it.startsWith("Cannot find bean") }
    }

    protected companion object {
        const val PREFIX = "naming"
        const val SCANNED_PACKAGE = "app"
        const val UNSCANNED_PACKAGE = "ext"
        const val PROPERTIES = "$SCANNED_PACKAGE.Props"
        const val UNSCANNED_PROPERTIES = "$UNSCANNED_PACKAGE.Props"
        const val SCAN_NAME = "props"
        const val NEUTRAL_FIELD = "holder"
        const val ENABLED_NAME = "$PREFIX-$PROPERTIES"
        const val UNSCANNED_ENABLED_NAME = "$PREFIX-$UNSCANNED_PROPERTIES"
    }
}
