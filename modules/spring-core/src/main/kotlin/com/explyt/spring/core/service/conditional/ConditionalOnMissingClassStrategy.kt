/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember

class ConditionalOnMissingClassStrategy(private val module: Module) : AnnotationConditionStrategy(
    listOf(
        SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_MISSING_CLASS)
    ),
    setOf(ConditionAssumption.COMPILE_CLASSPATH_IS_RUNTIME)
) {

    override fun unmetRequirement(
        holder: MetaAnnotationsHolder, carrier: PsiMember, activeBeans: Collection<PsiBean>
    ): String? = ConditionClassReferences(holder, carrier).classNames("value")
        .firstOrNull { module.hasClass(it) }
        ?.let { "class $it is on the classpath" }
}
