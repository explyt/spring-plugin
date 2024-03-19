package com.esprito.spring.core.service.kotlin

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.PackageScanService
import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.vfs.VirtualFile
import junit.framework.TestCase


class PackageScanServiceTest : EspritoKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

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
}