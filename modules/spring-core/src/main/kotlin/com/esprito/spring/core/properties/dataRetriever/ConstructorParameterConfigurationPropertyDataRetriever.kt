package com.esprito.spring.core.properties.dataRetriever

import ai.grazie.utils.capitalize
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter

class ConstructorParameterConfigurationPropertyDataRetriever(
    private val uParameter: UParameter,
    private val containingClass: PsiClass,
    private val name: String
) :
    ConfigurationPropertyDataRetriever() {

    override fun getContainingClass(): PsiClass = containingClass

    override fun getMemberName(): String = name

    override fun getNameElementPsi(): PsiElement? {
        return uParameter.uastAnchor?.sourcePsi
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