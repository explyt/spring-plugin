package com.esprito.spring.core.externalsystem


import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringCoreClasses.REST_CONTROLLER
import com.esprito.spring.core.externalsystem.model.SpringBeanData
import com.esprito.spring.core.externalsystem.model.SpringBeanType
import com.esprito.spring.core.externalsystem.model.SpringProfileData
import com.esprito.spring.core.externalsystem.process.BeanInfo
import com.esprito.spring.core.externalsystem.process.ExplytCapturingProcessAdapter
import com.esprito.spring.core.externalsystem.setting.NativeExecutionSettings
import com.esprito.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.esprito.spring.core.externalsystem.utils.NativeBootUtils
import com.esprito.spring.core.profile.SpringProfilesService
import com.esprito.spring.core.profile.SpringProfilesService.Companion.DEFAULT_PROFILE_LIST
import com.esprito.spring.core.runconfiguration.SpringBootRunConfiguration
import com.explyt.spring.boot.bean.reader.SpringBootBeanReaderStarter
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.isFile
import com.intellij.psi.PsiClass
import com.intellij.serviceContainer.AlreadyDisposedException
import com.intellij.task.ProjectTaskManager
import com.intellij.util.PathUtil
import java.awt.BorderLayout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MINUTES
import javax.swing.JPanel

class SpringBeanNativeResolver : ExternalSystemProjectResolver<NativeExecutionSettings> {

    private val cancellationMap = ConcurrentHashMap<ExternalSystemTaskId, ProcessHandler>()

    override fun cancelTask(taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
        cancellationMap.remove(taskId)?.destroyProcess()
        return true
    }

    override fun resolveProjectInfo(
        id: ExternalSystemTaskId,
        projectPath: String,
        isPreviewMode: Boolean,
        settings: NativeExecutionSettings?,
        listener: ExternalSystemTaskNotificationListener
    ): DataNode<ProjectData> {
        val runConfiguration = findRunConfigurationReadAction(projectPath, settings)
        if (isPreviewMode) {
            return DataNode(ProjectKeys.PROJECT, projectData(projectPath, runConfiguration), null)
        }
        settings ?: throw ExternalSystemException("No settings")
        runConfiguration ?: throw ExternalSystemException("No run configuration")
        try {
            return synchronized(this::class.java) {
                try {
                    getProjectDataNode(id, projectPath, runConfiguration, settings, listener)
                } catch (e: AlreadyDisposedException) {
                    throw ExternalSystemException("Project data is disposed. Please try again")
                }
            }
        } finally {
            cancellationMap.remove(id)
        }
    }

    private fun getProjectDataNode(
        id: ExternalSystemTaskId,
        projectPath: String,
        runConfiguration: SpringBootRunConfiguration,
        settings: NativeExecutionSettings,
        listener: ExternalSystemTaskNotificationListener,
    ): DataNode<ProjectData> {
        val mainClass = ApplicationManager.getApplication()
            .runReadAction(Computable { NativeBootUtils.getMainClass(runConfiguration) })
            ?: throw ExternalSystemException("No main class run configuration found")

        val modules = runConfiguration.modules
        val buildPromise = ProjectTaskManager.getInstance(settings.project).build(*modules)
        val buildResult = buildPromise.blockingGet(10, MINUTES)
        if (buildResult == null || buildResult.hasErrors() || buildResult.isAborted) {
            throw ExternalSystemException("Build failed. Try build project: Build -> Build Project (Ctrl + F9) and see log")
        }

        val clone = runConfiguration.clone() as SpringBootRunConfiguration
        clone.envs["explyt.spring.appClassName"] = ApplicationManager.getApplication()
            .runReadAction(Computable { mainClass.qualifiedName })
        clone.mainClassName = SpringBootBeanReaderStarter::class.qualifiedName
        clone.classpathModifications.add(getClasspathExplytModification())

        val processAdapter = ExplytCapturingProcessAdapter(id, listener)
        val descriptor = getDescriptor()
        val environment = getEnvironment(id, clone, processAdapter, descriptor)
        try {
            ProgramRunnerUtil.executeConfiguration(environment, false, false)
            processAdapter.await()
        } finally {
            Disposer.dispose(environment)
            Disposer.dispose(descriptor)
        }
        val beans = processAdapter.getBeans()

        val projectData = projectData(projectPath, runConfiguration)
        val projectDataNode = DataNode(ProjectKeys.PROJECT, projectData, null)
        beans.mapNotNull { toSpringBeanDataInReadAction(it, id, settings, listener) }
            .forEach { projectDataNode.createChild(SpringBeanData.KEY, it) }
        fillProfiles(projectDataNode, projectPath, settings.project, runConfiguration)
        return projectDataNode
    }

    private fun fillProfiles(
        projectDataNode: DataNode<ProjectData>,
        projectPath: String,
        project: Project,
        runConfiguration: SpringBootRunConfiguration
    ) {
        val module = getModule(projectPath, project) ?: return
        val profiles = SpringProfilesService.getInstance(project).loadExistingProfiles(module)
        if (profiles == DEFAULT_PROFILE_LIST) return
        profiles.map { SpringProfileData(it, runConfiguration.name) }
            .forEach { projectDataNode.createChild(SpringProfileData.KEY, it) }
    }

    private fun getModule(projectPath: String, project: Project): Module? {
        return ApplicationManager.getApplication().runReadAction(Computable {
            LocalFileSystem.getInstance().findFileByPath(projectPath)
                ?.let { ModuleUtilCore.findModuleForFile(it, project) }
        })
    }


