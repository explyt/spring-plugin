/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.TestSourcesFilter
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.intellij.lang.annotations.Language

/**
 * A `@SpringBootApplication` declared in test sources is the root of a test context only. The production application of
 * the same module scans its own packages, so a production component outside them is no bean of it, even when the test
 * application's root package encloses that component (#772, apollo's `LocalPortalApplication`).
 */
class TestSourceApplicationScanRootsTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.kotlin_1_9_22,
    )

    fun testJavaTestSourceApplicationDoesNotWidenTheProductionScanOfItsModule() {
        addFileToModule(module, "app/portal/PortalApplication.java", JAVA_PORTAL_APPLICATION)
        addFileToModule(module, "app/portal/PortalService.java", JAVA_PORTAL_SERVICE)
        addFileToModule(module, "app/audit/Audit.java", JAVA_AUDIT)
        val localApp = addTestSourceFileToModule(module, "app/LocalApp.java", JAVA_LOCAL_APP)

        assertTestApplicationDoesNotLeak(localApp, auditModule = module)
    }

    fun testKotlinTestSourceApplicationDoesNotWidenTheProductionScanOfItsModule() {
        addFileToModule(module, "app/portal/PortalApplication.kt", KOTLIN_PORTAL_APPLICATION)
        addFileToModule(module, "app/portal/PortalService.kt", KOTLIN_PORTAL_SERVICE)
        addFileToModule(module, "app/audit/Audit.kt", KOTLIN_AUDIT)
        val localApp = addTestSourceFileToModule(module, "app/LocalApp.kt", KOTLIN_LOCAL_APP)

        assertTestApplicationDoesNotLeak(localApp, auditModule = module)
    }

    fun testJavaTestSourceApplicationDoesNotAdmitComponentsOfADependencyModule() {
        val audit = addDependencyModule("audit")
        addFileToModule(audit, "app/audit/Audit.java", JAVA_AUDIT)
        addFileToModule(module, "app/portal/PortalApplication.java", JAVA_PORTAL_APPLICATION)
        addFileToModule(module, "app/portal/PortalService.java", JAVA_PORTAL_SERVICE)
        val localApp = addTestSourceFileToModule(module, "app/LocalApp.java", JAVA_LOCAL_APP)

        assertTestApplicationDoesNotLeak(localApp, auditModule = audit)
    }

    fun testKotlinTestSourceApplicationDoesNotAdmitComponentsOfADependencyModule() {
        val audit = addDependencyModule("audit")
        addFileToModule(audit, "app/audit/Audit.kt", KOTLIN_AUDIT)
        addFileToModule(module, "app/portal/PortalApplication.kt", KOTLIN_PORTAL_APPLICATION)
        addFileToModule(module, "app/portal/PortalService.kt", KOTLIN_PORTAL_SERVICE)
        val localApp = addTestSourceFileToModule(module, "app/LocalApp.kt", KOTLIN_LOCAL_APP)

        assertTestApplicationDoesNotLeak(localApp, auditModule = audit)
    }

    fun testJavaWithoutTheTestSourceApplicationTheComponentOutsideTheScanIsNoBean() {
        val audit = addDependencyModule("audit")
        addFileToModule(audit, "app/audit/Audit.java", JAVA_AUDIT)
        addFileToModule(module, "app/portal/PortalApplication.java", JAVA_PORTAL_APPLICATION)
        addFileToModule(module, "app/portal/PortalService.java", JAVA_PORTAL_SERVICE)

        assertProductionScanOnly(auditModule = audit)
    }

    fun testKotlinWithoutTheTestSourceApplicationTheComponentOutsideTheScanIsNoBean() {
        addFileToModule(module, "app/portal/PortalApplication.kt", KOTLIN_PORTAL_APPLICATION)
        addFileToModule(module, "app/portal/PortalService.kt", KOTLIN_PORTAL_SERVICE)
        addFileToModule(module, "app/audit/Audit.kt", KOTLIN_AUDIT)

        assertProductionScanOnly(auditModule = module)
    }

    private fun assertTestApplicationDoesNotLeak(localApp: PsiFile, auditModule: Module) {
        assertTrue(
            "Precondition: LocalApp must live in a test source root",
            TestSourcesFilter.isTestSources(localApp.virtualFile, project)
        )
        assertFalse(
            "Precondition: PortalApplication must live in production sources",
            TestSourcesFilter.isTestSources(findClass(PORTAL_APPLICATION)!!.containingFile.virtualFile, project)
        )
        assertNotNull("Precondition: the test application must resolve", findClass(LOCAL_APP))
        assertProductionScanOnly(auditModule)
    }

    private fun assertProductionScanOnly(auditModule: Module) {
        assertNotNull("Precondition: Audit must resolve", findClass(AUDIT))
        val productionApplications = readAction {
            val bootApplication = findClass(SPRING_BOOT_APPLICATION) ?: return@readAction emptyList()
            AnnotatedElementsSearch.searchPsiClasses(bootApplication, GlobalSearchScope.projectScope(project))
                .findAll()
                .filterNot { TestSourcesFilter.isTestSources(it.containingFile.virtualFile, project) }
                .mapNotNull { it.qualifiedName }
        }
        assertEquals("Precondition: the only production application", listOf(PORTAL_APPLICATION), productionApplications)

        val packages = readAction { PackageScanService.getInstance(project).getAllPackages().getPackages(module) }
        assertTrue("The production application scans its own package, got $packages", packages.contains("app.portal."))

        val beans = activeBeanNames(module)
        assertTrue("The production application's own component is a bean, got $beans", beans.contains(PORTAL_SERVICE))
        assertFalse(
            "A component outside the production application's scan is no bean of the application module, got $beans",
            beans.contains(AUDIT)
        )
        val auditModuleBeans = activeBeanNames(auditModule)
        assertFalse(
            "A component outside the production application's scan is no bean of its own module, got $auditModuleBeans",
            auditModuleBeans.contains(AUDIT)
        )
        assertFalse(
            "Only the production application's root applies to production code, got $packages",
            packages.contains("app.")
        )
    }

    private fun activeBeanNames(module: Module): Set<String> = readAction {
        SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }
    }

    private fun findClass(qualifiedName: String) = readAction {
        JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project))
    }

    private fun <T> readAction(action: () -> T): T = ReadAction.compute<T, RuntimeException> { action() }

    private companion object {
        const val PORTAL_APPLICATION = "app.portal.PortalApplication"
        const val PORTAL_SERVICE = "app.portal.PortalService"
        const val LOCAL_APP = "app.LocalApp"
        const val AUDIT = "app.audit.Audit"
        const val SPRING_BOOT_APPLICATION = "org.springframework.boot.autoconfigure.SpringBootApplication"

        @Language("java")
        val JAVA_PORTAL_APPLICATION = """
            package app.portal;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class PortalApplication {}
            """.trimIndent()

        @Language("java")
        val JAVA_PORTAL_SERVICE = """
            package app.portal;

            import org.springframework.stereotype.Service;

            @Service
            public class PortalService {}
            """.trimIndent()

        @Language("java")
        val JAVA_AUDIT = """
            package app.audit;

            import org.springframework.stereotype.Component;

            @Component
            public class Audit {}
            """.trimIndent()

        @Language("java")
        val JAVA_LOCAL_APP = """
            package app;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class LocalApp {}
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_PORTAL_APPLICATION = """
            package app.portal

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class PortalApplication
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_PORTAL_SERVICE = """
            package app.portal

            import org.springframework.stereotype.Service

            @Service
            class PortalService
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_AUDIT = """
            package app.audit

            import org.springframework.stereotype.Component

            @Component
            class Audit
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_LOCAL_APP = """
            package app

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class LocalApp
            """.trimIndent()
    }
}
