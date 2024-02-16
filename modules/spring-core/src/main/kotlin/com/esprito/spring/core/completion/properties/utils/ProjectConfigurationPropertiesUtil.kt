package com.esprito.spring.core.completion.properties.utils

import com.esprito.module.ExternalSystemModule
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.SpringSearchService
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.*
import com.intellij.psi.search.searches.AnnotatedElementsSearch

object ProjectConfigurationPropertiesUtil {

    fun getAnnotatedElements(module: Module): Iterable<PsiModifierListOwner> {
        val librariesSearchScope = ExternalSystemModule.of(module).librariesSearchScope
        val configurationPropertiesClass = JavaPsiFacade.getInstance(module.project)
            .findClass(SpringCoreClasses.CONFIGURATION_PROPERTIES, librariesSearchScope)
            ?: return emptyList()

        return AnnotatedElementsSearch.searchElements(
            configurationPropertiesClass,
            module.moduleWithDependenciesScope,
            PsiClass::class.java, PsiMethod::class.java
        )
    }

    fun extractConfigurationPropertyPrefix(module: Module, annotatedElement: PsiModifierListOwner): String? {
        if (annotatedElement !is PsiMember) {
            return null
        }
        val metaHolder = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.CONFIGURATION_PROPERTIES)
        val prefix = metaHolder.getAnnotationMemberValues(annotatedElement, setOf("value")).firstOrNull() ?: return null
        return AnnotationUtil.getStringAttributeValue(prefix)
    }

    fun extractConfigurationPropertyPrefix(module: Module, annotatedElement: PsiAnnotation): String? {
        val metaHolder = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.CONFIGURATION_PROPERTIES)
        val prefix = metaHolder.getAnnotationMemberValues(annotatedElement, setOf("value")).firstOrNull() ?: return null
        return AnnotationUtil.getStringAttributeValue(prefix)
    }

}
