package com.esprito.jpa.model.impl

import com.esprito.jpa.JpaClasses
import com.esprito.jpa.model.JpaEntityAttribute
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElementOfType

@Service(Service.Level.PROJECT)
class JpaEntityPsiCacheProvider(
    private val project: Project
) {
    fun computeName(psiElement: PsiElement): String? {
        val uClass = psiElement.toUElementOfType<UClass>()
            ?: return null

        val entityAnnotation = JpaClasses.entity.allFqns.asSequence()
            .mapNotNull { uClass.findAnnotation(it) }
            .firstOrNull()
            ?: return uClass.qualifiedName?.substringAfterLast('.')

        val nameAttribute = entityAnnotation.findAttributeValue("name")

        return nameAttribute?.evaluateString()
            ?: return uClass.qualifiedName?.substringAfterLast('.')
    }

    fun computeAttributes(psiElement: PsiElement): List<JpaEntityAttribute> {
        val uClass = psiElement.toUElementOfType<UClass>()
            ?: return emptyList()

        return uClass.fields
            .asSequence()
            .filter { !it.isStatic }
            .filter { isEntityAttribute(it) }
            .mapNotNull { JpaEntityAttributePsi(it) }
            .toList()
    }

    private fun isEntityAttribute(uField: UField): Boolean {
        return false
    }

}