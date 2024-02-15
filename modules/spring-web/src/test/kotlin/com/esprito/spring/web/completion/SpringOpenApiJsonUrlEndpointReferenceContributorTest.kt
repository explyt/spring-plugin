package com.esprito.spring.web.completion

import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.web.references.EspritoControllerMethodReference
import com.intellij.psi.PsiMember
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import junit.framework.TestCase

class SpringOpenApiJsonUrlEndpointReferenceContributorTest : EspritoJavaLightTestCase() {
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
        val multiResolve = ref!!.multiResolve(true)
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
        val multiResolve = ref!!.multiResolve(true)
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
                it as? EspritoControllerMethodReference
            }?.firstOrNull()

    private fun getResolvedText(resolveResult: ResolveResult): String? {
        val resolvedElement = resolveResult.element as? PsiMember ?: return null
        val classFqn = resolvedElement.containingClass?.qualifiedName ?: return null
        val memberName = resolvedElement.name

        return "$classFqn#$memberName"
    }
}