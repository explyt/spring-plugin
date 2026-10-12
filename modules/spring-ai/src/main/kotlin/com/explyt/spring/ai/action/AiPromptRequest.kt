/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.action

import com.explyt.spring.ai.service.AiPluginService
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.concurrency.Promise
import java.util.concurrent.Callable

class AiPromptRequest(val prompt: String, val files: List<VirtualFile>)

fun submitAiPromptInSmartMode(project: Project, composeRequest: () -> AiPromptRequest?): Promise<AiPromptRequest?> =
    ReadAction.nonBlocking(Callable { composeRequest() })
        .inSmartMode(project)
        .expireWhen { project.isDisposed }
        .finishOnUiThread(ModalityState.nonModal()) { request ->
            request?.let { AiPluginService.getInstance(project).performPrompt(it.prompt, it.files) }
        }
        .submit(AppExecutorUtil.getAppExecutorService())
