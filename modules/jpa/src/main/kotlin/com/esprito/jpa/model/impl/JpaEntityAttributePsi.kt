package com.esprito.jpa.model.impl

import com.esprito.jpa.model.JpaEntityAttribute
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.UField
import org.jetbrains.uast.toUElementOfType

class JpaEntityAttributePsi private constructor(
    psiElement: PsiElement
) : JpaEntityAttribute {
    private val psiElementPointer = SmartPointerManager.createPointer(psiElement)
    override val psiElement: PsiElement?
        get() = psiElementPointer.element

    override val isValid: Boolean
        get() = psiElement != null

    override val name: String?
        get() {
            @Suppress("UElementAsPsi")
            return psiElement?.toUElementOfType<UField>()?.name
        }

    override val type: PsiType
        get() = TODO("Not yet implemented")

    companion object {
        operator fun invoke(uField: UField): JpaEntityAttributePsi? {
            val sourcePsi = uField.sourcePsi
                ?: return null

            return CachedValuesManager.getCachedValue(sourcePsi) {
                CachedValueProvider.Result(
                    JpaEntityAttributePsi(sourcePsi),
                    sourcePsi
                )
            }
        }
    }
}