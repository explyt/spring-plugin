/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.JacksonClasses
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.DirectClassInheritorsSearch
import com.intellij.psi.util.ClassUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.toUElementOfType

internal data class Polymorphism(val typeInfo: JacksonTypeInfo?, val variants: List<PolymorphicVariant>)

internal data class JacksonTypeInfo(val use: String, val include: String?, val property: String?)

internal data class PolymorphicVariant(val psiClass: PsiClass, val typeId: String?)

internal object JacksonPolymorphism {

    fun of(psiClass: PsiClass): Polymorphism? {
        if (psiClass.isEnum || psiClass.isAnnotationType) return null
        val hierarchy = hierarchyOf(psiClass)
        val subtypes = (declaredSubtypesOf(psiClass, hierarchy) + sealedSubclassesOf(psiClass).map { DeclaredSubtype(it, null) })
            .distinctBy { it.psiClass.qualifiedName }
        if (!isAbstract(psiClass) && subtypes.isEmpty()) return null
        val typeInfo = typeInfoOf(hierarchy)
        val variants = subtypes.map { PolymorphicVariant(it.psiClass, typeInfo?.let { info -> typeIdOf(it, psiClass, info.use) }) }
        return Polymorphism(typeInfo, variants)
    }

    private data class DeclaredSubtype(val psiClass: PsiClass, val name: String?)

    private fun isAbstract(psiClass: PsiClass): Boolean =
        psiClass.isInterface || psiClass.hasModifierProperty(PsiModifier.ABSTRACT) || isKotlinSealed(psiClass)

    private fun isKotlinSealed(psiClass: PsiClass): Boolean =
        (psiClass as? KtLightClass)?.kotlinOrigin?.hasModifier(KtTokens.SEALED_KEYWORD) == true

    private fun hierarchyOf(psiClass: PsiClass): List<PsiClass> {
        val visited = linkedSetOf<PsiClass>()
        fun visit(type: PsiClass) {
            ProgressManager.checkCanceled()
            if (!visited.add(type)) return
            type.interfaces.forEach(::visit)
            if (!type.isInterface) type.superClass?.let(::visit)
        }
        visit(psiClass)
        return visited.toList()
    }

    private fun annotationIn(hierarchy: List<PsiClass>, fqn: String): UAnnotation? =
        hierarchy.firstOrNull { it.hasAnnotation(fqn) }?.toUElementOfType<UClass>()?.findAnnotation(fqn)

    private fun typeInfoOf(hierarchy: List<PsiClass>): JacksonTypeInfo? {
        val annotation = annotationIn(hierarchy, JacksonClasses.JSON_TYPE_INFO) ?: return null
        val use = annotation.enumAttribute("use")?.takeUnless { it == NONE } ?: return null
        if (use == DEDUCTION) return JacksonTypeInfo(use, include = null, property = null)
        val include = annotation.enumAttribute("include")
            ?.takeUnless { it == EXTERNAL_PROPERTY }
            ?: PROPERTY
        val property = (annotation.findDeclaredAttributeValue("property")?.evaluate() as? String)
            ?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_PROPERTIES[use]
        return JacksonTypeInfo(use, include, property.takeUnless { include in WRAPPERS })
    }

    private fun UAnnotation.enumAttribute(name: String): String? {
        val value = findDeclaredAttributeValue(name) ?: return null
        ((value as? UReferenceExpression)?.resolve() as? PsiEnumConstant)?.let { return it.name }
        return value.sourcePsi?.text?.substringAfterLast('.')?.takeIf { it.isNotBlank() }
    }

    private fun declaredSubtypesOf(base: PsiClass, hierarchy: List<PsiClass>): List<DeclaredSubtype> {
        val annotation = annotationIn(hierarchy, JacksonClasses.JSON_SUB_TYPES) ?: return emptyList()
        return subtypeEntriesOf(annotation).mapNotNull { type ->
            ProgressManager.checkCanceled()
            val literal = type.findDeclaredAttributeValue("value") as? UClassLiteralExpression
            val subclass = literal?.let(::classOf) ?: return@mapNotNull null
            val name = (type.findDeclaredAttributeValue("name")?.evaluate() as? String)?.takeIf { it.isNotEmpty() }
            DeclaredSubtype(subclass, name).takeIf { subclass.isInheritor(base, true) }
        }
    }

