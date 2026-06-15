/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.util.ExplytPsiUtil.isCollection
import com.explyt.util.ExplytPsiUtil.isList
import com.explyt.util.ExplytPsiUtil.isMap
import com.explyt.util.ExplytPsiUtil.isOptional
import com.explyt.util.ExplytPsiUtil.isString
import com.intellij.psi.PsiField
import com.intellij.psi.util.PsiTreeUtil

class ExplytPsiUtilTypeCheckJavaTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = "testdata/java/"

    override fun setUp() {
        super.setUp()
        myFixture.configureByFile("psiutil/TypeChecks.java")
    }

    private fun fieldType(name: String) =
        PsiTreeUtil.findChildrenOfType(myFixture.file, PsiField::class.java)
            .first { it.name == name }
            .type

    fun testJavaListIsList() {
        assertTrue("java.util.List should be isList", fieldType("javaList").isList)
    }

    fun testJavaListIsCollection() {
        assertTrue("java.util.List should be isCollection", fieldType("javaList").isCollection)
    }

    fun testJavaListIsNotMap() {
        assertFalse("java.util.List should not be isMap", fieldType("javaList").isMap)
    }

    fun testJavaCollectionIsCollection() {
        assertTrue("java.util.Collection should be isCollection", fieldType("javaCollection").isCollection)
    }

    fun testJavaCollectionIsNotList() {
        assertFalse("java.util.Collection should not be isList", fieldType("javaCollection").isList)
    }

    fun testJavaMapIsMap() {
        assertTrue("java.util.Map should be isMap", fieldType("javaMap").isMap)
    }

    fun testJavaMapIsNotCollection() {
        assertFalse("java.util.Map should not be isCollection", fieldType("javaMap").isCollection)
    }

    fun testJavaSetIsCollection() {
        assertTrue("java.util.Set should be isCollection", fieldType("javaSet").isCollection)
    }

    fun testJavaSetIsNotList() {
        assertFalse("java.util.Set should not be isList", fieldType("javaSet").isList)
    }

    fun testJavaArrayListIsList() {
        assertTrue("java.util.ArrayList should be isList", fieldType("javaArrayList").isList)
    }

    fun testJavaArrayListIsCollection() {
        assertTrue("java.util.ArrayList should be isCollection", fieldType("javaArrayList").isCollection)
    }

    fun testJavaHashMapIsMap() {
        assertTrue("java.util.HashMap should be isMap", fieldType("javaHashMap").isMap)
    }

    fun testJavaOptionalIsOptional() {
        assertTrue("java.util.Optional should be isOptional", fieldType("javaOptional").isOptional)
    }

    fun testJavaOptionalIsNotCollection() {
        assertFalse("java.util.Optional should not be isCollection", fieldType("javaOptional").isCollection)
    }

    fun testJavaStringIsString() {
        assertTrue("java.lang.String should be isString", fieldType("javaString").isString)
    }

    fun testJavaStringIsNotCollection() {
        assertFalse("java.lang.String should not be isCollection", fieldType("javaString").isCollection)
    }

    fun testJavaPrimitiveIsNotCollection() {
        assertFalse("int should not be isCollection", fieldType("javaPrimitive").isCollection)
    }

    fun testJavaPrimitiveIsNotMap() {
        assertFalse("int should not be isMap", fieldType("javaPrimitive").isMap)
    }
}
