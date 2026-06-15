/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties.java

import com.explyt.spring.test.TestLibrary

class JpaPropertiesCompletionContributorTest : AbstractSpringPropertiesCompletionContributorTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(
            TestLibrary.springBoot_3_1_1,
            TestLibrary.springContext_6_0_7,
            TestLibrary.hibernate_5_6_15
        )

    fun testJpaPropertiesHibernatePartial() {
        myFixture.configureByText("application.properties", "spring.jpa.properties.hibernate.arch<caret>")
        doTest(
            "spring.jpa.properties.hibernate.archive.autodetection",
            "spring.jpa.properties.hibernate.archive.interpreter",
            "spring.jpa.properties.hibernate.archive.scanner"
        )
    }

    fun testJpaPropertiesHibernate() {
        myFixture.configureByText("application.properties", "spring.jpa.properties.hibernate.archive.<caret>")
        doTest(
            "spring.jpa.properties.hibernate.archive.autodetection",
            "spring.jpa.properties.hibernate.archive.interpreter",
            "spring.jpa.properties.hibernate.archive.scanner"
        )
    }

    fun testJpaPropertiesHibernateNotShowDuplicate() {
        myFixture.configureByText(
            "application.properties",
            """
spring.jpa.properties.hibernate.archive.scanner=true
spring.jpa.properties.hibernate.archive.<caret>
            """.trimIndent()
        )
        doTest(
            "spring.jpa.properties.hibernate.archive.autodetection",
            "spring.jpa.properties.hibernate.archive.interpreter",
        )
    }

}