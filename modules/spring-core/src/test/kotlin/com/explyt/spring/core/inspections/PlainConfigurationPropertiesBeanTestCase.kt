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
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.GlobalSearchScope

abstract class PlainConfigurationPropertiesBeanTestCase : ExplytInspectionBaseTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
    )

    protected abstract val extension: String

    protected abstract fun applicationSource(propertiesScan: Boolean): String

    protected abstract fun propertiesSource(
        component: Boolean,
        packageName: String,
        className: String = PROPERTIES_CLASS,
        prefix: String = PREFIX,
    ): String

    protected abstract fun enablerSource(packageName: String): String

    protected abstract fun consumerSource(packageName: String): String

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

    fun testPlainPropertiesInScanRootIsNotABean() {
        addProject()

        assertPropertiesBeans(emptyList())
    }

    fun testPlainPropertiesInScanRootFailsAutowiring() {
        addProject()

        assertEquals(listOf("Autowire failed. No beans of 'Props' found"), autowiringErrorsOf(consumerSource(SCANNED_PACKAGE)))
    }

    fun testPlainPropertiesInScanRootEnabledIsNamedByTheRegistrar() {
        addProject(enabled = true)

        assertPropertiesBeans(listOf(ENABLED_NAME))
    }

    fun testPropertiesScanRegistersPlainPropertiesByTheRegistrarName() {
        addProject(propertiesScan = true)

        assertPropertiesBeans(listOf(ENABLED_NAME))
        assertEquals(emptyList<String>(), autowiringErrorsOf(consumerSource(SCANNED_PACKAGE)))
    }

    fun testPropertiesScanSkipsComponentProperties() {
        addProject(component = true, propertiesScan = true)

        assertPropertiesBeans(listOf(SCAN_NAME))
    }

    fun testComponentPropertiesInScanRootIsScanned() {
        addProject(component = true)

        assertPropertiesBeans(listOf(SCAN_NAME))
        assertEquals(emptyList<String>(), autowiringErrorsOf(consumerSource(SCANNED_PACKAGE)))
    }

    fun testPlainPropertiesOutsideScanEnabledIsNamedByTheRegistrar() {
        addProject(enabled = true, packageName = UNSCANNED_PACKAGE)

        assertPropertiesBeans(listOf(UNSCANNED_ENABLED_NAME), UNSCANNED_PROPERTIES)
        assertEquals(emptyList<String>(), autowiringErrorsOf(consumerSource(UNSCANNED_PACKAGE)))
    }

    fun testPropertiesScanFollowsClassRename() {
        addProject(propertiesScan = true, consumer = true)
        assertPropertiesBeans(listOf(ENABLED_NAME))

        myFixture.renameElement(findProjectClass(PROPERTIES).navigationElement, RENAMED_CLASS)

        assertNotNull("precondition: the rename produced $RENAMED_PROPERTIES", findProjectClassOrNull(RENAMED_PROPERTIES))
        assertPropertiesBeans(listOf("$PREFIX-$RENAMED_PROPERTIES"), RENAMED_PROPERTIES)
        assertEquals(emptyList<String>(), autowiringErrorsOfProjectConsumer())
    }

    fun testPropertiesScanFollowsNewFile() {
        addProject(propertiesScan = true)
        assertPropertiesBeans(listOf(ENABLED_NAME))

        myFixture.addFileToProject(
            "$SCANNED_PACKAGE/$EXTRA_CLASS.$extension",
            propertiesSource(component = false, SCANNED_PACKAGE, EXTRA_CLASS, EXTRA_PREFIX)
        )

        assertNotNull("precondition: $EXTRA_PROPERTIES resolves", findProjectClassOrNull(EXTRA_PROPERTIES))
        assertPropertiesBeans(listOf("$EXTRA_PREFIX-$EXTRA_PROPERTIES"), EXTRA_PROPERTIES)
    }

    fun testPropertiesScanFollowsMoveIntoScannedPackage() {
        addProject(propertiesScan = true, packageName = UNSCANNED_PACKAGE)
        assertPropertiesBeans(emptyList(), UNSCANNED_PROPERTIES)

        myFixture.moveFile("$UNSCANNED_PACKAGE/$PROPERTIES_CLASS.$extension", SCANNED_PACKAGE)

        assertNotNull("precondition: the move produced $PROPERTIES", findProjectClassOrNull(PROPERTIES))
        assertNull("precondition: $UNSCANNED_PROPERTIES is gone", findProjectClassOrNull(UNSCANNED_PROPERTIES))
        assertPropertiesBeans(listOf(ENABLED_NAME))
    }

    fun testPropertiesScanFollowsScanPackageEdit() {
        addProject(propertiesScan = true, scanPackage = UNSCANNED_PACKAGE)
        assertPropertiesBeans(emptyList())

        val application = myFixture.findFileInTempDir("$SCANNED_PACKAGE/App.$extension")
        val document = FileDocumentManager.getInstance().getDocument(application)!!
        WriteCommandAction.runWriteCommandAction(project) {
            document.setText(document.text.replace("\"$UNSCANNED_PACKAGE\"", "\"$SCANNED_PACKAGE\""))
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }

        assertPropertiesBeans(listOf(ENABLED_NAME))
    }

    private fun addProject(
        component: Boolean = false,
        enabled: Boolean = false,
        propertiesScan: Boolean = false,
        packageName: String = SCANNED_PACKAGE,
        consumer: Boolean = false,
        scanPackage: String? = null,
    ) {
        val properties = "$packageName.$PROPERTIES_CLASS"
        val application = applicationSource(propertiesScan).let { source ->
            if (scanPackage == null) source
            else source.replace("@ConfigurationPropertiesScan", "@ConfigurationPropertiesScan(\"$scanPackage\")")
        }
        myFixture.addFileToProject("$SCANNED_PACKAGE/App.$extension", application)
        myFixture.addFileToProject("$packageName/$PROPERTIES_CLASS.$extension", propertiesSource(component, packageName))
        if (enabled) myFixture.addFileToProject("$SCANNED_PACKAGE/PropsConfig.$extension", enablerSource(packageName))
        if (consumer) myFixture.addFileToProject("$SCANNED_PACKAGE/Consumer.$extension", consumerSource(packageName))
        ModificationTrackerManager.getInstance(project).invalidateAll()

        val propertiesClass = JavaPsiFacade.getInstance(project).findClass(properties, GlobalSearchScope.projectScope(project))
        assertNotNull("precondition: $properties resolves", propertiesClass)
        assertEquals(
            "precondition: $properties is a @Component only when declared so",
            component, AnnotationUtil.isAnnotated(propertiesClass!!, SpringCoreClasses.COMPONENT, 0)
        )
        if (propertiesScan) {
            val scanAnnotation = JavaPsiFacade.getInstance(project)
                .findClass(SpringCoreClasses.CONFIGURATION_PROPERTIES_SCAN, GlobalSearchScope.allScope(project))
            assertNotNull("precondition: @ConfigurationPropertiesScan is on the classpath", scanAnnotation)
        }

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

    private fun assertPropertiesBeans(expectedNames: List<String>, properties: String = PROPERTIES) {
        val names = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .filter { it.psiClass.qualifiedName == properties }
            .map { it.name }
            .sorted()
        assertEquals("names of the active beans of $properties", expectedNames.sorted(), names)
    }

    private fun autowiringErrorsOf(consumer: String): List<String> {
        myFixture.configureByText("Consumer.$extension", consumer)
        return autowiringErrors()
    }

    private fun autowiringErrorsOfProjectConsumer(): List<String> {
        myFixture.configureFromTempProjectFile("$SCANNED_PACKAGE/Consumer.$extension")
        return autowiringErrors()
    }

    private fun autowiringErrors(): List<String> = myFixture.doHighlighting().mapNotNull { it.description }
        .filter { it.startsWith("Autowire failed") || it.startsWith("Cannot find bean") }

    private fun findProjectClassOrNull(qualifiedName: String): PsiClass? =
        JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.projectScope(project))

    private fun findProjectClass(qualifiedName: String): PsiClass =
        checkNotNull(findProjectClassOrNull(qualifiedName)) { "precondition: $qualifiedName resolves" }

    protected companion object {
        const val PREFIX = "naming"
        const val SCANNED_PACKAGE = "app"
        const val UNSCANNED_PACKAGE = "ext"
        const val PROPERTIES_CLASS = "Props"
        const val PROPERTIES = "$SCANNED_PACKAGE.$PROPERTIES_CLASS"
        const val UNSCANNED_PROPERTIES = "$UNSCANNED_PACKAGE.$PROPERTIES_CLASS"
        const val RENAMED_CLASS = "AppProps"
        const val RENAMED_PROPERTIES = "$SCANNED_PACKAGE.$RENAMED_CLASS"
        const val EXTRA_CLASS = "Extra"
        const val EXTRA_PREFIX = "extra"
        const val EXTRA_PROPERTIES = "$SCANNED_PACKAGE.$EXTRA_CLASS"
        const val SCAN_NAME = "props"
        const val NEUTRAL_FIELD = "holder"
        const val ENABLED_NAME = "$PREFIX-$PROPERTIES"
        const val UNSCANNED_ENABLED_NAME = "$PREFIX-$UNSCANNED_PROPERTIES"
    }
}