    private fun toSpringBeanDataInReadAction(
        bean: BeanInfo,
        id: ExternalSystemTaskId,
        settings: NativeExecutionSettings,
        listener: ExternalSystemTaskNotificationListener
    ): SpringBeanData? {
        return ApplicationManager.getApplication()
            .runReadAction(Computable { toSpringBeanData(bean, id, settings, listener) })
    }

    private fun toSpringBeanData(
        bean: BeanInfo,
        id: ExternalSystemTaskId,
        settings: NativeExecutionSettings,
        listener: ExternalSystemTaskNotificationListener
    ): SpringBeanData? {
        val fileIndex = ProjectRootManager.getInstance(settings.project).fileIndex
        val psiClassLocation = NativeBootUtils.getPsiClassLocation(settings.project, bean.className)
        if (psiClassLocation == null) {
            listener.onTaskOutput(id, "[WARNING] ${bean.className} not found", true)
            return null
        }

        return SpringBeanData(
            beanName = bean.beanName,
            className = bean.className,
            scope = bean.scope,
            methodName = bean.methodName,
            methodType = bean.methodType,
            primary = bean.primary,
            rootBean = bean.rootBean,
            type = getBeanType(psiClassLocation, bean),
            projectBean = psiClassLocation.containingFile?.virtualFile?.let { fileIndex.isInSource(it) } == true
        )
    }

    private fun projectData(projectPath: String, configuration: RunConfiguration?): ProjectData {
        val directoryPath = LocalFileSystem.getInstance().findFileByPath(projectPath)
            ?.takeIf { it.isFile }?.parent?.canonicalPath
            ?: throw ExternalSystemException("File not found $projectPath")

        val projectData = ProjectData(
            SYSTEM_ID, configuration?.name ?: SYSTEM_ID.readableName, directoryPath, projectPath
        )
        return projectData
    }

    private fun findRunConfigurationReadAction(
        projectPath: String, settings: NativeExecutionSettings?
    ): SpringBootRunConfiguration? {
        return ApplicationManager.getApplication()
            .runReadAction(Computable { findRunConfiguration(projectPath, settings) as? SpringBootRunConfiguration })
    }

    private fun findRunConfiguration(projectPath: String, settings: NativeExecutionSettings?): RunConfiguration? {
        settings ?: return null
        val allConfigurationsList = RunManager.getInstance(settings.project).allConfigurationsList
        return if (settings.runConfigurationName != null) {
            allConfigurationsList.find { it is SpringBootRunConfiguration && it.name == settings.runConfigurationName }
        } else {
            allConfigurationsList.find { checkRunConfiguration(it, projectPath) }
        }
    }

    private fun checkRunConfiguration(runConfiguration: RunConfiguration, projectPath: String): Boolean {
        return if (runConfiguration is SpringBootRunConfiguration) {
            runConfiguration.mainClass?.containingFile?.virtualFile?.canonicalPath == projectPath
        } else false
    }

    private fun getEnvironment(
        id: ExternalSystemTaskId,
        clone: RunConfiguration,
        processAdapter: ExplytCapturingProcessAdapter,
        descriptor: RunContentDescriptor
    ): ExecutionEnvironment {
        val environment = ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), clone)
            .contentToReuse(descriptor)
            .activeTarget()
            .build {
                val processHandler = it?.processHandler ?: return@build
                cancellationMap[id] = processHandler
                processHandler.addProcessListener(processAdapter)
            }
        environment.setHeadless()
        return environment
    }

    private fun getClasspathExplytModification(): ModuleBasedConfigurationOptions.ClasspathModification {
        val classpathModification = ModuleBasedConfigurationOptions.ClasspathModification(
            PathUtil.getJarPathForClass(SpringBootBeanReaderStarter::class.java), false
        )
        return classpathModification
    }

    private fun getDescriptor(): RunContentDescriptor {
        return object : RunContentDescriptor(null, null, JPanel(BorderLayout()), this::class.qualifiedName) {
            override fun isContentReuseProhibited() = true
        }
    }

    private fun getBeanType(psiClass: PsiClass, bean: BeanInfo): SpringBeanType {
        return if (bean.methodName != null) {
            SpringBeanType.METHOD
        } else if (bean.type == "DATA" || isAnnotated(psiClass, SpringCoreClasses.REPOSITORY)) {
            SpringBeanType.REPOSITORY
        } else if (isAnnotated(psiClass, SpringCoreClasses.SPRING_BOOT_APPLICATION)) {
            SpringBeanType.APPLICATION
        } else if (isAnnotated(psiClass, SpringCoreClasses.CONTROLLER) || isAnnotated(psiClass, REST_CONTROLLER)) {
            SpringBeanType.CONTROLLER
        } else if (isAnnotated(psiClass, SpringCoreClasses.SERVICE)) {
            SpringBeanType.SERVICE
        } else if (isAnnotated(psiClass, SpringCoreClasses.COMPONENT)) {
            SpringBeanType.COMPONENT
        } else if (isAnnotated(psiClass, SpringCoreClasses.CONFIGURATION_PROPERTIES)) {
            SpringBeanType.CONFIGURATION_PROPERTIES
        } else if (isAnnotated(psiClass, SpringCoreClasses.CONFIGURATION)) {
            SpringBeanType.CONFIGURATION
        } else {
            SpringBeanType.OTHER
        }
    }

    private fun isAnnotated(psiClass: PsiClass, annotationQualifiedName: String): Boolean {
        return AnnotationUtil.isAnnotated(psiClass, annotationQualifiedName, 0)
    }
}