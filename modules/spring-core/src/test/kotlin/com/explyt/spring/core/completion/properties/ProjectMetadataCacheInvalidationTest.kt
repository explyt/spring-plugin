/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties

import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager

/**
 * The property and hint catalogues are cached against the model tracker, and the project's own
 * `additional-spring-configuration-metadata.json` is one of their inputs. An edit to that file - including the one the
 * "Create metadata" intention makes - must reach the next lookup instead of waiting for an unrelated code edit.
 */
class ProjectMetadataCacheInvalidationTest : ExplytBaseLightTestCase() {

    override fun getTestDataPath() = "testdata/property/"

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    fun testEditingProjectMetadataReachesTheCachedCatalogue() {
        val metadata = myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        val search = SpringConfigurationPropertiesSearch.getInstance(project)
        assertNull(
            "Precondition: the key must be absent before the edit",
            search.findProperty(module, "explyt.cache.added-later")
        )
        assertNull(search.getHintIndex(module).hintsNamed("explyt.cache.added-later").firstOrNull())

        val document = PsiDocumentManager.getInstance(project).getDocument(psiManager.findFile(metadata)!!)!!
        WriteCommandAction.runWriteCommandAction(project) {
            val hints = document.text.indexOf("\"hints\": [") + "\"hints\": [".length
            document.insertString(hints, """{ "name": "explyt.cache.added-later", "values": [{ "value": "one" }] },""")
            val properties = document.text.indexOf("\"properties\": [") + "\"properties\": [".length
            document.insertString(properties, """{ "name": "explyt.cache.added-later", "type": "java.lang.String" }""")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }

        assertNotNull(
            "A property added to the project metadata must be found without another edit",
            search.findProperty(module, "explyt.cache.added-later")
        )
        assertEquals(
            listOf("one"),
            search.getHintIndex(module).hintsNamed("explyt.cache.added-later").flatMap { it.values }.map { it.value }
        )
    }
}
