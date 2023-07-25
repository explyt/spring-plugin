package com.esprito.spring.core.runconfiguration

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringRunConfigurationBundle
import com.esprito.spring.core.notifications.SpringToolNotificationGroup
import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
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
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.PsiMethodUtil
import com.intellij.util.concurrency.AppExecutorUtil
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
            .finishOnUiThread(ModalityState.NON_MODAL) { configurations ->
                if (configurations.isEmpty())
                    return@finishOnUiThread

                saveRunConfigurations(configurations)
            }
            .submit(AppExecutorUtil.getAppScheduledExecutorService())
    }

    private fun searchForRunConfigurations(): List<SpringBootRunConfiguration> {
        val applicationAnnotation = JavaPsiFacade.getInstance(project)
            .findClass(
                SpringCoreClasses.SPRING_BOOT_APPLICATION,
                GlobalSearchScope.allScope(project)
            )
            ?: return emptyList()

        val bootApplicationClasses = AnnotatedElementsSearch.searchPsiClasses(
            applicationAnnotation,
            GlobalSearchScope.projectScope(project)
        )

        return bootApplicationClasses.filter {
            PsiMethodUtil.findMainInClass(it) != null
        }.mapNotNull { psiClass ->
            val module = ModuleUtilCore.findModuleForPsiElement(psiClass)
                ?: return@mapNotNull null

            SpringBootConfigurationFactory
                .createTemplateConfiguration(project)
                .apply {
                    setModule(module)
                    setMainClass(psiClass)
                    setGeneratedName()
                }
        }
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

        if(runManager.selectedConfiguration == null) {
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