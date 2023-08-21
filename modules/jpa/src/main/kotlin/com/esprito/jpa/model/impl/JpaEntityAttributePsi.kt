package com.esprito.jpa.model.impl

import com.esprito.jpa.model.JpaEntityAttribute
import com.esprito.jpa.model.JpaEntityAttributeType
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.UField
import org.jetbrains.uast.toUElementOfType

class JpaEntityAttributePsi private constructor(
    psiElement: PsiElement
) : JpaEntityAttribute {
    private val psiElementPointer = SmartPointerManager.createPointer(psiElement)

    private val psiParseService = JpaEntityAttributePsiParseService.getInstance(psiElementPointer.project)

    override val psiElement: PsiElement?
        get() = psiElementPointer.element

    override val isValid: Boolean
        get() = psiElement != null

    override val name: String?
        get() {
            @Suppress("UElementAsPsi")
            return psiElement?.toUElementOfType<UField>()?.name
        }

    override val type: JpaEntityAttributeType
        get() {
            val uField = (psiElement?.toUElementOfType<UField>()
                ?: return JpaEntityAttributeType.Unknown)

            return psiParseService.computeAttributeType(uField)
        }

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