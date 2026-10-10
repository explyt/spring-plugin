/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.JacksonClasses
import com.explyt.spring.core.properties.FoldedPropertyValue
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassObjectAccessExpression
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
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PropertyUtilBase
import org.jetbrains.kotlin.asJava.classes.KtLightClass
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
 * - a `@JsonProperty("order_id")` member is written under that name;
 * - a Java bean is written through its getters, so a private field without one is not written and a field named
 *   `vets` behind `getVetList()` is written as `vetList`.
 *
 * Containers - `ResponseEntity`, `Optional`, a publisher, a collection - are described by their payload, their first
 * type argument. What counts as a container is decided by where the class is declared, never by its package alone:
 * a project class under `org.springframework.` is the project's own DTO.
 */
internal object ResponseSchemaReader {

    /**
     * The schema of [type]. [module] is the module of the handler; the naming strategy its configuration declares
     * applies to every class of the schema that does not declare its own `@JsonNaming`.
     */
    fun schemaOf(type: PsiType, depth: Int, module: Module? = null): DtoSchemaJson? =
        schemaOf(type, depth, module?.let(JacksonNaming::configuredFor))

    private fun schemaOf(type: PsiType, depth: Int, configured: JacksonNaming?): DtoSchemaJson? {
        if (depth <= 0) return null
        val resolved = (type as? PsiClassType)?.resolve() ?: return null
        val fqn = resolved.qualifiedName ?: return null

        if (isContainer(resolved, fqn)) {
            return type.parameters.firstOrNull()?.let { schemaOf(it, depth, configured) }
        }
        if (resolved.isEnum) return enumSchemaOf(resolved, fqn)

        val naming = JacksonNaming.declaredOn(resolved) ?: configured
        val names = Names(naming, configured)
        if (!ProjectSources.declares(resolved)) return librarySchemaOf(resolved, fqn, depth, names)
        val fields = if (isJavaBean(resolved)) {
            beanPropertiesOf(resolved, depth, names).ifEmpty { fieldsOf(resolved, depth, names) }
        } else {
            fieldsOf(resolved, depth, names).ifEmpty { propertiesOf(resolved, depth, names) }
        }
        val polymorphism = JacksonPolymorphism.of(resolved)
        return DtoSchemaJson(
            className = fqn,
            fields = fields,
            namingStrategy = naming?.name,
            namingStrategySource = naming?.source,
            polymorphic = polymorphism?.let { true },
            discriminator = polymorphism?.typeInfo?.let { DiscriminatorJson(it.use, it.include, it.property) },
            variants = polymorphism?.variants?.mapNotNull { variantSchemaOf(it, depth - 1, configured) }?.ifEmpty { null },
        )
    }

    private fun variantSchemaOf(variant: PolymorphicVariant, depth: Int, configured: JacksonNaming?): DtoSchemaJson? {
        val type = JavaPsiFacade.getElementFactory(variant.psiClass.project).createType(variant.psiClass)
        return schemaOf(type, depth, configured)?.copy(typeId = variant.typeId)
    }

    private fun librarySchemaOf(psiClass: PsiClass, fqn: String, depth: Int, names: Names): DtoSchemaJson {
        val omitted = omissionOf(psiClass)
        val anyGetter = ANY_GETTER_MIXINS[fqn]
        val fields = when {
            omitted != null -> emptyList()
            isJavaBean(psiClass) -> beanPropertiesOf(psiClass, depth, names)
            else -> fieldsOf(psiClass, depth, names).ifEmpty { propertiesOf(psiClass, depth, names) }
        }.filter { it.name != anyGetter }
        val reason = omitted ?: SchemaOmitted.NO_VISIBLE_PROPERTIES.takeIf { fields.isEmpty() }
        return DtoSchemaJson(
            className = fqn,
            fields = fields.takeIf { reason == null },
            namingStrategy = names.own?.name,
            namingStrategySource = names.own?.source,
            schemaOmitted = reason?.name,
            additionalProperties = true.takeIf { anyGetter != null && reason == null },
        )
    }

    private fun omissionOf(psiClass: PsiClass): SchemaOmitted? {
        fun inherits(fqns: Collection<String>) = fqns.any { InheritanceUtil.isInheritor(psiClass, it) }
        return when {
            inherits(LIBRARY_INFRASTRUCTURE) -> SchemaOmitted.LIBRARY_INFRASTRUCTURE
            inherits(JSON_TREES) -> SchemaOmitted.JSON_TREE
            inherits(SELF_SERIALIZING) || hasCustomSerialization(psiClass) -> SchemaOmitted.CUSTOM_SERIALIZATION
            inherits(MAPS) -> SchemaOmitted.MAP_TYPE
            inherits(COLLECTIONS) -> SchemaOmitted.COLLECTION_TYPE
            psiClass.isInterface || psiClass.hasModifierProperty(PsiModifier.ABSTRACT) -> SchemaOmitted.ABSTRACT_TYPE
            else -> null
        }
    }

