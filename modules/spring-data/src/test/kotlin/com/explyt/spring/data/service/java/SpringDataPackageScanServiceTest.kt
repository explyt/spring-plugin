/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.data.service.java

import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry
import junit.framework.TestCase

class SpringDataPackageScanServiceTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springContext_6_0_7, TestLibrary.springDataJpa_3_1_0
    )

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
    }

    override fun tearDown() {
        super.tearDown()
        Registry.get("explyt.spring.root.runConfiguration").resetToDefault()
    }

    fun testDifferentPackage() {
        myFixture.copyDirectoryToProject("service/jpa-data", "")
        val allActiveBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
        TestCase.assertNotNull(allActiveBeans)
        TestCase.assertTrue(allActiveBeans.any { it.qualifiedName?.contains("TestRepo") == true })
    }

    fun testDefaultPackageApplicationFindsRepositoryInNamedPackage() {
        myFixture.addFileToProject(
            "DemoApplication.java",
            """
            @org.springframework.boot.autoconfigure.SpringBootApplication
            public class DemoApplication {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/x/PersonRepository.java",
            """
            package com.x;

            public interface PersonRepository extends org.springframework.data.repository.Repository<Object, Long> {}
            """.trimIndent()
        )

        val activeBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
            .mapNotNullTo(mutableSetOf()) { it.qualifiedName }

        TestCase.assertTrue("precondition: the application is a bean, got $activeBeans", "DemoApplication" in activeBeans)
        TestCase.assertTrue("the repository is scanned, got $activeBeans", "com.x.PersonRepository" in activeBeans)
    }

    fun testDefaultPackage() {
        myFixture.copyDirectoryToProject("service/jpa-data-default", "")
        val allActiveBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
        TestCase.assertNotNull(allActiveBeans)
        TestCase.assertTrue(allActiveBeans.none { it.qualifiedName?.contains("TestRepo") == true })
    }
}