package com.explyt.spring.web.completion

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.web.references.OpenApiJsonInnerReference
import com.intellij.json.psi.JsonProperty

class OpenApiJsonInnerReferenceContributorTest : ExplytJavaLightTestCase() {

    fun testResolved() {
        myFixture.configureByText(
            "open-api.json", """
                {
                  "openapi": "3.1.0",
                  "paths": {
                    "/api/some/{id}": {
                      "get": {
                        "parameters": [
                          {
                            "$REF": "#/components/parameters/Som<caret>eId"
                          } ] } } },
                  "components": {
                    "parameters": {
                      "SomeId": {
                        "name": "someId"
                      } } } }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? OpenApiJsonInnerReference
        assertNotNull(ref)
        val jsonProperty = ref!!.resolve() as? JsonProperty
        assertNotNull(jsonProperty)
        assertEquals("SomeId", jsonProperty?.name)
    }

    fun testNotResolved() {
        myFixture.configureByText(
            "open-api.json", """
                {
                  "openapi": "3.1.0",
                  "paths": {
                    "/api/some/{id}": {
                      "get": {
                        "parameters": [
                          {
                            "$REF": "#/components/parameters/Som<caret>eId"
                          } ] } } },
                  "components": {
                    "parameters": {
                      "SomeOtherId": {
                        "name": "someOtherId"
                      } } } }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? OpenApiJsonInnerReference
        assertNotNull(ref)
        val jsonProperty = ref!!.resolve() as? JsonProperty
        assertNull(jsonProperty)
    }

    fun testNotOpenApi() {
        myFixture.configureByText(
            "open-api.json", """
                {
                  "paths": {
                    "/api/some/{id}": {
                      "get": {
                        "parameters": [
                          {
                            "$REF": "#/components/parameters/Som<caret>eId"
                          } ] } } },
                  "components": {
                    "parameters": {
                      "SomeId": {
                        "name": "someId"
                      } } } }
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? OpenApiJsonInnerReference
        assertNull(ref)
    }

    companion object {
        const val REF = "\$ref"
    }

}