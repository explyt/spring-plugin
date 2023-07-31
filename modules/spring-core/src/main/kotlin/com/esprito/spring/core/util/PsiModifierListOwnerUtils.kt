package com.esprito.spring.core.util

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner

object PsiModifierListOwnerUtils {

    fun PsiModifierListOwner.isStatic() = this.hasModifierProperty(PsiModifier.STATIC)

    fun PsiModifierListOwner.isNonStatic() = !this.isStatic()

    fun PsiModifierListOwner.isPrivate() = this.hasModifierProperty(PsiModifier.PRIVATE)

    fun PsiModifierListOwner.isPublic() = this.hasModifierProperty(PsiModifier.PUBLIC)

    fun PsiModifierListOwner.isAnnotatedBy(annotations: Collection<String>, flags: Int = 0) =
        AnnotationUtil.isAnnotated(this, annotations, flags)

    fun PsiModifierListOwner.isAnnotatedBy(annotation: String, flags: Int = 0) =
        AnnotationUtil.isAnnotated(this, annotation, flags)

}