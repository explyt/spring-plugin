/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.service.ComposedBeanNameFixture
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.conditional.kotlin.KotlinConditionFixtures
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

abstract class ComposedConditionValuesTestCase : BeanConditionTestCase() {

    protected abstract val springMajor: Int

    private val kotlin: Boolean get() = fixtures is KotlinConditionFixtures

    private val conventionApplies: Boolean get() = springMajor <= 6

    override fun setUp() {
        super.setUp()
        addSource("Marker", "@org.springframework.stereotype.Component public class Marker {}", "@org.springframework.stereotype.Component class Marker")
        addSource("Absent", "public class Absent {}", "class Absent")
    }

    fun testMissingBeanAliasDefaultNamingPresentBeanIsInactive() {
        addAliasedTypes("MissingMarker", CONDITIONAL_ON_MISSING_BEAN, MARKER)
        val config = addConfiguration("AliasDefaultMissingConfig", "@MissingMarker")
        assertAliased(config, "MissingMarker", CONDITIONAL_ON_MISSING_BEAN, "types")

        assertInactive(config, "com.app.MissingMarker")
    }

    fun testMissingBeanAliasDefaultNamingAbsentBeanIsActive() {
        addAliasedTypes("MissingAbsent", CONDITIONAL_ON_MISSING_BEAN, ABSENT)
        val config = addConfiguration("AliasDefaultAbsentMissingConfig", "@MissingAbsent")
        assertAliased(config, "MissingAbsent", CONDITIONAL_ON_MISSING_BEAN, "types")

        assertActive(config)
    }

    fun testMissingBeanUseSiteAliasNamingAbsentBeanIsActive() {
        addAliasedTypes("MissingMarker", CONDITIONAL_ON_MISSING_BEAN, MARKER)
        val config = addConfiguration("UseSiteMissingConfig", usage("MissingMarker", "types", ABSENT))
        assertAliased(config, "MissingMarker", CONDITIONAL_ON_MISSING_BEAN, "types")

        assertActive(config)
    }

    fun testMissingBeanUseSiteAliasNamingPresentBeanIsInactive() {
        addAliasedTypes("MissingAbsent", CONDITIONAL_ON_MISSING_BEAN, ABSENT)
        val config = addConfiguration("UseSitePresentMissingConfig", usage("MissingAbsent", "types", MARKER))
        assertAliased(config, "MissingAbsent", CONDITIONAL_ON_MISSING_BEAN, "types")

        assertInactive(config, "com.app.MissingAbsent")
    }

    fun testMissingBeanMetaFixedTypeNamingPresentBeanIsInactive() {
        addFixedTypes("MissingMarkerFixed", CONDITIONAL_ON_MISSING_BEAN, MARKER)
        val config = addConfiguration("MetaFixedMissingConfig", "@MissingMarkerFixed")
        assertComposed(config, "MissingMarkerFixed", CONDITIONAL_ON_MISSING_BEAN)

        assertInactive(config, "com.app.MissingMarkerFixed")
    }

    fun testMissingBeanConventionTypeFollowsSpringMajor() {
        addConventionTypes("MissingConvention", CONDITIONAL_ON_MISSING_BEAN, MARKER)
        val config = addConfiguration("ConventionMissingConfig", usage("MissingConvention", "type", ABSENT))
        assertConvention(config, "MissingConvention", CONDITIONAL_ON_MISSING_BEAN, "type")

        if (conventionApplies) assertActive(config) else assertInactive(config, "com.app.MissingConvention")
    }

    fun testDirectMissingBeanNamingPresentBeanIsInactive() {
        val config = addConfiguration("DirectMissingConfig", usage("ConditionalOnMissingBean", "type", MARKER))
        assertAnnotatedBy(config, CONDITIONAL_ON_MISSING_BEAN)

        assertInactive(config, CONDITIONAL_ON_MISSING_BEAN)
    }