    private fun hasCustomSerialization(psiClass: PsiClass): Boolean =
        CUSTOM_SERIALIZATION_ANNOTATIONS.any { psiClass.hasAnnotation(it) } ||
                psiClass.allMethods.any { isEnabled(it.getAnnotation(JacksonClasses.JSON_VALUE)) }

    /**
     * A class whose payload is its first type argument. A class declared in the project never is, whatever its
     * package. A library class is when it is a reactive or asynchronous container, or a generic class the JDK, the
     * Kotlin standard library or Spring declares - a collection or a map, `Optional`, `ResponseEntity`, a `Page`. A
     * JDK or Kotlin value type such as `String` or `Instant` has no schema of its own; a non-generic Spring class is
     * described as a bean.
     */
    private fun isContainer(psiClass: PsiClass, fqn: String): Boolean {
        if (ProjectSources.declares(psiClass)) return false
        if (fqn in CONTAINERS) return true
        if (VALUE_TYPE_PACKAGES.any(fqn::startsWith)) return true
        return fqn.startsWith(SPRING_PACKAGE) && psiClass.hasTypeParameters()
    }

    /**
     * A Java class Jackson writes through its accessors. A record keeps its components, which Java exposes as fields
     * and Jackson writes under the same names; a Kotlin class, an interface and an enum keep their own readers.
     */
    private fun isJavaBean(psiClass: PsiClass): Boolean =
        psiClass !is KtLightClass && !psiClass.isRecord && !psiClass.isInterface && !psiClass.isEnum

    /**
     * The properties of a Java bean as Jackson's default visibility writes them: every public getter - `getX`, or
     * `isX` returning `boolean` - and every public field, under the getter's property name. An annotation on the
     * getter or on the field behind it applies to the property.
     *
     * A class with no accessor at all is read by its fields, as before: its accessors are generated at compile time
     * by an annotation processor the IDE does not model, or its mapper is configured for field visibility.
     */
    private fun beanPropertiesOf(psiClass: PsiClass, depth: Int, names: Names): List<DtoFieldJson> {
        val getters = psiClass.allMethods.filter(::isWrittenGetter)
            .groupBy(::jacksonPropertyName)
            .mapValues { (_, overloads) -> overloads.first() }
        val properties = getters.mapNotNull { (name, getter) ->
            val field = psiClass.findFieldByName(name, true)?.takeIf(::isWrittenField)
            val carriers = listOfNotNull(getter, field)
            if (carriers.any { isEnabled(it.getAnnotation(JacksonClasses.JSON_IGNORE)) }) return@mapNotNull null
            val type = getter.returnType ?: return@mapNotNull null
            fieldJson(
                declaredName = name,
                wireName = carriers.firstNotNullOfOrNull { renamedTo(it.getAnnotation(JacksonClasses.JSON_PROPERTY)) },
                type = type,
                origin = null,
                nullable = nullabilityOf(type, carriers, null),
                depth = depth,
                names = names,
            )
        }
        val publicFields = psiClass.allFields
            .filter { it.hasModifierProperty(PsiModifier.PUBLIC) && isWrittenField(it) && it.name !in getters }
            .mapNotNull { field ->
                if (isIgnored(field)) return@mapNotNull null
                fieldJson(
                    declaredName = field.name,
                    wireName = renamedTo(field.getAnnotation(JacksonClasses.JSON_PROPERTY)),
                    type = field.type,
                    origin = null,
                    nullable = nullabilityOf(field.type, listOf(field), null),
                    depth = depth,
                    names = names,
                )
            }
        return properties + publicFields
    }

    private fun isWrittenGetter(method: PsiMethod): Boolean =
        method.hasModifierProperty(PsiModifier.PUBLIC) &&
                !method.hasModifierProperty(PsiModifier.STATIC) &&
                PropertyUtilBase.isSimplePropertyGetter(method) &&
                !isJdkMember(method)

    /**
     * The property name Jackson's default naming gives a getter: the prefix is dropped and the leading run of
     * capitals is lower-cased as a whole - `getVetList` is `vetList`, `getURL` is `url` - unlike the JavaBeans rule,
     * which keeps `URL`.
     */
    private fun jacksonPropertyName(getter: PsiMethod): String {
        val base = getter.name.removePrefix(if (getter.name.startsWith("is")) "is" else "get")
        val capitals = base.takeWhile { it.isUpperCase() }.length
        return base.take(capitals).lowercase() + base.drop(capitals)
    }

