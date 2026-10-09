/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.java

import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.service.beans.BeanApplicationResolver
import com.explyt.spring.core.service.beans.BeanQueryException
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.junit.Assert

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

    fun testEnableAutoConfigurationWithComponentScanIsApplication() {
        addPortalApplication(SCANNING_PORTAL_APPLICATION)

        assertEquals(PORTAL_APPLICATION, resolveApplication(PORTAL_APPLICATION).qualifiedName)
    }

    fun testEnableAutoConfigurationWithComponentScanScansItsPackage() {
        addOtherApplication()
        addPortalApplication(SCANNING_PORTAL_APPLICATION)
        addPortalService()

        val activeBeans = activeBeanNames()

        assertTrue("precondition: the other application scans its package, got $activeBeans", OTHER_SERVICE in activeBeans)
        assertTrue("the portal application scans its package, got $activeBeans", PORTAL_SERVICE in activeBeans)
    }

    fun testEnableAutoConfigurationOnlyIsApplication() {
        addPortalApplication(AUTO_CONFIGURATION_ONLY_PORTAL_APPLICATION)

        assertEquals(PORTAL_APPLICATION, resolveApplication(PORTAL_APPLICATION).qualifiedName)
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

    fun testProjectMetaAnnotationWithEnableAutoConfigurationIsApplication() {
        myFixture.addFileToProject(
            "com/portal/MyApp.java",
            """
            package com.portal;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
            import org.springframework.context.annotation.ComponentScan;

            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            @EnableAutoConfiguration
            @ComponentScan
            public @interface MyApp {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/portal/PortalApplication.java",
            """
            package com.portal;

            @MyApp
            public class PortalApplication {}
            """.trimIndent()
        )
        assertTrue(
            "precondition: MyApp carries Boot's EnableAutoConfiguration",
            projectClass("com.portal.MyApp").hasAnnotation(ENABLE_AUTO_CONFIGURATION)
        )
        assertTrue("precondition: PortalApplication carries MyApp", projectClass(PORTAL_APPLICATION).hasAnnotation("com.portal.MyApp"))

        assertEquals(PORTAL_APPLICATION, resolveApplication(PORTAL_APPLICATION).qualifiedName)
    }

    fun testTestSliceIsNotApplication() {
        addOtherApplication()
        myFixture.addFileToProject(
            "com/portal/PortalRepositoryTest.java",
            """
            package com.portal;

            import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

            @DataJpaTest
            public class PortalRepositoryTest {}
            """.trimIndent()
        )
        assertNotNull("precondition: Boot's DataJpaTest is on the classpath", libraryClass(DATA_JPA_TEST))
        assertTrue(
            "precondition: the test class carries DataJpaTest",
            projectClass("com.portal.PortalRepositoryTest").hasAnnotation(DATA_JPA_TEST)
        )
        assertEquals(
            "precondition: the detector finds applications in this fixture",
            OTHER_APPLICATION, resolveApplication(OTHER_APPLICATION).qualifiedName
        )

        assertEquals(BeanApplicationResolver.APPLICATION_NOT_FOUND, failedResolution("com.portal.PortalRepositoryTest"))
    }

    fun testSpringBootApplicationIsStillApplication() {
        addOtherApplication()

        assertEquals(OTHER_APPLICATION, resolveApplication(OTHER_APPLICATION).qualifiedName)
        assertTrue("the application scans its package", OTHER_SERVICE in activeBeanNames())
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

    private fun resolveApplication(qualifiedName: String): PsiClass =
        BeanApplicationResolver(project).resolve(qualifiedName, null)

    private fun failedResolution(qualifiedName: String): String =
        Assert.assertThrows(BeanQueryException::class.java) {
            BeanApplicationResolver(project).resolve(qualifiedName, null)
        }.problem.code

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
        const val DATA_JPA_TEST = "org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest"
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