    fun testOnBeanAliasDefaultNamingAbsentBeanIsInactive() {
        addAliasedTypes("OnAbsent", CONDITIONAL_ON_BEAN, ABSENT)
        val config = addConfiguration("AliasDefaultOnBeanConfig", "@OnAbsent")
        assertAliased(config, "OnAbsent", CONDITIONAL_ON_BEAN, "types")

        assertInactive(config, "com.app.OnAbsent")
    }

    fun testOnBeanAliasDefaultNamingPresentBeanIsActive() {
        addAliasedTypes("OnMarker", CONDITIONAL_ON_BEAN, MARKER)
        val config = addConfiguration("AliasDefaultPresentOnBeanConfig", "@OnMarker")
        assertAliased(config, "OnMarker", CONDITIONAL_ON_BEAN, "types")

        assertActive(config)
    }

    fun testOnBeanUseSiteAliasNamingAbsentBeanIsInactive() {
        addAliasedTypes("OnMarker", CONDITIONAL_ON_BEAN, MARKER)
        val config = addConfiguration("UseSiteOnBeanConfig", usage("OnMarker", "types", ABSENT))
        assertAliased(config, "OnMarker", CONDITIONAL_ON_BEAN, "types")

        assertInactive(config, "com.app.OnMarker")
    }

    fun testOnBeanUseSiteAliasNamingPresentBeanIsActive() {
        addAliasedTypes("OnAbsent", CONDITIONAL_ON_BEAN, ABSENT)
        val config = addConfiguration("UseSitePresentOnBeanConfig", usage("OnAbsent", "types", MARKER))
        assertAliased(config, "OnAbsent", CONDITIONAL_ON_BEAN, "types")

        assertActive(config)
    }

    fun testOnBeanMetaFixedTypeNamingAbsentBeanIsInactive() {
        addFixedTypes("OnAbsentFixed", CONDITIONAL_ON_BEAN, ABSENT)
        val config = addConfiguration("MetaFixedOnBeanConfig", "@OnAbsentFixed")
        assertComposed(config, "OnAbsentFixed", CONDITIONAL_ON_BEAN)

        assertInactive(config, "com.app.OnAbsentFixed")
    }

    fun testOnBeanConventionTypeFollowsSpringMajor() {
        addConventionTypes("OnBeanConvention", CONDITIONAL_ON_BEAN, ABSENT)
        val config = addConfiguration("ConventionOnBeanConfig", usage("OnBeanConvention", "type", MARKER))
        assertConvention(config, "OnBeanConvention", CONDITIONAL_ON_BEAN, "type")

        if (conventionApplies) assertActive(config) else assertInactive(config, "com.app.OnBeanConvention")
    }

    fun testDirectOnBeanNamingAbsentBeanIsInactive() {
        val config = addConfiguration("DirectOnBeanConfig", usage("ConditionalOnBean", "type", ABSENT))
        assertAnnotatedBy(config, CONDITIONAL_ON_BEAN)

        assertInactive(config, CONDITIONAL_ON_BEAN)
    }

    fun testComposedPropertyAliasDefaultsMatchingValueIsActive() {
        addProperties("feature.composed=on")
        addFeatureFlag()
        val config = addConfiguration("FeatureFlagOnConfig", "@FeatureFlag")
        assertPropertyDefined("feature.composed")
        assertAliased(config, "FeatureFlag", CONDITIONAL_ON_PROPERTY, "flag")

        assertActive(config)
    }

    fun testComposedPropertyAliasDefaultHavingValueMismatchIsInactive() {
        addProperties("feature.composed=true")
        addFeatureFlag()
        val config = addConfiguration("FeatureFlagMismatchConfig", "@FeatureFlag")
        assertPropertyDefined("feature.composed")
        assertAliased(config, "FeatureFlag", CONDITIONAL_ON_PROPERTY, "expected")

        val evidence = assertInactive(config, "com.app.FeatureFlag")
        assertTrue("Detail '${evidence.detail}' must name the composed key", "feature.composed" in evidence.detail.orEmpty())
    }

