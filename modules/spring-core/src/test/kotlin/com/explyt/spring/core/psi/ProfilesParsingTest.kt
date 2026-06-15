/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.psi

import com.explyt.spring.core.language.profiles.ProfilesParserDefinition
import com.intellij.testFramework.ParsingTestCase

class ProfilesParsingTest : ParsingTestCase("psi", "profiles", ProfilesParserDefinition()) {

    //Valid
    fun testValue() = doTest(true)
    fun testBracedValue() = doTest(true)
    fun testNested() = doTest(true)

    //Error
    fun testEmpty() = doTest(true)
    fun testEmptyBraces() = doTest(true)
    fun testMixedOperations() = doTest(true)


    override fun getTestDataPath(): String = "testdata"

    override fun skipSpaces(): Boolean = true

    override fun includeRanges(): Boolean = true
}