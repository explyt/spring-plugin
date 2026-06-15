/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.profile

import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.module.Module

interface ProfileSearcher {
    fun searchProfiles(module: Module): List<String>
    fun searchActiveProfiles(module: Module): List<String>

    companion object {
        val EP_NAME = ProjectExtensionPointName<ProfileSearcher>("com.explyt.spring.core.profileSearcher")
    }
}