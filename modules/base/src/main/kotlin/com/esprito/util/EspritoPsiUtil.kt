package com.esprito.util

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.codeInspection.isInheritorOf
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType

object EspritoPsiUtil {
    val PsiModifierListOwner.isStatic: Boolean
        get() = this.hasModifierProperty(PsiModifier.STATIC)

    val PsiModifierListOwner.isNonStatic: Boolean
        get() = !this.isStatic

    val PsiModifierListOwner.isPrivate: Boolean
        get() = this.hasModifierProperty(PsiModifier.PRIVATE)

    val PsiModifierListOwner.isPublic: Boolean
        get() = this.hasModifierProperty(PsiModifier.PUBLIC)

    val PsiModifierListOwner.isAbstract: Boolean
        get() = this.hasModifierProperty(PsiModifier.ABSTRACT)

    fun PsiModifierListOwner.isAnnotatedBy(annotations: Collection<String>, flags: Int = 0) =
        AnnotationUtil.isAnnotated(this, annotations, flags)

    fun PsiModifierListOwner.isAnnotatedBy(annotation: String, flags: Int = 0) =
        AnnotationUtil.isAnnotated(this, annotation, flags)

    fun PsiModifierListOwner.isMetaAnnotatedBy(annotations: Collection<String>) =
        MetaAnnotationUtil.isMetaAnnotated(this, annotations)

    fun PsiModifierListOwner.isMetaAnnotatedBy(annotation: String) =
        MetaAnnotationUtil.isMetaAnnotated(this, listOf(annotation))

    fun PsiMember.inClassAnnotatedBy(annotation: String, flags: Int = 0) =
        this.containingClass?.let {
            it.qualifiedName != null && it.isAnnotatedBy(annotation, flags)
        } ?: false

    fun PsiMember.inClassMetaAnnotatedBy(annotation: String) =
        this.containingClass?.let {
            it.qualifiedName != null && it.isMetaAnnotatedBy(annotation)
        } ?: false

    fun PsiMember.getMetaAnnotation(annotation: String): PsiAnnotation? =
        this.annotations.first {
            it.qualifiedName == annotation || it.resolveAnnotationType()?.isMetaAnnotatedBy(annotation) ?: false
        }

    fun PsiClass.isEqualOrInheritor(baseClass: PsiClass, checkDeep: Boolean = true): Boolean {
        return this == baseClass || this.isInheritor(baseClass, checkDeep)
    }

    val PsiClass.isOrdinaryClass: Boolean
        get() = !isInterface && !isEnum && !isAnnotationType


    val PsiType.resolvedPsiClass: PsiClass?
        get() = (this as? PsiClassType)?.resolve()

    val PsiType.resolvedDeepPsiClass: PsiClass?
        get() = (this.deepComponentType as? PsiClassType)?.resolve()

    val PsiType.isCollection : Boolean
        get() = this.isInheritorOf(java.util.Collection::class.java.canonicalName)

    val PsiType.isMap : Boolean
        get() = this.isInheritorOf(java.util.Map::class.java.canonicalName)

    val PsiType.isOptional : Boolean
        get() = this.isInheritorOf(java.util.Optional::class.java.canonicalName)

    val PsiType.isString : Boolean
        get() = this.isInheritorOf(java.lang.String::class.java.canonicalName)

    val PsiMember.returnPsiClass: PsiClass?
        get() = this.childrenOfType<PsiTypeElement>().firstOrNull()?.type?.resolvedPsiClass


}