    private fun subtypeEntriesOf(annotation: UAnnotation): List<UAnnotation> {
        val fromSource = elementsOf(annotation.findDeclaredAttributeValue("value")).mapNotNull(::nestedAnnotationOf)
        if (fromSource.isNotEmpty()) return fromSource
        val declared = when (val value = annotation.javaPsi?.findDeclaredAttributeValue("value")) {
            is PsiAnnotation -> listOf(value)
            is PsiArrayInitializerMemberValue -> value.initializers.filterIsInstance<PsiAnnotation>()
            else -> emptyList()
        }
        return declared.mapNotNull { it.toUElementOfType<UAnnotation>() }
    }

    private fun nestedAnnotationOf(element: UExpression): UAnnotation? {
        if (element is UAnnotation) return element
        val source = element.sourcePsi ?: return null
        val call = (source as? KtQualifiedExpression)?.selectorExpression ?: source
        return call.toUElementOfType<UAnnotation>()
    }

    private fun classOf(literal: UClassLiteralExpression): PsiClass? =
        ((literal.expression as? UReferenceExpression)?.resolve() as? PsiClass)
            ?: (literal.type as? PsiClassType)?.resolve()

    private fun elementsOf(value: UExpression?): List<UExpression> = when {
        value == null -> emptyList()
        value is UCallExpression && value.kind == UastCallKind.NESTED_ARRAY_INITIALIZER -> value.valueArguments
        else -> listOf(value)
    }

    private fun sealedSubclassesOf(base: PsiClass): List<PsiClass> {
        if (!isKotlinSealed(base)) return emptyList()
        val scope = ModuleUtilCore.findModuleForPsiElement(base)?.moduleScope
            ?: GlobalSearchScope.projectScope(base.project)
        return DirectClassInheritorsSearch.search(base, scope).findAll()
            .filterNot { it is PsiAnonymousClass }
            .sortedWith(compareBy({ it.containingFile?.virtualFile?.path }, { it.textOffset }))
    }

    private fun typeIdOf(subtype: DeclaredSubtype, base: PsiClass, use: String): String? {
        val binaryName = binaryNameOf(subtype.psiClass) ?: return null
        val registered = subtype.name ?: typeNameOf(subtype.psiClass)
        return when (use) {
            NAME -> registered ?: binaryName.substringAfterLast('.')
            SIMPLE_NAME -> registered ?: binaryName.substring(maxOf(binaryName.lastIndexOf('.'), binaryName.lastIndexOf('$')) + 1)
            CLASS -> binaryName
            MINIMAL_CLASS -> minimalClassNameOf(binaryName, binaryNameOf(base))
            else -> null
        }
    }

    private fun minimalClassNameOf(binaryName: String, baseName: String?): String {
        val basePackage = baseName?.substringBeforeLast('.', "")?.takeIf { it.isNotEmpty() } ?: return binaryName
        return if (binaryName.startsWith("$basePackage.")) binaryName.removePrefix(basePackage) else binaryName
    }

    private fun binaryNameOf(psiClass: PsiClass): String? = ClassUtil.getJVMClassName(psiClass) ?: psiClass.qualifiedName

    private fun typeNameOf(psiClass: PsiClass): String? =
        psiClass.getAnnotation(JacksonClasses.JSON_TYPE_NAME)
            ?.let { AnnotationUtil.getStringAttributeValue(it, "value") }
            ?.takeIf { it.isNotEmpty() }

    private const val NONE = "NONE"
    private const val DEDUCTION = "DEDUCTION"
    private const val NAME = "NAME"
    private const val SIMPLE_NAME = "SIMPLE_NAME"
    private const val CLASS = "CLASS"
    private const val MINIMAL_CLASS = "MINIMAL_CLASS"
    private const val PROPERTY = "PROPERTY"
    private const val EXTERNAL_PROPERTY = "EXTERNAL_PROPERTY"

    private val WRAPPERS = setOf("WRAPPER_OBJECT", "WRAPPER_ARRAY")

    private val DEFAULT_PROPERTIES = mapOf(
        NAME to "@type",
        SIMPLE_NAME to "@type",
        CLASS to "@class",
        MINIMAL_CLASS to "@c",
    )
}
