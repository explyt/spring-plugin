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

package com.explyt.spring.core.completion.yaml

import com.explyt.spring.core.completion.ExplytCompletionJavaLightTestCase
import org.intellij.lang.annotations.Language


class DockerEnvVariableYamlCompletionContributorTest : ExplytCompletionJavaLightTestCase() {

    fun testSuccessfulCompletion() {
        @Language("yml") val ymlContent = """
version: '3.7'

services:
  clickhouse:
    image: clickhouse/clickhouse-server:25.5
    ports:
      - "9003:9003"
    environment:
      - po<caret>
      - CLICKHOUSE_USER=stagesuperuser
""".trimIndent()
        myFixture.configureByText("docker-compose.yaml", ymlContent)

        val lookupElements = myFixture.completeBasic()
        assertNotNull(lookupElements)
        assertTrue(lookupElements.isNotEmpty())
        assertTrue(lookupElements.map { it.lookupString }.contains("SERVER_PORT"))
    }

    fun testNoApplicationYamlCompletion() {
        @Language("yml") val ymlContent = """
version: '3.7'

services:
  clickhouse:
    image: clickhouse/clickhouse-server:25.5
    ports:
      - "9003:9003"
    environment:
      - po<caret>
      - CLICKHOUSE_USER=stagesuperuser
""".trimIndent()
        myFixture.configureByText("application-dev.yaml", ymlContent)

        val lookupElements = myFixture.completeBasic()
        assertTrue(lookupElements.isNullOrEmpty())
    }

    fun testNoEnvironmentCompletion() {
        @Language("yml") val ymlContent = """
version: '3.7'

services:
  clickhouse:
    image: clickhouse/clickhouse-server:25.5
    ports:
      - "9003:9003"
      - po<caret> 
    environment:
      - SERVER_PORT=8080
      - CLICKHOUSE_USER=stagesuperuser
""".trimIndent()
        myFixture.configureByText("docker-compose.yaml", ymlContent)

        val lookupElements = myFixture.completeBasic()
        assertTrue(lookupElements.isNullOrEmpty())
    }

    fun testNoScalarPositionCompletion() {
        @Language("yml") val ymlContent = """
version: '3.7'

services:
  clickhouse:
    image: clickhouse/clickhouse-server:25.5
    ports:
      - "9003:9003"   
    environment<caret>:
      - SERVER_PORT=8080
      - CLICKHOUSE_USER=stagesuperuser
""".trimIndent()
        myFixture.configureByText("docker-compose.yaml", ymlContent)

        val lookupElements = myFixture.completeBasic()
        assertTrue(lookupElements.isNullOrEmpty())
    }
}



