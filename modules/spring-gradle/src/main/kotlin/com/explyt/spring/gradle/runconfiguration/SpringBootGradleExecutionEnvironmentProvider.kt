/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.gradle.runconfiguration

import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.task.ExecuteRunConfigurationTask
import org.jetbrains.plugins.gradle.execution.build.GradleExecutionEnvironmentProvider

class SpringBootGradleExecutionEnvironmentProvider : GradleExecutionEnvironmentProvider {
    override fun isApplicable(task: ExecuteRunConfigurationTask): Boolean {
        return task.runProfile is SpringBootRunConfiguration
    }

    override fun createExecutionEnvironment(
        project: Project,
        task: ExecuteRunConfigurationTask,
        executor: Executor
    ): ExecutionEnvironment? {
        return null
    }
}
