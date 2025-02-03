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

package com.explyt.spring.web.httpclient.action

import com.explyt.spring.core.externalsystem.utils.Constants
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationConfigurable
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.web.SpringWebBundle.message
import com.explyt.spring.web.httpclient.HttpFileState
import com.explyt.spring.web.httpclient.HttpFileStateService
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ParametersList
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.wsl.WslPath
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.ShowPluginsWithSearchOptionAction
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.sh.run.ShConfigurationType
import com.intellij.sh.run.ShRunConfiguration
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.execution.ParametersListUtil
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.*


private const val IJ_HTTP = "ijhttp"
private const val HTTP_YAC = "httpyac"

class HttpRunFileAction : AnAction(AllIcons.Actions.Execute) {
    init {
        templatePresentation.icon = AllIcons.Actions.Execute
        templatePresentation.text = "Run All File Requests"
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        runHttpCommand(project, file)
    }

    companion object {
        /**
         * Line number start with 0
         */
        @RequiresEdt
        fun runHttpCommand(project: Project, file: VirtualFile, lineNumber: Int? = null) {
            val httpClipPath = SpringToolRunConfigurationsSettingsState.getInstance().httpCliPath
            val httpFileState = HttpFileStateService.getInstance().getOrCreateState(file.toNioPath())
            if (httpClipPath.isNullOrEmpty()) {
                Notification(
                    "com.explyt.spring.notification.web",
                    message("explyt.spring.http.cli.empty"),
                    NotificationType.WARNING
                ).addAction(NotificationAction.create(message("explyt.spring.http.cli.empty.setup")) { _ ->
                    ShowSettingsUtil.getInstance().showSettingsDialog(null, Constants.SYSTEM_ID.readableName)
                }).notify(project)
                return
            }
            if (!SpringToolRunConfigurationConfigurable.shellScriptEnabled()) {
                Notification(
                    "com.explyt.spring.notification.web",
                    message("explyt.spring.http.shell.disabled"),
                    NotificationType.WARNING
                ).addAction(
                    ShowPluginsWithSearchOptionAction(
                        message("explyt.spring.http.shell.disabled.action"),
                        "Shell Script"
                    )
                ).notify(project)
            }
            val configurationSettings = try {
                RunManager.getInstance(project)
                    .createConfiguration("Explyt http: ${file.name}", ShConfigurationType::class.java)
            } catch (e: NoClassDefFoundError) {
                return
            }
            val configuration = configurationSettings.configuration
            if (configuration !is ShRunConfiguration) {
                throw IllegalStateException("Configuration not found")
            }
            val parametersList = getParameters(file, httpClipPath, httpFileState, lineNumber)
            val runConfiguration = configurationSettings.configuration as ShRunConfiguration
            runConfiguration.scriptPath = getScriptPath(httpClipPath)
            runConfiguration.scriptOptions = parametersList.parametersString
            runConfiguration.isExecuteScriptFile = true
            runConfiguration.scriptWorkingDirectory = project.basePath
            runConfiguration.interpreterPath = ""

            val builder = ExecutionEnvironmentBuilder
                .createOrNull(DefaultRunExecutor.getRunExecutorInstance(), runConfiguration)
                ?: throw IllegalStateException("ExecutionEnvironmentBuilder not found")
            ExecutionManager.getInstance(project).restartRunProfile(builder.build())
        }

        private fun getParameters(
            file: VirtualFile, httpClipPath: String, fileState: HttpFileState, lineNumber: Int?
        ): ParametersList {
            val isIjHttp = httpClipPath.contains(IJ_HTTP, true)
            val parametersList = ParametersList()
            parametersList.add(getExecuteHttpFilePath(file, isIjHttp, lineNumber))
            if (isIjHttp) {
                parametersList.add("-L")
                parametersList.add("VERBOSE")
                fillIJEnv(parametersList, fileState)
            } else if (httpClipPath.contains(HTTP_YAC, true)) {
                if (noLineNumber(fileState, lineNumber)) {
                    parametersList.add("-a")
                }
                fillEnv(parametersList, fileState)
                fillAdditionalArgs(parametersList, fileState)
                if (lineNumber != null) {
                    parametersList.add("-l")
                    parametersList.add(lineNumber.toString())
                }
            } else {
                fillAdditionalArgs(parametersList, fileState)
            }
            return parametersList
        }

        private fun getExecuteHttpFilePath(file: VirtualFile, isIjHttp: Boolean, lineNumber: Int?): String {
            return if (isIjHttp && lineNumber != null) {
                val fileTmp = createSingleQueryPath(file, lineNumber)
                fileTmp.toPath().absolutePathString()
            } else {
                file.toNioPath().absolutePathString()
            }
        }

        private fun createSingleQueryPath(file: VirtualFile, lineNumber: Int): File {
            val lines = Files.lines(file.toNioPath()).toList()
            val singleRequestContent = getSingleRequestByLineNumber(lines, lineNumber)
            val fileTmp = File.createTempFile("http-", ".${file.extension}")
            fileTmp.writeText(singleRequestContent)
            fileTmp.deleteOnExit()
            AppExecutorUtil.getAppScheduledExecutorService().schedule(
                { FileUtil.asyncDelete(fileTmp) }, 5, TimeUnit.SECONDS
            )
            return fileTmp
        }

        @VisibleForTesting
        fun getSingleRequestByLineNumber(lines: List<String>, lineNumber: Int): String {
            var startLineIdx = 0
            var endLineIdx = lines.size
            for (i in lines.indices) {
                if (!lines[i].startsWith("###")) continue
                if (i < lineNumber) {
                    startLineIdx = i + 1
                } else {
                    endLineIdx = i
                    break
                }
            }
            val singleRequestContent = lines.subList(startLineIdx, endLineIdx).joinToString(System.lineSeparator())
            return singleRequestContent
        }

        private fun noLineNumber(fileState: HttpFileState, lineNumber: Int?): Boolean {
            return lineNumber == null && !fileState.additionalArgs.contains("-l")
        }

        private fun getScriptPath(httpClipPath: String): String {
            val path = Path(httpClipPath)
            if (httpClipPath.contains(IJ_HTTP, true) && path.exists() && path.isDirectory()) {
                if (!SystemInfo.isWindows || WslPath.isWslUncPath(path.absolutePathString())) {
                    val unixPath = path.toFile().listFiles()?.firstOrNull() { it.isFile && it.name.endsWith(IJ_HTTP) }
                    if (unixPath != null) return unixPath.absolutePath
                } else {
                    val winPath = path.toFile().listFiles()
                        ?.firstOrNull { it.isFile && (it.name.endsWith("ijhttp.bat") || it.name.endsWith("ijhttp.cmd")) }
                    if (winPath != null) return winPath.absolutePath
                }
            }
            return httpClipPath
        }

        private fun fillIJEnv(parametersList: ParametersList, fileState: HttpFileState) {
            fillEnvFile(fileState, parametersList)
            fillEnv(parametersList, fileState)
            fillAdditionalArgs(parametersList, fileState)
        }

        private fun fillEnvFile(fileState: HttpFileState, parametersList: ParametersList) {
            val envFilePath = getEnvFilePath(fileState) ?: return
            if (envFilePath.name.contains("privat", true)) parametersList.add("-p") else parametersList.add("-v")
            parametersList.add(envFilePath.absolutePathString())
        }

        private fun getEnvFilePath(fileState: HttpFileState): Path? {
            return fileState.filesPathByName[fileState.selectedFileName]?.let { Path.of(it) }
        }

        private fun fillEnv(parametersList: ParametersList, fileState: HttpFileState) {
            getEnvFilePath(fileState) ?: return
            val envName = fileState.selectedEnv.takeIf { it.isNotEmpty() } ?: return
            parametersList.add("-e")
            parametersList.add(envName)
        }

        private fun fillAdditionalArgs(parametersList: ParametersList, fileState: HttpFileState) {
            val additionalArgs = fileState.additionalArgs.takeIf { it.isNotEmpty() } ?: return
            val argsList = ParametersListUtil.parse(additionalArgs)
            argsList.forEach { parametersList.add(it) }
        }
    }
}