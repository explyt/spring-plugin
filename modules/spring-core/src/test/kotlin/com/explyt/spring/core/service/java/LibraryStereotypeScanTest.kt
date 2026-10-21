/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.java

import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

class LibraryStereotypeScanTest : ExplytJavaLightTestCase() {

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
        addApplication("""@SpringBootApplication(scanBasePackages = {"com.app", "$BASIC_ERROR_CONTROLLER_PACKAGE"})""")
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

    private fun addApplication(annotation: String) {
        myFixture.addFileToProject(
            "com/app/AppApplication.java",
            """
            package com.app;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            $annotation
            public class AppApplication {}
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
        const val BASIC_ERROR_CONTROLLER_PACKAGE = "org.springframework.boot.autoconfigure.web.servlet.error"
        const val BASIC_ERROR_CONTROLLER = "$BASIC_ERROR_CONTROLLER_PACKAGE.BasicErrorController"
        const val SERVER_PROPERTIES = "org.springframework.boot.autoconfigure.web.ServerProperties"
    }
}
