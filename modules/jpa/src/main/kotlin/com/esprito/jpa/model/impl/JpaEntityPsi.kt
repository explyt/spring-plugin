package com.esprito.jpa.model.impl

import com.esprito.jpa.model.JpaEntity
import com.esprito.jpa.model.JpaEntityAttribute
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.UClass

class JpaEntityPsi private constructor(
    psiElement: PsiElement
) : JpaEntity {
    private val cacheProvider = psiElement.project
        .service<JpaEntityPsiCacheProvider>()

    private val psiElementPointer = SmartPointerManager.createPointer(psiElement)

    override val psiElement: PsiElement?
        get() = psiElementPointer.element

    override val name: String?
        get() {
            val psiElement = psiElement ?: return null
            return cacheProvider.computeName(psiElement)
        }

    override val attributes: List<JpaEntityAttribute>
        get() {
            val psiElement = psiElement ?: return emptyList()

            return cacheProvider.computeAttributes(psiElement)
        }

    companion object {
        operator fun invoke(uClass: UClass): JpaEntityPsi? {
            val sourcePsi = uClass.sourcePsi
                ?: return null

            return CachedValuesManager.getCachedValue(sourcePsi) {
                CachedValueProvider.Result(
                    JpaEntityPsi(sourcePsi),
                    sourcePsi
                )
            }
        }
    }
}