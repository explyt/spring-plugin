/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.runconfiguration

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.application.ApplicationConfigurationType
import com.intellij.execution.configurations.JavaRunConfigurationModule
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationType

class RunConfigurationUtilTest : ExplytJavaLightTestCase() {

    // On the newer platform lines this test overrides getRunClass() to throw, because there the entry
    // point is read through `mainClassName` and getRunClass() resolves PSI. This line has no
    // `mainClassName`: getRunClass() is the raw options accessor (`return options.mainClassName`) and
    // findMainClassFile() is the only PSI path. There is therefore no resolving accessor to fence off,
    // and the override would fail the very call the production code is supposed to make.
    fun testKotlinRunClassNameIsReadFromOptions() {
        val mainClassName = "com.example.MainKt"
        val configuration = KotlinRunConfiguration(
            "test",
            JavaRunConfigurationModule(project, true),
            KotlinRunConfigurationType.instance
        ).apply {
            runClass = mainClassName
        }

        assertEquals(mainClassName, RunConfigurationUtil.getRunClassName(configuration))
    }

    /**
     * Covers every [ApplicationConfiguration], including the JetBrains Spring Boot configuration: its `getRunClass()`
     * searches for a main-class candidate through indexes and jar attributes, which froze the event dispatch thread
     * when a run configuration was selected.
     */
    fun testApplicationRunClassNameDoesNotResolvePsi() {
        val mainClassName = "com.example.DemoApplication"
        val configuration = object : ApplicationConfiguration(
            "test",
            project,
            ApplicationConfigurationType.getInstance()
        ) {
            override fun getRunClass(): String = error("Run class must not be resolved")

            override fun getMainClass(): PsiClass = error("Main class must not be resolved")
        }.apply {
            this.mainClassName = mainClassName
        }

        assertEquals(mainClassName, RunConfigurationUtil.getRunClassName(configuration))
    }
}
