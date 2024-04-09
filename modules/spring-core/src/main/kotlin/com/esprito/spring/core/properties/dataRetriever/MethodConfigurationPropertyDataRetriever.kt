package com.esprito.spring.core.properties.dataRetriever

import com.esprito.util.EspritoPsiUtil.isNonAbstract
import com.esprito.util.EspritoPsiUtil.isSetter
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UMethod

class MethodConfigurationPropertyDataRetriever private constructor(
    private val uMethod: UMethod,
    private val psiMethod: PsiMethod
) : ConfigurationPropertyDataRetriever() {

    override fun getContainingClass(): PsiClass? {
        val psiClass = psiMethod.containingClass ?: return null

        return if (!psiClass.isInterface && psiClass.isNonAbstract) {
            psiClass
        } else {
            null
        }
    }

    override fun getMemberName(): String? {
        return if (isSetter(psiMethod)) {
            toPascalFormat(psiMethod.name)
        } else {
            return null
        }
    }

    override fun getNameElementPsi(): PsiElement? {
        return uMethod.uastAnchor?.sourcePsi
    }

    companion object {
        fun create(uMethod: UMethod): ConfigurationPropertyDataRetriever {
            return MethodConfigurationPropertyDataRetriever(uMethod, uMethod.javaPsi)
        }
    }

}