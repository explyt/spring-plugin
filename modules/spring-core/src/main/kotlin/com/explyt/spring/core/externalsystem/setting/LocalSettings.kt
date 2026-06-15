/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.setting

import com.explyt.spring.core.externalsystem.utils.Constants
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "SpringBootNativeLocalSettings", storages = [Storage(StoragePathMacros.CACHE_FILE)])
class LocalSettings(project: Project) : AbstractExternalSystemLocalSettings<LocalSettings.NativeLocalState>(
    Constants.SYSTEM_ID, project, NativeLocalState()
) {
    class NativeLocalState : State()
}