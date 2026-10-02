/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.JacksonClasses
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.util.PropertyUtilBase
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * The response schema of an endpoint, as Jackson writes the value to the wire rather than as the class stores it.
 *
 * The two differ where a client generated from the schema would go wrong:
 * - an enum is written as a string, not as an object of its `name`/`ordinal` fields - and as the value of its
 *   `@JsonValue` member when it declares one, a value computed in code that cannot be listed;
 * - a `transient` or `@JsonIgnore` member is not written at all, and a field declared by a `java.*` class is the
 *   JDK's own state;
 * - a `@JsonProperty("order_id")` member is written under that name.
 */
internal object ResponseSchemaReader {

    fun schemaOf(type: PsiType, depth: Int): DtoSchemaJson? {
        if (depth <= 0) return null
        val resolved = (type as? PsiClassType)?.resolve() ?: return null
        val fqn = resolved.qualifiedName ?: return null

        if (WRAPPER_PACKAGES.any(fqn::startsWith)) {
            return type.parameters.firstOrNull()?.let { schemaOf(it, depth) }
        }
        if (resolved.isEnum) return enumSchemaOf(resolved, fqn)

        val fields = fieldsOf(resolved, depth).ifEmpty { propertiesOf(resolved, depth) }
        return DtoSchemaJson(className = fqn, fields = fields)
    }

    private fun enumSchemaOf(enum: PsiClass, fqn: String): DtoSchemaJson {
        val constants = enum.fields.filterIsInstance<PsiEnumConstant>()
        jsonValueOf(enum)?.let { (member, valueType) ->
            return DtoSchemaJson(
                className = fqn,
                fields = null,
                jsonValue = member,
                valueType = valueType,
                enumConstants = constants.map { it.name },
            )
        }
        val values = constants.map { constant ->
            renamedTo(annotationOf(constant, JacksonClasses.JSON_PROPERTY)) ?: constant.name
        }
        return DtoSchemaJson(className = fqn, fields = null, enumValues = values)
    }

    /** The member whose value Jackson writes instead of the constant, and its type. */
    private fun jsonValueOf(enum: PsiClass): Pair<String, String>? {
        val method = enum.methods.firstOrNull { method ->
            !method.hasModifierProperty(PsiModifier.STATIC) && method.parameterList.isEmpty &&
                    isEnabled(method.getAnnotation(JacksonClasses.JSON_VALUE))
        }
        if (method != null) {
            val type = method.returnType ?: return null
            return sourceNameOf(method) to renderDtoType(type, kotlinOriginOf(method))
        }
        val field = enum.fields.firstOrNull { field ->
            field !is PsiEnumConstant && !field.hasModifierProperty(PsiModifier.STATIC) &&
                    isEnabled(field.getAnnotation(JacksonClasses.JSON_VALUE))
        } ?: return null
        return field.name to renderDtoType(field.type, kotlinOriginOf(field))
    }

    private fun fieldsOf(psiClass: PsiClass, depth: Int): List<DtoFieldJson> =
        psiClass.allFields.filter(::isWrittenField).mapNotNull { field ->
            if (isIgnored(field)) return@mapNotNull null
            val type = field.type
            fieldJson(
                declaredName = field.name,
                wireName = renamedTo(annotationOf(field, JacksonClasses.JSON_PROPERTY)),
                type = type,
                origin = kotlinOriginOf(field),
                nullable = type is PsiPrimitiveType && type == PsiTypes.nullType()
                        || field.annotations.any { it.qualifiedName?.contains("Nullable") == true },
                depth = depth,
            )
        }

    /** Kotlin data class properties read through their getters, for a class that exposes no Java fields. */
    private fun propertiesOf(psiClass: PsiClass, depth: Int): List<DtoFieldJson> =
        psiClass.allMethods.mapNotNull { method ->
            if (method.hasModifierProperty(PsiModifier.STATIC)) return@mapNotNull null
            if (!method.name.startsWith("get") && !method.name.startsWith("is")) return@mapNotNull null
            if (method.parameterList.parametersCount != 0) return@mapNotNull null
            if (isJdkMember(method)) return@mapNotNull null
            if (isEnabled(method.getAnnotation(JacksonClasses.JSON_IGNORE))) return@mapNotNull null
            val type = method.returnType ?: return@mapNotNull null
            fieldJson(
                declaredName = method.name.removePrefix("get").removePrefix("is").replaceFirstChar { it.lowercase() },
                wireName = renamedTo(method.getAnnotation(JacksonClasses.JSON_PROPERTY)),
                type = type,
                origin = kotlinOriginOf(method),
                nullable = false,
                depth = depth,
            )
        }

