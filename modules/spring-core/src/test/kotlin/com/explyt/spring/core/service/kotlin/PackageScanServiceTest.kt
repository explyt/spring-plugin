/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.service.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.runconfiguration.SpringBootConfigurationFactory
import com.explyt.spring.core.service.AnnotationConfigApplicationService
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.execution.RunManager
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import junit.framework.TestCase


class PackageScanServiceTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
    }

    override fun tearDown() {
        super.tearDown()
        Registry.get("explyt.spring.root.runConfiguration").resetToDefault()
    }

    fun testPackageScan() {
        val virtualFile = myFixture.copyDirectoryToProject("service/packageScan", "")
        val allPackages = PackageScanService.getInstance(project).getAllPackages()
        TestCase.assertNotNull(allPackages)
        val module = getModule(virtualFile)
        val packages = allPackages.getPackages(module)
        TestCase.assertEquals(setOf("com.inner.", "com.outer."), packages)
    }

    fun testPackageScanAndSpringBootApp() {
        val virtualFile = myFixture.copyDirectoryToProject("service/packageScanAndSpringBootApp", "")
        val allPackages = PackageScanService.getInstance(project).getAllPackages()
        TestCase.assertNotNull(allPackages)
        val module = getModule(virtualFile)
        val packages = allPackages.getPackages(module)
        TestCase.assertEquals(setOf("com.inner.", "com.app."), packages)
    }

    fun testPackageScanAndSpringBootAppSomeClass() {
        val virtualFile = myFixture.copyDirectoryToProject("service/packageScanAndSpringBootAppSomeClass", "")
        val allPackages = PackageScanService.getInstance(project).getAllPackages()
        TestCase.assertNotNull(allPackages)
        val module = getModule(virtualFile)
        val packages = allPackages.getPackages(module)
        TestCase.assertEquals(setOf("com.inner."), packages)
    }

    private fun getModule(virtualFile: VirtualFile): Module {
        val module = ModuleUtilCore.findModuleForFile(virtualFile, project)
        assertNotNull(module)
        return module!!
    }

    fun testSpringBootScanBasePackageClasses() {
        val virtualFile = myFixture.configureByText(
            "SpringComponent.kt",
            """
            @${SpringCoreClasses.SPRING_BOOT_APPLICATION}(scanBasePackageClasses = [java.lang.String::class, java.util.ArrayList::class])            
            class SpringComponent
            """.trimIndent()
        )
        val allPackages = PackageScanService.getInstance(project).getAllPackages()
        TestCase.assertNotNull(allPackages)
        val module = getModule(virtualFile.virtualFile)
        val packages = allPackages.getPackages(module)
        TestCase.assertEquals(setOf("java.lang.", "java.util."), packages)
    }

    fun testSpringBootScanBasePackages() {
        val virtualFile = myFixture.configureByText(
            "SpringComponent.kt",
            """
            @${SpringCoreClasses.SPRING_BOOT_APPLICATION}(scanBasePackages = ["java.lang", "java.util"])            
            class SpringComponent
            """.trimIndent()
        )
        val allPackages = PackageScanService.getInstance(project).getAllPackages()
        TestCase.assertNotNull(allPackages)
        val module = getModule(virtualFile.virtualFile)
        val packages = allPackages.getPackages(module)
        TestCase.assertEquals(setOf("java.lang.", "java.util."), packages)
    }

    fun testRunConfigurationMainAppConfigConstructor() {
        myFixture.copyDirectoryToProject("service/packageScanRunConfigurationAppConfigConstructor", "")

        Registry.get("explyt.spring.root.runConfiguration").setValue(true)
        val runConfiguration = SpringBootConfigurationFactory.createTemplateConfiguration(project)
        runConfiguration.mainClassName = "com.app.MainClass"

        val manager = RunManager.getInstance(project)
        val settings = RunnerAndConfigurationSettingsImpl((manager as RunManagerImpl), runConfiguration)
        manager.setTemporaryConfiguration(settings)

        val mainClass = JavaPsiFacade.getInstance(project)
            .findClass(runConfiguration.mainClassName!!, GlobalSearchScope.projectScope(project))
            ?.let { listOf(it) } ?: throw RuntimeException()
        val actualList = AnnotationConfigApplicationService.getRootClasses(mainClass, emptyList())
            .mapNotNull { it.name }
        TestCase.assertEquals(listOf("AppTestConfiguration"), actualList)
    }
}