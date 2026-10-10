/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.ProfileActivation
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.util.PsiAnnotationUtils
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMember

class ProfileConditionStrategy(module: Module) : ConditionStrategy {
    private val searchService = SpringSearchService.getInstance(module.project)
    private val profileAnnotations = searchService.getMetaAnnotations(module, SpringCoreClasses.PROFILE)

    override fun handles(annotation: PsiAnnotation): Boolean = profileAnnotations.contains(annotation)

    override fun verdictOf(carrier: PsiMember, activeBeans: Collection<PsiBean>): ConditionVerdict {
        val annotation = carrier.annotations.firstOrNull { handles(it) } ?: return ConditionVerdict.Active
        if (searchService.profileActivation(carrier) != ProfileActivation.UNDECIDED) return ConditionVerdict.Active
        return ConditionVerdict.Undecided(
            listOf(
                ConditionEvidence(
                    annotationFqn = annotation.qualifiedName ?: SpringCoreClasses.PROFILE,
                    carrierFqn = carrier.conditionCarrierFqn,
                    detail = "profile expression cannot be evaluated",
                    reason = ConditionReason.PROFILE_NOT_DECIDABLE
                )
            )
        )
    }
}

class UnsupportedConditionStrategy(
    module: Module,
    private val supported: List<ConditionStrategy>
) : ConditionStrategy {
    private val conditionalAnnotations = SpringSearchService.getInstance(module.project)
        .getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL)

    override fun handles(annotation: PsiAnnotation): Boolean =
        conditionalAnnotations.contains(annotation) && supported.none { it.handles(annotation) }

    override fun verdictOf(carrier: PsiMember, activeBeans: Collection<PsiBean>): ConditionVerdict {
        val evidences = carrier.annotations.filter { handles(it) }.map { evidenceOf(it, carrier) }
        return if (evidences.isEmpty()) ConditionVerdict.Active else ConditionVerdict.Undecided(evidences)
    }

    @Suppress("DEPRECATION")
    private fun evidenceOf(annotation: PsiAnnotation, carrier: PsiMember) = ConditionEvidence(
        annotationFqn = annotation.qualifiedName ?: SpringCoreClasses.CONDITIONAL,
        carrierFqn = carrier.conditionCarrierFqn,
        detail = PsiAnnotationUtils.getTypeNames(conditionalAnnotations.getAnnotationMemberValues(annotation, VALUE))
            .takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "evaluated by ")
            ?: "condition is not evaluated statically",
        reason = ConditionReason.UNSUPPORTED_CONDITION
    )

    private companion object {
        const val VALUE = "value"
    }
}
