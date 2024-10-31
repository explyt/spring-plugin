package com.esprito.spring.core.properties.dataRetriever

import com.esprito.util.ExplytPsiUtil.isNonAbstract
import com.esprito.util.ExplytPsiUtil.isSetter
import com.esprito.util.ExplytPsiUtil.returnPsiClass
import com.esprito.util.ExplytPsiUtil.returnPsiType
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UMethod

class MethodConfigurationPropertyDataRetriever private constructor(
    private val uMethod: UMethod,
) : ConfigurationPropertyDataRetriever() {

    val psiMethod = uMethod.javaPsi

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

    override fun isMap(): Boolean {
        val psiType = uMethod.returnPsiClass?.returnPsiType ?: return false
        return InheritanceUtil.isInheritor(psiType, Iterable::class.java.name)
    }

    override fun isCollection(): Boolean {
        val psiType = uMethod.returnPsiClass?.returnPsiType ?: return false
        return InheritanceUtil.isInheritor(psiType, Iterable::class.java.name) || psiType is PsiArrayType
    }

    companion object {
        fun create(uMethod: UMethod): ConfigurationPropertyDataRetriever {
            return MethodConfigurationPropertyDataRetriever(uMethod)
        }
    }

}