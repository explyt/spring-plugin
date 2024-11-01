package com.explyt.jpa.model.impl

import com.explyt.jpa.model.JpaEntityAttributeType
import com.explyt.jpa.model.JpaEntityAttributeType.Unknown
import com.explyt.jpa.service.JpaService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClassType
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UField

@Service(Service.Level.PROJECT)
class JpaEntityAttributePsiParseService(
    private val project: Project
) {
    private val jpaService by lazy {
        JpaService.getInstance(project)
    }

    fun computeAttributeType(uField: UField): JpaEntityAttributeType {
        @Suppress("UElementAsPsi")
        val psiType = uField.type

        if (psiType is PsiClassType) {
            val aClass = psiType.resolve()
                ?: return Unknown

            if (jpaService.isJpaEntity(aClass)) {
                val jpaEntity = JpaEntityPsi(aClass)

                return JpaEntityAttributeType.Entity(jpaEntity)
            }

            if (InheritanceUtil.isInheritor(aClass, CommonClassNames.JAVA_UTIL_COLLECTION)) {
                return resolveCollectionClass(psiType)
            }
        }

        return JpaEntityAttributeType.Scalar(psiType)
    }

    private fun resolveCollectionClass(psiType: PsiClassType): JpaEntityAttributeType {
        val collectionContentType = psiType.parameters.getOrNull(0)
            ?: return Unknown

        if (collectionContentType is PsiClassType) {
            val collectionContentClass = collectionContentType.resolve() ?: return Unknown

            if (jpaService.isJpaEntity(collectionContentClass)) {
                return JpaEntityAttributeType.Entity(
                    JpaEntityPsi(collectionContentClass)
                )
            }
        }
        return Unknown
    }

    companion object {
        fun getInstance(project: Project): JpaEntityAttributePsiParseService = project.service()
    }
}