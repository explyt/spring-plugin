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

package com.explyt.spring.core.runconfiguration

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringRunConfigurationBundle
import com.explyt.spring.core.notifications.SpringToolNotificationGroup
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.DumbService.DumbModeListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiMethodUtil
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType
import java.util.concurrent.Callable

@Service(Service.Level.PROJECT)
class SpringRunConfigurationDetectService(
    private val project: Project
) : Disposable {
    private val runManager by lazy { RunManager.getInstance(project) }

    private fun runDetection() {
        ReadAction.nonBlocking(Callable {
            if (!SpringCoreUtil.isSpringBootProject(project)) return@Callable emptyList()
            searchForRunConfigurations()
        })
            .inSmartMode(project)
            .finishOnUiThread(ModalityState.nonModal()) { configurations ->
                if (configurations.isEmpty())
                    return@finishOnUiThread

                saveRunConfigurations(configurations)
            }
            .submit(AppExecutorUtil.getAppScheduledExecutorService())
    }

    private fun searchForRunConfigurations(): List<SpringBootRunConfiguration> {
        return PsiShortNamesCache.getInstance(project)
            .getMethodsByName("main", GlobalSearchScope.projectScope(project)).asSequence()
            .filter { PsiMethodUtil.isMainMethod(it) }
            .filter { isBootApplicationMethod(it) }
            .mapNotNull { it.containingClass }
            .mapNotNullTo(mutableListOf()) { psiClass ->
                val module = ModuleUtilCore.findModuleForPsiElement(psiClass)
                    ?: return@mapNotNullTo null

                SpringBootConfigurationFactory
                    .createTemplateConfiguration(project)
                    .apply {
                        setModule(module)
                        setMainClass(psiClass)
                        setGeneratedName()
                    }
            }
    }

    private fun isBootApplicationMethod(psiMethod: PsiMethod): Boolean {
        if (psiMethod.containingClass?.isMetaAnnotatedBy(SpringCoreClasses.SPRING_BOOT_APPLICATION) == true) return true

        val bodyPsi = psiMethod.toUElementOfType<UMethod>()?.uastBody?.sourcePsi ?: return false
        return bodyPsi.text.contains("runApplication")
    }

    private fun saveRunConfigurations(
        configurations: List<SpringBootRunConfiguration>
    ) {
        val moduleToMainClass = runManager
            .allConfigurationsList
            .filterIsInstance<SpringBootRunConfiguration>()
            .map { it.configurationModule.module to it.mainClassName }

        val configurationsToCreate = configurations
            .filter {
                (it.configurationModule.module to it.mainClassName) !in moduleToMainClass
            }.map {
                runManager
                    .createConfiguration(
                        it,
                        SpringBootConfigurationFactory
                    )
            }

        if (configurationsToCreate.isEmpty())
            return

        configurationsToCreate.forEach {
            runManager.addConfiguration(it)
        }

        if (runManager.selectedConfiguration == null) {
            runManager.selectedConfiguration = configurationsToCreate.first()
        }

        SpringToolNotificationGroup
            .createNotification(
                SpringRunConfigurationBundle.plural(
                    "run.configuration.boot.detection.created",
                    configurationsToCreate.size,
                    configurationsToCreate.size
                ),
                NotificationType.INFORMATION
            ).addAction(
                RollbackConfigurationCreation(configurationsToCreate, false),
            ).addAction(
                RollbackConfigurationCreation(configurationsToCreate, true)
            )
            .notify(project)
    }

    private inner class RollbackConfigurationCreation(
        private val configurations: List<RunnerAndConfigurationSettings>,
        private val disableAutoDetect: Boolean
    ) : NotificationAction(
        if (disableAutoDetect)
            SpringRunConfigurationBundle.message("run.configuration.boot.detection.disable")
        else
            SpringRunConfigurationBundle.message("run.configuration.boot.detection.rollback")
    ) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            configurations.forEach(runManager::removeConfiguration)

            if (disableAutoDetect) {
                SpringToolRunConfigurationsSettingsState.getInstance()
                    .state
                    .isAutoDetectConfigurations = false
            }

            notification.expire()
        }
    }

    class StartupActivity : ProjectActivity {
        override suspend fun execute(project: Project) {
            PropertiesComponent.getInstance().setValue("framework.suggestion.dismissed.spring.boot", true)
            PropertiesComponent.getInstance().setValue("promo.framework.suggestion.dismissed.spring.boot", true)
            PropertiesComponent.getInstance().setValue("swagger.suggestion.dismissed", true)
            PropertiesComponent.getInstance().setValue("ide.try.ultimate.disabled", true)

            val runConfigurationsSettingsState = SpringToolRunConfigurationsSettingsState.getInstance()

            if (!runConfigurationsSettingsState.state.isAutoDetectConfigurations) {
                return
            }

            val detectService = getInstance(project)

            project.messageBus
                .connect(detectService)
                .subscribe(DumbService.DUMB_MODE, object : DumbModeListener {
                    override fun exitDumbMode() {
                        detectService.runDetection()
                    }
                })

            detectService.runDetection()
        }
    }

    companion object {
        fun getInstance(project: Project): SpringRunConfigurationDetectService = project.service()
    }

    override fun dispose() {}
}