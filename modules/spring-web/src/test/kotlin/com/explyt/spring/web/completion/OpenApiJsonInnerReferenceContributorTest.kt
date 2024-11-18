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