/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.service.conditional.ConditionVerdict
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

class LibraryStereotypeScanTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springContext_6_0_7,
    )

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
    }

    override fun tearDown() {
        try {
            Registry.get("explyt.spring.root.runConfiguration").resetToDefault()
        } finally {
            super.tearDown()
        }
    }

    fun testLibraryControllerOutsideScannedPackagesIsNotAComponent() {
        addApplication("@SpringBootApplication")
        assertLibraryClassOutsideScan(BASIC_ERROR_CONTROLLER)

        val components = componentRecordsOf(BASIC_ERROR_CONTROLLER)

        assertEmpty(
            "no scan reaches $BASIC_ERROR_CONTROLLER; Boot registers it only through its auto-configuration factory",
            components
        )
    }

    fun testLibraryControllerInScannedPackageIsAComponent() {
        addApplication("""@SpringBootApplication(scanBasePackages = ["com.app", "$BASIC_ERROR_CONTROLLER_PACKAGE"])""")
        assertLibraryClass(BASIC_ERROR_CONTROLLER)

        val components = componentRecordsOf(BASIC_ERROR_CONTROLLER)

        assertEquals("a scan of its package registers $BASIC_ERROR_CONTROLLER", 1, components.size)
    }

    fun testLibraryConfigurationPropertiesStayBeansWithoutScan() {
        addApplication("@SpringBootApplication")
        assertLibraryClassOutsideScan(SERVER_PROPERTIES)

        val beans = activeBeansOf(SERVER_PROPERTIES)

        assertEquals(
            "library @ConfigurationProperties classes stay beans until @EnableConfigurationProperties is read",
            1, beans.size
        )
    }

    fun testLibraryStereotypeImportedByAnActiveConfigurationIsAnActiveBean() {
        addApplication("@SpringBootApplication")
        addErrorControllerImporter(gated = false)
        assertImportedLibraryStereotypeOutsideScan(BASIC_ERROR_CONTROLLER)
        assertEquals("precondition: $IMPORTER is active", ConditionVerdict.Active, verdictOf(IMPORTER))

        val verdict = verdictOf(BASIC_ERROR_CONTROLLER)

        assertEquals(ConditionVerdict.Active, verdict)
        assertEquals("@Import registers $BASIC_ERROR_CONTROLLER without a scan", 1, componentRecordsOf(BASIC_ERROR_CONTROLLER).size)
    }

    fun testLibraryStereotypeImportedByAnInactiveConfigurationIsInactiveWithTheImporterAsCarrier() {
        addApplication("@SpringBootApplication")
        addErrorControllerImporter(gated = true)
        assertImportedLibraryStereotypeOutsideScan(BASIC_ERROR_CONTROLLER)
        assertEquals("precondition: $IMPORTER is inactive by its own condition", IMPORTER, inactiveCarrierOf(IMPORTER))

        val carrier = inactiveCarrierOf(BASIC_ERROR_CONTROLLER)

        assertEquals(IMPORTER, carrier)
        assertEmpty(componentRecordsOf(BASIC_ERROR_CONTROLLER))
        assertTrue(
            "$BASIC_ERROR_CONTROLLER must be an excluded class bean",
            SpringSearchServiceFacade.getInstance(project).getExcludedBeansClasses(module)
                .any { it.psiMember is PsiClass && it.psiClass.qualifiedName == BASIC_ERROR_CONTROLLER }
        )
    }

    private fun addErrorControllerImporter(gated: Boolean) {
        val gate = if (gated) """@ConditionalOnProperty(name = ["importer.enabled"], havingValue = "true")""" else ""
        myFixture.addFileToProject(
            "com/app/ErrorControllerImporter.kt",
            """
            package com.app

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController
            import org.springframework.context.annotation.Configuration
            import org.springframework.context.annotation.Import

            @Configuration
            $gate
            @Import(BasicErrorController::class)
            class ErrorControllerImporter
            """.trimIndent()
        )
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    private fun assertImportedLibraryStereotypeOutsideScan(qualifiedName: String) {
        assertLibraryClassOutsideScan(qualifiedName)
        val psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project))!!
        assertTrue(
            "precondition: $qualifiedName is a stereotype",
            AnnotationUtil.isAnnotated(psiClass, SpringCoreClasses.CONTROLLER, 0)
        )
        assertTrue(
            "precondition: $qualifiedName is reached through @Import",
            qualifiedName in PackageScanService.getInstance(project).getAllPackages().importQualified
        )
    }

    private fun verdictOf(qualifiedName: String): ConditionVerdict? {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        val psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project))!!
        return SpringSearchService.getInstance(project).conditionVerdictOf(psiClass, module)
    }

    private fun inactiveCarrierOf(qualifiedName: String): String? {
        val verdict = verdictOf(qualifiedName)
        assertInstanceOf(verdict, ConditionVerdict.Inactive::class.java)
        val condition = (verdict as ConditionVerdict.Inactive).condition
        assertEquals(CONDITIONAL_ON_PROPERTY, condition.annotationFqn)
        return condition.carrierFqn
    }

    private fun addApplication(annotation: String) {
        myFixture.addFileToProject(
            "com/app/AppApplication.kt",
            """
            package com.app

            import org.springframework.boot.autoconfigure.SpringBootApplication

            $annotation
            class AppApplication
            """.trimIndent()
        )
        assertTrue(
            "precondition: the application is a root component",
            PackageScanService.getInstance(project).getAllPackages().isRootComponent(APPLICATION)
        )
    }

    private fun assertLibraryClassOutsideScan(qualifiedName: String) {
        assertLibraryClass(qualifiedName)
        val packages = PackageScanService.getInstance(project).getAllPackages().getPackages(module)
        assertFalse("precondition: the application declares scan roots", packages.isEmpty())
        assertTrue(
            "precondition: no scan root $packages covers $qualifiedName",
            packages.none { qualifiedName.startsWith(it) }
        )
    }

    private fun assertLibraryClass(qualifiedName: String) {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project))
        assertNotNull("precondition: $qualifiedName resolves", psiClass)
        assertTrue(
            "precondition: $qualifiedName comes from a library",
            ProjectFileIndex.getInstance(project).isInLibrary(psiClass!!.containingFile.virtualFile)
        )
    }

    private fun activeBeansOf(qualifiedName: String): List<PsiBean> =
        SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .filter { it.psiClass.qualifiedName == qualifiedName }

    private fun componentRecordsOf(qualifiedName: String): List<PsiBean> =
        activeBeansOf(qualifiedName).filter { it.psiMember is PsiClass }

    private companion object {
        const val APPLICATION = "com.app.AppApplication"
        const val IMPORTER = "com.app.ErrorControllerImporter"
        const val CONDITIONAL_ON_PROPERTY = "org.springframework.boot.autoconfigure.condition.ConditionalOnProperty"
        const val BASIC_ERROR_CONTROLLER_PACKAGE = "org.springframework.boot.autoconfigure.web.servlet.error"
        const val BASIC_ERROR_CONTROLLER = "$BASIC_ERROR_CONTROLLER_PACKAGE.BasicErrorController"
        const val SERVER_PROPERTIES = "org.springframework.boot.autoconfigure.web.ServerProperties"
    }
}
