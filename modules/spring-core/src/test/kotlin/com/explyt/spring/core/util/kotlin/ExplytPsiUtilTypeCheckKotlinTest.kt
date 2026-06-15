/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.util.ExplytPsiUtil.isCollection
import com.explyt.util.ExplytPsiUtil.isList
import com.explyt.util.ExplytPsiUtil.isMap
import com.explyt.util.ExplytPsiUtil.isString
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope

class ExplytPsiUtilTypeCheckKotlinTest : ExplytKotlinLightTestCase() {

    override fun getTestDataPath(): String = "testdata/kotlin/"

    override fun setUp() {
        super.setUp()
        myFixture.configureByFile("psiutil/TypeChecks.kt")
    }

    private fun fieldType(name: String): PsiType {
        val lightClass = JavaPsiFacade.getInstance(project)
            .findClass("TypeChecks", GlobalSearchScope.projectScope(project))
            ?: error("Light class TypeChecks not found")
        return lightClass.findFieldByName(name, false)?.type
            ?: error("Field '$name' not found in TypeChecks light class")
    }

    // kotlin.collections.List
    fun testKotlinListIsList() {
        assertTrue("kotlin List should be isList", fieldType("kotlinList").isList)
    }

    fun testKotlinListIsCollection() {
        assertTrue("kotlin List should be isCollection", fieldType("kotlinList").isCollection)
    }

    fun testKotlinListIsNotMap() {
        assertFalse("kotlin List should not be isMap", fieldType("kotlinList").isMap)
    }

    // kotlin.collections.MutableList
    fun testKotlinMutableListIsList() {
        assertTrue("kotlin MutableList should be isList", fieldType("kotlinMutableList").isList)
    }

    fun testKotlinMutableListIsCollection() {
        assertTrue("kotlin MutableList should be isCollection", fieldType("kotlinMutableList").isCollection)
    }

    // kotlin.collections.Collection
    fun testKotlinCollectionIsCollection() {
        assertTrue("kotlin Collection should be isCollection", fieldType("kotlinCollection").isCollection)
    }

    fun testKotlinCollectionIsNotList() {
        assertFalse("kotlin Collection should not be isList", fieldType("kotlinCollection").isList)
    }

    fun testKotlinCollectionIsNotMap() {
        assertFalse("kotlin Collection should not be isMap", fieldType("kotlinCollection").isMap)
    }

    // kotlin.collections.MutableCollection
    fun testKotlinMutableCollectionIsCollection() {
        assertTrue("kotlin MutableCollection should be isCollection", fieldType("kotlinMutableCollection").isCollection)
    }

    // kotlin.collections.Map
    fun testKotlinMapIsMap() {
        assertTrue("kotlin Map should be isMap", fieldType("kotlinMap").isMap)
    }

    fun testKotlinMapIsNotCollection() {
        assertFalse("kotlin Map should not be isCollection", fieldType("kotlinMap").isCollection)
    }

    // kotlin.collections.MutableMap
    fun testKotlinMutableMapIsMap() {
        assertTrue("kotlin MutableMap should be isMap", fieldType("kotlinMutableMap").isMap)
    }

    // kotlin.collections.Set (should be collection but not list)
    fun testKotlinSetIsCollection() {
        assertTrue("kotlin Set should be isCollection", fieldType("kotlinSet").isCollection)
    }

    fun testKotlinSetIsNotList() {
        assertFalse("kotlin Set should not be isList", fieldType("kotlinSet").isList)
    }

    // kotlin.collections.MutableSet
    fun testKotlinMutableSetIsCollection() {
        assertTrue("kotlin MutableSet should be isCollection", fieldType("kotlinMutableSet").isCollection)
    }

    // kotlin.String
    fun testKotlinStringIsString() {
        assertTrue("kotlin String should be isString", fieldType("kotlinString").isString)
    }

    fun testKotlinStringIsNotCollection() {
        assertFalse("kotlin String should not be isCollection", fieldType("kotlinString").isCollection)
    }

    // kotlin.Int
    fun testKotlinIntIsNotCollection() {
        assertFalse("kotlin Int should not be isCollection", fieldType("kotlinInt").isCollection)
    }

    fun testKotlinIntIsNotMap() {
        assertFalse("kotlin Int should not be isMap", fieldType("kotlinInt").isMap)
    }
}
