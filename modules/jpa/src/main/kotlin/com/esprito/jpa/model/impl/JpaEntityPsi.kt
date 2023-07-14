package com.esprito.jpa.model.impl

import com.esprito.jpa.model.JpaEntity
import com.esprito.jpa.model.JpaEntityAttribute
import com.intellij.openapi.components.service
import com.intellij.psi.PsiClass
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

class JpaEntityPsi private constructor(
    psiClass: PsiClass
) : JpaEntity {
    private val psiElementPointer = SmartPointerManager.createPointer(psiClass)

    private val psiParseService = JpaEntityPsiParseService.getInstance(psiElementPointer.project)

    override val psiElement: PsiClass?
        get() = psiElementPointer.element

    override val isValid: Boolean
        get() = psiElementPointer.element != null

    override val name: String?
        get() {
            val psiElement = psiElement ?: return null
            return psiParseService.computeName(psiElement)
        }

    override val attributes: List<JpaEntityAttribute>
        get() {
            val psiElement = psiElement ?: return emptyList()

            return psiParseService.computeAttributes(psiElement)
        }

    override val isPersistent: Boolean
        get() {
            val psiElement = psiElement ?: return false

            return psiParseService.isPersistent(psiElement)
        }

    override fun toString(): String {
        if (isValid) {
            return "${psiElement?.qualifiedName}: $name"
        }

        return "--outdated--"
    }

    companion object {
        operator fun invoke(psiClass: PsiClass): JpaEntityPsi {
            return CachedValuesManager.getCachedValue(psiClass) {
                CachedValueProvider.Result(
                    JpaEntityPsi(psiClass),
                    psiClass
                )
            }
        }
    }
}