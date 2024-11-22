/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.util.PropertyUtil
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