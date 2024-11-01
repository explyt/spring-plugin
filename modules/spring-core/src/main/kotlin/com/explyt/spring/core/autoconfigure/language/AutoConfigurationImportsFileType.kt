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

package com.explyt.spring.core.autoconfigure.language

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.SpringProperties
import com.intellij.lang.properties.PropertiesLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile
import com.intellij.openapi.vfs.VirtualFile

object AutoConfigurationImportsFileType : LanguageFileType(PropertiesLanguage.INSTANCE, true),
    FileTypeIdentifiableByVirtualFile {
    override fun getName() = SpringProperties.AUTOCONFIGURATION_IMPORTS

    override fun getDescription() = SpringProperties.AUTOCONFIGURATION_IMPORTS

    override fun getDefaultExtension() = ""

    override fun getIcon() = SpringIcons.SpringFactories

    override fun getDisplayName() = "Auto configuration imports properties"

    override fun isMyFileType(file: VirtualFile): Boolean {
        val parent = file.parent ?: return false
        return SpringProperties.AUTOCONFIGURATION_IMPORTS == file.name && parent.name == SpringProperties.SPRING
                && parent.parent?.name == SpringProperties.META_INF
    }
}