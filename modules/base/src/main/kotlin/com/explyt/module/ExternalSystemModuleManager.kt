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

package com.explyt.module

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.Function

@Service(Service.Level.PROJECT)
class ExternalSystemModuleManager(private val project: Project) {
    private val moduleManager by lazy { ModuleManager.getInstance(project) }

    private val myModuleListener = object : ModuleListener {
        override fun modulesAdded(project: Project, modules: List<Module>) = resetKeys()

        override fun beforeModuleRemoved(project: Project, module: Module) = resetKeys()

        override fun moduleRemoved(project: Project, module: Module) = resetKeys()

        override fun modulesRenamed(
            project: Project, modules: MutableList<out Module>, oldNameProvider: Function<in Module, String>
        ) = resetKeys()
    }

    init {
        project.messageBus.connect().subscribe(ModuleListener.TOPIC, myModuleListener)
    }

    fun getModules(): List<ExternalSystemModule> {
        val moduleGroupsByExternalProjectId: Map<String?, List<Module>> = moduleManager.modules.groupBy {
            ExternalSystemApiUtil.getExternalProjectId(it)?.removeSuffix(":test")?.removeSuffix(":main")
        }

        val result = mutableListOf<ExternalSystemModule>()

        moduleGroupsByExternalProjectId.forEach { (_, modules) ->
            val firstModule = modules.first()

            val externalSystemId = ExternalSystemModulePropertyManager.getInstance(firstModule).getExternalSystemId()
            if ("gradle".equals(externalSystemId, true)) {
                val gradleModule = buildGradleModule(modules)
                if (gradleModule != null) {
                    result.add(gradleModule)
                } else { // fallback
                    modules.mapTo(result) {
                        ExternalSystemModuleImpl(it)
                    }
                }
            }
            if ("maven".equals(externalSystemId, true)) {
                modules.mapTo(result) {
                    MavenModule(it)
                }
            } else {
                modules.mapTo(result) {
                    ExternalSystemModuleImpl(it)
                }
            }
        }


        for (hModule in result) {
            hModule.ideaModules.forEach {
                it.putUserData(MODULE_KEY, hModule)
            }
        }

        val moduleClasses = result.map { it.javaClass }.distinct()

        if (moduleClasses.size > 1) {
            log.warn("HModules of different types were created for project ${project.name}: ${moduleClasses.joinToString { it.simpleName }}")
        }

        return result
    }

    private fun buildGradleModule(modules: List<Module>): ExternalSystemModule? {
        if (modules.size > 3) {
            log.warn("More than 3 idea modules are found for gradle module: [${modules.joinToString { it.name }}]")
            return null
        }

        // root module has the shortest name
        val rootModule = modules.minByOrNull {
            it.name.length
        }!!

        val rootName = rootModule.name

        val mainModule = modules.firstOrNull {
            rootName in it.name && it.name.removePrefix(rootName).endsWith("main")
        }

        val testModule = modules.firstOrNull {
            rootName in it.name && it.name.removePrefix(rootName).endsWith("test")
        }

        return GradleModule(rootModule, mainModule, testModule)
    }

    fun getExternalSystemModuleModule(ideaModule: Module): ExternalSystemModule {
        ideaModule.getUserData(MODULE_KEY)?.let {
            return it
        }

        // init keys
        getModules()

        ideaModule.getUserData(MODULE_KEY)?.let {
            return it
        }

        log.warn("HModule was not created for module ${ideaModule.name} during modules initialization")

        return ExternalSystemModuleImpl(ideaModule)
    }

    private fun resetKeys() = moduleManager.modules.forEach {
        it.putUserData(MODULE_KEY, null)
    }

    companion object {
        fun getInstance(project: Project): ExternalSystemModuleManager = project.service()

        private val log = logger<ExternalSystemModuleManager>()

        private val MODULE_KEY = Key.create<ExternalSystemModule>("external_system_module")
    }
}