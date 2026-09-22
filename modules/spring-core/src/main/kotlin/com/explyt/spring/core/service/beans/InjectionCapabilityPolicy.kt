/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.util.ExplytPsiUtil.isCollection
import com.explyt.util.ExplytPsiUtil.isMap
import com.explyt.util.ExplytPsiUtil.isObjectProvider
import com.explyt.util.ExplytPsiUtil.isOptional
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClassType

import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.java.library.JavaLibraryUtil
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.toUElementOfType

/**
 * What a declaration proves about a dependency, separately from whether a bean exists for it.
 *
 * Kotlin optionality is only claimed when the framework on the classpath would honour it: Spring reads a Kotlin
 * default or a nullable type through Kotlin reflection, so without `kotlin-reflect` - or with a Spring whose
 * behaviour here is not established - the fact is reported as unknown rather than as "not required". Stating
 * `required = false` on a classpath that would still fail at startup is worse than admitting ignorance.
 */
object InjectionCapabilityPolicy {

    const val JAVA_DECLARATION = "JAVA_DECLARATION"
    const val JAVA_REQUIRED_FALSE = "JAVA_REQUIRED_FALSE"
    const val CONTAINER_TYPE = "CONTAINER_TYPE"
    const val UNKNOWN_REQUIREDNESS = "UNKNOWN_REQUIREDNESS"
    const val KOTLIN_NULLABLE_SUPPORTED = "KOTLIN_NULLABLE_SUPPORTED"

    const val KOTLIN_REFLECT_MISSING = "KOTLIN_REFLECT_MISSING"
    const val SPRING_VERSION_UNKNOWN = "SPRING_VERSION_UNKNOWN"
    const val AUTOWIRED_REQUIRED_NOT_CONSTANT = "AUTOWIRED_REQUIRED_NOT_CONSTANT"
    const val ELEMENT_TYPE_NOT_PROVEN = "ELEMENT_TYPE_NOT_PROVEN"

    private val CONTAINER_SHAPES = setOf(InjectionShape.OPTIONAL, InjectionShape.COLLECTION, InjectionShape.PROVIDER)

    private const val SPRING_BEANS_MAVEN = "org.springframework:spring-beans"
    private const val KOTLIN_REFLECT_MARKER = "kotlin.reflect.full.KClasses"

    /** The lowest Spring line whose Kotlin default/nullable handling this policy was checked against. */
    private const val SUPPORTED_SPRING_MAJOR = 6

    fun inspect(variable: PsiVariable): InjectionFacts {
        val shape = shapeOf(variable.type)
        val limitations = linkedSetOf<String>()
        if (shape == InjectionShape.UNKNOWN) limitations += ELEMENT_TYPE_NOT_PROVEN

        val declaredRequired = declaredRequiredFalse(variable, limitations)
        if (declaredRequired != null) {
            return InjectionFacts(shape, required = false, hasDefault(variable), declaredRequired, limitations)
        }

        if (shape in CONTAINER_SHAPES) {
            return InjectionFacts(shape, required = false, hasDefault(variable), CONTAINER_TYPE, limitations)
        }

        val kotlinParameter = kotlinParameterOf(variable)
            ?: return InjectionFacts(shape, required = true, hasDefaultValue = false, JAVA_DECLARATION, limitations)

        return kotlinFacts(variable, kotlinParameter, shape, limitations)
    }

    /**
     * The element type a candidate search must match: `T` of a `List<T>`, of an `Optional<T>`, of a provider.
     * A container whose element type cannot be proven yields null rather than the container itself.
     */
    fun beanType(type: PsiType): PsiType? = when (shapeOf(type)) {
        InjectionShape.SINGLE -> type
        InjectionShape.OPTIONAL, InjectionShape.PROVIDER -> firstArgument(type)
        InjectionShape.COLLECTION -> elementOfCollection(type)
        InjectionShape.UNKNOWN -> null
    }


    private fun shapeOf(type: PsiType): InjectionShape {
        if (type is PsiArrayType) return InjectionShape.COLLECTION
        if (type !is PsiClassType) return InjectionShape.SINGLE
        if (type.isOptional) return if (firstArgument(type) != null) InjectionShape.OPTIONAL else InjectionShape.UNKNOWN
        if (type.isObjectProvider || type.isJsr330Provider() || type.isObjectFactory()) {
            return if (firstArgument(type) != null) InjectionShape.PROVIDER else InjectionShape.UNKNOWN
        }
        if (type.isMap) return if (isStringKeyed(type)) InjectionShape.COLLECTION else InjectionShape.UNKNOWN
        if (type.isCollection) {
            return if (firstArgument(type) != null) InjectionShape.COLLECTION else InjectionShape.UNKNOWN
        }
        return InjectionShape.SINGLE
    }

