package com.esprito.spring.core.inspections.quickfix

import com.esprito.spring.core.SpringCoreBundle
import com.intellij.codeInsight.daemon.impl.quickfix.AddMethodFix
import com.intellij.psi.PsiClass

/**
 * This class provides a solution to inspection
 */
class AddMethodQuickFix(private val methodName: String, private val abstract: Boolean, aClass: PsiClass) :
    AddMethodFix(getMethodAsText(methodName, abstract, aClass), aClass) {

    override fun getFamilyName(): String = SpringCoreBundle.message("esprito.spring.inspection.bean.method.quickfix")

    override fun getText(): String {
        return if (abstract) {
            SpringCoreBundle.message("esprito.spring.inspection.bean.method.quickfix.createAbstract", methodName)
        } else {
            SpringCoreBundle.message("esprito.spring.inspection.bean.method.quickfix.create", methodName)
        }
    }

    companion object {
        fun getMethodAsText(methodName: String, abstract: Boolean, aClass: PsiClass): String {
            return when {
                abstract -> "protected abstract void $methodName();"
                aClass.isInterface -> "void $methodName();"
                else -> "private void $methodName() {}"
            }
        }
    }
}

