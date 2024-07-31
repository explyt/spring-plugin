package com.esprito.spring.core.properties.dataRetriever

import ai.grazie.utils.capitalize
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter

class ConstructorParameterConfigurationPropertyDataRetriever(
    private val uParameter: UParameter,
    private val containingClass: PsiClass,
    private val name: String
) : ConfigurationPropertyDataRetriever() {

    override fun getContainingClass(): PsiClass = containingClass

    override fun getMemberName(): String = name

    override fun getNameElementPsi(): PsiElement? {
        return uParameter.uastAnchor?.sourcePsi
    }

    override fun isMap(): Boolean {
        val psiType = uParameter.typeReference?.type ?: return false
        return InheritanceUtil.isInheritor(psiType, Map::class.java.name)
    }

    override fun isCollection(): Boolean {
        val psiType = uParameter.typeReference?.type ?: return false
        return InheritanceUtil.isInheritor(psiType, Iterable::class.java.name) || psiType is PsiArrayType
    }

    companion object {
        fun create(uParameter: UParameter): ConfigurationPropertyDataRetriever? {
            val psiParameter = (uParameter.javaPsi as? PsiParameter) ?: return null
            if (psiParameter.language != KotlinLanguage.INSTANCE) return null
            val uMethod = (uParameter.uastParent as? UMethod) ?: return null
            if (!uMethod.isConstructor) return null
            val containingClass = uMethod.javaPsi.containingClass ?: return null

            return ConstructorParameterConfigurationPropertyDataRetriever(
                uParameter,
                containingClass,
                psiParameter.name.capitalize()
            )
        }
    }

}