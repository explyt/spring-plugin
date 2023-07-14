package com.esprito.jpa.service

import com.esprito.jpa.JpaClasses
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass

@Service(Service.Level.PROJECT)
class JpaService {
    fun isJpaEntity(psiClass: PsiClass): Boolean {
        val jpaAnnotations = listOf(
            JpaClasses.entity,
            JpaClasses.mappedSuperclass,
            JpaClasses.embeddable
        ).flatMap {
            it.allFqns
        }

        return psiClass.annotations.any { it.qualifiedName in jpaAnnotations }
    }

    companion object {
        fun getInstance(project: Project): JpaService = project.service()
    }
}