    private fun fieldJson(
        declaredName: String,
        wireName: String?,
        type: PsiType,
        origin: KtCallableDeclaration?,
        nullable: Boolean,
        depth: Int,
    ) = DtoFieldJson(
        name = wireName ?: declaredName,
        declaredName = declaredName.takeIf { wireName != null && wireName != declaredName },
        type = renderDtoType(type, origin),
        nullable = nullable,
        nested = schemaOf(type, depth - 1),
    )

    private fun isWrittenField(field: PsiField): Boolean =
        field !is PsiEnumConstant &&
                !field.hasModifierProperty(PsiModifier.STATIC) &&
                !field.hasModifierProperty(PsiModifier.TRANSIENT) &&
                !isJdkMember(field)

    private fun isJdkMember(member: PsiMember): Boolean =
        member.containingClass?.qualifiedName?.startsWith(JDK_PACKAGE) == true

    private fun isIgnored(field: PsiField): Boolean = isEnabled(annotationOf(field, JacksonClasses.JSON_IGNORE))

    /**
     * A Jackson annotation of a property, wherever Kotlin or Java put it: on the field, on its getter - Kotlin's
     * `@get:` target - or on the constructor parameter of the same name, where Kotlin puts an annotation written on
     * a constructor `val` without a use-site target.
     */
    private fun annotationOf(field: PsiField, fqn: String): PsiAnnotation? =
        carriersOf(field).firstNotNullOfOrNull { it.getAnnotation(fqn) }

    private fun carriersOf(field: PsiField): Sequence<PsiModifierListOwner> = sequence {
        yield(field)
        val owner = field.containingClass ?: return@sequence
        PropertyUtilBase.findPropertyGetter(owner, field.name, false, false)?.let { yield(it) }
        owner.constructors.asSequence()
            .flatMap { it.parameterList.parameters.asSequence() }
            .filter { it.name == field.name }
            .forEach { yield(it) }
    }

    private fun renamedTo(annotation: PsiAnnotation?): String? =
        annotation?.let { AnnotationUtil.getStringAttributeValue(it, "value") }?.takeIf { it.isNotEmpty() }

    /** Present and not switched off: `@JsonIgnore(false)` and `@JsonValue(false)` cancel an inherited annotation. */
    private fun isEnabled(annotation: PsiAnnotation?): Boolean =
        annotation != null && AnnotationUtil.getBooleanAttributeValue(annotation, "value") != false

    private fun sourceNameOf(method: PsiMethod): String =
        ((method as? KtLightMethod)?.kotlinOrigin as? KtNamedDeclaration)?.name ?: method.name

    private fun kotlinOriginOf(member: PsiMember): KtCallableDeclaration? = when (member) {
        is KtLightField -> member.kotlinOrigin as? KtCallableDeclaration
        is KtLightMethod -> member.kotlinOrigin as? KtCallableDeclaration
        else -> null
    }

    private fun renderDtoType(type: PsiType, origin: KtCallableDeclaration?): String =
        renderDtoType(type, origin?.typeReference)

    private fun renderDtoType(type: PsiType, source: KtTypeReference?): String {
        val element = source?.typeElement ?: return type.canonicalText
        val typeArguments = (type as? PsiClassType)?.parameters.orEmpty()
        val sourceArguments = element.typeArgumentsAsTypes
        val name = if (typeArguments.isNotEmpty() && typeArguments.size == sourceArguments.size) {
            val arguments = typeArguments.indices.joinToString(",") {
                renderDtoType(typeArguments[it], sourceArguments[it])
            }
            "${type.canonicalText.substringBefore('<')}<$arguments>"
        } else {
            type.canonicalText
        }
        return name + if (element is KtNullableType) "?" else ""
    }

    private const val JDK_PACKAGE = "java."

    /** Containers and framework types: their first type argument is what the client receives. */
    private val WRAPPER_PACKAGES = listOf(JDK_PACKAGE, "kotlin.", "org.springframework.")
}
