/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.nativecontext

import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.service.beans.BeanApplicationIdentity
import com.explyt.spring.core.service.beans.BeanApplicationResolver
import com.explyt.spring.core.service.beans.BeanContextSelector
import com.explyt.spring.core.service.beans.BeanQueryException
import com.explyt.spring.core.service.beans.BeanQueryProblem
import com.explyt.spring.core.service.beans.BeanSourcePreference
import com.explyt.spring.core.service.beans.NativeBeanContext
import com.explyt.spring.core.service.beans.NativeBeanSnapshotReader
import com.intellij.mcpserver.McpExpectedError
import com.intellij.mcpserver.util.checkUserConfirmationIfNeeded
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiClass

internal data class NativeSyncTarget(
    val context: NativeBeanContext,
    val runConfiguration: String?,
    val linkedPath: String,
) {
    val confirmationText: String
        get() = "Refresh the Native Context '${context.label}': build the modules and start the application " +
                "until its beans are registered"

    val command: String get() = "Refresh Native Context ${context.label}"
}

internal class NativeSyncTargets(private val project: Project) {

    fun select(applicationClassName: String?, contextId: String?): NativeSyncTarget {
        val namedApplication = applicationClassName?.let(::resolveApplication)
        val contexts = NativeBeanSnapshotReader(project).contexts()
        if (contexts.isEmpty()) throw notLinked("No Native Context is linked in the project.")
        val context = if (namedApplication == null && contextId != null) {
            contexts.find { it.id == contextId } ?: throw notLinked("The requested context is not loaded.")
        } else {
            val application = namedApplication ?: resolveApplication(null)
            BeanContextSelector.select(identityOf(application), contexts, BeanSourcePreference.NATIVE, contextId)
                .nativeContext ?: throw notLinked("No loaded context is linked to the application.")
        }
        return NativeSyncTarget(context, runConfigurationOf(context.linkedPath), projectRelative(context.linkedPath))
    }

    private fun resolveApplication(applicationClassName: String?): PsiClass =
        BeanApplicationResolver(project).resolve(applicationClassName, null)

    private fun identityOf(application: PsiClass) = BeanApplicationIdentity(
        className = application.qualifiedName ?: error("An application without a qualified name cannot be scoped"),
        moduleName = ModuleUtilCore.findModuleForPsiElement(application)?.name ?: "",
        mainSourceKey = application.navigationElement.containingFile?.virtualFile?.path ?: ""
    )

    private fun runConfigurationOf(linkedPath: String): String? =
        (ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).getLinkedProjectSettings(linkedPath)
                as? NativeProjectSettings)?.runConfigurationName

    private fun projectRelative(path: String): String {
        val basePath = project.basePath ?: return path.substringAfterLast('/')
        return FileUtil.getRelativePath(basePath, path, '/') ?: path.substringAfterLast('/')
    }

    private fun notLinked(message: String) =
        BeanQueryException(BeanQueryProblem(BeanContextSelector.NATIVE_CONTEXT_NOT_AVAILABLE, message))
}

fun interface NativeSyncConfirmation {
    suspend fun confirm(project: Project, text: String, command: String): Boolean
}

object McpServerConfirmation : NativeSyncConfirmation {
    override suspend fun confirm(project: Project, text: String, command: String): Boolean = try {
        checkUserConfirmationIfNeeded(text, command, project)
        true
    } catch (declined: McpExpectedError) {
        false
    }
}
