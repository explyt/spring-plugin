package com.esprito.spring.core.service.conditional

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.esprito.spring.core.service.PsiBean
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.PropertyUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember

class ConditionalOnPropertyStrategy(val module: Module) : ExclusionStrategy {
    private val annotationHolder = SpringSearchService.getInstance(module.project)
        .getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_PROPERTY)

    override fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean {
        val psiAnnotation = dependant.annotations.firstOrNull { annotationHolder.contains(it) }
        if (psiAnnotation == null) return false

        val prefix = annotationHolder.getAnnotationMemberValues(dependant, setOf("prefix"))
            .asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .firstOrNull()
        val prefixValue = PropertyUtil.prefixValue(prefix)

        val propertyMap = DefinedConfigurationPropertiesSearch.getInstance(module.project)
            .getPropertiesCommonKeyMap(module)

        val propertyValue = annotationHolder.getAnnotationMemberValues(dependant, setOf("name", "value"))
            .asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .map { PropertyUtil.toCommonPropertyForm("$prefixValue$it") }
            .mapNotNull { propertyMap[it] }
            .firstOrNull()

        val havingValue = annotationHolder.getAnnotationMemberValues(dependant, setOf("havingValue"))
            .asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .firstOrNull()

        if (havingValue != null) {
            return havingValue != propertyValue?.map { it.value }?.firstOrNull()
        }

        if (propertyValue == null) {
            val matchIfMissing = AnnotationUtil.getBooleanAttributeValue(psiAnnotation, "matchIfMissing") ?: false
            if (matchIfMissing) {
                return false
            }
        }
        return propertyValue == null
    }

}