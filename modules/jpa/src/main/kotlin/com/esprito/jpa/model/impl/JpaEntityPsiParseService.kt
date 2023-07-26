package com.esprito.jpa.model.impl

import com.esprito.jpa.JpaClasses
import com.esprito.jpa.model.JpaEntityAttribute
import com.esprito.jpa.service.JpaService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElementOfType

@Service(Service.Level.PROJECT)
class JpaEntityPsiParseService(
    private val project: Project
) {
    private val jpaService = JpaService.getInstance(project)

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

        return loadPossibleEntityAttributeFields(uClass)
            .asSequence()
            .filter { isEntityAttribute(it) }
            .mapNotNull { JpaEntityAttributePsi(it) }
            .toList()
    }

    private fun loadPossibleEntityAttributeFields(
        uClass: UClass?,
        visited: MutableSet<UClass> = mutableSetOf()
    ): Array<UField> {
        if (uClass == null)
            return emptyArray()

        if (uClass in visited)
            return emptyArray()

        visited.add(uClass)

        val superClass = uClass.javaPsi.superClass
            ?.toUElementOfType<UClass>()

        return uClass.fields + loadPossibleEntityAttributeFields(superClass, visited)
    }

    private fun isEntityAttribute(uField: UField): Boolean {
        val psiField = uField.javaPsi as? PsiField ?: return false

        return jpaService.isJpaEntityAttribute(psiField)
    }

    fun isPersistent(psiElement: PsiClass): Boolean = JpaClasses.entity.allFqns.any {
        psiElement.hasAnnotation(it)
    }

    companion object {
        fun getInstance(project: Project): JpaEntityPsiParseService = project.service()
    }

}