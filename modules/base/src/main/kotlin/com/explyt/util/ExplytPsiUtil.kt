/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.util

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.codeInspection.isInheritorOf
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.pom.Navigatable
import com.intellij.psi.*
import com.intellij.psi.util.PropertyUtilBase
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.childrenOfType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object ExplytPsiUtil {
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
        qualifiedName in annotationNames || resolveUAnnotationType()?.isMetaAnnotatedBy(annotationNames) ?: false

    fun PsiAnnotation.isMetaAnnotatedByOrSelf(annotation: String) =
        qualifiedName == annotation || resolveUAnnotationType()?.isMetaAnnotatedBy(annotation) ?: false

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
            it.qualifiedName in annotationNames || it.resolveUAnnotationType()?.isAnnotatedBy(annotationNames) ?: false
        }

    fun PsiAnnotation.resolveUAnnotationType(): PsiClass? {
        val element = nameReferenceElement
        val declaration = element?.resolve().toUElement()?.javaPsi
        if (declaration !is PsiClass || !declaration.isAnnotationType) return null
        return declaration
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

    fun PsiClass.isEqualOrInheritor(fullyQualifiedName: String): Boolean {
        return qualifiedName == fullyQualifiedName
                || supers.any { it.isEqualOrInheritor(fullyQualifiedName) }
    }

    fun PsiClassType.isEqualOrInheritor(baseType: PsiClassType): Boolean {
        return baseType.resolve()?.qualifiedName?.let { className -> this.isInheritorOf(className) } ?: false
    }

    fun PsiArrayType.isEqualOrInheritor(baseType: PsiType): Boolean {
        return baseType.resolvedPsiClass?.qualifiedName
            ?.let { this.deepPsiClassType?.resolvedPsiClass?.isEqualOrInheritor(it) }
            ?: false
    }

    fun PsiClass.isGeneric(psiType: PsiType): Boolean {
        return psiType is PsiClassType
                && this.typeParameters.isNotEmpty()
                && (psiType.parameterCount == this.typeParameters.size)
                && this.typeParameters.all { it.references.isEmpty() }
    }

    fun PsiClass.allSupers(): Set<PsiClass> {
        val superPsiClass = this.supers.asSequence().flatMap { it.allSupers() }.toMutableSet()
        return superPsiClass + this
    }

    fun PsiClass.onlyAllSupers(): Set<PsiClass> {
        return this.supers.asSequence().flatMap { it.allSupers() }.toMutableSet()
    }

    fun PsiElement.toSmartPointer(): SmartPsiElementPointer<PsiElement> {
        return SmartPointerManager.getInstance(project).createSmartPsiElementPointer(this, this.containingFile)
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

    val PsiType.isList: Boolean
        get() = this.isInheritorOf(java.util.List::class.java.canonicalName)

    val PsiType.isInterface: Boolean
        get() = resolvedPsiClass?.isInterface ?: false

    val PsiType.isMap: Boolean
        get() = this.isInheritorOf(java.util.Map::class.java.canonicalName)

    val PsiType.isOptional: Boolean
        get() = this.isInheritorOf(java.util.Optional::class.java.canonicalName)

    val PsiType.isString: Boolean
        get() = this.isInheritorOf(java.lang.String::class.java.canonicalName)

    val PsiType.isObjectProvider: Boolean
        get() = this.isInheritorOf("org.springframework.beans.factory.ObjectProvider")

    val PsiType.isObject: Boolean
        get() {
            when (this) {
                is PsiWildcardType -> {
                    if (isExtends) {
                        return extendsBound.resolvedPsiClass?.qualifiedName == java.lang.Object::class.java.canonicalName
                    }
                    if (isSuper) {
                        return superBound.resolvedPsiClass?.qualifiedName == java.lang.Object::class.java.canonicalName
                    }
                }
            }
            return this.resolvedPsiClass == null
                    || this.resolvedPsiClass?.qualifiedName == java.lang.Object::class.java.canonicalName
        }

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

    fun PsiElement.getContainingMethod(): PsiMethod? {
        var context = context
        while (context != null) {
            @Suppress("LocalVariableName") val _context = context
            if (_context is PsiMethod) return _context
            context = _context.context
        }
        return null
    }

    fun PsiElement.getContainingConstructor(): PsiMethod? {
        val method = getContainingMethod()
        return if (method?.isConstructor == true) method else null
    }

    fun UMethod.containKotlinKeyword(ktModifierKeywordToken: KtModifierKeywordToken): Boolean {
        val ktNamedFunction = this.sourcePsi as? KtNamedFunction ?: return false
        return ktNamedFunction.modifierList?.getModifier(ktModifierKeywordToken) != null
    }

    fun navigate(psiElement: PsiElement?) {
        val navigatable = psiElement as? Navigatable ?: return

        ApplicationManager.getApplication().invokeLater {
            navigatable.navigate(false)
        }
    }

    fun getUnquotedText(psiElement: PsiElement): String {
        return StringUtil.unquoteString(ElementManipulators.getValueText(psiElement))
    }

}