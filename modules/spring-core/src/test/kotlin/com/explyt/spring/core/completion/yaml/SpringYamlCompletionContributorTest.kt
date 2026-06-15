/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.yaml

import com.explyt.spring.core.completion.ExplytCompletionJavaLightTestCase


class SpringYamlCompletionContributorTest : ExplytCompletionJavaLightTestCase() {

    override fun getTestDataPath(): String = "testdata/completion/yaml"

    fun testVariantsViaDot() = doTest {
        fileName = "application.yaml"
        initSource = "sp.da.tom.nam<caret>"
        expectedLookupElements = setOf(
            "spring.datasource.tomcat.name",
            "spring.datasource.tomcat.driver-class-name",
            "spring.datasource.tomcat.validator-class-name"
        )
        sourceAfterComplete = """spring.datasource.tomcat.name: """.trimIndent()
    }

    fun testInsertToExistKey() = doTest {
        fileName = "application.yaml"
        initSource = """
sprindatatomna<caret>

spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb?MODE=MYSQL
    username: sa
    hikari:
      max-lifetime: 120_000 # ms
      maximum-pool-size: 5
        """.trimIndent()
        expectedLookupElements = setOf(
            "spring.datasource.tomcat.name",
            "spring.datasource.tomcat.driver-class-name",
            "spring.datasource.tomcat.validator-class-name"
        )
        sourceAfterComplete = """


spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb?MODE=MYSQL
    username: sa
    hikari:
      max-lifetime: 120_000 # ms
      maximum-pool-size: 5
    tomcat:
      name: 
      """.trimIndent()
    }

    fun testInsertToExistKeyFromOneLevelParent() = doTest {
        fileName = "application.yaml"
        initSource = """
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb?MODE=MYSQL
    username: sa
    hikari:
      max-lifetime: 120_000 # ms
      maximum-pool-size: 5
    tomcat:
      name: frog
  datomna<caret>
        """.trimIndent()
        expectedLookupElements = setOf(
            "datasource.tomcat.driver-class-name",
            "datasource.tomcat.validator-class-name"
        )
        sourceAfterComplete = """
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb?MODE=MYSQL
    username: sa
    hikari:
      max-lifetime: 120_000 # ms
      maximum-pool-size: 5
    tomcat:
      name: frog
      driver-class-name: 
  
      """.trimIndent()
    }


    fun testSimpleInsertToKey() = doTest {
        fileName = "application.yaml"
        initSource = """
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb?MODE=MYSQL
    con-on<caret>
    username: sa
    hikari:
      max-lifetime: 120_000 # ms
      maximum-pool-size: 5
        """.trimIndent()
        expectedLookupElements = setOf(
            "continue-on-error",
            "oracleucp.validate-connection-on-borrow"
        )
        sourceAfterComplete = """
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb?MODE=MYSQL
    continue-on-error: 
    username: sa
    hikari:
      max-lifetime: 120_000 # ms
      maximum-pool-size: 5
      """.trimIndent()
    }

    fun testInsertToExistKeyToDown() = doTest {
        fileName = "application.yaml"
        initSource = """
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb?MODE=MYSQL
    hia<caret>
    username: sa
    hikari:
      max-lifetime: 120_000 # ms
      maximum-pool-size: 5
        """.trimIndent()
        expectedLookupElements = setOf(
            "hikari.allow-pool-suspension",
            "hikari.auto-commit"
        )
        sourceAfterComplete = """
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb?MODE=MYSQL
    
    username: sa
    hikari:
      max-lifetime: 120_000 # ms
      maximum-pool-size: 5
      allow-pool-suspension: 
      """.trimIndent()
    }

}

