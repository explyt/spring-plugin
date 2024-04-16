package com.esprito.spring.core.runconfiguration.service

import com.esprito.spring.core.SpringRunConfigurationBundle
import com.esprito.spring.core.runconfiguration.EspritoLicenseState
import com.esprito.spring.core.runconfiguration.SpringToolRunConfigurationConfigurable
import com.esprito.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.esprito.spring.core.runconfiguration.clients.EspritoLicenseClient
import com.intellij.ide.impl.ProjectUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.hours


@Service(Service.Level.PROJECT)
class LicenseTaskSchedulerService(private val project: Project) : Disposable {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val settingsState = SpringToolRunConfigurationsSettingsState.getInstance()

    private val propertyGraph = PropertyGraph()
    private val textLicense = propertyGraph.lazyProperty { settingsState.textLicenseConfigurations }


    fun startScheduler() {
        scope.launch {
            while (isActive) {
                val pem = textLicense.get() ?: return@launch
                verify(pem)
                delay(6.hours)
            }
        }
    }

    private fun shutdown() {
        scope.cancel()
    }

    override fun dispose() {
        shutdown()
    }

    private fun verify(pem: String) {
        try {
            val request = EspritoLicenseClient.requestSign(pem)
            val verify = (request["verify"] as? Double)?.toInt() ?: return

            val state = EspritoLicenseState.entries.find { it.state == verify } ?: EspritoLicenseState.Unknown
            if (state == EspritoLicenseState.Expired) {
                settingsState.stateLicenseConfigurations = state.state

                val notification = Notification(
                    "com.esprito.spring.notification",
                    SpringRunConfigurationBundle.message("run.configuration.boot.license.notification.title"),
                    SpringRunConfigurationBundle.message("run.configuration.boot.license.notification.expired"),
                    NotificationType.WARNING
                )
                notification.addAction(
                    LicenseConfiguration(
                        SpringRunConfigurationBundle.message("run.configuration.boot.license.notification.input")
                    )
                )
                notification.notify(ProjectUtil.getActiveProject())
            }
        } catch (e: Exception) {
            println("Not connect")
        }
    }

    private inner class LicenseConfiguration(
        textAction: String
    ) : NotificationAction(textAction) {
        override fun actionPerformed(anActionEvent: AnActionEvent, notification: Notification) {
            ShowSettingsUtil.getInstance()
                .showSettingsDialog(project, SpringToolRunConfigurationConfigurable::class.java)
        }
    }

    class StartupActivity : ProjectActivity {
        override suspend fun execute(project: Project) {
            val licenseTaskScheduler = getInstance(project)
            licenseTaskScheduler.startScheduler()
        }

        companion object {
            fun getInstance(project: Project): LicenseTaskSchedulerService = project.service()
        }
    }

}