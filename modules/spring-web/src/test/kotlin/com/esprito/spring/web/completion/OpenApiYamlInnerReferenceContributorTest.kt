package com.esprito.spring.web.completion

import com.esprito.spring.test.ExplytJavaLightTestCase
import com.esprito.spring.web.references.OpenApiYamlInnerReference
import org.jetbrains.yaml.psi.YAMLKeyValue

class OpenApiYamlInnerReferenceContributorTest : ExplytJavaLightTestCase() {

    fun testResolved() {
        myFixture.configureByText(
            "open-api.yaml", """
                openapi: "3.1.0"
                paths:   
                  /api/some/{id}:
                    get:
                      parameters:
                        - $REF: '#/components/parameters/Som<caret>eId'
                components:
                  parameters:
                    SomeId:
                      name: someId
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? OpenApiYamlInnerReference
        assertNotNull(ref)
        val yamlKeyValue = ref!!.resolve() as? YAMLKeyValue
        assertNotNull(yamlKeyValue)
        assertEquals("SomeId", yamlKeyValue?.key?.text)
    }

    fun testNotResolved() {
        myFixture.configureByText(
            "open-api.yaml", """
                openapi: "3.1.0"
                paths:   
                  /api/some/{id}:
                    get:
                      parameters:
                        - $REF: '#/components/parameters/Som<caret>eId'
                components:
                  parameters:
                    SomeOtherId:
                      name: someOtherId
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? OpenApiYamlInnerReference
        assertNotNull(ref)
        val yamlKeyValue = ref!!.resolve() as? YAMLKeyValue
        assertNull(yamlKeyValue)
    }

    fun testNotOpenApi() {
        myFixture.configureByText(
            "open-api.yaml", """
                paths:   
                  /api/some/{id}:
                    get:
                      parameters:
                        - $REF: '#/components/parameters/Som<caret>eId'
                components:
                  parameters:
                    SomeId:
                      name: someId
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? OpenApiYamlInnerReference
        assertNull(ref)
    }

    companion object {
        const val REF = "\$ref"
    }

}