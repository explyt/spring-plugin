/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.reference

import com.explyt.spring.core.completion.properties.ProjectConfigurationPropertiesLoader
import com.explyt.spring.core.properties.providers.ConfigKeyPsiElement
import com.explyt.spring.core.properties.references.PropertiesKeyMapValueReference
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference

abstract class EnumMapCacheInvalidationFixture : ExplytBaseLightTestCase() {
    protected abstract val kotlin: Boolean
    override val libraries = arrayOf(TestLibrary.springBoot_3_1_1, TestLibrary.springContext_6_0_7)

    fun testPropertySignatureTypeEditUpdatesCachedFactAndReference() {
        val declarations = declarations(true)
        val loader = ProjectConfigurationPropertiesLoader(project)
        configure()
        assertState(loader, true, "explyt.Holder", "setModes")
        replace(declarations.first, "String, Mode", "String, Entry")
        val parameterType = myFixture.findClass("explyt.Holder").findMethodsByName("setModes", false)
            .single().parameterList.parameters.single().type as com.intellij.psi.PsiClassType
        val valueType = parameterType.parameters[1] as com.intellij.psi.PsiClassType
        assertEquals("explyt.Entry", valueType.resolve()?.qualifiedName)
        assertState(loader, false, "explyt.Entry", "setName")
    }

    fun testReferencedEnumBecomesClassInDifferentFile() {
        val declarations = declarations(true)
        val loader = ProjectConfigurationPropertiesLoader(project)
        configure()
        assertState(loader, true, "explyt.Holder", "setModes")
        replace(declarations.second, declarations.second.text, mode(false))
        assertFalse(myFixture.findClass("explyt.Mode").isEnum)
        assertState(loader, false, "explyt.Mode", "setName")
    }

    fun testReferencedClassBecomesEnumInDifferentFile() {
        val declarations = declarations(false)
        val loader = ProjectConfigurationPropertiesLoader(project)
        configure()
        assertState(loader, false, "explyt.Mode", "setName")
        replace(declarations.second, declarations.second.text, mode(true))
        assertTrue(myFixture.findClass("explyt.Mode").isEnum)
        assertState(loader, true, "explyt.Holder", "setModes")
    }

    private fun declarations(enum: Boolean): Pair<PsiFile, PsiFile> {
        val extension = if (kotlin) "kt" else "java"
        val mode = myFixture.addFileToProject("explyt/Mode.$extension", mode(enum))
        myFixture.addFileToProject("explyt/Entry.$extension", if (kotlin) "package explyt\nclass Entry { var name: String = \"\" }" else "package explyt; public class Entry { private String name; public void setName(String name) { this.name = name; } }")
        val holder = myFixture.addFileToProject("explyt/Holder.$extension", if (kotlin) """
            package explyt
            import org.springframework.boot.context.properties.ConfigurationProperties
            @ConfigurationProperties("explyt")
            class Holder { var modes: Map<String, Mode> = emptyMap() }
        """.trimIndent() else """
            package explyt;
            import org.springframework.boot.context.properties.ConfigurationProperties;
            @ConfigurationProperties(prefix = "explyt")
            public class Holder {
                private java.util.Map modes;
                public void setModes(java.util.Map<String, Mode> modes) { this.modes = modes; }
            }
        """.trimIndent())
        assertEquals(enum, myFixture.findClass("explyt.Mode").isEnum)
        return holder to mode
    }

    private fun mode(enum: Boolean): String = if (kotlin) {
        if (enum) "package explyt\nenum class Mode { ENABLED }" else "package explyt\nclass Mode { var name: String = \"\" }"
    } else {
        if (enum) "package explyt; public enum Mode { ENABLED }" else "package explyt; public class Mode { private String name; public void setName(String name) { this.name = name; } }"
    }

    private fun configure() {
        myFixture.configureByText("application.properties", "explyt.modes.team.na<caret>me=enabled")
    }

    private fun replace(file: PsiFile, old: String, new: String) {
        val manager = PsiDocumentManager.getInstance(project)
        val document = manager.getDocument(file)!!
        val offset = document.text.indexOf(old)
        assertTrue("edit must replace an existing declaration", offset >= 0)
        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(offset, offset + old.length, new)
            manager.commitDocument(document)
        }
    }

    private fun assertState(loader: ProjectConfigurationPropertiesLoader, scalar: Boolean, owner: String, memberName: String) {
        val properties = loader.loadProperties(module).filter { it.name == "explyt.modes" }
        assertEquals("real configuration-properties producer", 1, properties.size)
        val property = properties.single()
        assertEquals("cached enum fact for $owner", scalar, property.hasEnumMapValue)
        assertEquals(scalar, PropertyUtil.isScalarMap(property))
        assertEquals(if (scalar) "team.name" to "" else "team" to "name", PropertyUtil.getKeyValuePair("explyt.modes.team.name", property))
        val found = file.findReferenceAt(myFixture.caretOffset)
        val refs = if (found is PsiMultiReference) found.references.toList() else listOfNotNull(found)
        val reference = refs.single { it is PropertiesKeyMapValueReference } as PsiPolyVariantReference
        assertEquals(if (scalar) "team.name" else "name", reference.rangeInElement.substring(reference.element.text))
        val targets = reference.multiResolve(false).mapNotNull { it.element }
        assertEquals(1, targets.size)
        val target = targets.single()
        val member = (if (target is ConfigKeyPsiElement) target.parent else target) as PsiMember
        assertEquals(owner, member.containingClass?.qualifiedName)
        assertEquals(memberName, member.name)
    }
}

class JavaEnumMapCacheInvalidationTest : EnumMapCacheInvalidationFixture() {
    override val kotlin = false
}

class KotlinEnumMapCacheInvalidationTest : EnumMapCacheInvalidationFixture() {
    override val kotlin = true
}