    fun testComposedPropertyAliasDefaultMatchIfMissingIsActive() {
        addProperties("feature.other=on")
        addFeatureFlag()
        val config = addConfiguration("FeatureFlagMissingConfig", "@FeatureFlag")
        assertPropertyUndefined("feature.composed")
        assertAliased(config, "FeatureFlag", CONDITIONAL_ON_PROPERTY, "whenMissing")

        assertActive(config)
    }

    fun testDirectPropertyMismatchIsInactive() {
        addProperties("feature.direct=true")
        val config = addConfiguration(
            "DirectPropertyConfig",
            if (kotlin) "@ConditionalOnProperty(prefix = \"feature\", name = [\"direct\"], havingValue = \"on\")"
            else "@ConditionalOnProperty(prefix = \"feature\", name = \"direct\", havingValue = \"on\")"
        )
        assertPropertyDefined("feature.direct")
        assertAnnotatedBy(config, CONDITIONAL_ON_PROPERTY)

        assertInactive(config, CONDITIONAL_ON_PROPERTY)
    }

    fun testWebAliasDefaultReactiveInServletApplicationIsInactive() {
        addAliasedWebType("ReactiveByDefault", REACTIVE)
        val config = addConfiguration("AliasDefaultWebConfig", "@ReactiveByDefault")
        assertServletApplication()
        assertAliased(config, "ReactiveByDefault", CONDITIONAL_ON_WEB_APPLICATION, "kind")

        assertInactive(config, "com.app.ReactiveByDefault")
    }

    fun testWebUseSiteAliasServletInServletApplicationIsActive() {
        addAliasedWebType("ReactiveByDefault", REACTIVE)
        val config = addConfiguration("UseSiteWebConfig", "@ReactiveByDefault(kind = ${webType(SERVLET)})")
        assertServletApplication()
        assertAliased(config, "ReactiveByDefault", CONDITIONAL_ON_WEB_APPLICATION, "kind")

        assertActive(config)
    }

    fun testWebMetaFixedReactiveInServletApplicationIsInactive() {
        addSource(
            "ReactiveOnly",
            "@Retention(RetentionPolicy.RUNTIME) @ConditionalOnWebApplication(type = ${webType(REACTIVE)}) public @interface ReactiveOnly {}",
            "@Retention(AnnotationRetention.RUNTIME) @ConditionalOnWebApplication(type = ${webType(REACTIVE)}) annotation class ReactiveOnly"
        )
        val config = addConfiguration("MetaFixedWebConfig", "@ReactiveOnly")
        assertServletApplication()
        assertComposed(config, "ReactiveOnly", CONDITIONAL_ON_WEB_APPLICATION)

        assertInactive(config, "com.app.ReactiveOnly")
    }

    fun testWebConventionTypeFollowsSpringMajor() {
        addSource(
            "ServletWeb",
            "@Retention(RetentionPolicy.RUNTIME) @ConditionalOnWebApplication(type = ${webType(SERVLET)}) " +
                "public @interface ServletWeb { ConditionalOnWebApplication.Type type() default ${webType(SERVLET)}; }",
            "@Retention(AnnotationRetention.RUNTIME) @ConditionalOnWebApplication(type = ${webType(SERVLET)}) " +
                "annotation class ServletWeb(val type: ConditionalOnWebApplication.Type = ${webType(SERVLET)})"
        )
        val config = addConfiguration("ConventionWebConfig", "@ServletWeb(type = ${webType(REACTIVE)})")
        assertServletApplication()
        assertConvention(config, "ServletWeb", CONDITIONAL_ON_WEB_APPLICATION, "type")

        if (conventionApplies) assertInactive(config, "com.app.ServletWeb") else assertActive(config)
    }

    fun testDirectWebReactiveInServletApplicationIsInactive() {
        val config = addConfiguration("DirectReactiveWebConfig", "@ConditionalOnWebApplication(type = ${webType(REACTIVE)})")
        assertServletApplication()
        assertAnnotatedBy(config, CONDITIONAL_ON_WEB_APPLICATION)

        assertInactive(config, CONDITIONAL_ON_WEB_APPLICATION)
    }

