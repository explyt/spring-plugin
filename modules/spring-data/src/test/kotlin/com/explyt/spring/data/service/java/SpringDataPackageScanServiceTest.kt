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

    fun testDefaultPackage() {
        myFixture.copyDirectoryToProject("service/jpa-data-default", "")
        val allActiveBeans = SpringSearchService.getInstance(project).getAllActiveBeans(module)
        TestCase.assertNotNull(allActiveBeans)
        TestCase.assertTrue(allActiveBeans.none { it.qualifiedName?.contains("TestRepo") == true })
    }
}