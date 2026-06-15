/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.autoconfigure.language

import com.explyt.plugin.PluginIds
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.SpringProperties.META_INF
import com.explyt.spring.core.SpringProperties.SPRING_FACTORIES_FILE_NAME
import com.intellij.lang.properties.PropertiesLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile
import com.intellij.openapi.vfs.VirtualFile


object FactoriesFileType : LanguageFileType(PropertiesLanguage.INSTANCE, true), FileTypeIdentifiableByVirtualFile {
    override fun getName() = "Spring.factories Explyt"

    override fun getDescription() = "Spring factories Explyt"

    override fun getDefaultExtension() = ""

    override fun getIcon() = SpringIcons.SpringFactories

    override fun getDisplayName() = "Factories configuration properties"

    override fun isMyFileType(file: VirtualFile): Boolean {
        if (PluginIds.SPRING_BOOT_JB.isEnabled()) return false
        return SPRING_FACTORIES_FILE_NAME == file.name && file.parent?.name == META_INF
    }
}