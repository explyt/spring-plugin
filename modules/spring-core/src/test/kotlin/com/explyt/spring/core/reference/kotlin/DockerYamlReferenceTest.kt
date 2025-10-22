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