    /**
     * Whether `null` can be written for a property: `false` for a primitive, a Kotlin non-null type or a
     * `@NotNull`-family annotation; `true` for a Kotlin `T?` or a `@Nullable`-family annotation; `null` - unknown -
     * for an unannotated Java reference, which a client must not read as a guarantee either way.
     */
    private fun nullabilityOf(type: PsiType, carriers: List<PsiModifierListOwner>, origin: KtCallableDeclaration?): Boolean? {
        if (type is PsiPrimitiveType) return false
        origin?.typeReference?.typeElement?.let { return it is KtNullableType }
        val annotations = carriers.flatMap { it.annotations.mapNotNull { annotation -> annotation.qualifiedName?.substringAfterLast('.') } }
        if (annotations.any { it.contains("Nullable") }) return true
        if (annotations.any { it in NON_NULL_ANNOTATIONS }) return false
        return null
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

    private fun fieldsOf(psiClass: PsiClass, depth: Int, names: Names): List<DtoFieldJson> =
        psiClass.allFields.filter(::isWrittenField).mapNotNull { field ->
            if (isIgnored(field)) return@mapNotNull null
            val type = field.type
            fieldJson(
                declaredName = field.name,
                wireName = renamedTo(annotationOf(field, JacksonClasses.JSON_PROPERTY)),
                type = type,
                origin = kotlinOriginOf(field),
                nullable = nullabilityOf(type, carriersOf(field).toList(), kotlinOriginOf(field)),
                depth = depth,
                names = names,
            )
        }

    /** Kotlin data class properties read through their getters, for a class that exposes no Java fields. */
    private fun propertiesOf(psiClass: PsiClass, depth: Int, names: Names): List<DtoFieldJson> =
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
                nullable = nullabilityOf(type, listOf(method), kotlinOriginOf(method)),
                depth = depth,
                names = names,
            )
        }

    private fun fieldJson(
        declaredName: String,
        wireName: String?,
        type: PsiType,
        origin: KtCallableDeclaration?,
        nullable: Boolean?,
        depth: Int,
        names: Names,
    ): DtoFieldJson {
        val written = wireName ?: names.own?.translate(declaredName) ?: declaredName
        return DtoFieldJson(
            name = written,
            declaredName = declaredName.takeIf { written != declaredName },
            type = renderDtoType(type, origin),
            nullable = nullable,
            nested = schemaOf(type, depth - 1, names.configured),
        )
    }

    /** The naming that renames this class's properties, and the configured one its nested classes start from. */
    private class Names(val own: JacksonNaming?, val configured: JacksonNaming?)

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

    private val NON_NULL_ANNOTATIONS = setOf("NotNull", "NonNull", "Nonnull")

    private const val SPRING_PACKAGE = "org.springframework."

    /** Packages whose library classes are described by their first type argument, or by nothing without one. */
    private val VALUE_TYPE_PACKAGES = listOf(JDK_PACKAGE, "kotlin.")

    /** Spring types a handler returns to be rendered, streamed or bound rather than serialized as a bean. */
    private val LIBRARY_INFRASTRUCTURE = listOf(
        "org.springframework.web.servlet.ModelAndView",
        "org.springframework.web.servlet.View",
        "org.springframework.web.reactive.result.view.Rendering",
        "org.springframework.core.io.Resource",
        "org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody",
        "org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter",
        "org.springframework.ui.Model",
        "org.springframework.ui.ModelMap",
    )

    private val JSON_TREES = listOf("com.fasterxml.jackson.databind.JsonNode", "tools.jackson.databind.JsonNode")

    private val SELF_SERIALIZING = listOf(
        "com.fasterxml.jackson.databind.JsonSerializable",
        "tools.jackson.databind.JacksonSerializable",
    )

    private val CUSTOM_SERIALIZATION_ANNOTATIONS = listOf(
        "com.fasterxml.jackson.databind.annotation.JsonSerialize",
        "tools.jackson.databind.annotation.JsonSerialize",
        "com.fasterxml.jackson.annotation.JsonFormat",
    )

    private val MAPS = listOf("java.util.Map", "org.springframework.util.MultiValueMap")

    private val COLLECTIONS = listOf("java.lang.Iterable")

    /** Library classes a mixin registered by Spring's Jackson builders writes with a `@JsonAnyGetter` property. */
    private val ANY_GETTER_MIXINS = mapOf("org.springframework.http.ProblemDetail" to "properties")

    private enum class SchemaOmitted {
        LIBRARY_INFRASTRUCTURE,
        JSON_TREE,
        CUSTOM_SERIALIZATION,
        MAP_TYPE,
        COLLECTION_TYPE,
        ABSTRACT_TYPE,
        NO_VISIBLE_PROPERTIES,
    }

    /** Reactive and coroutine containers outside those packages, whose payload is their first type argument. */
    private val CONTAINERS = setOf(
        "reactor.core.publisher.Mono",
        "reactor.core.publisher.Flux",
        "kotlinx.coroutines.flow.Flow",
    )
}
