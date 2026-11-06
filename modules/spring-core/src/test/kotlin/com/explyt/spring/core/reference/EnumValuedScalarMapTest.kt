/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.reference

import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.explyt.spring.core.completion.properties.PropertyType
import com.explyt.spring.core.properties.providers.ConfigKeyPsiElement
import com.explyt.spring.core.properties.references.PropertiesKeyMapValueReference
import com.explyt.spring.core.properties.references.YamlKeyMapValueReference
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference

abstract class EnumValuedScalarMapFixture : ExplytBaseLightTestCase() {
    protected abstract val language: String

    override fun getTestDataPath(): String = "testdata/$language/reference/properties"

    override val libraries = arrayOf(TestLibrary.springBoot_3_1_1, TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("EnumMapProperties.${if (language == "java") "java" else "kt"}")
    }

    fun testPropertiesDottedEnumValueKeyHasAtomicReference() {
        configureProperties("explyt.modes.team.<caret>name=enabled")
        assertReferenceText("team.name")
    }

    fun testResolvedEnumValueMapIsScalar() {
        configureProperties("explyt.modes.team.<caret>name=enabled")
        assertTrue("resolved enum value must classify as scalar", PropertyUtil.isScalarMap(property("modes")))
    }

    fun testDottedEnumValueKeyHasNoObjectMemberSuffix() {
        configureProperties("explyt.modes.team.<caret>name=enabled")
        assertEquals(
            "atomic enum-valued map suffix",
            "team.name" to "",
            PropertyUtil.getKeyValuePair("explyt.modes.team.name", property("modes"))
        )
    }

    fun testPropertiesDottedEnumValueKeyFallsBackToMapDeclaration() {
        configureProperties("explyt.modes.team.<caret>name=enabled")
        assertMember("src.EnumMapProperties", "setModes", "modes")
    }

    fun testYamlFlatEnumValueKeyHasAtomicReference() {
        configureYaml("explyt:\n  modes:\n    team.<caret>name: enabled")
        assertReferenceText("team.name")
    }

    fun testYamlFlatEnumValueKeyFallsBackToMapDeclaration() {
        configureYaml("explyt:\n  modes:\n    team.<caret>name: enabled")
        assertMember("src.EnumMapProperties", "setModes", "modes")
    }

    fun testYamlNestedEnumValueLeafFallsBackToMapDeclaration() {
        configureYaml("explyt:\n  modes:\n    team:\n      na<caret>me: enabled")
        assertReferenceText("name")
        assertMember("src.EnumMapProperties", "setModes", "modes")
    }

    fun testPropertiesBracketEnumValueKeyRemainsAtomic() {
        configureProperties("explyt.modes[team.<caret>name]=enabled")
        assertReferenceText("[team.name]")
        assertMember("src.EnumMapProperties", "setModes", "modes")
    }

    fun testYamlBracketEnumValueKeyRemainsAtomic() {
        configureYaml("explyt:\n  modes:\n    \"[team.<caret>name]\": enabled")
        assertReferenceText("\"[team.name]\"")
        assertMember("src.EnumMapProperties", "setModes", "modes")
    }

    fun testYamlBracketStringMapKeyRangeIncludesQuotes() {
        configureYaml("explyt:\n  labels:\n    \"[team.<caret>name]\": value")
        assertTrue(PropertyUtil.isScalarMap(property("labels")))
        assertReferenceText("\"[team.name]\"")
        assertMember("src.EnumMapProperties", "setLabels", "labels")
    }

    fun testPropertiesEnumValueKeyUsesScalarHighlight() {
        configureProperties("explyt.modes.team.<caret>name=enabled")
        mapReference()
        assertHighlighted("team.name")
    }

    fun testYamlFlatEnumValueKeyUsesScalarHighlight() {
        configureYaml("explyt:\n  modes:\n    team.<caret>name: enabled")
        mapReference()
        assertHighlighted("team.name")
    }

    fun testYamlNestedEnumValueKeyHighlightsBothSegments() {
        configureYaml("explyt:\n  modes:\n    team:\n      na<caret>me: enabled")
        mapReference()
        assertHighlighted("team", "name")
    }

    fun testPropertiesObjectMapStillNavigatesMember() {
        configureProperties("explyt.entries.team.na<caret>me=value")
        assertObjectFixture("entries", "src.EnumMapProperties.Entry")
        assertReferenceText("name")
        assertMember("src.EnumMapProperties.Entry", "setName", "name")
        assertHighlighted()
    }

    fun testYamlObjectMapStillNavigatesMember() {
        configureYaml("explyt:\n  entries:\n    team:\n      na<caret>me: value")
        assertObjectFixture("entries", "src.EnumMapProperties.Entry")
        assertReferenceText("name")
        assertMember("src.EnumMapProperties.Entry", "setName", "name")
        assertHighlighted()
    }

