package com.esprito.jpa.ql.psi

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