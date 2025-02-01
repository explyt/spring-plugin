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