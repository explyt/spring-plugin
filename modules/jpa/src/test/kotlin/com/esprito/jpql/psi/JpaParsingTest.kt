package com.esprito.jpql.psi

import com.esprito.jpa.ql.psi.JpqlParserDefinition
import com.intellij.testFramework.ParsingTestCase

class JpaParsingTest : ParsingTestCase("", "jpql", JpqlParserDefinition()) {
    fun testSimpleSelect() = doTest(true)

    fun testUpdate() = doTest(true)

    fun testLike() = doTest(true)

    fun testBetween() = doTest(true)

    fun testGeneralCase() = doTest(true)

    fun testSimpleCase() = doTest(true)

//    fun testDates() = doTest(true)

    fun testDateLiteral() = doTest(true)

    fun testJoin() = doTest(true)

    override fun getTestDataPath(): String = "src/test/testData"

    override fun skipSpaces(): Boolean = true

    override fun includeRanges(): Boolean = true
}