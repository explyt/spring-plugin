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

package com.explyt.spring.web.inspections.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.OpenApiJsonUnresolvedReferenceInspection
import org.intellij.lang.annotations.Language

class OpenApiJsonUnresolvedReferenceInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWeb_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(OpenApiJsonUnresolvedReferenceInspection::class.java)
    }

    fun testOk() {
        @Language("JSON") val text = """
            {
              "openapi": "3.0.0",
              "paths": {
                "/schemas": {
                  "post": {
                    "requestBody": {
                      "$REF": "#/components/requestBodies/KnownBody"
                    },
                    "parameters": [
                      {
                        "$REF": "#/components/parameters/KnownParameter"
                      }
                    ],
                    "responses": {
                      "201": {
                        "content": {
                          "application/json": {
                            "schema": {
                              "$REF": "#/components/schemas/KnownSchema"
                            }
                          }
                        }
                      },
                      "404": {
                        "$REF": "#/components/responses/KnownResponse"
                      }
                    }
                  }
                }
              },
              "components": {
                "parameters": {
                  "KnownParameter": {
                    "name": "known",
                    "in": "path",
                    "description": "some known parameter",
                    "required": true,
                    "schema": {
                      "type": "string",
                      "example": {
                        "$REF": "#/components/examples/KnownExample"
                      }
                    }
                  }
                },
                "responses": {
                  "KnownResponse": {
                    "description": "some known response",
                    "content": {
                      "application/json": {
                        "schema": {
                          "$REF": "#/components/schemas/KnownSchema"
                        }
                      }
                    }
                  }
                },
                "schemas": {
                  "KnownSchema": {
                    "type": "object",
                    "properties": {
                      "code": {
                        "description": "code",
                        "type": "string"
                      }
                    }
                  }
                },
                "examples": {
                  "KnownExample": {
                    "summary": "ExampleValue"
                  }
                },
                "requestBodies": {
                  "KnownBody": {
                    "content": {
                      "application/json": {
                        "schema": {
                          "$REF": "#/components/schemas/KnownSchema"
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
        myFixture.configureByText(FILENAME, text)
        myFixture.testHighlighting(FILENAME)
    }

    fun testUnknownParameter() {
        @Language("JSON") val text = """
            {
              "openapi": "3.0.0",
              "paths": {
                "/schemas": {
                  "patch": {
                    "parameters": [
                      {
                        <error>"$REF": "#/components/parameters/UnknownParameter"</error>
                      }
                    ]
                  }
                }
              }
            }
        """.trimIndent()
        myFixture.configureByText(FILENAME, text)
        myFixture.testHighlighting(FILENAME)
    }

    fun testUnknownSchema() {
        @Language("JSON") val text = """
            {
              "openapi": "3.0.0",
              "paths": {
                "/schemas": {
                  "patch": {
                    "responses": {
                      "200": {
                        "content": {
                          "application/json": {
                            "schema": {
                              <error>"$REF": "#/components/schemas/UnknownSchema"</error>
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
        myFixture.configureByText(FILENAME, text)
        myFixture.testHighlighting(FILENAME)
    }

    fun testUnknownExample() {
        @Language("JSON") val text = """
            {
              "openapi": "3.0.0",
              "components": {
                "schemas": {
                  "KnownSchema": {
                    "type": "object",
                    "properties": {
                      "code": {
                        "description": "code",
                        "type": "string",
                        "example": {
                          <error>"$REF": "#/components/examples/UnknownExample"</error>
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
        myFixture.configureByText(FILENAME, text)
        myFixture.testHighlighting(FILENAME)
    }

    fun testUnknownResponse() {
        @Language("JSON") val text = """
            {
              "openapi": "3.0.0",
              "paths": {
                "/schemas": {
                  "patch": {
                    "responses": {
                      "409": {
                        <error>"$REF": "#/components/responses/UnknownResponse"</error>
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
        myFixture.configureByText(FILENAME, text)
        myFixture.testHighlighting(FILENAME)
    }

    fun testUnknownBody() {
        @Language("JSON") val text = """
            {
              "openapi": "3.0.0",
              "paths": {
                "/schemas": {
                  "patch": {
                    "requestBody": {
                      <error>"$REF": "#/components/requestBodies/UnknownBody"</error>
                    }
                  }
                }
              }
            }
        """.trimIndent()
        myFixture.configureByText(FILENAME, text)
        myFixture.testHighlighting(FILENAME)
    }

    companion object {
        const val FILENAME = "openapi.json"
        const val REF = "\$ref"
    }

}