/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.PsiBean
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMember

interface ConditionStrategy {
    fun handles(annotation: PsiAnnotation): Boolean

    fun verdictOf(carrier: PsiMember, activeBeans: Collection<PsiBean>): ConditionVerdict
}

abstract class AnnotationConditionStrategy(
    private val holders: List<MetaAnnotationsHolder>,
    private val assumptions: Set<ConditionAssumption> = emptySet()
) : ConditionStrategy {

    override fun handles(annotation: PsiAnnotation): Boolean = holders.any { it.contains(annotation) }

    override fun verdictOf(carrier: PsiMember, activeBeans: Collection<PsiBean>): ConditionVerdict =
        holders.asSequence().map { holder -> verdictOf(holder, carrier, activeBeans) }.combined()

    protected abstract fun unmetRequirement(
        holder: MetaAnnotationsHolder, carrier: PsiMember, activeBeans: Collection<PsiBean>
    ): String?

    private fun verdictOf(
        holder: MetaAnnotationsHolder, carrier: PsiMember, activeBeans: Collection<PsiBean>
    ): ConditionVerdict {
        val annotation = carrier.annotations.firstOrNull { holder.contains(it) } ?: return ConditionVerdict.Active
        val unmet = unmetRequirement(holder, carrier, activeBeans) ?: return ConditionVerdict.Active
        return ConditionVerdict.Inactive(
            ConditionEvidence(
                annotationFqn = annotation.qualifiedName ?: holder.getRootClassQualified(),
                carrierFqn = carrier.conditionCarrierFqn,
                detail = unmet,
                reason = ConditionReason.NOT_MATCHED,
                assumptions = assumptions
            )
        )
    }
}