    fun testPropertiesNonScalarMapControlsHaveNoScalarHighlight() {
        configureProperties("explyt.labels.team.<caret>name=value\nexplyt.entries.team.name=value\nexplyt.groups.team.name=enabled\nexplyt.objects.team.name=value")
        assertNonScalarFixtures()
        mapReference()
        assertHighlighted("team.name")
    }

    fun testYamlNonScalarMapControlsHaveNoScalarHighlight() {
        configureYaml("explyt:\n  labels:\n    team.<caret>name: value\n  entries:\n    team:\n      name: value\n  groups:\n    team:\n      name: enabled\n  objects:\n    team:\n      name: value")
        assertNonScalarFixtures()
        mapReference()
        assertHighlighted("team.name")
    }

    fun testPropertiesEnumKeysRemainEnumMapWithoutScalarHighlight() {
        configureProperties("explyt.enum-keys.ENA<caret>BLED=value\nexplyt.labels.team.name=value")
        assertEnumKeyFixture()
        assertReferenceText("ENABLED")
        assertMember("src.EnumMapProperties.Mode", "ENABLED")
        assertHighlighted("team.name")
    }

    fun testYamlEnumKeysRemainEnumMapWithoutScalarHighlight() {
        configureYaml("explyt:\n  enum-keys:\n    ENA<caret>BLED: value\n  labels:\n    team.name: value")
        assertEnumKeyFixture()
        assertReferenceText("ENABLED")
        assertMember("src.EnumMapProperties.Mode", "ENABLED")
        assertHighlighted("team.name")
    }

    fun testPropertiesEnumValueMapDoesNotCompleteEnumObjectMembers() {
        configureProperties("explyt.modes.team.<caret>")
        assertNoObjectCompletion("explyt.modes.team.")
    }

    fun testYamlEnumValueMapDoesNotCompleteEnumObjectMembers() {
        configureYaml("explyt:\n  modes:\n    team:\n      l<caret>: enabled")
        assertNoObjectCompletion("explyt:\n  modes:\n    team:\n      l: enabled")
    }

    fun testPropertiesObjectMapCompletionStillOffersMembers() {
        configureProperties("explyt.entries.team.<caret>")
        assertObjectFixture("entries", "src.EnumMapProperties.Entry")
        assertCompletion("explyt.entries.team.name", "explyt.entries.team.namespace", "explyt.entries.team.active")
    }

    fun testYamlObjectMapCompletionStillOffersMembers() {
        configureYaml("explyt:\n  entries:\n    team:\n      n<caret>: value")
        assertObjectFixture("entries", "src.EnumMapProperties.Entry")
        assertCompletion("explyt.entries.team.name", "explyt.entries.team.namespace")
    }

    fun testEnumKeyCompletionStillOffersConstants() {
        configureProperties("explyt.enum-keys.<caret>")
        assertEnumKeyFixture()
        assertCompletion("explyt.enum-keys.ENABLED", "explyt.enum-keys.DISABLED")
    }

    private fun configureProperties(text: String) {
        myFixture.configureByText("application.properties", text)
        assertEnumValueFixture()
    }

    private fun configureYaml(text: String) {
        myFixture.configureByText("application.yaml", text)
        assertEnumValueFixture()
    }

    private fun assertEnumValueFixture() {
        val type = mapType("modes")
        assertEquals("java.lang.String", resolved(type.parameters[0] as PsiClassType).qualifiedName)
        val value = resolved(type.parameters[1] as PsiClassType)
        assertEquals("src.EnumMapProperties.Mode", value.qualifiedName)
        assertTrue("fixture value resolves to a module enum", value.isEnum)
        assertEquals(PropertyType.MAP, property("modes").propertyType)
    }

    private fun property(member: String): ConfigurationProperty {
        val property = PropertyUtil.configurationProperty(module, "explyt.$member")
        assertNotNull("source property producer must apply to explyt.$member", property)
        assertEquals("explyt.$member", property!!.name)
        assertEquals("src.EnumMapProperties", property.sourceType?.substringBefore('#'))
        return property
    }

    private fun mapType(member: String): PsiClassType {
        val type = myFixture.findClass("src.EnumMapProperties")
            .findMethodsByName("get${member.replaceFirstChar { it.uppercase() }}", false)
            .single().returnType as PsiClassType
        assertEquals("java.util.Map", resolved(type).qualifiedName)
        assertEquals(2, type.parameters.size)
        return type
    }

    private fun resolved(type: PsiClassType): PsiClass {
        val resolved = type.resolve()
        assertNotNull("fixture type must resolve: ${type.canonicalText}", resolved)
        assertEquals(
            resolved,
            myFixture.javaFacade.findClass(
                resolved!!.qualifiedName!!,
                module.getModuleWithDependenciesAndLibrariesScope(false)
            )
        )
        return resolved
    }

