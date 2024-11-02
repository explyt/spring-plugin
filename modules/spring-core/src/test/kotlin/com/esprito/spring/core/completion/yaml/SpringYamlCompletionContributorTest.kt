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

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl


class SpringYamlCompletionContributorTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springContext_6_0_7)

    override fun getTestDataPath(): String = "testdata/completion/yaml"

    fun testVariantsViaDot() = doTest {
        initSource = "sp.da.tom.nam<caret>"
        expectedLookupElements = setOf(
            "spring.datasource.tomcat.name",
            "spring.datasource.tomcat.driver-class-name",
            "spring.datasource.tomcat.validator-class-name"
        )
        sourceAfterComplete = """spring.datasource.tomcat.name: """.trimIndent()
    }

    fun testInsertToExistKey() = doTest {
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

    private fun doTest(init: TestModel.() -> Unit) {
        val model = TestModel()
        model.init()

        myFixture.configureByText("application.yaml", model.initSource)

        val lookupElements = myFixture.complete(CompletionType.BASIC)
        assertNotNull(lookupElements)

        val lookupElementStrings = lookupElements.map { it.lookupString }

        assertEquals(
            model.expectedLookupElements, lookupElementStrings.toSet()
        )

        getActiveLookup().currentItem = lookupElements[0]
        getActiveLookup().finishLookup(Lookup.NORMAL_SELECT_CHAR)
        myFixture.checkResult(model.sourceAfterComplete)
    }

    private fun getActiveLookup(): LookupImpl {
        return LookupManager.getActiveLookup(myFixture.editor) as LookupImpl
    }

    private class TestModel {
        lateinit var initSource: String
        lateinit var sourceAfterComplete: String
        lateinit var expectedLookupElements: Set<String>
    }
}

