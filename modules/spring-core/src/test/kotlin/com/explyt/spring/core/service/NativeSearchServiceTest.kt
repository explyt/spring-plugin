/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

class NativeSearchServiceTest : ExplytJavaLightTestCase() {

    fun testFilterValidBeansExcludesInvalidCachedBean() {
        val validBean = PsiBean(psiClass(myFixture.configureByText("ValidBean.java", "public class ValidBean {}")))
        val invalidFile = myFixture.addFileToProject("InvalidBean.java", "public class InvalidBean {}")
        val invalidBean = PsiBean(psiClass(invalidFile))

        WriteCommandAction.runWriteCommandAction(project) {
            invalidFile.virtualFile.delete(this)
        }
        myFixture.addFileToProject("InvalidBean.java", "public class ReplacementBean {}")

        assertTrue("the unchanged bean must stay valid", validBean.psiClass.isValid)
        assertFalse("the deleted bean must be invalidated", invalidBean.psiClass.isValid)
        assertEquals(
            setOf(validBean),
            NativeSearchService(project).filterValidBeans(setOf(validBean, invalidBean))
        )
    }

    private fun psiClass(file: PsiFile): PsiClass = PsiTreeUtil.findChildOfType(file, PsiClass::class.java)!!
}
