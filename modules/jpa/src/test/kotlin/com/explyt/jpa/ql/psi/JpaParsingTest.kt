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

package com.explyt.jpa.ql.psi

import com.intellij.testFramework.ParsingTestCase

class JpaParsingTest : ParsingTestCase("psi", "jpql", JpqlParserDefinition()) {
    fun testSimpleSelect() = doTest(true)

    fun testUpdate() = doTest(true)

    fun testLike() = doTest(true)

    fun testBetween() = doTest(true)

    fun testGeneralCase() = doTest(true)

    fun testSimpleCase() = doTest(true)

//    fun testDates() = doTest(true)

    fun testDateLiteral() = doTest(true)

    fun testJoin() = doTest(true)

    fun testConditional() = doTest(true)

    fun testTrim() = doTest(true)

    fun testConstructor() = doTest(true)

    fun testMisc() = doTest(true)

    fun testAggregate() = doTest(true)

    fun testFunction() = doTest(true)

    fun testType() = doTest(true)

    fun testDelete() = doTest(true)

    fun testInsert() = doTest(true)


    override fun getTestDataPath(): String = "testdata"

    override fun skipSpaces(): Boolean = true

    override fun includeRanges(): Boolean = true
}