    fun testDirectWebServletInServletApplicationIsActive() {
        val config = addConfiguration("DirectServletWebConfig", "@ConditionalOnWebApplication(type = ${webType(SERVLET)})")
        assertServletApplication()
        assertAnnotatedBy(config, CONDITIONAL_ON_WEB_APPLICATION)

        assertActive(config)
    }

    private fun addAliasedTypes(name: String, condition: String, defaultType: String) {
        val simple = condition.substringAfterLast('.')
        addSource(
            name,
            "@Retention(RetentionPolicy.RUNTIME) @$simple public @interface $name { " +
                "@AliasFor(annotation = $simple.class, attribute = \"type\") String[] types() default \"$defaultType\"; }",
            "@Retention(AnnotationRetention.RUNTIME) @$simple annotation class $name(" +
                "@get:AliasFor(annotation = $simple::class, attribute = \"type\") val types: Array<String> = [\"$defaultType\"])"
        )
    }

    private fun addFixedTypes(name: String, condition: String, fixedType: String) {
        val simple = condition.substringAfterLast('.')
        addSource(
            name,
            "@Retention(RetentionPolicy.RUNTIME) @$simple(type = \"$fixedType\") public @interface $name {}",
            "@Retention(AnnotationRetention.RUNTIME) @$simple(type = [\"$fixedType\"]) annotation class $name"
        )
    }

    private fun addConventionTypes(name: String, condition: String, metaType: String) {
        val simple = condition.substringAfterLast('.')
        addSource(
            name,
            "@Retention(RetentionPolicy.RUNTIME) @$simple(type = \"$metaType\") public @interface $name { String[] type() default {}; }",
            "@Retention(AnnotationRetention.RUNTIME) @$simple(type = [\"$metaType\"]) annotation class $name(val type: Array<String> = [])"
        )
    }

    private fun addAliasedWebType(name: String, defaultType: String) = addSource(
        name,
        "@Retention(RetentionPolicy.RUNTIME) @ConditionalOnWebApplication public @interface $name { " +
            "@AliasFor(annotation = ConditionalOnWebApplication.class, attribute = \"type\") " +
            "ConditionalOnWebApplication.Type kind() default ${webType(defaultType)}; }",
        "@Retention(AnnotationRetention.RUNTIME) @ConditionalOnWebApplication annotation class $name(" +
            "@get:AliasFor(annotation = ConditionalOnWebApplication::class, attribute = \"type\") " +
            "val kind: ConditionalOnWebApplication.Type = ${webType(defaultType)})"
    )

    private fun addFeatureFlag() = addSource(
        "FeatureFlag",
        "@Retention(RetentionPolicy.RUNTIME) @ConditionalOnProperty public @interface FeatureFlag { " +
            "@AliasFor(annotation = ConditionalOnProperty.class, attribute = \"prefix\") String group() default \"feature\"; " +
            "@AliasFor(annotation = ConditionalOnProperty.class, attribute = \"name\") String[] flag() default \"composed\"; " +
            "@AliasFor(annotation = ConditionalOnProperty.class, attribute = \"havingValue\") String expected() default \"on\"; " +
            "@AliasFor(annotation = ConditionalOnProperty.class, attribute = \"matchIfMissing\") boolean whenMissing() default true; }",
        "@Retention(AnnotationRetention.RUNTIME) @ConditionalOnProperty annotation class FeatureFlag(" +
            "@get:AliasFor(annotation = ConditionalOnProperty::class, attribute = \"prefix\") val group: String = \"feature\", " +
            "@get:AliasFor(annotation = ConditionalOnProperty::class, attribute = \"name\") val flag: Array<String> = [\"composed\"], " +
            "@get:AliasFor(annotation = ConditionalOnProperty::class, attribute = \"havingValue\") val expected: String = \"on\", " +
            "@get:AliasFor(annotation = ConditionalOnProperty::class, attribute = \"matchIfMissing\") val whenMissing: Boolean = true)"
    )

    private fun usage(annotation: String, attribute: String, type: String): String =
        if (kotlin) "@$annotation($attribute = [\"$type\"])" else "@$annotation($attribute = \"$type\")"

