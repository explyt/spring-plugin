/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties

import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.util.TestLibrarySourceRoot
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder

class ScopedEnumMapMetadataTest : ExplytMultiModuleTestCase() {
    override fun tuneFixture(moduleBuilder: JavaModuleFixtureBuilder<*>) {
        moduleBuilder.addJdkVersion(LanguageLevel.JDK_21)
    }
    fun testEnumVisibleOnlyInIndependentReceivingModule() {
        val other = receiverModule()
        val enumFile = addFileToModule(other, "explyt/Mode.java", "package explyt; public enum Mode { ENABLED }")
        assertIndependent(other)
        assertNull(resolve(module))
        assertEquals(enumFile, resolve(other)!!.containingFile)
        assertTrue(resolve(other)!!.isEnum)
        val loader = metadata(other)
        assertClassification(loader, module, false)
        assertClassification(loader, other, true)
    }

    fun testSameQualifiedNameClassAndEnumRemainModuleScoped() {
        val other = receiverModule()
        val classFile = addFileToModule(module, "explyt/Mode.java", "package explyt; public class Mode {}")
        val enumFile = addFileToModule(other, "explyt/Mode.java", "package explyt; public enum Mode { ENABLED }")
        assertIndependent(other)
        assertEquals(classFile, resolve(module)!!.containingFile)
        assertFalse(resolve(module)!!.isEnum)
        assertEquals(enumFile, resolve(other)!!.containingFile)
        assertTrue(resolve(other)!!.isEnum)
        val loader = metadata(other)
        assertClassification(loader, other, true)
        assertClassification(loader, module, false)
        assertClassification(loader, other, true)
    }

    fun testLibraryMetadataCacheTracksReferencedEnumKindChange() {
        val other = receiverModule()
        val enumFile = addFileToModule(module, "explyt/Mode.java", "package explyt; public enum Mode { ENABLED }")
        val loader = metadata(other)
        assertClassification(loader, module, true)
        val manager = PsiDocumentManager.getInstance(project)
        val document = manager.getDocument(enumFile)!!
        WriteCommandAction.runWriteCommandAction(project) {
            document.setText("package explyt; public class Mode {}")
            manager.commitDocument(document)
        }
        assertFalse(resolve(module)!!.isEnum)
        assertClassification(loader, module, false)
    }

    private fun receiverModule(): Module {
        val receiver = addStandaloneModule("enumReceiver")
        val sdk = ModuleRootManager.getInstance(module).sdk
        assertNotNull("main fixture SDK must be registered", sdk)
        ModuleRootModificationUtil.setModuleSdk(receiver, sdk)
        return receiver
    }

    private fun metadata(other: Module): SpringMetadataLibraryConfigurationPropertiesLoader {
        val library = TestLibrarySourceRoot.create(module, testRootDisposable, "explyt-map-metadata")
        library.attachTo(other)
        library.addFile("META-INF/additional-spring-configuration-metadata.json", """{"properties":[{"name":"explyt.modes","description":"first"}]}""")
        library.addFile("META-INF/spring-configuration-metadata.json", """{"properties":[{"name":"explyt.modes","type":"java.util.Map<java.lang.String,explyt.Mode>"}]}""")
        return SpringMetadataLibraryConfigurationPropertiesLoader(project)
    }

    private fun assertIndependent(other: Module) {
        assertFalse(other in ModuleRootManager.getInstance(module).dependencies)
        assertFalse(module in ModuleRootManager.getInstance(other).dependencies)
    }

    private fun resolve(target: Module) = JavaPsiFacade.getInstance(project)
        .findClass("explyt.Mode", target.getModuleWithDependenciesAndLibrariesScope(false))

    private fun assertClassification(loader: SpringMetadataLibraryConfigurationPropertiesLoader, target: Module, scalar: Boolean) {
        assertNotNull("receiver scope must resolve java.util.Map", JavaPsiFacade.getInstance(project)
            .findClass("java.util.Map", target.getModuleWithDependenciesAndLibrariesScope(false)))
        val properties = loader.loadProperties(target).filter { it.name == "explyt.modes" }
        assertEquals("real metadata producer in ${target.name}", 1, properties.size)
        val property = properties.single()
        assertEquals("first", property.description)
        assertEquals(PropertyType.MAP, property.propertyType)
        assertEquals(scalar, property.hasEnumMapValue)
        assertEquals(scalar, PropertyUtil.isScalarMap(property))
        assertEquals(if (scalar) "team.name" to "" else "team" to "name", PropertyUtil.getKeyValuePair("explyt.modes.team.name", property))
    }
}
