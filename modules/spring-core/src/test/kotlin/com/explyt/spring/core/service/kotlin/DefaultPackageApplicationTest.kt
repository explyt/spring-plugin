/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.kotlin

import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.search.GlobalSearchScope

class DefaultPackageApplicationTest : ExplytKotlinLightTestCase() {

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

    fun testDefaultPackageApplicationScansNamedPackageComponents() {
        addDefaultPackageApplication()
        addComponent("com/explyt/sample/SampleService.kt", "com.explyt.sample", "SampleService", "Service")

        val activeBeans = activeBeanNames()

        assertTrue("precondition: the application is a bean, got $activeBeans", DEMO_APPLICATION in activeBeans)
        assertTrue("a default-package scan reaches $SAMPLE_SERVICE, got $activeBeans", SAMPLE_SERVICE in activeBeans)
    }

    fun testDefaultPackageApplicationScansDefaultPackageComponents() {
        addDefaultPackageApplication()
        addComponent("DefaultService.kt", null, DEFAULT_SERVICE, "Component")

        val activeBeans = activeBeanNames()

        assertTrue("precondition: the application is a bean, got $activeBeans", DEMO_APPLICATION in activeBeans)
        assertTrue("a default-package scan reaches $DEFAULT_SERVICE, got $activeBeans", DEFAULT_SERVICE in activeBeans)
    }

    fun testDefaultPackageApplicationScanRootsCoverEveryPackage() {
        addDefaultPackageApplication()

        val packages = PackageScanService.getInstance(project).getAllPackages().getPackages(module)

        assertFalse("precondition: the application declares a scan root", packages.isEmpty())
        for (qualifiedName in listOf(SAMPLE_SERVICE, DEFAULT_SERVICE)) {
            assertTrue("$qualifiedName must be covered by $packages", packages.any { qualifiedName.startsWith(it) })
        }
    }

    fun testNamedPackageApplicationDoesNotScanSiblingPackage() {
        myFixture.addFileToProject(
            "com/app/AppApplication.kt",
            """
            package com.app

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class AppApplication
            """.trimIndent()
        )
        addComponent("com/app/Inside.kt", "com.app", "Inside", "Component")
        addComponent("com/other/Outside.kt", "com.other", "Outside", "Component")

        val activeBeans = activeBeanNames()

        assertTrue("precondition: the application scans its package, got $activeBeans", "com.app.Inside" in activeBeans)
        assertFalse("no scan reaches com.other, got $activeBeans", "com.other.Outside" in activeBeans)
    }

    fun testWildcardScanRootDoesNotScanUnrelatedPackage() {
        myFixture.addFileToProject(
            "com/app/AppApplication.kt",
            """
            package com.app

            @org.springframework.boot.autoconfigure.SpringBootApplication(scanBasePackages = ["a*"])
            class AppApplication
            """.trimIndent()
        )
        addComponent("com/other/OutsideService.kt", "com.other", "OutsideService", "Component")
        val roots = PackageScanService.getInstance(project).getAllPackages()
        assertTrue("precondition: the application is recognised", roots.isRootComponent("com.app.AppApplication"))
        assertFalse("precondition: the scan packages are read", roots.getPackages(module).isEmpty())
        assertEquals("precondition: the outside component exists", "com.other.OutsideService", projectClass("com.other.OutsideService").qualifiedName)

        val activeBeans = activeBeanNames()

        assertFalse("a wildcard-derived empty prefix must not scan com.other.OutsideService", "com.other.OutsideService" in activeBeans)
    }

    fun testNormalizeDeclaredEmptyPackage() {
        assertEquals("", PackageScanService.normalizePackage(""))
    }

    fun testNormalizeNamedPackage() {
        assertEquals("com.app.", PackageScanService.normalizePackage("com.app"))
        assertEquals("com.app.", PackageScanService.normalizePackage("com.app.*"))
    }

    fun testNormalizeShortWildcardKeepsLegacyPrefix() {
        assertEquals(".", PackageScanService.normalizePackage("a*"))
    }

    fun testNormalizeDotWildcardKeepsLegacyPrefix() {
        assertEquals(".", PackageScanService.normalizePackage(".*"))
    }

    fun testNormalizeDoubleWildcardKeepsLegacyPrefix() {
        assertEquals(".", PackageScanService.normalizePackage("**"))
    }

    private fun addDefaultPackageApplication() {
        myFixture.addFileToProject(
            "DemoApplication.kt",
            """
            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class DemoApplication
            """.trimIndent()
        )
        val application = projectClass(DEMO_APPLICATION)
        assertEquals(
            "precondition: the application lives in the default package",
            "", (application.containingFile as PsiClassOwner).packageName
        )
        assertTrue(
            "precondition: the application is a root component",
            PackageScanService.getInstance(project).getAllPackages().isRootComponent(DEMO_APPLICATION)
        )
    }

    private fun addComponent(path: String, packageName: String?, className: String, stereotype: String) {
        val packageLine = packageName?.let { "package $it" }.orEmpty()
        myFixture.addFileToProject(
            path,
            """
            $packageLine

            @org.springframework.stereotype.$stereotype
            class $className
            """.trimIndent()
        )
    }

    private fun activeBeanNames(): Set<String> =
        SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }

    private fun projectClass(qualifiedName: String): PsiClass =
        JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.projectScope(project))
            ?: error("precondition: $qualifiedName must resolve in the project")

    private companion object {
        const val DEMO_APPLICATION = "DemoApplication"
        const val SAMPLE_SERVICE = "com.explyt.sample.SampleService"
        const val DEFAULT_SERVICE = "DefaultService"
    }
}
