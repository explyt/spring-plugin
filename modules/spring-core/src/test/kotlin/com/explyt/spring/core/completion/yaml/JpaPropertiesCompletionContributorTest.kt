/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.yaml

import com.explyt.spring.core.completion.properties.java.AbstractSpringPropertiesCompletionContributorTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.completion.CompletionType

class JpaPropertiesCompletionContributorTest : AbstractSpringPropertiesCompletionContributorTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(
            TestLibrary.springBoot_3_1_1,
            TestLibrary.springContext_6_0_7,
            TestLibrary.hibernate_5_6_15
        )

    fun testJpaPropertiesHibernate() {
        myFixture.configureByText(
            "application.yaml", """
spring:
  jpa:
    properties:
      <caret>:
        """.trimIndent()
        )
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
    }


    fun testJpaPropertiesHibernateScalar() {
        myFixture.configureByText(
            "application.yaml", """
spring:
  jpa:
    properties:
      hibernate:
        arc<caret>            
        """.trimIndent()
        )
        doTest(
            "archive.autodetection",
            "archive.interpreter",
            "archive.scanner"
        )
    }

    fun testJpaPropertiesHibernateNewRow() {
        myFixture.configureByText(
            "application.yaml", """
spring:
  jpa:
    properties:
      hibernate:
        archive:
          <caret>            
        """.trimIndent()
        )
        doTest(
            "autodetection",
            "interpreter",
            "scanner"
        )
    }

    fun testJpaPropertiesHibernateNotShowDuplicate() {
        myFixture.configureByText(
            "application.yaml", """
spring:
  jpa:
    properties:
      hibernate:
        archive:
          scanner: true
          <caret>            
        """.trimIndent()
        )
        doTest(
            "autodetection",
            "interpreter"
        )
    }
}