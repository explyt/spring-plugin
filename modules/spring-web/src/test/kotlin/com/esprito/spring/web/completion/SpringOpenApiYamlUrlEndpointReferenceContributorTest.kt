package com.esprito.spring.web.completion

import com.esprito.spring.test.ExplytJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.web.references.ExplytControllerMethodReference
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.ResolveResult
import junit.framework.TestCase

class SpringOpenApiYamlUrlEndpointReferenceContributorTest : ExplytJavaLightTestCase() {
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
            "open-api.yaml", """
                openapi: "3.0.3"
                info: 
                  title: "Just test"
                  version: "1.0.0"
                paths:   
                  /product/{product-id}:
                    ge<caret>t:
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ExplytControllerMethodReference
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
            "open-api.yaml", """
                openapi: "3.0.3"
                info: 
                  title: "Just test"
                  version: "1.0.0"
                paths:   
                  /product/{product-id}:
                    pat<caret>ch:
        """.trimIndent()

        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ExplytControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true).filter { it.element is PsiMethod }
        assertEquals(0, multiResolve.size)
    }

    fun testUrl() {
        myFixture.copyFileToProject("ProductController.java")
        myFixture.configureByText(
            "open-api.yaml", """
                openapi: "3.0.3"
                info: 
                  title: "Just test"
                  version: "1.0.0"
                paths:   
                  /prod<caret>uct:
        """.trimIndent()

        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ExplytControllerMethodReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(2, multiResolve.size)
        val resolvedTexts = multiResolve.mapNotNull { getResolvedText(it) }
        TestCase.assertEquals(2, resolvedTexts.size)
        assertContainsElements(resolvedTexts, "ProductController#getProducts", "ProductController#save")
    }

    private fun getResolvedText(resolveResult: ResolveResult): String? {
        val resolvedElement = resolveResult.element as? PsiMember ?: return null
        val classFqn = resolvedElement.containingClass?.qualifiedName ?: return null
        val memberName = resolvedElement.name

        return "$classFqn#$memberName"
    }
}