/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.reference

import com.explyt.spring.core.completion.properties.ProjectConfigurationPropertiesLoader
import com.explyt.spring.core.completion.properties.PropertyType
import com.explyt.spring.core.properties.providers.ConfigKeyPsiElement
import com.explyt.spring.core.properties.references.PropertiesKeyMapValueReference
import com.explyt.spring.core.properties.references.YamlKeyMapValueReference
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference

class MetadataEnumMapTest : ExplytBaseLightTestCase() {
    override val libraries = arrayOf(TestLibrary.springBoot_3_1_1, TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.addClass("package explyt; public enum Mode { ENABLED, DISABLED }")
        myFixture.addClass("package explyt; public class Entry { public void setName(String name) {} }")
        myFixture.addClass("package explyt; public class Holder { public void setModes(java.util.Map<String, Mode> modes) {} }")
    }

    fun testMetadataOnlyPropertiesKeyAndFallback() = assertMetadataReference(false)

    fun testMetadataOnlyYamlKeyAndFallback() = assertMetadataReference(true)

    fun testMetadataMergeFillsMissingType() {
        metadata("""{"name":"explyt.modes","description":"first"},""" + enumProperty())
        val property = property()
        assertEquals("first", property.description)
        assertEquals("java.util.Map<java.lang.String,explyt.Mode>", property.type)
        assertTrue(property.hasEnumMapValue)
        assertTrue(PropertyUtil.isScalarMap(property))
        assertEquals("team.name" to "", PropertyUtil.getKeyValuePair("explyt.modes.team.name", property))
    }

    fun testMetadataMergeRetainsExistingEnumType() {
        metadata(enumProperty() + """,{"name":"explyt.modes","type":"java.util.Map<java.lang.String,explyt.Entry>","description":"second"}""")
        val property = property()
        assertEquals("second", property.description)
        assertEquals("java.util.Map<java.lang.String,explyt.Mode>", property.type)
        assertTrue(property.hasEnumMapValue)
        assertTrue(PropertyUtil.isScalarMap(property))
    }

    fun testMetadataNonScalarContrastsHaveRealTypes() {
        assertTrue(myFixture.findClass("explyt.Mode").isEnum)
        assertFalse(myFixture.findClass("explyt.Entry").isEnum)
        metadata(listOf(
            "explyt.entries" to "java.util.Map<java.lang.String,explyt.Entry>",
            "explyt.groups" to "java.util.Map<java.lang.String,java.util.Map<java.lang.String,explyt.Mode>>",
            "explyt.objects" to "java.util.Map<java.lang.String,java.lang.Object>",
            "explyt.enum-keys" to "java.util.Map<explyt.Mode,java.lang.String>"
        ).joinToString(",") { (name, type) -> """{"name":"$name","type":"$type"}""" })
        val properties = ProjectConfigurationPropertiesLoader(project).loadProperties(module).filter { it.name.startsWith("explyt.") }
        assertEquals(4, properties.size)
        properties.forEach {
            assertFalse(it.name, it.hasEnumMapValue)
            assertFalse(it.name, PropertyUtil.isScalarMap(it))
            assertEquals("team" to "name", PropertyUtil.getKeyValuePair("${it.name}.team.name", it))
        }
        assertEquals(PropertyType.ENUM_MAP, properties.single { it.name == "explyt.enum-keys" }.propertyType)
    }

    private fun assertMetadataReference(yaml: Boolean) {
        metadata(enumProperty())
        val property = property()
        assertEquals(PropertyType.MAP, property.propertyType)
        assertTrue(property.hasEnumMapValue)
        assertTrue(PropertyUtil.isScalarMap(property))
        assertEquals("team.name" to "", PropertyUtil.getKeyValuePair("explyt.modes.team.name", property))
        myFixture.configureByText(
            if (yaml) "application.yaml" else "application.properties",
            if (yaml) "explyt:\n  modes:\n    team.na<caret>me: enabled" else "explyt.modes.team.na<caret>me=enabled"
        )
        val found = file.findReferenceAt(myFixture.caretOffset)
        val refs = if (found is PsiMultiReference) found.references.toList() else listOfNotNull(found)
        val reference = refs.single { it is PropertiesKeyMapValueReference || it is YamlKeyMapValueReference } as PsiPolyVariantReference
        assertEquals("team.name", reference.rangeInElement.substring(reference.element.text))
        val targets = reference.multiResolve(false).mapNotNull { it.element }
        assertEquals(1, targets.size)
        val target = targets.single()
        val member = (if (target is ConfigKeyPsiElement) target.parent else target) as PsiMember
        assertEquals("explyt.Holder", member.containingClass?.qualifiedName)
        assertEquals("setModes", member.name)
    }

    private fun property() = ProjectConfigurationPropertiesLoader(project).loadProperties(module)
        .single { it.name == "explyt.modes" }

    private fun metadata(properties: String) {
        myFixture.addFileToProject("META-INF/additional-spring-configuration-metadata.json", """{"properties":[$properties]}""")
    }

    private fun enumProperty() = """{"name":"explyt.modes","type":"java.util.Map<java.lang.String,explyt.Mode>","sourceType":"explyt.Holder#setModes"}"""
}
