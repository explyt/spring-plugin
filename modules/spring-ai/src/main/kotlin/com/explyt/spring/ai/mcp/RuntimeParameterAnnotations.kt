/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.util.ExplytPsiUtil.resolveUAnnotationType
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiReference
import java.lang.annotation.ElementType
import java.lang.annotation.RetentionPolicy

private val UNRESOLVABLE_KOTLIN_NULLABILITY_ANNOTATIONS = setOf(
    "org.jetbrains.annotations.NotNull",
    "org.jetbrains.annotations.Nullable",
)

internal fun PsiAnnotation.isRuntimeParameterAnnotation(): Boolean {
    val annotationClass = annotationClass()
        ?: return qualifiedName !in UNRESOLVABLE_KOTLIN_NULLABILITY_ANNOTATIONS
    return annotationClass.retentionPolicy() == RetentionPolicy.RUNTIME.name &&
            annotationClass.targets()?.contains(ElementType.PARAMETER.name) != false
}

private fun PsiAnnotation.annotationClass(): PsiClass? =
    resolveAnnotationType() ?: resolveUAnnotationType() ?: findAnnotationClassByName()

private fun PsiAnnotation.findAnnotationClassByName(): PsiClass? {
    val name = qualifiedName ?: return null
    return JavaPsiFacade.getInstance(project).findClass(name, resolveScope)?.takeIf { it.isAnnotationType }
}

private fun PsiClass.retentionPolicy(): String =
    metaAnnotationValue(CommonClassNames.JAVA_LANG_ANNOTATION_RETENTION)
        ?.let(::enumConstantNames)?.singleOrNull()
        ?: RetentionPolicy.CLASS.name

private fun PsiClass.targets(): Set<String>? =
    metaAnnotationValue(CommonClassNames.JAVA_LANG_ANNOTATION_TARGET)?.let(::enumConstantNames)?.toSet()

private fun PsiClass.metaAnnotationValue(annotationFqn: String): PsiAnnotationMemberValue? =
    annotations.firstOrNull { it.qualifiedName == annotationFqn }?.findAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME)

private fun enumConstantNames(value: PsiAnnotationMemberValue): List<String> = when (value) {
    is PsiArrayInitializerMemberValue -> value.initializers.flatMap(::enumConstantNames)
    is PsiReference -> listOfNotNull((value.resolve() as? PsiEnumConstant)?.name)
    else -> emptyList()
}
