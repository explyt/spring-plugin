package com.esprito.spring.web.completion

import com.esprito.spring.test.ExplytJavaLightTestCase
import com.intellij.testFramework.UsefulTestCase

class OpenApiJsonRefCompletionContributorTest : ExplytJavaLightTestCase() {

    fun testSuccess() {
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

        val lookupElements = myFixture.completeBasic()
        assertSize(1, lookupElements)
        assertTrue(lookupElements.first().toString().contains("#/components/parameters/SomeId"))
    }

    fun testNotFound() {
        myFixture.configureByText(
            "open-api.json", """
                {
                  "openapi": "3.1.0",
                  "paths": {
                    "/api/some/{id}": {
                      "get": {
                        "parameters": [
                          {
                            "$REF": "#/components/parameters/Unk<caret>nown"
                          } ] } } },
                  "components": {
                    "parameters": {
                      "SomeOtherId": {
                        "name": "someOtherId"
                      } } } }
        """.trimIndent()

        )
        val lookupElements = myFixture.completeBasic()
        UsefulTestCase.assertSize(0, lookupElements)
    }

    companion object {
        const val REF = "\$ref"
    }

}