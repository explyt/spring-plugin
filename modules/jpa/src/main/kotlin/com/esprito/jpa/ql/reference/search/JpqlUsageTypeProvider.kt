package com.esprito.jpa.ql.reference.search

import com.esprito.jpa.JpaBundle
import com.esprito.jpa.ql.psi.JpqlIdentifier
import com.intellij.psi.PsiElement
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProvider

class JpqlUsageTypeProvider : UsageTypeProvider {

    override fun getUsageType(element: PsiElement): UsageType? {
        if (element is JpqlIdentifier) {
            return myUsageType
        }

        return null
    }

    companion object {
        private val myUsageType = UsageType { JpaBundle.message("explyt.jpa.usage.type") }
    }
}