    private fun webType(type: String) = "ConditionalOnWebApplication.Type.$type"

    private fun addConfiguration(name: String, annotations: String): PsiClass {
        addSource(name, "@Configuration $annotations public class $name {}", "@Configuration $annotations class $name")
        return beanClass("com.app.$name")
    }

    private fun addSource(name: String, javaDeclaration: String, kotlinDeclaration: String) {
        val imports = listOf(
            "org.springframework.boot.autoconfigure.condition.*",
            "org.springframework.context.annotation.*",
            "org.springframework.core.annotation.AliasFor",
        )
        val source = if (kotlin) {
            "package com.app\n${imports.joinToString("\n") { "import $it" }}\n$kotlinDeclaration"
        } else {
            "package com.app;\n${(imports + "java.lang.annotation.*").joinToString("\n") { "import $it;" }}\n$javaDeclaration"
        }
        myFixture.addFileToProject("com/app/$name.${if (kotlin) "kt" else "java"}", source)
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    private fun assertComposed(config: PsiClass, annotation: String, condition: String) {
        ComposedBeanNameFixture.assertSpringCoreMajorVersion(myFixture, module, springMajor)
        assertAnnotatedBy(beanClass("com.app.$annotation"), condition)
        assertAnnotatedBy(config, "com.app.$annotation")
    }

    private fun assertAliased(config: PsiClass, annotation: String, condition: String, attribute: String) {
        assertComposed(config, annotation, condition)
        val alias = beanClass("com.app.$annotation").findMethodsByName(attribute, false).single()
            .getAnnotation(ALIAS_FOR)
        assertNotNull("Precondition: $annotation.$attribute declares @AliasFor", alias)
    }

    private fun assertConvention(config: PsiClass, annotation: String, condition: String, attribute: String) {
        assertComposed(config, annotation, condition)
        val method = beanClass("com.app.$annotation").findMethodsByName(attribute, false).single()
        assertNull("Precondition: $annotation.$attribute has no @AliasFor", method.getAnnotation(ALIAS_FOR))
    }

    private fun assertServletApplication() {
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        val facade = JavaPsiFacade.getInstance(project)
        assertNotNull("Precondition: servlet web classes are on the classpath", facade.findClass(SERVLET_MARKER, scope))
        assertNull("Precondition: reactive web classes are absent", facade.findClass(REACTIVE_MARKER, scope))
    }

    private fun verdictOf(config: PsiClass): ConditionVerdict? {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return SpringSearchService.getInstance(project).conditionVerdictOf(config, module)
    }

    private fun assertActive(config: PsiClass) {
        assertEquals("Verdict of ${config.qualifiedName}", ConditionVerdict.Active, verdictOf(config))
        assertTrue("${config.qualifiedName} must be active", config.qualifiedName in activeBeans())
    }

    private fun assertInactive(config: PsiClass, annotationFqn: String): ConditionEvidence {
        val verdict = verdictOf(config)
        assertInstanceOf(verdict, ConditionVerdict.Inactive::class.java)
        val evidence = (verdict as ConditionVerdict.Inactive).condition
        assertEquals(annotationFqn, evidence.annotationFqn)
        assertEquals(ConditionReason.NOT_MATCHED, evidence.reason)
        assertTrue("${config.qualifiedName} must be excluded", config.qualifiedName in excludedBeans())
        return evidence
    }

    private companion object {
        const val MARKER = "com.app.Marker"
        const val ABSENT = "com.app.Absent"
        const val SERVLET = "SERVLET"
        const val REACTIVE = "REACTIVE"
        const val ALIAS_FOR = "org.springframework.core.annotation.AliasFor"
        const val SERVLET_MARKER = "org.springframework.web.context.support.GenericWebApplicationContext"
        const val REACTIVE_MARKER = "org.springframework.web.reactive.HandlerResult"
        const val CONDITIONAL_ON_MISSING_BEAN = "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean"
        const val CONDITIONAL_ON_WEB_APPLICATION =
            "org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication"
    }
}
