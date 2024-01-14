package com.esprito.spring.core.profile

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class SpringProfilesService(
    private val project: Project
) {
    fun loadActiveProfiles(module: Module): List<String> = runReadAction {
        val modules = mutableSetOf<Module>()
        ModuleUtilCore.getDependencies(module, modules)

        return@runReadAction ProfileSearcher.EP_NAME
            .getExtensions(project)
            .flatMap {
                modules.flatMap { module ->
                    it.searchActiveProfiles(module)
                }
            }
            .toSet()
            .sorted()
    }

    fun loadExistingProfiles(module: Module): List<String> = runReadAction {
        val modules = mutableSetOf<Module>()
        ModuleUtilCore.getDependencies(module, modules)

        return@runReadAction ProfileSearcher.EP_NAME
            .getExtensions(project)
            .flatMap {
                modules.flatMap { module ->
                    it.searchProfiles(module)
                }
            }
            .toSet()
            .sorted()
            .ifEmpty { listOf("default") }
    }

    companion object {
        fun getInstance(project: Project): SpringProfilesService = project.service()
    }
}