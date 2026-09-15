/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem

import com.explyt.spring.core.externalsystem.process.SpringBootOpenProjectProvider
import com.explyt.spring.core.externalsystem.setting.NativeExecutionSettings
import com.explyt.spring.core.externalsystem.setting.RunConfigurationType
import com.explyt.spring.core.runconfiguration.RunConfigurationUtil
import com.explyt.spring.core.runconfiguration.SpringBootConfigurationFactory
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import javax.swing.Icon

/**
 * Linking a Gradle `bootRun` configuration: the task prefix (`billing:bootRun`) resolves the module, the
 * configuration's environment and VM options travel into the fabricated launch configuration, and any ambiguity
 * resolves to nothing rather than to a guessed module or a silently substituted configuration.
 */
class ExternalSystemLinkTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    override fun tearDown() {
        try {
            ExternalSystemModulePropertyManager.getInstance(myFixture.module).setLinkedProjectId(null)
            ExternalSystemModulePropertyManager.getInstance(myFixture.module).setLinkedProjectPath(null)
        } finally {
            super.tearDown()
        }
    }

    fun testGradleConfigurationMapsToExternalSystemType() {
        val gradleConfiguration = gradleRunConfiguration("local billing", "billing:bootRun")

        val configurationType = SpringBootOpenProjectProvider().getConfigurationType(gradleConfiguration)

        assertEquals(RunConfigurationType.EXTERNAL_SYSTEM, configurationType)
    }

    fun testModuleResolvedFromTaskPrefix() {
        linkModuleAs(":billing:main")
        val gradleConfiguration = gradleRunConfiguration("local billing", "billing:bootRun")

        val module = RunConfigurationUtil.findModuleForExternalSystemRunConfiguration(gradleConfiguration)

        assertSame(myFixture.module, module)
    }

    fun testModuleResolutionIgnoresSiblingGradlePaths() {
        linkModuleAs(":billing-api:main")
        val gradleConfiguration = gradleRunConfiguration("local billing", "billing:bootRun")

        assertNull(RunConfigurationUtil.findModuleForExternalSystemRunConfiguration(gradleConfiguration))
    }

    fun testSingleBootApplicationResolvesMainClass() {
        linkModuleAs(":billing:main")
        addBootApplicationFile("DemoApplication")
        val gradleConfiguration = gradleRunConfiguration("local billing", "billing:bootRun")

        val psiClasses = RunConfigurationUtil.getRunPsiClass(gradleConfiguration)

        assertEquals(listOf("com.demo.DemoApplication"), psiClasses.map { it.qualifiedName })
    }

    fun testTwoBootApplicationsInOneModuleFailClosed() {
        linkModuleAs(":billing:main")
        addBootApplicationFile("FirstApplication")
        addBootApplicationFile("SecondApplication")
        val gradleConfiguration = gradleRunConfiguration("local billing", "billing:bootRun")

        assertEmpty(RunConfigurationUtil.getRunPsiClass(gradleConfiguration))
    }

    fun testExtractorCopiesEnvironmentFromGradleConfiguration() {
        val mainFile = addBootApplicationFile("DemoApplication")
        val gradleConfiguration = gradleRunConfiguration("local billing", "billing:bootRun")
        gradleConfiguration.settings.env["SPRING_PROFILES_ACTIVE"] = "local"
        gradleConfiguration.settings.env["MANAGEMENT_SERVER_PORT"] = "9086"
        gradleConfiguration.settings.vmOptions = "-Xmx1g"
        gradleConfiguration.settings.isPassParentEnvs = false

        // The end-to-end branch resolves the stored path through the local filesystem, which cannot see a light
        // fixture's in-memory files, so the mapping is tested through its VirtualFile seam.
        val runConfiguration = RunConfigurationExtractor.mapExternalSystemRunConfiguration(
            gradleConfiguration, mainFile, nativeSettings(mainFile.path)
        )

        assertNotNull("EXTERNAL_SYSTEM must produce a launch configuration", runConfiguration)
        assertEquals(
            mapOf("SPRING_PROFILES_ACTIVE" to "local", "MANAGEMENT_SERVER_PORT" to "9086"),
            runConfiguration!!.envs
        )
        assertEquals("-Xmx1g", runConfiguration.vmParameters)
        assertFalse(runConfiguration.isPassParentEnvs)
    }

    /**
     * A renamed-away Gradle configuration must report a dangling link, not fall through to the path match: the
     * auto-detected configuration it would find carries no environment, recreating the original silent failure.
     * The EXPLYT probe proves the fixture is arranged so that a fall-through would succeed.
     */
    fun testDanglingGradleLinkDoesNotFallThroughToPathMatch() {
        linkModuleAs(":billing:main")
        val mainFilePath = addBootApplicationFile("DemoApplication").path
        addSpringBootConfiguration("BillingApplicationKt")

        val probe = nativeSettings(mainFilePath).apply {
            runConfigurationType = RunConfigurationType.EXPLYT
            runConfigurationName = null
        }
        assertNotNull(
            "fixture must allow a path match, or the dangling assertion below is vacuous",
            RunConfigurationExtractor.findRunConfiguration(mainFilePath, probe)
        )

        val dangling = nativeSettings(mainFilePath).apply {
            runConfigurationType = RunConfigurationType.EXTERNAL_SYSTEM
            runConfigurationName = "renamed away"
        }
        assertNull(RunConfigurationExtractor.findRunConfiguration(mainFilePath, dangling))
    }

    private fun linkModuleAs(linkedProjectId: String) {
        ExternalSystemModulePropertyManager.getInstance(myFixture.module).setLinkedProjectId(linkedProjectId)
    }

    private fun nativeSettings(mainFilePath: String): NativeExecutionSettings {
        return NativeExecutionSettings(project).apply {
            externalProjectMainFilePath = mainFilePath
            runConfigurationType = RunConfigurationType.EXTERNAL_SYSTEM
            runConfigurationName = "local billing"
            qualifiedMainClassName = "com.demo.DemoApplication"
        }
    }

    private fun addBootApplicationFile(className: String): com.intellij.openapi.vfs.VirtualFile {
        return myFixture.addFileToProject(
            "$className.kt",
            """
            package com.demo

            import org.springframework.boot.autoconfigure.SpringBootApplication
            import org.springframework.boot.runApplication

            @SpringBootApplication
            class $className

            fun main(args: Array<String>) {
                runApplication<$className>(*args)
            }
            """.trimIndent()
        ).virtualFile
    }

    private fun addSpringBootConfiguration(name: String) {
        val runConfiguration = SpringBootConfigurationFactory.createTemplateConfiguration(project)
        runConfiguration.name = name
        runConfiguration.setModule(myFixture.module)
        runConfiguration.mainClassName = "com.demo.DemoApplicationKt"
        addToRunManager(runConfiguration)
    }

    private fun addToRunManager(runConfiguration: com.intellij.execution.configurations.RunConfiguration) {
        val runManager = RunManager.getInstance(project) as RunManagerImpl
        runManager.addConfiguration(RunnerAndConfigurationSettingsImpl(runManager, runConfiguration))
    }

    private fun gradleRunConfiguration(name: String, vararg taskNames: String): ExternalSystemRunConfiguration {
        val configuration = ExternalSystemRunConfiguration(
            ProjectSystemId("GRADLE"), project, StubConfigurationFactory, name
        )
        configuration.settings.taskNames = taskNames.toMutableList()
        return configuration
    }

    private object StubConfigurationType : ConfigurationType {
        override fun getDisplayName() = "Stub"
        override fun getConfigurationTypeDescription() = "Stub"
        override fun getIcon(): Icon? = null
        override fun getId() = "StubExternalSystem"
        override fun getConfigurationFactories() = arrayOf(StubConfigurationFactory)
    }

    private object StubConfigurationFactory : ConfigurationFactory(StubConfigurationType) {
        override fun getId() = "Stub"
        override fun createTemplateConfiguration(project: com.intellij.openapi.project.Project) =
            ExternalSystemRunConfiguration(ProjectSystemId("GRADLE"), project, this, "Stub")
    }
}
