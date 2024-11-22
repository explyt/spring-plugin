/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.web.completion

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import junit.framework.TestCase

class SpringOpenApiJsonUrlEndpointReferenceContributorTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = "${super.getTestDataPath()}/completion/openApi"

    override val libraries: Array<TestLibrary>
        get() = arrayOf(
            TestLibrary.springWeb_6_0_7,
            TestLibrary.springTest_6_0_7,
            TestLibrary.springBootAutoConfigure_3_1_1
        )

    fun testGet() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "open-api.json", """
                {
                  "openapi": "3.0.3",
                  "info": {
                    "title": "Just test",
                    "version": "1.0.0"
                  },
                  "paths": {
                    "/product/{product-id}": {
                      "ge<caret>t": {}
                    }
                  }
                }
        """.trimIndent()
        )

        val ref = getControllerMethodReference()
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true).filter { it.element is PsiMethod }
        assertEquals(1, multiResolve.size)
        val resolvedText = getResolvedText(multiResolve.first())
        assertNotNull(resolvedText)
        assertEquals("ProductController#getProduct", resolvedText)
    }

    fun testNoPatch() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "open-api.json", """
                {
                  "openapi": "3.0.3",
                  "info": {
                    "title": "Just test",
                    "version": "1.0.0"
                  },
                  "paths": {
                    "/product/{product-id}": {
                      "pat<caret>ch": {}
                    }
                  }
                }
        """.trimIndent()

        )

        val ref = getControllerMethodReference()
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true).filter { it.element is PsiMethod }
        assertEquals(0, multiResolve.size)
    }

    fun testUrl() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "open-api.json", """
                {
                  "openapi": "3.0.3",
                  "info": {
                    "title": "Just test",
                    "version": "1.0.0"
                  },
                  "paths": {
                    "/prod<caret>uct": {
                    }
                  }
                }

        """.trimIndent()

        )

        val ref = getControllerMethodReference()
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(2, multiResolve.size)
        val resolvedTexts = multiResolve.mapNotNull { getResolvedText(it) }
        TestCase.assertEquals(2, resolvedTexts.size)
        assertContainsElements(resolvedTexts, "ProductController#getProducts", "ProductController#save")
    }

    private fun getControllerMethodReference() =
        (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? ExplytControllerMethodReference
            }?.firstOrNull()

    private fun getResolvedText(resolveResult: ResolveResult): String? {
        val resolvedElement = resolveResult.element as? PsiMember ?: return null
        val classFqn = resolvedElement.containingClass?.qualifiedName ?: return null
        val memberName = resolvedElement.name

        return "$classFqn#$memberName"
    }
}