    private fun PsiClassType.isJsr330Provider(): Boolean {
        val fqn = resolvedPsiClass?.qualifiedName ?: return false
        return fqn == "javax.inject.Provider" || fqn == "jakarta.inject.Provider"
    }

    private fun PsiClassType.isObjectFactory(): Boolean =
        resolvedPsiClass?.qualifiedName == "org.springframework.beans.factory.ObjectFactory"

    private fun isStringKeyed(type: PsiClassType): Boolean {
        val parameters = type.parameters
        return parameters.size == 2 &&
                (parameters[0] as? PsiClassType)?.resolvedPsiClass?.qualifiedName == CommonClassNames.JAVA_LANG_STRING
    }

    private fun firstArgument(type: PsiType): PsiType? =
        (type as? PsiClassType)?.parameters?.firstOrNull()?.takeIf { it !is com.intellij.psi.PsiWildcardType }

    private fun elementOfCollection(type: PsiType): PsiType? {
        if (type is PsiArrayType) return type.componentType
        val classType = type as? PsiClassType ?: return null
        if (classType.isMap) return classType.parameters.getOrNull(1)
        return firstArgument(classType)
    }

    /** `@Autowired(required = false)` is only honoured when the attribute is a constant this model can read. */
    private fun declaredRequiredFalse(variable: PsiVariable, limitations: MutableSet<String>): String? {
        val owner = autowiredOwner(variable) ?: return null
        val attribute = owner.findAttributeValue("required") ?: return null
        val constant = JavaPsiFacade.getInstance(variable.project).constantEvaluationHelper
            .computeConstantExpression(attribute)
        if (constant == null) {
            limitations += AUTOWIRED_REQUIRED_NOT_CONSTANT
            return null
        }
        return if (constant == false) JAVA_REQUIRED_FALSE else null
    }

    private fun autowiredOwner(variable: PsiVariable): com.intellij.psi.PsiAnnotation? {
        variable.modifierList?.findAnnotation(SpringCoreClasses.AUTOWIRED)?.let { return it }
        val method = PsiTreeUtil.getParentOfType(variable, com.intellij.psi.PsiMethod::class.java)
        return method?.modifierList?.findAnnotation(SpringCoreClasses.AUTOWIRED)
    }

    private fun kotlinFacts(
        variable: PsiVariable,
        parameter: KtParameter,
        shape: InjectionShape,
        limitations: MutableSet<String>
    ): InjectionFacts {
        val hasDefault = parameter.hasDefaultValue()
        val nullable = parameter.typeReference?.typeElement is KtNullableType
        if (!hasDefault && !nullable) {
            return InjectionFacts(shape, required = true, hasDefaultValue = false, JAVA_DECLARATION, limitations)
        }

        val support = kotlinSupport(variable)
        if (support != null) {
            limitations += support
            return InjectionFacts(shape, null, hasDefault, UNKNOWN_REQUIREDNESS, limitations)
        }
        val basis = if (hasDefault) KOTLIN_DEFAULT_SUPPORTED else KOTLIN_NULLABLE_SUPPORTED
        return InjectionFacts(shape, false, hasDefault, basis, limitations)
    }

    /** Returns the limitation that blocks a confident answer, or null when the classpath supports it. */
    private fun kotlinSupport(variable: PsiVariable): String? {
        val module = ModuleUtilCore.findModuleForPsiElement(variable) ?: return SPRING_VERSION_UNKNOWN
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
        if (JavaPsiFacade.getInstance(variable.project).findClass(KOTLIN_REFLECT_MARKER, scope) == null) {
            return KOTLIN_REFLECT_MISSING
        }
        val version = JavaLibraryUtil.getLibraryVersion(module, SPRING_BEANS_MAVEN) ?: return SPRING_VERSION_UNKNOWN
        val major = version.substringBefore('.').toIntOrNull() ?: return SPRING_VERSION_UNKNOWN
        return if (major >= SUPPORTED_SPRING_MAJOR) null else SPRING_VERSION_UNKNOWN
    }

    private fun kotlinParameterOf(variable: PsiVariable): KtParameter? =
        (variable.toUElementOfType<UParameter>()?.sourcePsi as? KtParameter)
            ?: (variable.navigationElement as? KtParameter)

    private fun hasDefault(variable: PsiVariable): Boolean =
        kotlinParameterOf(variable)?.hasDefaultValue() ?: false
}

const val KOTLIN_DEFAULT_SUPPORTED = "KOTLIN_DEFAULT_SUPPORTED"
