/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringPropertiesInspection
import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.lang.properties.IProperty
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiPolyVariantReference
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue

abstract class ConditionalOnPropertyReferencesBoot35TestCase : ExplytBaseLightTestCase() {
    protected abstract val kotlinSource: Boolean

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_4_1_0)

    fun testBooleanValueResolvesToPropertiesAndYaml() = assertReference(booleanProperty, "value")
    fun testBooleanNameResolvesToPropertiesAndYaml() = assertReference(booleanProperty, "name")
    fun testBooleanPrefixedNameResolvesToPropertiesAndYaml() = assertReference(booleanProperty, "prefix")
    fun testPropertyValueResolvesToPropertiesAndYaml() = assertReference(property, "value")
    fun testPropertyNameResolvesToPropertiesAndYaml() = assertReference(property, "name")
    fun testPropertyPrefixedNameResolvesToPropertiesAndYaml() = assertReference(property, "prefix")

    fun testFirstBooleanContainerElementResolvesToPropertiesAndYaml() =
        assertContainerReference(true, "feature.enabled")

    fun testSecondBooleanContainerElementResolvesToPropertiesAndYaml() =
        assertContainerReference(true, "feature.disabled")

    fun testFirstPropertyContainerElementResolvesToPropertiesAndYaml() =
        assertContainerReference(false, "feature.enabled")

    fun testSecondPropertyContainerElementResolvesToPropertiesAndYaml() =
        assertContainerReference(false, "feature.disabled")

    fun testBooleanPrefixedNameCompletionOffersUnqualifiedKeys() = assertNameCompletion(booleanProperty)
    fun testPropertyPrefixedNameCompletionOffersUnqualifiedKeys() = assertNameCompletion(property)
    fun testBooleanPrefixCompletionOffersConfigurationPrefix() = assertPrefixCompletion(booleanProperty)
    fun testPropertyPrefixCompletionOffersConfigurationPrefix() = assertPrefixCompletion(property)

    fun testBooleanOnlyUsagePreventsUnresolvedPropertiesWarning() = assertUsedKeyIsNotReported(booleanProperty, false)
    fun testPropertyOnlyUsagePreventsUnresolvedPropertiesWarning() = assertUsedKeyIsNotReported(property, false)
    fun testBooleanOnlyUsagePreventsUnresolvedYamlWarning() = assertUsedKeyIsNotReported(booleanProperty, true)
    fun testPropertyOnlyUsagePreventsUnresolvedYamlWarning() = assertUsedKeyIsNotReported(property, true)

    fun testUnusedPropertiesKeyIsStillReported() = assertUnusedKeyIsReported(false)
    fun testUnusedYamlKeyIsStillReported() = assertUnusedKeyIsReported(true)

    private fun assertReference(annotation: String, attribute: String) {
        addPropertyFiles()
        val key = if (attribute == "prefix") "enabled" else "feature.enabled"
        val arguments = when (attribute) {
            "value" -> "\"$key\""
            "name" -> nameArgument(key)
            else -> "prefix = \"feature\", ${nameArgument(key)}"
        }
        val consumer = configureConsumer("@${annotation.substringAfterLast('.')}($arguments)")
        assertAnnotationsResolve(consumer, listOf(annotation))
        assertKeyReference(consumer, key, "feature.enabled")
    }

    private fun assertContainerReference(boolean: Boolean, key: String) {
        addPropertyFiles()
        val annotation = if (boolean) booleanProperty else property
        val container =
            if (boolean) SpringCoreClasses.CONDITIONAL_ON_BOOLEAN_PROPERTIES else SpringCoreClasses.CONDITIONAL_ON_PROPERTIES
        val shortName = annotation.substringAfterLast('.')
        val first = "$shortName(${nameArgument("feature.enabled")})"
        val second = "$shortName(${nameArgument("feature.disabled")})"
        val annotations =
            if (kotlinSource) "@$first\n@$second" else "@${container.substringAfterLast('.')}({@$first, @$second})"
        val consumer = configureConsumer(annotations)
        assertAnnotationsResolve(
            consumer,
            if (kotlinSource) listOf(annotation, annotation) else listOf(container, annotation, annotation)
        )
        assertKeyReference(consumer, key, key)
    }

    private fun assertNameCompletion(annotation: String) {
        addPropertyFiles()
        val consumer =
            configureConsumer("@${annotation.substringAfterLast('.')}(prefix = \"feature\", ${nameArgument("<caret>")})")
        assertAnnotationsResolve(consumer, listOf(annotation))
        myFixture.complete(CompletionType.BASIC)
        assertEquals(
            "Prefix-aware name completion must offer unqualified keys",
            setOf("enabled", "disabled"),
            myFixture.lookupElementStrings.orEmpty().toSet()
        )
    }

    private fun assertPrefixCompletion(annotation: String) {
        addConfigurationProperties()
        val consumer =
            configureConsumer("@${annotation.substringAfterLast('.')}(prefix = \"<caret>\", ${nameArgument("enabled")})")
        assertAnnotationsResolve(consumer, listOf(annotation))
        myFixture.complete(CompletionType.BASIC)
        assertTrue(
            "Prefix completion must offer feature: ${myFixture.lookupElementStrings}",
            "feature" in myFixture.lookupElementStrings.orEmpty()
        )
    }

    private fun assertUsedKeyIsNotReported(annotation: String, yaml: Boolean) {
        addConfigurationProperties()
        val consumer = configureConsumer("@${annotation.substringAfterLast('.')}(${nameArgument("feature.enabled")})")
        assertAnnotationsResolve(consumer, listOf(annotation))
        myFixture.enableInspections(SpringPropertiesInspection::class.java, SpringYamlInspection::class.java)
        myFixture.configureByText(
            if (yaml) "application.yaml" else "application.properties",
            if (yaml) "feature:\n  enabled: true" else "feature.enabled=true"
        )
        myFixture.testHighlighting(true, false, false)
    }

    private fun assertUnusedKeyIsReported(yaml: Boolean) {
        addConfigurationProperties()
        val consumer = configureConsumer("@${property.substringAfterLast('.')}(${nameArgument("feature.other")})")
        assertAnnotationsResolve(consumer, listOf(property))
        myFixture.enableInspections(SpringPropertiesInspection::class.java, SpringYamlInspection::class.java)
        val description = "Cannot resolve key property 'feature.enabled'"
        myFixture.configureByText(
            if (yaml) "application.yaml" else "application.properties",
            if (yaml) "feature:\n  <warning descr=\"$description\">enabled</warning>: true"
            else "<warning descr=\"$description\">feature.enabled</warning>=true"
        )
        myFixture.testHighlighting(true, false, false)
    }

    private fun addConfigurationProperties() {
        myFixture.addClass(
            """
            @org.springframework.context.annotation.Configuration
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "feature")
            public class FeatureProperties {
                private String database;
                public String getDatabase() { return database; }
                public void setDatabase(String database) { this.database = database; }
            }
            """.trimIndent()
        )
    }

    private fun addPropertyFiles() {
        myFixture.addFileToProject("application.properties", "feature.enabled=true\nfeature.disabled=false")
        myFixture.addFileToProject("application.yaml", "feature:\n  enabled: true\n  disabled: false")
    }

    private fun configureConsumer(annotations: String): PsiFile {
        val imports = listOf(
            property,
            booleanProperty,
            SpringCoreClasses.CONDITIONAL_ON_PROPERTIES,
            SpringCoreClasses.CONDITIONAL_ON_BOOLEAN_PROPERTIES
        )
            .joinToString("\n") { "import $it${if (kotlinSource) "" else ";"}" }
        return myFixture.configureByText(
            if (kotlinSource) "FeatureConsumer.kt" else "FeatureConsumer.java",
            "$imports\n$annotations\n${if (kotlinSource) "class FeatureConsumer" else "public class FeatureConsumer {}"}"
        )
    }

    private fun nameArgument(key: String): String = if (kotlinSource) "name = [\"$key\"]" else "name = \"$key\""

    private fun assertAnnotationsResolve(consumer: PsiFile, expected: List<String>) {
        val annotations = mutableListOf<String?>()
        val uFile = consumer.toUElementOfType<UFile>()
        assertNotNull("Consumer must have a UAST file", uFile)
        uFile!!.accept(object : AbstractUastVisitor() {
            override fun visitAnnotation(node: UAnnotation): Boolean {
                annotations += node.resolve()?.qualifiedName
                return false
            }
        })
        assertEquals("Every condition annotation must resolve to its Spring Boot class", expected, annotations)
    }

    private fun assertKeyReference(consumer: PsiFile, literal: String, expectedKey: String) {
        val offset = consumer.text.indexOf("\"$literal\"") + 1
        assertTrue("Consumer must contain the key literal $literal", offset > 0)
        val reference = consumer.findReferenceAt(offset) as? PsiPolyVariantReference
        assertNotNull("Property reference missing for $expectedKey", reference)
        val targets = reference!!.multiResolve(true).mapNotNull { it.element }
        assertEquals("Property reference must resolve to both configuration files", 2, targets.size)
        assertEquals(
            setOf("application.properties", "application.yaml"),
            targets.map { it.containingFile.name }.toSet()
        )
        val keys = targets.map {
            when (it) {
                is IProperty -> it.key
                is YAMLKeyValue -> YAMLUtil.getConfigFullName(it)
                else -> fail("Unexpected property reference target: ${it.javaClass.name}")
            }
        }
        assertEquals("Resolved property keys", listOf(expectedKey, expectedKey), keys)
    }

    private val property = SpringCoreClasses.CONDITIONAL_ON_PROPERTY
    private val booleanProperty = SpringCoreClasses.CONDITIONAL_ON_BOOLEAN_PROPERTY
}