    private fun assertObjectFixture(member: String, valueType: String) {
        val type = mapType(member)
        assertEquals("java.lang.String", resolved(type.parameters[0] as PsiClassType).qualifiedName)
        assertEquals(valueType, resolved(type.parameters[1] as PsiClassType).qualifiedName)
        assertEquals(PropertyType.MAP, property(member).propertyType)
        assertFalse("$member must not classify as scalar", PropertyUtil.isScalarMap(property(member)))
        assertEquals("team" to "name", PropertyUtil.getKeyValuePair("explyt.$member.team.name", property(member)))
    }

    private fun assertNonScalarFixtures() {
        assertObjectFixture("entries", "src.EnumMapProperties.Entry")
        assertObjectFixture("objects", "java.lang.Object")
        assertObjectFixture("groups", "java.util.Map")
        val nested = mapType("groups").parameters[1] as PsiClassType
        assertEquals(2, nested.parameters.size)
        assertEquals("java.lang.String", resolved(nested.parameters[0] as PsiClassType).qualifiedName)
        assertTrue(resolved(nested.parameters[1] as PsiClassType).isEnum)
        assertEquals("java.lang.String", resolved(mapType("labels").parameters[1] as PsiClassType).qualifiedName)
        assertTrue(PropertyUtil.isScalarMap(property("labels")))
    }

    private fun assertEnumKeyFixture() {
        val type = mapType("enumKeys")
        assertTrue("contrast key really is an enum", resolved(type.parameters[0] as PsiClassType).isEnum)
        assertEquals("java.lang.String", resolved(type.parameters[1] as PsiClassType).qualifiedName)
        assertEquals(
            "source enum-key classification, not a scalar value map",
            PropertyType.ENUM_MAP,
            property("enum-keys").propertyType
        )
        assertFalse(PropertyUtil.isScalarMap(property("enum-keys")))
    }

    private fun assertReferenceText(expected: String) {
        val reference = mapReference()
        assertEquals(
            "whole enum-valued scalar map key",
            expected,
            reference.rangeInElement.substring(reference.element.text)
        )
    }

    private fun assertMember(owner: String, vararg names: String) {
        val results = mapReference().multiResolve(false).mapNotNull { it.element }
        assertEquals("one map navigation target", 1, results.size)
        val target = results.single()
        val member = (if (target is ConfigKeyPsiElement) target.parent else target) as? PsiMember
        assertNotNull("navigation target must be a member", member)
        assertEquals(owner, member!!.containingClass?.qualifiedName)
        assertTrue("expected map member ${names.toList()}, got ${member.name}", member.name in names)
    }

    private fun mapReference(): PsiPolyVariantReference {
        val found = file.findReferenceAt(myFixture.caretOffset)
        val references = when (found) {
            is PsiMultiReference -> found.references.toList()
            null -> emptyList()
            else -> listOf(found)
        }.filter { it is PropertiesKeyMapValueReference || it is YamlKeyMapValueReference }
        assertEquals("map reference producer must cover caret", 1, references.size)
        return references.single() as PsiPolyVariantReference
    }

    private fun assertHighlighted(vararg expected: String) {
        val ranges = myFixture.doHighlighting()
            .filter { it.forcedTextAttributesKey?.externalName == "EXPLYT_MAP_KEY_HIGHLIGHTER" }
            .map { TextRange(it.startOffset, it.endOffset) }
        assertEquals("scalar-map highlighting", expected.toList(), ranges.map { it.substring(file.text) })
    }

    private fun assertNoObjectCompletion(expectedText: String) {
        assertTrue("completion file gate must apply", SpringCoreUtil.isConfigurationPropertyFile(file))
        val element = file.findElementAt(myFixture.caretOffset - 1)!!
        assertEquals(module, ModuleUtilCore.findModuleForPsiElement(element))
        if (file.name.endsWith("yaml")) {
            assertNotNull(
                "YAML completion requires a real key/value node",
                PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java)
            )
        }
        val variants = myFixture.completeBasic().orEmpty().map { it.lookupString }
        assertEquals("scalar enum map must not offer enum object members", emptyList<String>(), variants)
        assertEquals("scalar enum map completion must not insert an object member", expectedText, file.text)
    }

    private fun assertCompletion(vararg expected: String) {
        val variants = myFixture.completeBasic().orEmpty().map { it.lookupString }.toSet()
        assertEquals("real map completion producer", expected.toSet(), variants)
    }
}

class JavaEnumValuedScalarMapTest : EnumValuedScalarMapFixture() {
    override val language = "java"
}

class KotlinEnumValuedScalarMapTest : EnumValuedScalarMapFixture() {
    override val language = "kotlin"
}
