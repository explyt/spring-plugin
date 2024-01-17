package com.esprito.util

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.codeInspection.isInheritorOf
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PropertyUtilBase
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.childrenOfType
import org.jetbrains.uast.toUElement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object EspritoPsiUtil {
    val PsiModifierListOwner.isStatic: Boolean
        get() = this.hasModifierProperty(PsiModifier.STATIC)

    val PsiModifierListOwner.isNonStatic: Boolean
        get() = !this.isStatic

    val PsiModifierListOwner.isPrivate: Boolean
        get() = this.hasModifierProperty(PsiModifier.PRIVATE)

    val PsiModifierListOwner.isNonPrivate: Boolean
        get() = !this.isPrivate

    val PsiModifierListOwner.isPublic: Boolean
        get() = this.hasModifierProperty(PsiModifier.PUBLIC)

    val PsiModifierListOwner.isAbstract: Boolean
        get() = this.hasModifierProperty(PsiModifier.ABSTRACT)

    val PsiModifierListOwner.isNonAbstract: Boolean
        get() = !this.isAbstract

    val PsiModifierListOwner.isFinal: Boolean
        get() = this.hasModifierProperty(PsiModifier.FINAL)

    val PsiModifierListOwner.isTransient: Boolean
        get() = this.hasModifierProperty(PsiModifier.TRANSIENT)

    fun PsiModifierListOwner.isAnnotatedBy(annotations: Collection<String>, flags: Int = 0) =
        AnnotationUtil.isAnnotated(this, annotations, flags)

    fun PsiModifierListOwner.isAnnotatedBy(annotation: String, flags: Int = 0) =
        AnnotationUtil.isAnnotated(this, annotation, flags)

    fun PsiModifierListOwner.isMetaAnnotatedBy(annotations: Collection<String>) =
        MetaAnnotationUtil.isMetaAnnotated(this, annotations)

    fun PsiModifierListOwner.isMetaAnnotatedBy(annotation: String) =
        MetaAnnotationUtil.isMetaAnnotated(this, listOf(annotation))

    fun PsiAnnotation.isMetaAnnotatedByOrSelf(annotationNames: Collection<String>) =
        qualifiedName in annotationNames || resolveAnnotationType()?.isMetaAnnotatedBy(annotationNames) ?: false

    fun PsiAnnotation.isMetaAnnotatedByOrSelf(annotation: String) =
        qualifiedName == annotation || resolveAnnotationType()?.isMetaAnnotatedBy(annotation) ?: false

    fun PsiModifierListOwner.getMetaAnnotation(annotation: String): PsiAnnotation? =
        this.annotations.firstOrNull {
            it.isMetaAnnotatedByOrSelf(annotation)
        }

    fun PsiModifierListOwner.getMetaAnnotation(annotationNames: Collection<String>): PsiAnnotation? =
        this.annotations.firstOrNull {
            it.isMetaAnnotatedByOrSelf(annotationNames)
        }

    fun PsiModifierListOwner.getAnnotation(annotationNames: Collection<String>): PsiAnnotation? =
        this.annotations.firstOrNull {
            it.qualifiedName in annotationNames || it.resolveAnnotationType()?.isAnnotatedBy(annotationNames) ?: false
        }

    fun PsiMember.inClassAnnotatedBy(annotation: String, flags: Int = 0) =
        this.containingClass?.let {
            it.qualifiedName != null && it.isAnnotatedBy(annotation, flags)
        } ?: false

    fun PsiMember.inClassMetaAnnotatedBy(annotation: String) =
        this.containingClass?.let {
            it.qualifiedName != null && it.isMetaAnnotatedBy(annotation)
        } ?: false

    fun fitsForReference(method: PsiMethod) =
        !method.hasParameters()
                && !method.isConstructor
                && (method.containingFile?.name !in setOf("Annotation.class", "Object.class"))

    fun PsiClass.isEqualOrInheritor(baseClass: PsiClass, checkDeep: Boolean = true): Boolean {
        return this.qualifiedName == baseClass.qualifiedName || this.isInheritor(baseClass, checkDeep)
    }

    fun PsiClassType.isEqualOrInheritor(baseType: PsiClassType): Boolean {
        return baseType.resolve()?.qualifiedName?.let { className -> this.isInheritorOf(className) } ?: false
    }

    fun PsiClass.isGeneric(psiType: PsiType): Boolean {
        return psiType is PsiClassType
                && this.typeParameters.isNotEmpty()
                && (psiType.parameterCount == this.typeParameters.size)
                && this.typeParameters.all { it.references.isEmpty() }
    }

    fun PsiElement.getHighlightRange(): TextRange = textRangeInParent.shiftLeft(textRangeInParent.startOffset)

    inline fun <reified T : PsiElement> PsiElement.findChildrenOfType(): List<T> {
        return PsiTreeUtil.findChildrenOfType(this, T::class.java).toList()
    }

    val PsiClass.isOrdinaryClass: Boolean
        get() = !isInterface && !isEnum && !isAnnotationType

    val PsiType.resolvedPsiClass: PsiClass?
        get() = psiClassType?.resolve()

    val PsiType.psiClassType: PsiClassType?
        get() = this as? PsiClassType

    val PsiType.resolvedDeepPsiClass: PsiClass?
        get() = deepPsiClassType?.resolve()

    val PsiType.deepPsiClassType: PsiClassType?
        get() = this.deepComponentType as? PsiClassType

    val PsiType.isCollection: Boolean
        get() = this.isInheritorOf(java.util.Collection::class.java.canonicalName)

    val PsiType.isInterface: Boolean
        get() = resolvedPsiClass?.isInterface ?: false

    val PsiType.isMap: Boolean
        get() = this.isInheritorOf(java.util.Map::class.java.canonicalName)

    val PsiType.isOptional: Boolean
        get() = this.isInheritorOf(java.util.Optional::class.java.canonicalName)

    val PsiType.isString: Boolean
        get() = this.isInheritorOf(java.lang.String::class.java.canonicalName)

    val PsiMember.returnPsiType: PsiType?
        get() = if (this is PsiMethod) {
            returnType
        } else {
            this.childrenOfType<PsiTypeElement>().firstOrNull()?.type
        }

    val PsiMember.returnPsiClass: PsiClass?
        get() = returnPsiType?.resolvedPsiClass

    val PsiMember.returnPsiClassType: PsiClassType?
        get() = returnPsiType?.psiClassType

    fun PsiElement?.toSourcePsi(): PsiElement? =
        this?.let { it.toUElement()?.sourcePsi }

    @OptIn(ExperimentalContracts::class)
    fun isSetter(psiMethod: PsiMethod?): Boolean {
        contract { returns(true) implies (psiMethod != null) }

        return psiMethod?.let {
            isOpen(it)
                    && PropertyUtilBase.isSimplePropertySetter(it)
                    && it.returnPsiClass == null
        } ?: false
    }

    @OptIn(ExperimentalContracts::class)
    fun isGetter(psiMethod: PsiMethod?): Boolean {
        contract { returns(true) implies (psiMethod != null) }

        return psiMethod?.let {
            isOpen(it) &&
                    PropertyUtilBase.isSimplePropertyGetter(it)
        } == true
    }

    @OptIn(ExperimentalContracts::class)
    fun isOpen(psiMember: PsiMember?): Boolean {
        contract { returns(true) implies (psiMember != null) }

        return psiMember != null
                && psiMember.isNonStatic
                && psiMember.isNonAbstract
                && psiMember.isNonPrivate
    }

}