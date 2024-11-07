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

