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

package com.explyt.spring.core.externalsystem


import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringCoreClasses.REST_CONTROLLER
import com.explyt.spring.core.externalsystem.model.SpringAspectData
import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.externalsystem.model.SpringBeanType
import com.explyt.spring.core.externalsystem.model.SpringProfileData
import com.explyt.spring.core.externalsystem.process.AspectInfo
import com.explyt.spring.core.externalsystem.process.BeanInfo
import com.explyt.spring.core.externalsystem.process.ExplytCapturingProcessAdapter
import com.explyt.spring.core.externalsystem.setting.NativeExecutionSettings
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.explyt.spring.core.profile.SpringProfilesService
import com.explyt.spring.core.profile.SpringProfilesService.Companion.DEFAULT_PROFILE_LIST
import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
import com.explyt.spring.boot.bean.reader.SpringBootBeanEnhancerReaderStarter
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
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.isFile
import com.intellij.psi.PsiClass
import com.intellij.psi.util.InheritanceUtil
import com.intellij.serviceContainer.AlreadyDisposedException
import com.intellij.task.ProjectTaskManager
import com.intellij.util.PathUtil
import java.awt.BorderLayout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MINUTES
import javax.swing.JPanel

private const val SPRING_BOOT_2_4_CLASS = "org.springframework.boot.context.config.ConfigData"

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
        runConfiguration ?: nothingException(settings)
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

    private fun nothingException(settings: NativeExecutionSettings): Nothing {
        if (settings.runConfigurationName != null) {
            throw ExternalSystemException("No run configuration found by name: " + settings.runConfigurationName)
        }
        throw ExternalSystemException("No run configuration found by path: " + settings.externalProjectMainFilePath)
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

        buildProject(settings, modules)

        val clone = runConfiguration.clone() as SpringBootRunConfiguration
        clone.envs["explyt.spring.appClassName"] = ApplicationManager.getApplication()
            .runReadAction(Computable { mainClass.qualifiedName })

        clone.mainClassName = getMainClassName(modules, id, listener)
        clone.classpathModifications.add(getClasspathExplytModification())

        val processAdapter = ExplytCapturingProcessAdapter(id, listener)
        executeRunConfiguration(id, clone, processAdapter)

        val contextInfo = processAdapter.getSpringContextInfo()
        val beans = contextInfo.beans
        val aspects = contextInfo.aspects
        val aspectBeanInfoByName = getAspectBeanInfoMapByName(beans, aspects)

        val projectData = projectData(projectPath, runConfiguration)
        val projectDataNode = DataNode(ProjectKeys.PROJECT, projectData, null)
        beans.mapNotNull { toSpringBeanDataInReadAction(it, id, settings, listener) }
            .forEach { projectDataNode.createChild(SpringBeanData.KEY, it) }
        aspects.mapNotNull { toSpringAspectData(it, aspectBeanInfoByName) }
            .forEach { projectDataNode.createChild(SpringAspectData.KEY, it) }
        fillProfiles(projectDataNode, projectPath, settings.project, runConfiguration)
        return projectDataNode
    }

    private fun buildProject(
        settings: NativeExecutionSettings,
        modules: Array<out Module>
    ) {
        val buildPromise = ProjectTaskManager.getInstance(settings.project).build(*modules)
        val buildResult = buildPromise.blockingGet(10, MINUTES)
        if (buildResult == null || buildResult.hasErrors() || buildResult.isAborted) {
            throw ExternalSystemException(SpringCoreBundle.message("explyt.external.project.sync.build.error"))
        }
    }

    private fun executeRunConfiguration(
        id: ExternalSystemTaskId, clone: SpringBootRunConfiguration, processAdapter: ExplytCapturingProcessAdapter
    ) {
        val descriptor = getDescriptor()
        val environment = getEnvironment(id, clone, processAdapter, descriptor)
        try {
            ProgramRunnerUtil.executeConfiguration(environment, false, false)
            processAdapter.await()
            checkErrors(processAdapter)
        } finally {
            Disposer.dispose(environment)
            Disposer.dispose(descriptor)
        }
    }

    private fun getMainClassName(
        modules: Array<Module>,
        id: ExternalSystemTaskId,
        listener: ExternalSystemTaskNotificationListener
    ): String {
        if (!Registry.`is`("explyt.spring.native.old")) return SpringBootBeanReaderStarter::class.qualifiedName!!
        val isSpringBoot24 = ApplicationManager.getApplication()
            .runReadAction(Computable {
                modules.any { LibraryClassCache.searchForLibraryClass(it, SPRING_BOOT_2_4_CLASS) != null }
            }) ?: false
        return if (isSpringBoot24) {
            SpringBootBeanReaderStarter::class.qualifiedName!!
        } else {
            listener.onTaskOutput(id, "WARNING! Using Spring Boot Bean Enhancer for old versions before 2.4.0", true)
            listener.onTaskOutput(id, System.lineSeparator(), true)
            listener.onTaskOutput(id, System.lineSeparator(), true)
            SpringBootBeanEnhancerReaderStarter::class.qualifiedName!!
        }
    }

    private fun checkErrors(processAdapter: ExplytCapturingProcessAdapter) {
        if (!Registry.`is`("explyt.spring.native.old") && processAdapter.classNotFoundError) {
            throw ExternalSystemException(
                SYSTEM_ID.readableName + ": " + SpringCoreBundle.message("explyt.external.project.sync.old.error")
            )
        } else if (processAdapter.getSpringContextInfo().beans.isEmpty()) {
            throw ExternalSystemException(SpringCoreBundle.message("explyt.external.project.sync.empty.error"))
        }
    }

    private fun getAspectBeanInfoMapByName(beans: List<BeanInfo>, aspects: List<AspectInfo>): Map<String, BeanInfo> {
        if (aspects.isEmpty()) return emptyMap()
        val aspectsBeanNames = aspects.flatMapTo(mutableSetOf()) { setOf(it.aspectName, it.beanName) }
        return beans.filter { aspectsBeanNames.contains(it.beanName) }.associateBy { it.beanName }
    }

    private fun toSpringAspectData(
        aspect: AspectInfo, aspectBeanInfoByName: Map<String, BeanInfo>
    ): SpringAspectData? {
        val aspectBean = aspectBeanInfoByName[aspect.aspectName] ?: return null
        val wrappedBean = aspectBeanInfoByName[aspect.beanName] ?: return null
        return SpringAspectData(
            NativeBootUtils.toQualifiedClassName(aspectBean.className),
            aspect.aspectMethodName,
            NativeBootUtils.toQualifiedClassName(wrappedBean.methodType ?: wrappedBean.className),
            aspect.methodName,
            if (aspect.methodParams.isEmpty()) emptyList() else aspect.methodParams.split(",")
        )
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
        profiles.asSequence().filter { it.isNotEmpty() }
            .map { SpringProfileData(it, runConfiguration.name) }
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
        } else if (isAnnotated(psiClass, SpringCoreClasses.REPOSITORY)
            || InheritanceUtil.isInheritor(psiClass, "org.springframework.data.repository.Repository")
            || isAnnotated(psiClass, "org.springframework.data.repository.RepositoryDefinition")
        ) {
            SpringBeanType.REPOSITORY
        } else {
            SpringBeanType.OTHER
        }
    }

    private fun isAnnotated(psiClass: PsiClass, annotationQualifiedName: String): Boolean {
        return AnnotationUtil.isAnnotated(psiClass, annotationQualifiedName, 0)
    }
}