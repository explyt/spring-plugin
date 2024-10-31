package com.esprito.spring.core.properties.dataRetriever

import com.esprito.util.ExplytPsiUtil.isNonAbstract
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UField
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.getContainingUClass

class FieldConfigurationPropertyDataRetriever private constructor(
    private val uField: UField
) : ConfigurationPropertyDataRetriever() {


    override fun getContainingClass(): PsiClass? {
        val psiClass = uField.getContainingUClass() ?: return null

        return if (!psiClass.isInterface && psiClass.isNonAbstract) {
            psiClass
        } else {
            null
        }
    }

    override fun getMemberName(): String? {
        return (uField.uastAnchor as? UIdentifier)?.name
    }

    override fun getNameElementPsi(): PsiElement? {
        return uField.uastAnchor?.sourcePsi
    }

    override fun isMap(): Boolean {
        val psiType = uField.typeReference?.type ?: return false
        return InheritanceUtil.isInheritor(psiType, Map::class.java.name)
    }

    override fun isCollection(): Boolean {
        val psiType = uField.typeReference?.type ?: return false
        return InheritanceUtil.isInheritor(psiType, Iterable::class.java.name) || psiType is PsiArrayType
    }

    companion object {
        fun create(uField: UField): ConfigurationPropertyDataRetriever {
            return FieldConfigurationPropertyDataRetriever(uField)
        }
    }

}
