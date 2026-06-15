/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class AiStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        AiPluginService.getInstance(project).checkAiPlugin(project)
    }
}