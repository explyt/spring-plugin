package com.esprito.spring.core.properties.dataRetriever

import com.esprito.util.EspritoPsiUtil.isNonAbstract
import com.esprito.util.EspritoPsiUtil.isSetter
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.util.childrenOfType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.uast.*

class FieldConfigurationPropertyDataRetriever private constructor(
    private val uMethod: UMethod
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


    companion object {
        fun create(uField: UField): ConfigurationPropertyDataRetriever? {
            val uMethod = getMethodFromKtField(uField) ?: return null
            return FieldConfigurationPropertyDataRetriever(uMethod)
        }

        private fun getMethodFromKtField(uField: UField): UMethod? {
            val psiField = uField.javaPsi as? PsiField ?: return null
            if (psiField.language != KotlinLanguage.INSTANCE) return null

            val name = "set${StringUtil.capitalize(psiField.name)}"

            val setter = psiField.toUElement()
                ?.sourcePsi
                ?.childrenOfType<KtPropertyAccessor>()
                ?.firstOrNull { it.isSetter }

            return if (setter == null) {
                uField.getUastParentOfType<UClass>()?.methods
                    ?.firstOrNull { it.name == name }
            } else null
        }
    }

}
