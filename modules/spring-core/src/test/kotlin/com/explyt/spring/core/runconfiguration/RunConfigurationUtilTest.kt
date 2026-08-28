/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.runconfiguration

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.intellij.execution.configurations.JavaRunConfigurationModule
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationType

class RunConfigurationUtilTest : ExplytJavaLightTestCase() {

    fun testKotlinRunClassNameDoesNotResolvePsi() {
        val mainClassName = "com.example.MainKt"
        val configuration = object : KotlinRunConfiguration(
            "test",
            JavaRunConfigurationModule(project, true),
            KotlinRunConfigurationType.instance
        ) {
            override fun getRunClass(): String? = error("Kotlin run class must not be resolved")
        }.apply {
            this.mainClassName = mainClassName
        }

        assertEquals(mainClassName, RunConfigurationUtil.getRunClassName(configuration))
    }
}
