/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.core.JavaEeClasses
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.util.ExplytPsiUtil.isCollection
import com.explyt.util.ExplytPsiUtil.isMap
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isObjectProvider
import com.explyt.util.ExplytPsiUtil.isOptional
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.AnnotationTargetUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.java.library.JavaLibraryUtil
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UField
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
    const val JAVA_NULLABLE = "JAVA_NULLABLE"

    const val KOTLIN_REFLECT_MISSING = "KOTLIN_REFLECT_MISSING"
    const val SPRING_VERSION_UNKNOWN = "SPRING_VERSION_UNKNOWN"
    const val AUTOWIRED_REQUIRED_NOT_CONSTANT = "AUTOWIRED_REQUIRED_NOT_CONSTANT"
    const val ELEMENT_TYPE_NOT_PROVEN = "ELEMENT_TYPE_NOT_PROVEN"
    const val NULLABLE_NOT_VISIBLE_TO_REFLECTION = "NULLABLE_NOT_VISIBLE_TO_REFLECTION"
    const val COLLECTION_FALLBACK_NOT_PROVEN = "COLLECTION_FALLBACK_NOT_PROVEN"

    private val CONTAINER_SHAPES = setOf(InjectionShape.OPTIONAL, InjectionShape.PROVIDER)

    private const val NULLABLE_SIMPLE_NAME = "Nullable"
    private const val RUNTIME_RETENTION = "RUNTIME"
    private val INJECTION_ANNOTATIONS = listOf(SpringCoreClasses.AUTOWIRED) + JavaEeClasses.INJECT.allFqns

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
        if (shape == InjectionShape.COLLECTION) {
            when (emptyCollectionFallback(variable)) {
                true -> return InjectionFacts(shape, required = false, hasDefault(variable), CONTAINER_TYPE, limitations)
                null -> {
                    limitations += COLLECTION_FALLBACK_NOT_PROVEN
                    return InjectionFacts(shape, null, hasDefault(variable), UNKNOWN_REQUIREDNESS, limitations)
                }

                false -> Unit
            }
        }

        kotlinParameterOf(variable)?.let {
            return kotlinFacts(variable, it.hasDefaultValue(), it.isNullable(), shape, limitations)
        }
        kotlinPropertyOf(variable)?.let { return kotlinFacts(variable, false, it.isNullable(), shape, limitations) }

        return javaNullableFacts(variable, shape, limitations)
            ?: InjectionFacts(shape, required = true, hasDefaultValue = false, JAVA_DECLARATION, limitations)
    }

    /**
     * Whether Spring hands this collection an empty instance when no bean matches.
     *
     * `ConstructorResolver` does so only for the arguments of the single candidate constructor or factory method;
     * a field or a setter with no candidate fails with `NoSuchBeanDefinitionException`. `null` when the candidate
     * count is decided by rules this model does not evaluate.
     */
    private fun emptyCollectionFallback(variable: PsiVariable): Boolean? {
        val method = (variable as? PsiParameter)?.declarationScope as? PsiMethod ?: return false
        val owner = method.containingClass ?: return null
        if (method.isMetaAnnotatedBy(SpringCoreClasses.BEAN)) {
            return trueOrUnknown(owner.findMethodsByName(method.name, false).sourceDeclarations().size == 1)
        }
        if (!method.isConstructor) return false
        if (owner.constructors.sourceDeclarations().size == 1) return true
        val injected = owner.constructors.filter { it.isMetaAnnotatedBy(INJECTION_ANNOTATIONS) }.sourceDeclarations()
        return trueOrUnknown(method.navigationElement in injected && injected.size == 1)
    }

    private fun trueOrUnknown(proven: Boolean): Boolean? = if (proven) true else null

    /** Kotlin publishes one light method per defaulted-parameter overload; the declarations behind them are counted. */
    private fun Array<PsiMethod>.sourceDeclarations(): Set<PsiElement> = mapTo(HashSet()) { it.navigationElement }

    private fun List<PsiMethod>.sourceDeclarations(): Set<PsiElement> = mapTo(HashSet()) { it.navigationElement }

    /**
     * Spring treats any annotation named `Nullable` on the declaration as optional, but it reads declarations
     * through reflection. An annotation retained only in the class file never reaches it, so it proves nothing;
     * one that cannot be resolved, or that targets only the type, may or may not be seen, so it is unknown.
     */
    private fun javaNullableFacts(
        variable: PsiVariable,
        shape: InjectionShape,
        limitations: MutableSet<String>
    ): InjectionFacts? {
        val annotation = variable.modifierList?.annotations
            ?.firstOrNull { it.qualifiedName?.substringAfterLast('.') == NULLABLE_SIMPLE_NAME }
            ?: return null
        val annotationClass = annotation.resolveAnnotationType()
        if (annotationClass != null && !annotationClass.isRetainedAtRuntime()) return null
        if (annotationClass == null || !annotation.appliesToDeclarationOf(variable)) {
            limitations += NULLABLE_NOT_VISIBLE_TO_REFLECTION
            return InjectionFacts(shape, null, hasDefaultValue = false, UNKNOWN_REQUIREDNESS, limitations)
        }
        return InjectionFacts(shape, required = false, hasDefaultValue = false, JAVA_NULLABLE, limitations)
    }

    private fun PsiClass.isRetainedAtRuntime(): Boolean {
        val retention = modifierList?.findAnnotation(CommonClassNames.JAVA_LANG_ANNOTATION_RETENTION)
            ?: return false
        return retention.findAttributeValue(null)?.text?.substringAfterLast('.') == RUNTIME_RETENTION
    }

    private fun PsiAnnotation.appliesToDeclarationOf(variable: PsiVariable): Boolean {
        val declarationTarget = when (variable) {
            is PsiField -> PsiAnnotation.TargetType.FIELD
            is PsiParameter -> PsiAnnotation.TargetType.PARAMETER
            else -> return false
        }
        return AnnotationTargetUtil.findAnnotationTarget(this, declarationTarget) == declarationTarget
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
        (type as? PsiClassType)?.parameters?.firstOrNull()?.elementType()

    private fun elementOfCollection(type: PsiType): PsiType? {
        if (type is PsiArrayType) return type.componentType
        val classType = type as? PsiClassType ?: return null
        if (classType.isMap) return classType.parameters.getOrNull(1)?.elementType()
        return firstArgument(classType)
    }

    /**
     * Kotlin publishes `List<Clock>` of an open type as `List<? extends Clock>`, so the upper bound is the element
     * type Spring resolves. A `? super` or unbounded wildcard names no element type at all.
     */
    private fun PsiType.elementType(): PsiType? = when (this) {
        is PsiWildcardType -> if (isExtends) extendsBound else null
        else -> this
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

    private fun autowiredOwner(variable: PsiVariable): PsiAnnotation? {
        variable.modifierList?.findAnnotation(SpringCoreClasses.AUTOWIRED)?.let { return it }
        val method = PsiTreeUtil.getParentOfType(variable, PsiMethod::class.java)
        return method?.modifierList?.findAnnotation(SpringCoreClasses.AUTOWIRED)
    }

    private fun kotlinFacts(
        variable: PsiVariable,
        hasDefault: Boolean,
        nullable: Boolean,
        shape: InjectionShape,
        limitations: MutableSet<String>
    ): InjectionFacts {
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

    private fun kotlinPropertyOf(variable: PsiVariable): KtProperty? =
        (variable.toUElementOfType<UField>()?.sourcePsi as? KtProperty)
            ?: (variable.navigationElement as? KtProperty)

    private fun KtParameter.isNullable(): Boolean = typeReference?.typeElement is KtNullableType

    private fun KtProperty.isNullable(): Boolean = typeReference?.typeElement is KtNullableType

    private fun hasDefault(variable: PsiVariable): Boolean =
        kotlinParameterOf(variable)?.hasDefaultValue() ?: false
}

const val KOTLIN_DEFAULT_SUPPORTED = "KOTLIN_DEFAULT_SUPPORTED"
