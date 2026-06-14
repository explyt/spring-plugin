/*
 * Copyright © 2025 Explyt Ltd
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
