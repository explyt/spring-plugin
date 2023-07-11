package com.esprito.jpa.model.impl

import com.esprito.jpa.model.JpaEntityAttribute
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.UField

class JpaEntityAttributePsi private constructor(
    psiElement: PsiElement
) : JpaEntityAttribute {
    private val psiElementPointer = SmartPointerManager.createPointer(psiElement)

    override val name: String
        get() = TODO("Not yet implemented")

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