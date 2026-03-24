/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.reference.kotlin

import com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

class DockerYamlReferenceTest : ExplytKotlinLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + "reference/properties"

    override val libraries: Array<TestLibrary> =
        arrayOf(
            TestLibrary.springBoot_3_1_1,
            TestLibrary.springContext_6_0_7,
        )

    fun testSuccessReference() {
        @Language("yml") val ymlContent = """
version: '3.7'

services:
  clickhouse:
    image: clickhouse/clickhouse-server:25.5
    ports:
      - "9003:9003"
    environment:
      - SERVER_POR<caret>T=8080
      - CLICKHOUSE_USER=stagesuperuser
""".trimIndent()
        myFixture.configureByText("docker-compose.yaml", ymlContent)
        val psiReference = myFixture.getReferenceAtCaretPosition()
        assertNotNull(psiReference)
        assertTrue(psiReference is ConfigurationPropertyKeyReference)
    }

    fun testNoReference() {
        @Language("yml") val ymlContent = """
version: '3.7'

services:
  clickhouse:
    image: clickhouse/clickhouse-server:25.5
    ports:
      - SERVER_POR<caret>T=8080
      - "9003:9003"
    environment:  
      - CLICKHOUSE_USER=stagesuperuser
""".trimIndent()
        myFixture.configureByText("docker-compose.yaml", ymlContent)
        val psiReference = myFixture.getReferenceAtCaretPosition()
        assertNull(psiReference)
    }
}