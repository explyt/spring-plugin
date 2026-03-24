/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.integration.util

import com.explyt.base.LibraryClassCache
import com.explyt.spring.integration.SpringIntegrationClasses
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

object SpringIntegrationUtil {

    fun isSpringIntegrationProject(project: Project): Boolean {
        return LibraryClassCache.searchForLibraryClass(
            project,
            SpringIntegrationClasses.DIRECT_CHANNEL
        ) != null
    }

    fun isSpringIntegrationModule(module: Module): Boolean {
        return LibraryClassCache.searchForLibraryClass(
            module,
            SpringIntegrationClasses.DIRECT_CHANNEL
        ) != null
    }

}