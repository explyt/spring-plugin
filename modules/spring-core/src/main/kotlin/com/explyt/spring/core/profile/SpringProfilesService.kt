/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.profile

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
            .filter { !it.startsWith("\${") }
            .toSet()
            .sorted()
            .ifEmpty { DEFAULT_PROFILE_LIST }
    }

    companion object {
        val DEFAULT_PROFILE_LIST = listOf("default")
        fun getInstance(project: Project): SpringProfilesService = project.service()
    }
}