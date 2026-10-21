/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.SpringSearchService
import com.intellij.openapi.module.Module

object PropertyConditionAnnotations {
    private val keyAnnotations = setOf(
        SpringCoreClasses.CONDITIONAL_ON_PROPERTY,
        SpringCoreClasses.CONDITIONAL_ON_BOOLEAN_PROPERTY
    )

    fun relatedAnnotationHolder(
        module: Module,
        annotationFqn: String,
        attributeName: String,
        targetAttributes: Set<String>
    ): MetaAnnotationsHolder? {
        val searchService = SpringSearchService.getInstance(module.project)
        return keyAnnotations.firstNotNullOfOrNull { rootFqn ->
            searchService.getMetaAnnotations(module, rootFqn).takeIf {
                it.isAttributeRelatedWith(annotationFqn, attributeName, rootFqn, targetAttributes)
            }
        }
    }
}
