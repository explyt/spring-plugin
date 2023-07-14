package com.esprito.jpa.model.impl

import com.esprito.jpa.model.JpaEntityAttributeType
import com.esprito.jpa.model.JpaEntityAttributeType.Unknown
import com.esprito.jpa.service.JpaService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassType
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.uast.UField

@Service(Service.Level.PROJECT)
class JpaEntityAttributePsiParseService(
    private val project: Project
) {
    private val jpaService by lazy { JpaService.getInstance(project) }
    private val javaPsiFacade by lazy { JavaPsiFacade.getInstance(project) }

    fun computeAttributeType(uField: UField): JpaEntityAttributeType {
        @Suppress("UElementAsPsi")
        val psiType = uField.type

        if (TypeConversionUtil.isPrimitiveAndNotNull(psiType) ||
            TypeConversionUtil.isPrimitiveWrapper(psiType)
        ) {
            return JpaEntityAttributeType.Scalar(psiType)
        }

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

        return Unknown
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