/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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

    fun testSuccessfulCompletionK8S() {
        @Language("yml") val ymlContent = """
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    metadata:
      labels:
        app: demo-db
    spec:
      containers:
        - image: postgres:18.0
          name: postgresql
          env:
            - name: po<caret>
""".trimIndent()
        myFixture.configureByText("docker-compose.yaml", ymlContent)

        val lookupElements = myFixture.completeBasic()
        assertNotNull(lookupElements)
        assertTrue(lookupElements.isNotEmpty())
        assertTrue(lookupElements.map { it.lookupString }.contains("SERVER_PORT"))
    }

    fun testNoEnvironmentCompletionK8S() {
        @Language("yml") val ymlContent = """
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    metadata:
      labels:
        app: po<caret>     
""".trimIndent()
        myFixture.configureByText("docker-compose.yaml", ymlContent)

        val lookupElements = myFixture.completeBasic()
        assertTrue(lookupElements.isNullOrEmpty())
    }
}



