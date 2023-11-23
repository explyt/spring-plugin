package com.esprito.spring.core.service.conditional

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.service.PsiBean
import com.esprito.spring.core.util.PropertyUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember

class ConditionalOnPropertyStrategy(val module: Module) : ExclusionStrategy {
    private val annotationHolder = MetaAnnotationsHolder.of(module, SpringCoreClasses.CONDITIONAL_ON_PROPERTY)

    override fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean {
        if (dependant.annotations.none { annotationHolder.contains(it) }) {
            return false
        }

        val prefix = annotationHolder.getAnnotationMemberValues(dependant, setOf("prefix"))
            .asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .firstOrNull()
        val prefixValue = PropertyUtil.prefixValue(prefix)

        val propertyKeys = DefinedConfigurationPropertiesSearch.getInstance(module.project)
            .getAllProperties(module)
            .mapTo(mutableSetOf()) { it.key }

        return annotationHolder.getAnnotationMemberValues(dependant, setOf("name", "value"))
            .asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .map { "$prefixValue$it" }
            .any { !propertyKeys.contains(it) }
    }

}