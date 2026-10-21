/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service

import com.explyt.spring.core.inspections.SpringBeanIncorrectAutowiringInspection
import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.util.registry.Registry
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

    override fun tearDown() {
        try {
            Registry.get("explyt.spring.root.runConfiguration").resetToDefault()
        } finally {
            super.tearDown()
        }
    }

    fun testJavaTestClassAutowiresAComponentOnlyTheTestApplicationScans() {
        addFileToModule(module, "app/portal/PortalApplication.java", JAVA_PORTAL_APPLICATION)
        addFileToModule(module, "app/audit/Audit.java", JAVA_AUDIT)
        val localApp = addTestSourceFileToModule(module, "app/LocalApp.java", JAVA_LOCAL_APP)
        val auditIt = addTestSourceFileToModule(module, "app/AuditIT.java", JAVA_AUDIT_IT)

        assertTestClassFindsTheAudit(localApp, auditIt)
    }

    fun testKotlinTestClassAutowiresAComponentOnlyTheTestApplicationScans() {
        addFileToModule(module, "app/portal/PortalApplication.kt", KOTLIN_PORTAL_APPLICATION)
        addFileToModule(module, "app/audit/Audit.kt", KOTLIN_AUDIT)
        val localApp = addTestSourceFileToModule(module, "app/LocalApp.kt", KOTLIN_LOCAL_APP)
        val auditIt = addTestSourceFileToModule(module, "app/AuditIT.kt", KOTLIN_AUDIT_IT)

        assertTestClassFindsTheAudit(localApp, auditIt)
    }

    fun testJavaTestClassAutowiresADependencyModuleComponentOnlyTheTestApplicationScans() {
        val audit = addDependencyModule("audit")
        addFileToModule(audit, "app/audit/Audit.java", JAVA_AUDIT)
        addFileToModule(module, "app/portal/PortalApplication.java", JAVA_PORTAL_APPLICATION)
        val localApp = addTestSourceFileToModule(module, "app/LocalApp.java", JAVA_LOCAL_APP)
        val auditIt = addTestSourceFileToModule(module, "app/AuditIT.java", JAVA_AUDIT_IT)

        assertTestClassFindsTheAudit(localApp, auditIt)
    }

    fun testKotlinTestClassAutowiresADependencyModuleComponentOnlyTheTestApplicationScans() {
        val audit = addDependencyModule("audit")
        addFileToModule(audit, "app/audit/Audit.kt", KOTLIN_AUDIT)
        addFileToModule(module, "app/portal/PortalApplication.kt", KOTLIN_PORTAL_APPLICATION)
        val localApp = addTestSourceFileToModule(module, "app/LocalApp.kt", KOTLIN_LOCAL_APP)
        val auditIt = addTestSourceFileToModule(module, "app/AuditIT.kt", KOTLIN_AUDIT_IT)

        assertTestClassFindsTheAudit(localApp, auditIt)
    }

    fun testJavaProductionComponentStillFindsNoAuditBesideATestSourceApplication() {
        addFileToModule(module, "app/portal/PortalApplication.java", JAVA_PORTAL_APPLICATION)
        addFileToModule(module, "app/audit/Audit.java", JAVA_AUDIT)
        addTestSourceFileToModule(module, "app/LocalApp.java", JAVA_LOCAL_APP)
        val consumer = addFileToModule(module, "app/portal/AuditConsumer.java", JAVA_AUDIT_CONSUMER)

        assertProductionConsumerFindsNoAudit(consumer)
    }

    fun testKotlinProductionComponentStillFindsNoAuditBesideATestSourceApplication() {
        addFileToModule(module, "app/portal/PortalApplication.kt", KOTLIN_PORTAL_APPLICATION)
        addFileToModule(module, "app/audit/Audit.kt", KOTLIN_AUDIT)
        addTestSourceFileToModule(module, "app/LocalApp.kt", KOTLIN_LOCAL_APP)
        val consumer = addFileToModule(module, "app/portal/AuditConsumer.kt", KOTLIN_AUDIT_CONSUMER)

        assertProductionConsumerFindsNoAudit(consumer)
    }

    fun testJavaTestClassQualifiesAComponentOnlyTheTestApplicationScans() {
        addFileToModule(module, "app/portal/PortalApplication.java", JAVA_PORTAL_APPLICATION)
        addFileToModule(module, "app/audit/Audit.java", JAVA_QUALIFIED_AUDITS)
        val consumer = addFileToModule(module, "app/portal/AuditConsumer.java", JAVA_QUALIFIED_AUDIT_CONSUMER)
        val localApp = addTestSourceFileToModule(module, "app/LocalApp.java", JAVA_LOCAL_APP)
        val auditIt = addTestSourceFileToModule(module, "app/AuditIT.java", JAVA_QUALIFIED_AUDIT_IT)

        assertTestClassQualifiesTheAudit(localApp, auditIt, consumer)
    }

    fun testKotlinTestClassQualifiesAComponentOnlyTheTestApplicationScans() {
        addFileToModule(module, "app/portal/PortalApplication.kt", KOTLIN_PORTAL_APPLICATION)
        addFileToModule(module, "app/audit/Audit.kt", KOTLIN_QUALIFIED_AUDITS)
        val consumer = addFileToModule(module, "app/portal/AuditConsumer.kt", KOTLIN_QUALIFIED_AUDIT_CONSUMER)
        val localApp = addTestSourceFileToModule(module, "app/LocalApp.kt", KOTLIN_LOCAL_APP)
        val auditIt = addTestSourceFileToModule(module, "app/AuditIT.kt", KOTLIN_QUALIFIED_AUDIT_IT)

        assertTestClassQualifiesTheAudit(localApp, auditIt, consumer)
    }

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

    private fun assertTestClassFindsTheAudit(localApp: PsiFile, auditIt: PsiFile) {
        addLibraries(module, listOf(TestLibrary.springBootTest_3_2_3), DependencyScope.TEST)
        assertTrue(
            "Precondition: LocalApp must live in a test source root",
            TestSourcesFilter.isTestSources(localApp.virtualFile, project)
        )
        assertTrue(
            "Precondition: AuditIT must live in a test source root",
            TestSourcesFilter.isTestSources(auditIt.virtualFile, project)
        )
        assertFalse(
            "Precondition: Audit must live in production sources",
            TestSourcesFilter.isTestSources(findClass(AUDIT)!!.containingFile.virtualFile, project)
        )
        assertNotNull("Precondition: @SpringBootTest must resolve", findClass(SPRING_BOOT_TEST))

        val errors = autowiringErrorsOf(auditIt)
        assertTrue(
            "A @SpringBootTest of the test application autowires a component its scan reaches, got $errors",
            errors.none { it.contains("No beans of 'Audit'") }
        )
    }

    private fun assertProductionConsumerFindsNoAudit(consumer: PsiFile) {
        assertFalse(
            "Precondition: AuditConsumer must live in production sources",
            TestSourcesFilter.isTestSources(consumer.virtualFile, project)
        )

        val errors = autowiringErrorsOf(consumer)
        assertTrue(
            "A production component outside the test application's context finds no Audit, got $errors",
            errors.any { it.contains("No beans of 'Audit'") }
        )
    }

    private fun assertTestClassQualifiesTheAudit(localApp: PsiFile, auditIt: PsiFile, productionConsumer: PsiFile) {
        addLibraries(module, listOf(TestLibrary.springBootTest_3_2_3), DependencyScope.TEST)
        assertTrue(
            "Precondition: LocalApp must live in a test source root",
            TestSourcesFilter.isTestSources(localApp.virtualFile, project)
        )
        assertTrue(
            "Precondition: AuditIT must live in a test source root",
            TestSourcesFilter.isTestSources(auditIt.virtualFile, project)
        )
        assertFalse(
            "Precondition: AuditConsumer must live in production sources",
            TestSourcesFilter.isTestSources(productionConsumer.virtualFile, project)
        )
        assertNotNull("Precondition: @SpringBootTest must resolve", findClass(SPRING_BOOT_TEST))
        val productionErrors = qualifiedAutowiringErrorsOf(productionConsumer)
        assertTrue(
            "Precondition: the production consumer outside the test context is reported, got $productionErrors",
            productionErrors.isNotEmpty()
        )

        val errors = qualifiedAutowiringErrorsOf(auditIt)
        assertEquals(
            "A @SpringBootTest of the test application qualifies the components its scan reaches",
            emptyList<String>(),
            errors
        )
    }

    private fun qualifiedAutowiringErrorsOf(file: PsiFile): List<String> {
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
        myFixture.enableInspections(SpringBeanIncorrectAutowiringInspection::class.java)
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        return myFixture.doHighlighting().mapNotNull { it.description }
            .filter { it.startsWith("Autowire failed") || it.startsWith("Cannot find bean with @Qualifier") }
    }

    private fun autowiringErrorsOf(file: PsiFile): List<String> {
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
        myFixture.enableInspections(SpringBeanIncorrectAutowiringInspection::class.java)
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        return myFixture.doHighlighting().mapNotNull { it.description }
            .filter { it.startsWith("Autowire failed") }
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
        const val SPRING_BOOT_TEST = "org.springframework.boot.test.context.SpringBootTest"

        @Language("java")
        val JAVA_AUDIT_IT = """
            package app;

            import app.audit.Audit;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.boot.test.context.SpringBootTest;

            @SpringBootTest(classes = LocalApp.class)
            class AuditIT {
                @Autowired
                Audit audit;
            }
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_AUDIT_IT = """
            package app

            import app.audit.Audit
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.boot.test.context.SpringBootTest

            @SpringBootTest(classes = [LocalApp::class])
            class AuditIT {
                @Autowired
                lateinit var audit: Audit
            }
            """.trimIndent()

        @Language("java")
        val JAVA_QUALIFIED_AUDITS = """
            package app.audit;

            import org.springframework.beans.factory.annotation.Qualifier;
            import org.springframework.stereotype.Component;

            public interface Audit {}

            @Component
            @Qualifier("x")
            class AuditA implements Audit {}

            @Component
            @Qualifier("x")
            class AuditC implements Audit {}

            @Component
            @Qualifier("y")
            class AuditB implements Audit {}
            """.trimIndent()

        @Language("java")
        val JAVA_QUALIFIED_AUDIT_IT = """
            package app;

            import app.audit.Audit;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.beans.factory.annotation.Qualifier;
            import org.springframework.boot.test.context.SpringBootTest;

            @SpringBootTest(classes = LocalApp.class)
            class AuditIT {
                @Autowired
                @Qualifier("x")
                Audit audit;
            }
            """.trimIndent()

        @Language("java")
        val JAVA_QUALIFIED_AUDIT_CONSUMER = """
            package app.portal;

            import app.audit.Audit;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.beans.factory.annotation.Qualifier;
            import org.springframework.stereotype.Component;

            @Component
            public class AuditConsumer {
                @Autowired
                @Qualifier("x")
                Audit audit;
            }
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_QUALIFIED_AUDITS = """
            package app.audit

            import org.springframework.beans.factory.annotation.Qualifier
            import org.springframework.stereotype.Component

            interface Audit

            @Component
            @Qualifier("x")
            class AuditA : Audit

            @Component
            @Qualifier("x")
            class AuditC : Audit

            @Component
            @Qualifier("y")
            class AuditB : Audit
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_QUALIFIED_AUDIT_IT = """
            package app

            import app.audit.Audit
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.beans.factory.annotation.Qualifier
            import org.springframework.boot.test.context.SpringBootTest

            @SpringBootTest(classes = [LocalApp::class])
            class AuditIT {
                @Autowired
                @Qualifier("x")
                lateinit var audit: Audit
            }
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_QUALIFIED_AUDIT_CONSUMER = """
            package app.portal

            import app.audit.Audit
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.beans.factory.annotation.Qualifier
            import org.springframework.stereotype.Component

            @Component
            class AuditConsumer {
                @Autowired
                @Qualifier("x")
                lateinit var audit: Audit
            }
            """.trimIndent()

        @Language("java")
        val JAVA_AUDIT_CONSUMER = """
            package app.portal;

            import app.audit.Audit;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.stereotype.Component;

            @Component
            public class AuditConsumer {
                @Autowired
                Audit audit;
            }
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_AUDIT_CONSUMER = """
            package app.portal

            import app.audit.Audit
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.stereotype.Component

            @Component
            class AuditConsumer {
                @Autowired
                lateinit var audit: Audit
            }
            """.trimIndent()

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
