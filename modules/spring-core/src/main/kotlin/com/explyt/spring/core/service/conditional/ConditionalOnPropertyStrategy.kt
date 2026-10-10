/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMember

class ConditionalOnPropertyStrategy(val module: Module) : ConditionStrategy {
    private val specReader = PropertyConditionSpecReader(
        SpringSearchService.getInstance(module.project).getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_PROPERTY)
    )
    private val values by lazy { PropertyConditionValues(module) }

    override fun handles(annotation: PsiAnnotation): Boolean = specReader.read(annotation).isNotEmpty()

    override fun verdictOf(carrier: PsiMember, activeBeans: Collection<PsiBean>): ConditionVerdict =
        specReader.read(carrier).asSequence()
            .flatMap { (annotation, specs) -> specs.asSequence().map { verdictOf(carrier, annotation, it) } }
            .combined()

    private fun verdictOf(carrier: PsiMember, annotation: PsiAnnotation, spec: PropertyConditionSpec) =
        spec.verdictOf(values::valueOf) { detail, reason ->
            ConditionEvidence(
                annotationFqn = annotation.qualifiedName ?: SpringCoreClasses.CONDITIONAL_ON_PROPERTY,
                carrierFqn = carrier.conditionCarrierFqn,
                detail = detail,
                reason = reason
            )
        }
}
