/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedByOrSelf
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner

/**
 * Reads the declaration evidence of one bean, on demand.
 *
 * Kept out of enumeration deliberately: a query for a single `Clock` would otherwise pay for the annotations of
 * every bean in the context. Everything here is what the source *says*, never what it would evaluate to - a
 * `@Profile("dev")` is reported as declared, not as active.
 *
 * Must run under the same read action that produced the record.
 */
object BeanDetailsReader {

    fun read(record: ScopedBeanRecord): BeanDetailsEvidence {
        val declaration = record.declaration?.takeIf { it.isValid }
            ?: return BeanDetailsEvidence(aliases = record.knownNames.toList(), primary = record.primary)

        return BeanDetailsEvidence(
            aliases = record.knownNames.toList(),
            qualifiers = declaration.annotationsMetaAnnotatedBy(SpringCoreClasses.QUALIFIER),
            primary = record.primary ?: declaration.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY),
            profiles = declaration.declaredProfiles(),
            conditions = declaration.annotationsMetaAnnotatedBy(SpringCoreClasses.CONDITIONAL),
            module = ModuleUtilCore.findModuleForPsiElement(declaration)?.name ?: record.declarationModule
        )
    }

    /**
     * Profiles of the member and of the class that declares it: a `@Bean` inside a `@Profile("dev")`
     * configuration is only ever created under that profile, and omitting the enclosing declaration would
     * describe the bean as unconditional.
     */
    private fun PsiMember.declaredProfiles(): List<String> = declarationChain()
        .flatMap { owner -> owner.annotationValues(SpringCoreClasses.PROFILE) }
        .distinct()

    private fun PsiMember.annotationsMetaAnnotatedBy(metaAnnotation: String): List<BeanAnnotationEvidence> =
        declarationChain().flatMap { owner ->
            owner.annotations().filter { it.isMetaAnnotatedByOrSelf(metaAnnotation) }.map { it.toEvidence(owner) }
        }

    /** The member itself, then the class that declares it - the enclosing annotations apply to both. */
    private fun PsiMember.declarationChain(): List<PsiModifierListOwner> = when (this) {
        is PsiMethod -> listOfNotNull(this, containingClass)
        is PsiClass -> listOf(this)
        else -> listOfNotNull(this as? PsiModifierListOwner)
    }

    private fun PsiModifierListOwner.annotations(): List<PsiAnnotation> =
        modifierList?.annotations?.toList() ?: emptyList()

    private fun PsiModifierListOwner.annotationValues(annotationFqn: String): List<String> =
        annotations().filter { it.isMetaAnnotatedByOrSelf(annotationFqn) }
            .flatMap { it.findAttributeValue("value").asStrings() }

    private fun PsiAnnotation.toEvidence(owner: PsiModifierListOwner): BeanAnnotationEvidence {
        val attributes = mutableMapOf<String, String>()
        val unknown = mutableSetOf<String>()
        parameterList.attributes.forEach { attribute ->
            val name = attribute.name ?: "value"
            val values = attribute.value.asStrings()
            // A non-constant attribute is named as unknown rather than dropped: reporting nothing would claim the
            // annotation carries no such attribute at all.
            if (values.isEmpty()) unknown += name else attributes[name] = values.joinToString(",")
        }
        return BeanAnnotationEvidence(
            annotationClass = qualifiedName ?: "",
            attributes = attributes,
            unknownAttributes = unknown,
            origin = (owner as? PsiClass)?.qualifiedName ?: (owner as? PsiMethod)?.containingClass?.qualifiedName
        )
    }

    private fun PsiAnnotationMemberValue?.asStrings(): List<String> = when (this) {
        null -> emptyList()
        is PsiArrayInitializerMemberValue -> initializers.flatMap { it.asStrings() }
        else -> listOfNotNull(constantText())
    }

    /** Only a string literal is a value we can report; anything computed is reported as unknown instead. */
    private fun PsiElement.constantText(): String? = text
        ?.takeIf { it.length >= 2 && it.startsWith("\"") && it.endsWith("\"") }
        ?.removeSurrounding("\"")
}
