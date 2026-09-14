/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.runconfiguration.SpringBootConfigurationFactory
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.execution.RunManager
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager

/**
 * Covers the run-configuration lookup behind the Load Beans gutter icon.
 *
 * The lookup resolves the clicked file to PSI once and then compares each run configuration by its *stored* main
 * class name. Asking every configuration for its resolved main class instead cost one PSI resolution per
 * configuration on the event dispatch thread, which is what [testDoesNotResolveMainClassOfEveryConfiguration]
 * pins down.
 */
class AttachProjectIconGutterHandlerTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    /**
     * A Kotlin top-level `main()` has two identities: the `@SpringBootApplication` class and the `...Kt` file facade
     * that actually declares `main`. A run configuration stores the facade, so the lookup only works if the name set
     * taken from the file contains it. This test states what the file reports rather than assuming it.
     */
    fun testKotlinFileReportsBothTheClassAndTheFacade() {
        val mainClassFile = configureKotlinApplication()

        val names = classNamesOf(mainClassFile)

        assertTrue("Expected the application class among $names", names.contains("com.demo.DemoApplication"))
        assertTrue("Expected the file facade among $names", names.contains("com.demo.DemoApplicationKt"))
    }

    fun testReturnsConfigurationLaunchingTheJavaMainClass() {
        val path = configureJavaApplication()
        val expected = addSpringBootConfiguration("Demo", "com.demo.JavaDemoApplication")

        assertSame(expected, lookup(path))
    }

    /** The Kotlin configuration stores the facade name, which the application class alone would never match. */
    fun testReturnsConfigurationLaunchingTheKotlinFacade() {
        val path = configureKotlinApplication()
        val expected = addSpringBootConfiguration("Demo", "com.demo.DemoApplicationKt")

        assertSame(expected, lookup(path))
    }

    fun testIgnoresConfigurationOfAnotherFile() {
        val path = configureKotlinApplication()
        addSpringBootConfiguration("Other", "com.demo.OtherApplicationKt")

        assertNull(lookup(path))
    }

    fun testNoConfigurationMatchesGivesNull() {
        val path = configureKotlinApplication()

        assertNull(lookup(path))
    }

    /** The user's current selection wins over any later match, because it is the one they are working with. */
    fun testSelectedConfigurationWins() {
        val path = configureKotlinApplication()
        val selected = addSpringBootConfiguration("Selected", "com.demo.DemoApplicationKt")
        addSpringBootConfiguration("Later", "com.demo.DemoApplicationKt")
        runManager().selectedConfiguration = settingsNamed("Selected")

        assertSame(selected, lookup(path))
    }

    /**
     * The regression guard, and the only test here that can fail if the lookup goes back to resolving PSI per
     * configuration.
     *
     * The *selected* configuration is one whose main class cannot be resolved, while the match is a different
     * configuration. The lookup always inspects the selected configuration first: comparing its stored name is free,
     * whereas asking it for its resolved main class blows up. A non-matching selection must simply be skipped.
     */
    fun testDoesNotResolveMainClassOfTheSelectedConfiguration() {
        val mainClassFile = configureKotlinApplication()
        addExplodingConfiguration()
        val expected = addSpringBootConfiguration("Demo", "com.demo.DemoApplicationKt")
        runManager().selectedConfiguration = settingsNamed("Exploding")

        assertSame(expected, lookup(mainClassFile))
    }

    private fun lookup(mainClassFile: VirtualFile): RunConfiguration? =
        AttachProjectIconGutterHandler(mainClassFile.path, null).findRunConfiguration(project, mainClassFile)

    private fun classNamesOf(mainClassFile: VirtualFile): Set<String> {
        val classOwner = PsiManager.getInstance(project).findFile(mainClassFile) as? PsiClassOwner
            ?: error("Not a PsiClassOwner: ${mainClassFile.path}")
        return classOwner.classes.mapNotNullTo(mutableSetOf()) { it.qualifiedName }
    }

    private fun runManager() = RunManager.getInstance(project) as RunManagerImpl

    private fun settingsNamed(name: String): RunnerAndConfigurationSettingsImpl =
        runManager().allSettings.first { it.name == name } as RunnerAndConfigurationSettingsImpl

    private fun addSpringBootConfiguration(name: String, mainClassName: String): RunConfiguration {
        val runManager = runManager()
        val runConfiguration = SpringBootConfigurationFactory.createTemplateConfiguration(project)
        runConfiguration.name = name
        runConfiguration.mainClassName = mainClassName
        runManager.addConfiguration(RunnerAndConfigurationSettingsImpl(runManager, runConfiguration))
        return runConfiguration
    }

    /** Fails loudly if anything asks it to resolve its main class. */
    private fun addExplodingConfiguration(): RunConfiguration {
        val runManager = runManager()
        val runConfiguration = object : ApplicationConfiguration("Exploding", project, SpringBootConfigurationFactory) {
            override fun getMainClass() = error("Main class of an unrelated configuration must not be resolved")

            override fun getRunClass() = error("Run class of an unrelated configuration must not be resolved")
        }
        runManager.addConfiguration(RunnerAndConfigurationSettingsImpl(runManager, runConfiguration))
        return runConfiguration
    }

    private fun configureKotlinApplication(): VirtualFile = myFixture.configureByText(
        "DemoApplication.kt",
        """
        package com.demo

        import org.springframework.boot.autoconfigure.SpringBootApplication
        import org.springframework.boot.runApplication

        @SpringBootApplication
        class DemoApplication

        fun main(args: Array<String>) {
            runApplication<DemoApplication>(*args)
        }
        """.trimIndent()
    ).virtualFile

    private fun configureJavaApplication(): VirtualFile = myFixture.configureByText(
        "JavaDemoApplication.java",
        """
        package com.demo;

        import org.springframework.boot.autoconfigure.SpringBootApplication;

        @SpringBootApplication
        public class JavaDemoApplication {
            public static void main(String[] args) {
            }
        }
        """.trimIndent()
    ).virtualFile
}
