/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.service

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.web.model.OpenApiSpecificationFinder
import com.explyt.spring.web.model.OpenApiSpecificationType
import org.intellij.lang.annotations.Language

class OpenApiSpecificationManagerTest : ExplytJavaLightTestCase() {

    fun testDetectV30() {
        assertInstanceOf(specificationTypeOf("3.0.0"), OpenApiSpecificationType.OpenApiV30::class.java)
    }

    fun testDetectV31() {
        assertInstanceOf(specificationTypeOf("3.1.0"), OpenApiSpecificationType.OpenApiV31::class.java)
    }

    fun testDetectV32() {
        assertInstanceOf(specificationTypeOf("3.2.0"), OpenApiSpecificationType.OpenApiV32::class.java)
    }

    fun testUnsupportedVersionIsUndefined() {
        assertEquals(OpenApiSpecificationType.OpenApiUndefined, specificationTypeOf("2.0.0"))
    }

    /**
     * Every supported version must resolve to a bundled schema that actually parses:
     * a missing or malformed `schema/openapi_*.json` resource makes this return null.
     */
    fun testBundledSchemaIsResolvedForEverySupportedVersion() {
        val manager = project.getService(OpenApiSpecificationManager::class.java)

        for (version in listOf("3.0.0", "3.1.0", "3.2.0")) {
            val specificationType = specificationTypeOf(version)
            val schemaPair = manager.getSchemaByFile(specificationType)

            assertNotNull("No bundled schema resolved for OpenAPI $version", schemaPair)
            assertTrue(
                "Bundled schema for OpenAPI $version is empty",
                schemaPair!!.file.let { it.isValid && it.length > 0 }
            )
        }
    }

    fun testUndefinedSpecificationHasNoSchema() {
        val manager = project.getService(OpenApiSpecificationManager::class.java)

        assertNull(manager.getSchemaByFile(OpenApiSpecificationType.OpenApiUndefined))
    }

    private fun specificationTypeOf(version: String): OpenApiSpecificationType {
        @Language("JSON") val text = """
            {
              "openapi": "$version",
              "info": {
                "title": "Sample",
                "version": "1.0.0"
              },
              "paths": {}
            }
        """.trimIndent()

        val psiFile = myFixture.configureByText("openapi.json", text)
        return OpenApiSpecificationFinder.findSpecificationType(psiFile.virtualFile, psiFile)
    }
}
