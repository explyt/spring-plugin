/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.java

import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

class EnableAutoConfigurationApplicationTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootTestAutoConfigure_3_1_1,
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

    fun testEnableAutoConfigurationWithComponentScanScansItsPackage() {
        addOtherApplication()
        addPortalApplication(SCANNING_PORTAL_APPLICATION)
        addPortalService()

        val activeBeans = activeBeanNames()

        assertTrue("precondition: the other application scans its package, got $activeBeans", OTHER_SERVICE in activeBeans)
        assertTrue("the portal application scans its package, got $activeBeans", PORTAL_SERVICE in activeBeans)
    }

    fun testEnableAutoConfigurationOnlyDoesNotScanItsPackage() {
        addOtherApplication()
        addPortalApplication(AUTO_CONFIGURATION_ONLY_PORTAL_APPLICATION)
        addPortalService()

        val activeBeans = activeBeanNames()

        assertTrue("precondition: the other application scans its package, got $activeBeans", OTHER_SERVICE in activeBeans)
        assertFalse("no component scan reaches the portal package, got $activeBeans", PORTAL_SERVICE in activeBeans)
    }

    fun testEnableAutoConfigurationOnlyApplicationIsItselfABean() {
        addOtherApplication()
        addPortalApplication(AUTO_CONFIGURATION_ONLY_PORTAL_APPLICATION)

        val activeBeans = activeBeanNames()

        assertTrue("precondition: the other application scans its package, got $activeBeans", OTHER_SERVICE in activeBeans)
        assertTrue("the primary source is registered, got $activeBeans", PORTAL_APPLICATION in activeBeans)
    }

    private fun addPortalApplication(text: String) {
        myFixture.addFileToProject("com/portal/PortalApplication.java", text)
        val portal = projectClass(PORTAL_APPLICATION)
        assertNotNull("precondition: Boot's EnableAutoConfiguration is on the classpath", libraryClass(ENABLE_AUTO_CONFIGURATION))
        assertTrue("precondition: the portal carries Boot's EnableAutoConfiguration", portal.hasAnnotation(ENABLE_AUTO_CONFIGURATION))
        assertEquals("precondition: the portal is in the fixture module", module, ModuleUtilCore.findModuleForPsiElement(portal))
    }

    private fun addPortalService() {
        myFixture.addFileToProject(
            "com/portal/PortalService.java",
            """
            package com.portal;

            import org.springframework.stereotype.Component;

            @Component
            public class PortalService {}
            """.trimIndent()
        )
    }

    private fun addOtherApplication() {
        myFixture.addFileToProject(
            "com/other/OtherApplication.java",
            """
            package com.other;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class OtherApplication {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/other/OtherService.java",
            """
            package com.other;

            import org.springframework.stereotype.Component;

            @Component
            public class OtherService {}
            """.trimIndent()
        )
    }

    private fun activeBeanNames(): Set<String> =
        SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }

    private fun projectClass(qualifiedName: String): PsiClass =
        JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.projectScope(project))
            ?: error("precondition: $qualifiedName must resolve in the project")

    private fun libraryClass(qualifiedName: String): PsiClass? =
        JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project))

    private companion object {
        const val ENABLE_AUTO_CONFIGURATION = "org.springframework.boot.autoconfigure.EnableAutoConfiguration"
        const val PORTAL_APPLICATION = "com.portal.PortalApplication"
        const val PORTAL_SERVICE = "com.portal.PortalService"
        const val OTHER_APPLICATION = "com.other.OtherApplication"
        const val OTHER_SERVICE = "com.other.OtherService"

        val SCANNING_PORTAL_APPLICATION = """
            package com.portal;

            import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
            import org.springframework.context.annotation.ComponentScan;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @EnableAutoConfiguration
            @ComponentScan
            public class PortalApplication {}
        """.trimIndent()

        val AUTO_CONFIGURATION_ONLY_PORTAL_APPLICATION = """
            package com.portal;

            import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @EnableAutoConfiguration
            public class PortalApplication {}
        """.trimIndent()
    }
}
